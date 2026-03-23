package io.github.scuba10steve.s3.advanced;

import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import io.github.scuba10steve.s3.advanced.init.ModCreativeTabs;
import io.github.scuba10steve.s3.advanced.init.ModItems;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(StevesAdvancedStorage.MOD_ID)
public class StevesAdvancedStorage {

    public static final String MOD_ID = "s3_advanced";

    public StevesAdvancedStorage(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, S3AdvancedConfig.SPEC, "s3_advanced-server.toml");

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ADVANCED_STORAGE_CORE.get(),
            (be, side) -> be.energyStorage);

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.SOLAR_GENERATOR.get(),
            (be, side) -> be.energyStorage);

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.COAL_GENERATOR.get(),
            (be, side) -> be.energyStorage);

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.COAL_GENERATOR.get(),
            (be, side) -> be.fuelHandler);
    }
}
