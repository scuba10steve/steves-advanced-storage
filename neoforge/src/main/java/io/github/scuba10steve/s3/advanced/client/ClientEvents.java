package io.github.scuba10steve.s3.advanced.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.client.*;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageCraftingDisplayMenu;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageDisplayMenu;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.gui.screens.MenuScreens;

@EventBusSubscriber(modid = StevesAdvancedStorage.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ADVANCED_STORAGE_CORE.get(), AdvancedStorageCoreScreen::new);
        event.register(ModMenuTypes.SOLAR_GENERATOR.get(), SolarGeneratorScreen::new);
        event.register(ModMenuTypes.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
        event.register(ModMenuTypes.RECIPE_MEMORY_BOX.get(), RecipeMemoryBoxScreen::new);
        event.register(ModMenuTypes.RECIPE_PATTERN.get(), RecipePatternScreen::new);
        event.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        event.register(ModMenuTypes.MACHINE_INTERFACE.get(), MachineInterfaceScreen::new);
        event.register(ModMenuTypes.ADVANCED_STATISTICS.get(), AdvancedStatisticsScreen::new);
        registerDisplayScreen(event);
        registerCraftingDisplayScreen(event);
    }

    @SuppressWarnings("unchecked")
    private static void registerDisplayScreen(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ADVANCED_STORAGE_DISPLAY.get(),
            (MenuScreens.ScreenConstructor) (m, inv, title) ->
                new AdvancedStorageDisplayScreen((AdvancedStorageDisplayMenu) m, inv, title));
    }

    @SuppressWarnings("unchecked")
    private static void registerCraftingDisplayScreen(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ADVANCED_STORAGE_CRAFTING_DISPLAY.get(),
            (MenuScreens.ScreenConstructor) (m, inv, title) ->
                new AdvancedStorageCraftingDisplayScreen((AdvancedStorageCraftingDisplayMenu) m, inv, title));
    }
}
