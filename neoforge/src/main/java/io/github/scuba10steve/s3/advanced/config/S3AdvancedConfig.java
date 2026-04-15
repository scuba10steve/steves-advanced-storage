package io.github.scuba10steve.s3.advanced.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class S3AdvancedConfig {

    public static final ModConfigSpec SPEC;

    // Advanced Storage Core
    public static final ModConfigSpec.IntValue CORE_CAPACITY;
    public static final ModConfigSpec.IntValue CORE_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue RECIPE_MEMORY_BOX_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue AUTO_CRAFTER_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue MACHINE_INTERFACE_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue MACHINE_INTERFACE_TICK_INTERVAL;
    public static final ModConfigSpec.IntValue ADVANCED_STATISTICS_ENERGY_PER_TICK;

    // Solar Generator
    public static final ModConfigSpec.IntValue SOLAR_CAPACITY;
    public static final ModConfigSpec.IntValue SOLAR_GENERATION_RATE;
    public static final ModConfigSpec.IntValue SOLAR_GENERATION_RAIN;
    public static final ModConfigSpec.IntValue SOLAR_MAX_OUTPUT;

    // Coal Generator
    public static final ModConfigSpec.IntValue COAL_CAPACITY;
    public static final ModConfigSpec.IntValue COAL_GENERATION_RATE;
    public static final ModConfigSpec.IntValue COAL_MAX_OUTPUT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Advanced Storage Core").push("advanced_storage_core");
        CORE_CAPACITY = builder
            .comment("FE buffer capacity")
            .defineInRange("capacity", 100_000, 1, Integer.MAX_VALUE);
        CORE_ENERGY_PER_TICK = builder
            .comment("FE consumed per tick while active")
            .defineInRange("energy_per_tick", 80, 0, Integer.MAX_VALUE);
        RECIPE_MEMORY_BOX_ENERGY_PER_TICK = builder
            .comment("FE consumed per tick per Recipe Memory Box in the multiblock")
            .defineInRange("recipe_memory_box_energy_per_tick", 10, 0, Integer.MAX_VALUE);
        AUTO_CRAFTER_ENERGY_PER_TICK = builder
            .comment("FE consumed per tick per Auto-Crafter in the multiblock")
            .defineInRange("auto_crafter_energy_per_tick", 20, 0, Integer.MAX_VALUE);
        MACHINE_INTERFACE_ENERGY_PER_TICK = builder
            .comment("FE consumed per tick per Machine Interface in the multiblock")
            .defineInRange("machine_interface_energy_per_tick", 15, 0, Integer.MAX_VALUE);
        MACHINE_INTERFACE_TICK_INTERVAL = builder
            .comment("Default ticks between each Machine Interface push/pull cycle")
            .defineInRange("machine_interface_tick_interval", 20, 1, Integer.MAX_VALUE);
        ADVANCED_STATISTICS_ENERGY_PER_TICK = builder
            .comment("FE consumed per tick per Advanced Statistics Block in the multiblock")
            .defineInRange("advanced_statistics_energy_per_tick", 5, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Solar Generator").push("solar_generator");
        SOLAR_CAPACITY = builder
            .comment("FE buffer capacity")
            .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
        SOLAR_GENERATION_RATE = builder
            .comment("FE generated per tick during clear day")
            .defineInRange("generation_rate", 30, 0, Integer.MAX_VALUE);
        SOLAR_GENERATION_RAIN = builder
            .comment("FE generated per tick during rain/storm")
            .defineInRange("generation_rain", 15, 0, Integer.MAX_VALUE);
        SOLAR_MAX_OUTPUT = builder
            .comment("Max FE pushed to adjacent blocks per tick")
            .defineInRange("max_output", 60, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Coal Generator").push("coal_generator");
        COAL_CAPACITY = builder
            .comment("FE buffer capacity")
            .defineInRange("capacity", 50_000, 1, Integer.MAX_VALUE);
        COAL_GENERATION_RATE = builder
            .comment("FE generated per tick while burning")
            .defineInRange("generation_rate", 160, 0, Integer.MAX_VALUE);
        COAL_MAX_OUTPUT = builder
            .comment("Max FE pushed to adjacent blocks per tick")
            .defineInRange("max_output", 200, 1, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }
}
