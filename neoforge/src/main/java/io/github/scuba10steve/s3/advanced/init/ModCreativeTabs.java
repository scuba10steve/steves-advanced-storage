package io.github.scuba10steve.s3.advanced.init;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StevesAdvancedStorage.MOD_ID);

    public static final Supplier<CreativeModeTab> S3_ADVANCED_TAB = CREATIVE_MODE_TABS.register("tab", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.s3_advanced.tab"))
            .icon(() -> new ItemStack(ModBlocks.ADVANCED_STORAGE_CORE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ADVANCED_STORAGE_CORE.get());
                output.accept(ModItems.ADVANCED_STORAGE_CORE_UPGRADE.get());
                output.accept(ModItems.SOLAR_GENERATOR.get());
                output.accept(ModItems.COAL_GENERATOR.get());
                output.accept(ModItems.RECIPE_MEMORY_BOX.get());
                output.accept(ModItems.AUTO_CRAFTER.get());
                output.accept(ModItems.MACHINE_INTERFACE.get());
                output.accept(ModItems.ADVANCED_STATISTICS.get());
                output.accept(ModItems.BLOCK_STORAGE_1.get());
            })
            .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
