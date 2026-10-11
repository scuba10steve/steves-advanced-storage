package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.block.BlockTank;
import io.github.scuba10steve.s3.advanced.block.TankTier;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class TankBlockEntity extends BaseBlockEntity {

    public static final int RENDER_STEPS = 64;

    private final TankTier tier;
    private final FluidTank tank;
    @Nullable
    private Fluid lockedFluid;

    private Fluid lastSyncedFluid = Fluids.EMPTY;
    private int lastSyncedStep = -1;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);
        this.tier = ((BlockTank) state.getBlock()).getTier();
        this.tank = new FluidTank(tier.capacityMb(), this::isFluidAllowed) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                // FluidTank.fill() mishandles a tank already at/over capacity (e.g. after
                // loading over-capacity NBT): it computes a negative "filled" amount and,
                // as a side effect, truncates the stored fluid down to capacity. Refuse
                // the fill outright instead so over-capacity contents are preserved.
                if (!fluid.isEmpty() && fluid.getAmount() >= capacity) {
                    return 0;
                }
                return super.fill(resource, action);
            }

            @Override
            protected void onContentsChanged() {
                onTankChanged();
            }
        };
    }

    public TankTier getTier() {
        return tier;
    }

    public FluidTank getTank() {
        return tank;
    }

    @Nullable
    public Fluid getLockedFluid() {
        return lockedFluid;
    }

    public boolean isFluidAllowed(FluidStack stack) {
        return lockedFluid == null || stack.is(lockedFluid);
    }

    /** Locked → unlock. Unlocked with fluid → lock to it. Unlocked and empty → no-op. */
    public void toggleLock() {
        if (lockedFluid != null) {
            lockedFluid = null;
        } else if (!tank.isEmpty()) {
            lockedFluid = tank.getFluid().getFluid();
        } else {
            return;
        }
        setChanged();
        syncToClient(true);
    }

    protected void onTankChanged() {
        setChanged();
        syncToClient(false);
    }

    /** Sends a block update only when the fluid type, lock or render step changed (or when forced). */
    protected void syncToClient(boolean force) {
        if (level == null || level.isClientSide) {
            return;
        }
        Fluid fluid = tank.getFluid().getFluid();
        int step = renderStep(tank.getFluidAmount(), tank.getCapacity());
        if (force || fluid != lastSyncedFluid || step != lastSyncedStep) {
            lastSyncedFluid = fluid;
            lastSyncedStep = step;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static int renderStep(int amount, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        return (int) Math.min(RENDER_STEPS, (long) amount * RENDER_STEPS / capacity);
    }

    private void writeFluidState(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        if (lockedFluid != null) {
            tag.putString("LockedFluid", BuiltInRegistries.FLUID.getKey(lockedFluid).toString());
        }
    }

    private void readFluidState(CompoundTag tag, HolderLookup.Provider registries) {
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        lockedFluid = null;
        if (tag.contains("LockedFluid", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("LockedFluid"));
            Fluid fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
            lockedFluid = fluid == Fluids.EMPTY ? null : fluid;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeFluidState(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readFluidState(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeFluidState(tag, registries);
        return tag;
    }
}
