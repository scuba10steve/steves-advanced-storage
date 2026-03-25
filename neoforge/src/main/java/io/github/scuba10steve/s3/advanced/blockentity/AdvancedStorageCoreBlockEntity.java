package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.StorageCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import io.github.scuba10steve.s3.advanced.crafting.CraftingCoordinator;
import io.github.scuba10steve.s3.advanced.crafting.CraftingEngine;
import net.neoforged.neoforge.energy.EnergyStorage;

public class AdvancedStorageCoreBlockEntity extends StorageCoreBlockEntity {

    public final InternalEnergyStorage energyStorage;
    public final CraftingEngine craftingEngine = new CraftingEngine();
    public final CraftingCoordinator craftingCoordinator = new CraftingCoordinator(craftingEngine);
    // Field initializer avoids a zero-value window before the constructor body runs.
    private int totalPowerDraw = S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();

    // ContainerData slots: [0-1] energy, [2-3] capacity, [4] energyPerTick, [5] isPowered
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> totalPowerDraw;
                case 5 -> isPowered() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { /* read-only from server */ }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public AdvancedStorageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_STORAGE_CORE.get(), pos, state);
        energyStorage = new InternalEnergyStorage(
            S3AdvancedConfig.CORE_CAPACITY.get(),
            1_000);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.advanced_storage_core");
    }

    public boolean isPowered() {
        return energyStorage.getEnergyStored() >= totalPowerDraw;
    }

    @Override
    public void scanMultiblock() {
        super.scanMultiblock();
        // Reset totalPowerDraw to base cost. Future issues (#6 Block Storage, #11 Auto-Crafter,
        // #12 Machine Interface) will add their per-block FE/t here.
        totalPowerDraw = S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();
    }

    @Override
    public void tick() {
        super.tick(); // handles multiblock scan
        if (level == null || level.isClientSide) return;
        if (energyStorage.consume(totalPowerDraw)) {
            setChanged();
            craftingCoordinator.tick(getInventory());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
    }

    public static class InternalEnergyStorage extends EnergyStorage {

        InternalEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        public boolean consume(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            return true;
        }

        public void setEnergy(int amount) {
            this.energy = Math.min(amount, capacity);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }
    }
}
