package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.BlockStorageBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.CoalGeneratorBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.ConfigBlockBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class BlockDropGameTests {

    private static final BlockPos RACK_POS = new BlockPos(2, 1, 1);
    private static final BlockPos FREE_POS = new BlockPos(0, 1, 1);
    private static final double DROP_RADIUS = 2.0;

    private static Item item(String namespace, String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, path)).orElseThrow();
    }

    /**
     * Breaks the block the way a player would, rolling its loot table.
     * {@code helper.destroyBlock} passes {@code dropBlock=false}, so it can't be used here.
     */
    private static void breakWithDrops(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void every_block_has_loot_table(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (var entry : ModBlocks.BLOCKS.getEntries()) {
            Block block = entry.get();
            LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(block.getLootTable());
            if (table == LootTable.EMPTY) {
                missing.add(entry.getId().toString());
            }
        }
        if (!missing.isEmpty()) {
            helper.fail("Blocks without a loot table: " + String.join(", ", missing));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void recipe_memory_box_drops_itself(GameTestHelper helper) {
        helper.setBlock(FREE_POS, ModBlocks.RECIPE_MEMORY_BOX.get());
        breakWithDrops(helper, FREE_POS);
        helper.assertItemEntityPresent(ModBlocks.RECIPE_MEMORY_BOX.get().asItem(), FREE_POS, DROP_RADIUS);
        helper.succeed();
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_drops_contents(GameTestHelper helper) {
        if (!(helper.getBlockEntity(RACK_POS) instanceof BlockStorageBlockEntity rack)) {
            helper.fail("BlockStorageBlockEntity not found at " + RACK_POS);
            return;
        }
        Item storageBox = item("s3", "storage_box");
        rack.handler.insertItem(0, new ItemStack(storageBox), false);

        breakWithDrops(helper, RACK_POS);

        helper.assertItemEntityPresent(ModBlocks.BLOCK_STORAGE_1.get().asItem(), RACK_POS, DROP_RADIUS);
        helper.assertItemEntityPresent(storageBox, RACK_POS, DROP_RADIUS);
        helper.succeed();
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void config_block_drops_contents(GameTestHelper helper) {
        helper.setBlock(FREE_POS, ModBlocks.BLOCK_CONFIG_BLOCK.get());
        if (!(helper.getBlockEntity(FREE_POS) instanceof ConfigBlockBlockEntity config)) {
            helper.fail("ConfigBlockBlockEntity not found at " + FREE_POS);
            return;
        }
        Item craftingBox = item("s3", "crafting_box");
        config.handler.insertItem(0, new ItemStack(craftingBox), false);

        breakWithDrops(helper, FREE_POS);

        helper.assertItemEntityPresent(ModBlocks.BLOCK_CONFIG_BLOCK.get().asItem(), FREE_POS, DROP_RADIUS);
        helper.assertItemEntityPresent(craftingBox, FREE_POS, DROP_RADIUS);
        helper.succeed();
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void coal_generator_drops_fuel(GameTestHelper helper) {
        helper.setBlock(FREE_POS, ModBlocks.COAL_GENERATOR.get());
        if (!(helper.getBlockEntity(FREE_POS) instanceof CoalGeneratorBlockEntity generator)) {
            helper.fail("CoalGeneratorBlockEntity not found at " + FREE_POS);
            return;
        }
        generator.fuelHandler.insertItem(0, new ItemStack(Items.COAL, 8), false);

        breakWithDrops(helper, FREE_POS);

        helper.assertItemEntityPresent(ModBlocks.COAL_GENERATOR.get().asItem(), FREE_POS, DROP_RADIUS);
        helper.assertItemEntityPresent(Items.COAL, FREE_POS, DROP_RADIUS);
        helper.succeed();
    }
}
