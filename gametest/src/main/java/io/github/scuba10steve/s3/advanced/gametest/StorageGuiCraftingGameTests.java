package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class StorageGuiCraftingGameTests {

    private static final BlockPos CORE_POS = new BlockPos(1, 1, 1);

    // -----------------------------------------------------------------------
    // Test: GUI craft request enqueues a job and produces the output item
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void gui_craft_request_enqueues_and_executes(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper);
            if (core == null) {
                return;
            }

            // Put ingredients in storage
            core.getInventory().insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            // Pattern: 4 OAK_PLANKS in slot 0 → CRAFTING_TABLE
            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.OAK_PLANKS, 4));
            pattern.setOutput(new ItemStack(Items.CRAFTING_TABLE));

            BlockPos syntheticBoxPos = new BlockPos(99, 99, 99);
            PatternKey key = new PatternKey(syntheticBoxPos, 0);

            List<CraftingCoordinator.BoxData> boxData = List.of(
                new CraftingCoordinator.BoxData(syntheticBoxPos, List.of(pattern)));

            Map<PatternKey, PerPatternConfig> assignments = new HashMap<>();
            assignments.put(key, PerPatternConfig.DEFAULT);
            List<CraftingCoordinator.CrafterData> crafterData = List.of(
                new CraftingCoordinator.CrafterData(assignments));

            core.craftingCoordinator.enqueue(key, 1, CraftingSource.GUI_REQUEST);
            core.craftingCoordinator.tick(core.getInventory(), boxData, crafterData);

            ItemStack result = core.getInventory().extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (result.isEmpty()) {
                helper.fail("Expected CRAFTING_TABLE in storage after GUI craft request");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------
    private static AdvancedStorageCoreBlockEntity getCore(GameTestHelper helper) {
        if (helper.getBlockEntity(CORE_POS) instanceof AdvancedStorageCoreBlockEntity be) {
            return be;
        }
        helper.fail("AdvancedStorageCoreBlockEntity not found at " + CORE_POS);
        return null;
    }
}
