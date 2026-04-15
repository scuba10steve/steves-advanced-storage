package io.github.scuba10steve.s3.advanced.init;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.block.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(BuiltInRegistries.BLOCK, StevesAdvancedStorage.MOD_ID);

    public static final Supplier<Block> ADVANCED_STORAGE_CORE =
        BLOCKS.register("advanced_storage_core", BlockAdvancedStorageCore::new);

    public static final Supplier<Block> SOLAR_GENERATOR =
        BLOCKS.register("solar_generator", BlockSolarGenerator::new);

    public static final Supplier<Block> COAL_GENERATOR =
        BLOCKS.register("coal_generator", BlockCoalGenerator::new);

    public static final Supplier<Block> RECIPE_MEMORY_BOX =
        BLOCKS.register("recipe_memory_box", BlockRecipeMemoryBox::new);

    public static final Supplier<Block> AUTO_CRAFTER =
        BLOCKS.register("auto_crafter", BlockAutoCrafter::new);

    public static final Supplier<Block> MACHINE_INTERFACE =
        BLOCKS.register("machine_interface", BlockMachineInterface::new);

    public static final Supplier<Block> ADVANCED_STATISTICS =
        BLOCKS.register("advanced_statistics", BlockAdvancedStatistics::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
