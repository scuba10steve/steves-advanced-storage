package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.CrafterSlot;
import io.github.scuba10steve.s3.advanced.crafting.CraftingSource;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Collections;
import java.util.Map;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class StorageGuiCraftingGameTests {

    // Template: core_with_rmb_and_crafter — Core(1,1,1), RMB(2,1,1), Crafter(3,1,1)
    private static final BlockPos CORE_POS    = new BlockPos(1, 1, 1);
    private static final BlockPos RMB_POS     = new BlockPos(2, 1, 1);
    private static final BlockPos CRAFTER_POS = new BlockPos(3, 1, 1);

    // -----------------------------------------------------------------------
    // Test: GUI craft request enqueues a job and produces the output item
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_rmb_and_crafter", setupTicks = 10)
    public static void gui_craft_request_enqueues_and_executes(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper);
            RecipeMemoryBoxBlockEntity rmb = getRmb(helper, RMB_POS);
            AutoCrafterBlockEntity crafter = getCrafter(helper, CRAFTER_POS);
            if (core == null || rmb == null || crafter == null) {
                return;
            }

            core.getInventory().setMaxItems(1000L);

            // Pattern: OAK_PLANKS in grid slot 0 → CRAFTING_TABLE
            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.OAK_PLANKS, 4));
            pattern.setOutput(new ItemStack(Items.CRAFTING_TABLE));
            rmb.setPattern(0, pattern);

            // Put ingredients in storage
            core.getInventory().insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            // Enqueue using the real crafter's absolute position
            CrafterSlot slot = new CrafterSlot(helper.absolutePos(CRAFTER_POS), 0);
            core.craftingCoordinator.enqueue(slot, 1, CraftingSource.GUI_REQUEST);
            core.craftingCoordinator.tick(core.getInventory(), core.getRmbToCrafter());

            ItemStack result = core.getInventory().extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (result.isEmpty()) {
                helper.fail("Expected CRAFTING_TABLE in storage after GUI craft request");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private static AdvancedStorageCoreBlockEntity getCore(GameTestHelper helper) {
        if (helper.getBlockEntity(CORE_POS) instanceof AdvancedStorageCoreBlockEntity be) {
            return be;
        }
        helper.fail("AdvancedStorageCoreBlockEntity not found at " + CORE_POS);
        return null;
    }

    private static RecipeMemoryBoxBlockEntity getRmb(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof RecipeMemoryBoxBlockEntity be) {
            return be;
        }
        helper.fail("RecipeMemoryBoxBlockEntity not found at " + pos);
        return null;
    }

    private static AutoCrafterBlockEntity getCrafter(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AutoCrafterBlockEntity be) {
            return be;
        }
        helper.fail("AutoCrafterBlockEntity not found at " + pos);
        return null;
    }
}
