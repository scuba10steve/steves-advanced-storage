package io.github.scuba10steve.s3.advanced.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.client.*;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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
    }
}
