package io.github.scuba10steve.s3.advanced.init;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.CoalGeneratorBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.SolarGeneratorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, StevesAdvancedStorage.MOD_ID);

    public static final Supplier<BlockEntityType<AdvancedStorageCoreBlockEntity>> ADVANCED_STORAGE_CORE =
        BLOCK_ENTITIES.register("advanced_storage_core", () ->
            BlockEntityType.Builder.of(AdvancedStorageCoreBlockEntity::new, ModBlocks.ADVANCED_STORAGE_CORE.get()).build(null));

    public static final Supplier<BlockEntityType<SolarGeneratorBlockEntity>> SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("solar_generator", () ->
            BlockEntityType.Builder.of(SolarGeneratorBlockEntity::new, ModBlocks.SOLAR_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR =
        BLOCK_ENTITIES.register("coal_generator", () ->
            BlockEntityType.Builder.of(CoalGeneratorBlockEntity::new, ModBlocks.COAL_GENERATOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
