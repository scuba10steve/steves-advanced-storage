package io.github.scuba10steve.s3.advanced.init;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.item.ItemAdvancedStorageCoreUpgrade;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(BuiltInRegistries.ITEM, StevesAdvancedStorage.MOD_ID);

    public static final Supplier<Item> ADVANCED_STORAGE_CORE = ITEMS.register("advanced_storage_core",
        () -> new BlockItem(ModBlocks.ADVANCED_STORAGE_CORE.get(), new Item.Properties()));

    public static final Supplier<Item> SOLAR_GENERATOR = ITEMS.register("solar_generator",
        () -> new BlockItem(ModBlocks.SOLAR_GENERATOR.get(), new Item.Properties()));

    public static final Supplier<Item> COAL_GENERATOR = ITEMS.register("coal_generator",
        () -> new BlockItem(ModBlocks.COAL_GENERATOR.get(), new Item.Properties()));

    public static final Supplier<Item> ADVANCED_STORAGE_CORE_UPGRADE = ITEMS.register("advanced_storage_core_upgrade",
        ItemAdvancedStorageCoreUpgrade::new);

    public static final Supplier<Item> RECIPE_MEMORY_BOX = ITEMS.register("recipe_memory_box",
        () -> new BlockItem(ModBlocks.RECIPE_MEMORY_BOX.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
