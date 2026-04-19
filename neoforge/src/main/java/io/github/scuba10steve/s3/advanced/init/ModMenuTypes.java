package io.github.scuba10steve.s3.advanced.init;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.server.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(BuiltInRegistries.MENU, StevesAdvancedStorage.MOD_ID);

    public static final Supplier<MenuType<AdvancedStorageCoreMenu>> ADVANCED_STORAGE_CORE =
        MENU_TYPES.register("advanced_storage_core", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new AdvancedStorageCoreMenu(windowId, inv, data)));

    public static final Supplier<MenuType<SolarGeneratorMenu>> SOLAR_GENERATOR =
        MENU_TYPES.register("solar_generator", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new SolarGeneratorMenu(windowId, inv, data)));

    public static final Supplier<MenuType<CoalGeneratorMenu>> COAL_GENERATOR =
        MENU_TYPES.register("coal_generator", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new CoalGeneratorMenu(windowId, inv, data)));

    public static final Supplier<MenuType<RecipeMemoryBoxMenu>> RECIPE_MEMORY_BOX =
        MENU_TYPES.register("recipe_memory_box", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new RecipeMemoryBoxMenu(windowId, inv, data)));

    public static final Supplier<MenuType<RecipePatternMenu>> RECIPE_PATTERN =
        MENU_TYPES.register("recipe_pattern", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new RecipePatternMenu(windowId, inv, data)));

    public static final Supplier<MenuType<AutoCrafterMenu>> AUTO_CRAFTER =
        MENU_TYPES.register("auto_crafter", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new AutoCrafterMenu(windowId, inv, data)));

    public static final Supplier<MenuType<MachineInterfaceMenu>> MACHINE_INTERFACE =
        MENU_TYPES.register("machine_interface", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new MachineInterfaceMenu(windowId, inv, data)));

    public static final Supplier<MenuType<AdvancedStorageDisplayMenu>> ADVANCED_STORAGE_DISPLAY =
        MENU_TYPES.register("advanced_storage_display", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new AdvancedStorageDisplayMenu(windowId, inv, data)));

    public static final Supplier<MenuType<AdvancedStorageCraftingDisplayMenu>> ADVANCED_STORAGE_CRAFTING_DISPLAY =
        MENU_TYPES.register("advanced_storage_crafting_display", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new AdvancedStorageCraftingDisplayMenu(windowId, inv, data)));

    public static final Supplier<MenuType<AdvancedStatisticsMenu>> ADVANCED_STATISTICS =
        MENU_TYPES.register("advanced_statistics", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new AdvancedStatisticsMenu(windowId, inv, data)));

    public static final Supplier<MenuType<BlockStorageMenu>> BLOCK_STORAGE =
        MENU_TYPES.register("block_storage", () ->
            IMenuTypeExtension.create((windowId, inv, data) ->
                new BlockStorageMenu(windowId, inv, data)));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
