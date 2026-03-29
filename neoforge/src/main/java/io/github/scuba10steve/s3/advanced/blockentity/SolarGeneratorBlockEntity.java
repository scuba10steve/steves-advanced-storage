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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SolarGeneratorBlockEntity extends BaseBlockEntity implements MenuProvider {

    private int currentRate;

    public final GeneratorEnergyStorage energyStorage;

    // ContainerData slots: [0-1] energy, [2-3] capacity, [4] currentRate
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> currentRate;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 4) {
                currentRate = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_GENERATOR.get(), pos, state);
        energyStorage = new GeneratorEnergyStorage(
            S3AdvancedConfig.SOLAR_CAPACITY.get(),
            S3AdvancedConfig.SOLAR_MAX_OUTPUT.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarGeneratorBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }

        int maxOutput = S3AdvancedConfig.SOLAR_MAX_OUTPUT.get();

        // Check sky light at the block above (the generator block itself is opaque)
        int skyLight = level.getBrightness(LightLayer.SKY, pos.above());
        if (skyLight > 0 && level.isDay()) {
            int rate = level.isRaining()
                ? S3AdvancedConfig.SOLAR_GENERATION_RAIN.get()
                : S3AdvancedConfig.SOLAR_GENERATION_RATE.get();
            if (be.energyStorage.generate(rate) > 0) {
                be.currentRate = rate;
                be.setChanged();
            } else {
                be.currentRate = 0; // buffer full
            }
        } else {
            be.currentRate = 0;
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.solar_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new io.github.scuba10steve.s3.advanced.gui.server.SolarGeneratorMenu(id, inv, this);
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
}
