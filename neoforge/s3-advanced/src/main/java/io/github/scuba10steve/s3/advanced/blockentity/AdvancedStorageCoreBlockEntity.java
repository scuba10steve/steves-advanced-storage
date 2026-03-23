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
import net.neoforged.neoforge.energy.EnergyStorage;

public class AdvancedStorageCoreBlockEntity extends StorageCoreBlockEntity {

    public final InternalEnergyStorage energyStorage;

    // ContainerData slots: [0-1] energy, [2-3] capacity, [4] energyPerTick, [5] isPowered
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();
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
        super(pos, state);
        energyStorage = new InternalEnergyStorage(
            S3AdvancedConfig.CORE_CAPACITY.get(),
            1_000);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.advanced_storage_core");
    }

    public boolean isPowered() {
        return energyStorage.getEnergyStored() >= S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();
    }

    @Override
    public void tick() {
        super.tick(); // handles multiblock scan
        if (level == null || level.isClientSide) return;
        if (energyStorage.consume(S3AdvancedConfig.CORE_ENERGY_PER_TICK.get())) {
            setChanged();
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
