package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CoalGeneratorBlockEntity extends BaseBlockEntity implements MenuProvider {

    // Defaults — actual values come from S3AdvancedConfig at runtime
    public static final int DEFAULT_CAPACITY = 50_000;
    public static final int DEFAULT_GENERATION_RATE = 160;
    public static final int DEFAULT_MAX_OUTPUT = 200;

    public int litTime = 0;
    public int litDuration = 0;

    public final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getBurnTime(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public final GeneratorEnergyStorage energyStorage;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> S3AdvancedConfig.COAL_GENERATION_RATE.get();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR.get(), pos, state);
        energyStorage = new GeneratorEnergyStorage(
            S3AdvancedConfig.COAL_CAPACITY.get(),
            S3AdvancedConfig.COAL_MAX_OUTPUT.get());
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item == Items.COAL || item == Items.CHARCOAL) return 1600;
        if (item == Items.COAL_BLOCK) return 16000;
        return 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CoalGeneratorBlockEntity be) {
        if (level.isClientSide()) return;

        boolean wasBurning = be.litTime > 0;

        int generationRate = S3AdvancedConfig.COAL_GENERATION_RATE.get();
        int capacity = S3AdvancedConfig.COAL_CAPACITY.get();
        int maxOutput = S3AdvancedConfig.COAL_MAX_OUTPUT.get();

        if (be.litTime > 0) {
            be.litTime--;
            be.energyStorage.generate(generationRate);
        }

        // Try to light new fuel when extinguished and buffer not full
        if (be.litTime == 0 && be.energyStorage.getEnergyStored() < capacity) {
            ItemStack fuel = be.fuelHandler.getStackInSlot(0);
            int burnTime = getBurnTime(fuel);
            if (burnTime > 0) {
                be.litTime = burnTime;
                be.litDuration = burnTime;
                be.fuelHandler.extractItem(0, 1, false);
            }
        }

        if (wasBurning != (be.litTime > 0)) {
            be.setChanged();
        }

        // Push energy to adjacent blocks
        if (be.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neighbour = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbour != null && neighbour.canReceive()) {
                    int toSend = Math.min(be.energyStorage.getEnergyStored(), maxOutput);
                    int sent = neighbour.receiveEnergy(toSend, false);
                    if (sent > 0) {
                        be.energyStorage.extractEnergy(sent, false);
                        be.setChanged();
                    }
                }
            }
        }
    }

    public boolean isLit() {
        return litTime > 0;
    }

    /** Reconstruct energy stored from two 16-bit ContainerData slots. */
    public static int decodeEnergy(ContainerData data) {
        return (data.get(3) << 16) | (data.get(2) & 0xFFFF);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.coal_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new io.github.scuba10steve.s3.advanced.gui.server.CoalGeneratorMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("litTime", litTime);
        tag.putInt("litDuration", litDuration);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("fuel", fuelHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        litTime = tag.getInt("litTime");
        litDuration = tag.getInt("litDuration");
        energyStorage.setEnergy(tag.getInt("energy"));
        fuelHandler.deserializeNBT(registries, tag.getCompound("fuel"));
    }
}
