package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.CrafterSlot;
import io.github.scuba10steve.s3.advanced.crafting.CraftingSource;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class PairingGameTests {

    // Template: core_with_rmb_and_crafter
    // Template positions: Core(1,0,1), RMB(2,0,1) FACING=east, AutoCrafter(3,0,1).
    // Game test helper adds 1 to Y (floor is Y=0 in template, Y=1 in test coords).
    private static final BlockPos CORE_POS    = new BlockPos(1, 1, 1);
    private static final BlockPos RMB_POS     = new BlockPos(2, 1, 1);
    private static final BlockPos CRAFTER_POS = new BlockPos(3, 1, 1);

    // Capacity needed for insertItem() to succeed (default maxItems=0 blocks inserts).
    private static final long TEST_CAPACITY = 1000L;

    @GameTest(template = "core_with_rmb_and_crafter", setupTicks = 10)
    public static void rmb_paired_to_crafter_via_facing(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            AutoCrafterBlockEntity crafter = getCrafter(helper, CRAFTER_POS);
            if (crafter == null) {
                return;
            }

            RecipeMemoryBoxBlockEntity pairedRmb = core.getRmbForCrafter(crafter);
            if (pairedRmb == null) {
                helper.fail("Expected RMB at " + RMB_POS + " to be paired to crafter at " + CRAFTER_POS
                    + "; rmbToCrafter map: " + core.getRmbToCrafter());
                return;
            }
            if (!pairedRmb.getBlockPos().equals(helper.absolutePos(RMB_POS))) {
                helper.fail("Paired RMB pos mismatch: expected " + helper.absolutePos(RMB_POS)
                    + " got " + pairedRmb.getBlockPos());
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_rmb_and_crafter", setupTicks = 10)
    public static void crafting_executes_via_paired_rmb(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            RecipeMemoryBoxBlockEntity rmb = getRmb(helper, RMB_POS);
            AutoCrafterBlockEntity crafter = getCrafter(helper, CRAFTER_POS);
            if (core == null || rmb == null || crafter == null) {
                return;
            }

            // Ensure inventory has capacity
            core.getInventory().setMaxItems(TEST_CAPACITY);

            // Set up: 4 OAK_PLANKS (one per grid slot) → 1 CRAFTING_TABLE in RMB slot 0.
            // Slots 0,1,3,4 of the 3x3 grid form the 2x2 pattern.
            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setIngredient(1, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setIngredient(3, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setIngredient(4, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setOutput(new ItemStack(Items.CRAFTING_TABLE, 1));
            rmb.setPattern(0, pattern);

            // Put ingredients in storage
            core.getInventory().insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            // Enqueue a GUI_REQUEST and tick
            CrafterSlot slot = new CrafterSlot(helper.absolutePos(CRAFTER_POS), 0);
            core.craftingCoordinator.enqueue(slot, 1, CraftingSource.GUI_REQUEST);
            core.craftingCoordinator.tick(core.getInventory(), core.getRmbToCrafter());

            ItemStack result = core.getInventory().extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (result.isEmpty()) {
                helper.fail("Expected CRAFTING_TABLE in inventory after face-based craft dispatch");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_rmb_and_crafter", setupTicks = 10)
    public static void auto_buffer_triggers_via_paired_rmb(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            RecipeMemoryBoxBlockEntity rmb = getRmb(helper, RMB_POS);
            AutoCrafterBlockEntity crafter = getCrafter(helper, CRAFTER_POS);
            if (core == null || rmb == null || crafter == null) {
                return;
            }

            // Ensure inventory has capacity
            core.getInventory().setMaxItems(TEST_CAPACITY);

            // Set up: 4 OAK_PLANKS → 1 CRAFTING_TABLE
            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setIngredient(1, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setIngredient(3, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setIngredient(4, new ItemStack(Items.OAK_PLANKS, 1));
            pattern.setOutput(new ItemStack(Items.CRAFTING_TABLE, 1));
            rmb.setPattern(0, pattern);

            // autoEnabled=true, minimumBuffer=1; inventory has 0 crafting tables → auto-buffer triggers
            crafter.setConfig(0, new PerPatternConfig(true, 1));
            core.getInventory().insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            // tick() auto-enqueues because stock(0) < minimumBuffer(1), then dispatches
            core.craftingCoordinator.tick(core.getInventory(), core.getRmbToCrafter());

            ItemStack result = core.getInventory().extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (result.isEmpty()) {
                helper.fail("Expected CRAFTING_TABLE crafted by auto-buffer via paired RMB");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private static AdvancedStorageCoreBlockEntity getCore(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity be) {
            return be;
        }
        helper.fail("AdvancedStorageCoreBlockEntity not found at " + pos);
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
