package io.github.scuba10steve.s3.advanced.init;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageCoreMenu;
import io.github.scuba10steve.s3.advanced.gui.server.CoalGeneratorMenu;
import io.github.scuba10steve.s3.advanced.gui.server.RecipeMemoryBoxMenu;
import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import io.github.scuba10steve.s3.advanced.gui.server.SolarGeneratorMenu;
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

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
