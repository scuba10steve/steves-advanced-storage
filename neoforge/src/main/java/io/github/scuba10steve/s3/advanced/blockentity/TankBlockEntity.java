package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.block.BlockTank;
import io.github.scuba10steve.s3.advanced.block.TankTier;
import io.github.scuba10steve.s3.advanced.gui.server.TankMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.advanced.init.ModDataComponents;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class TankBlockEntity extends BaseBlockEntity implements MenuProvider {

    public static final int RENDER_STEPS = 64;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final TankTier tier;
    private final FluidTank tank;
    @Nullable
    private Fluid lockedFluid;

    private Fluid lastSyncedFluid = Fluids.EMPTY;
    private int lastSyncedStep = -1;

    public final SimpleContainer bucketSlots = new SimpleContainer(2);
    private boolean processing;

    // ContainerData: [0-1] amount (low/high 16 bits), [2-3] capacity (low/high 16 bits)
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.getFluidAmount() & 0xFFFF;
                case 1 -> (tank.getFluidAmount() >>> 16) & 0xFFFF;
                case 2 -> tank.getCapacity() & 0xFFFF;
                case 3 -> (tank.getCapacity() >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

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
        bucketSlots.addListener(container -> {
            setChanged();
            processBucketSlots();
        });
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced." + tier.getSerializedName() + "_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TankMenu(id, inventory, this);
    }

    protected void onTankChanged() {
        setChanged();
        syncToClient(false);
        processBucketSlots();
    }

    /**
     * Moves one container from the input slot through the tank into the output slot.
     * Simulates first and only executes when the result fits in the output slot,
     * so a blocked output never changes the tank.
     */
    public void processBucketSlots() {
        if (processing || level == null || level.isClientSide) {
            return;
        }
        ItemStack input = bucketSlots.getItem(SLOT_INPUT);
        if (input.isEmpty()) {
            return;
        }
        processing = true;
        try {
            ItemStack single = input.copyWithCount(1);
            boolean emptying = true;
            FluidActionResult simulated = FluidUtil.tryEmptyContainer(single, tank, Integer.MAX_VALUE, null, false);
            if (!simulated.isSuccess()) {
                emptying = false;
                simulated = FluidUtil.tryFillContainer(single, tank, Integer.MAX_VALUE, null, false);
            }
            if (!simulated.isSuccess() || !canAcceptOutput(simulated.getResult())) {
                return;
            }
            FluidActionResult executed = emptying
                ? FluidUtil.tryEmptyContainer(single, tank, Integer.MAX_VALUE, null, true)
                : FluidUtil.tryFillContainer(single, tank, Integer.MAX_VALUE, null, true);
            if (!executed.isSuccess()) {
                return;
            }
            ItemStack remainingInput = input.copy();
            remainingInput.shrink(1);
            bucketSlots.setItem(SLOT_INPUT, remainingInput);
            ItemStack result = executed.getResult();
            if (!result.isEmpty()) {
                ItemStack output = bucketSlots.getItem(SLOT_OUTPUT);
                if (output.isEmpty()) {
                    bucketSlots.setItem(SLOT_OUTPUT, result.copy());
                } else {
                    ItemStack grown = output.copy();
                    grown.grow(result.getCount());
                    bucketSlots.setItem(SLOT_OUTPUT, grown);
                }
            }
        } finally {
            processing = false;
        }
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        ItemStack output = bucketSlots.getItem(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
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
        NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
        items.set(SLOT_INPUT, bucketSlots.getItem(SLOT_INPUT));
        items.set(SLOT_OUTPUT, bucketSlots.getItem(SLOT_OUTPUT));
        CompoundTag buckets = new CompoundTag();
        ContainerHelper.saveAllItems(buckets, items, registries);
        tag.put("Buckets", buckets);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readFluidState(tag, registries);
        if (tag.contains("Buckets")) {
            NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Buckets"), items, registries);
            processing = true; // don't process while restoring saved slots
            try {
                bucketSlots.setItem(SLOT_INPUT, items.get(SLOT_INPUT));
                bucketSlots.setItem(SLOT_OUTPUT, items.get(SLOT_OUTPUT));
            } finally {
                processing = false;
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        processBucketSlots();
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

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        // Only write non-default state so empty, unlocked tanks stack with freshly crafted ones.
        if (!tank.isEmpty()) {
            components.set(ModDataComponents.TANK_FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
        }
        if (lockedFluid != null) {
            components.set(ModDataComponents.LOCKED_FLUID.get(), BuiltInRegistries.FLUID.wrapAsHolder(lockedFluid));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        tank.setFluid(input.getOrDefault(ModDataComponents.TANK_FLUID.get(), SimpleFluidContent.EMPTY).copy());
        Holder<Fluid> locked = input.get(ModDataComponents.LOCKED_FLUID.get());
        lockedFluid = (locked == null || locked.value() == Fluids.EMPTY) ? null : locked.value();
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Tank");
        tag.remove("LockedFluid");
    }
}
