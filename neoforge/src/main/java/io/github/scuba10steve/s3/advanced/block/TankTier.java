package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public enum TankTier implements StringRepresentable {
    BASIC("basic", () -> S3AdvancedConfig.BASIC_TANK_CAPACITY_MB.get(),
        () -> S3AdvancedConfig.BASIC_TANK_ENERGY_PER_TICK.get()),
    ADVANCED("advanced", () -> S3AdvancedConfig.ADVANCED_TANK_CAPACITY_MB.get(),
        () -> S3AdvancedConfig.ADVANCED_TANK_ENERGY_PER_TICK.get()),
    SUPER("super", () -> S3AdvancedConfig.SUPER_TANK_CAPACITY_MB.get(),
        () -> S3AdvancedConfig.SUPER_TANK_ENERGY_PER_TICK.get()),
    HYPER("hyper", () -> S3AdvancedConfig.HYPER_TANK_CAPACITY_MB.get(),
        () -> S3AdvancedConfig.HYPER_TANK_ENERGY_PER_TICK.get());

    private final String name;
    private final Supplier<Integer> capacityMb;
    private final Supplier<Integer> energyPerTick;

    TankTier(String name, Supplier<Integer> capacityMb, Supplier<Integer> energyPerTick) {
        this.name = name;
        this.capacityMb = capacityMb;
        this.energyPerTick = energyPerTick;
    }

    public int capacityMb() {
        return capacityMb.get();
    }

    public int energyPerTick() {
        return energyPerTick.get();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
