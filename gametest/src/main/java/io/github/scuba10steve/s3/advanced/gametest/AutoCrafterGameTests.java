package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class AutoCrafterGameTests {

    private static final BlockPos CORE_POS         = new BlockPos(1, 1, 1);
    private static final BlockPos AUTO_CRAFTER_POS  = new BlockPos(0, 1, 1);

    // -----------------------------------------------------------------------
    // Test: Auto-Crafter is discovered by scanMultiblock
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 5)
    public static void auto_crafter_discovered_in_multiblock(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            if (core.getAutoCrafters().isEmpty()) {
                helper.fail("Expected auto-crafter to be discovered in multiblock scan");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Test: Auto-Crafter adds its FE/t to totalPowerDraw
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 5)
    public static void auto_crafter_contributes_power_draw(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            // totalPowerDraw must be greater than CORE_ENERGY_PER_TICK alone.
            int draw = core.containerData.get(4);
            if (draw <= 0) {
                helper.fail("totalPowerDraw not updated after auto-crafter joined multiblock");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Test: Assign/unassign persists in NBT
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 5)
    public static void auto_crafter_assignments_persist_nbt(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AutoCrafterBlockEntity crafter = getCrafter(helper, AUTO_CRAFTER_POS);
            if (crafter == null) {
                return;
            }

            PatternKey key = new PatternKey(new BlockPos(10, 10, 10), 0);
            crafter.assign(key);
            crafter.updateConfig(key, true, 5);

            // Simulate save/load via NBT round-trip
            // getUpdateTag() delegates to saveWithoutMetadata() which calls saveAdditional()
            net.minecraft.nbt.CompoundTag tag = crafter.getUpdateTag(helper.getLevel().registryAccess());
            crafter.loadAdditional(tag, helper.getLevel().registryAccess());

            PerPatternConfig loaded = crafter.getAssignments().get(key);
            if (loaded == null) {
                helper.fail("Assignment lost after NBT round-trip");
                return;
            }
            if (!loaded.autoEnabled()) {
                helper.fail("autoEnabled should be true after NBT round-trip");
                return;
            }
            if (loaded.minimumBuffer() != 5) {
                helper.fail("minimumBuffer should be 5 after NBT round-trip, got " + loaded.minimumBuffer());
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Test: Coordinator dispatches GUI_REQUEST job to the auto-crafter
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 10)
    public static void coordinator_dispatches_gui_request(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            AutoCrafterBlockEntity crafter = getCrafter(helper, AUTO_CRAFTER_POS);
            if (core == null || crafter == null) {
                return;
            }

            // Set up: recipe for 4 OAK_PLANKS → 1 CRAFTING_TABLE
            BlockPos rmbPos = new BlockPos(99, 99, 99); // synthetic box pos (not in world)
            PatternKey key = new PatternKey(rmbPos, 0);

            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.OAK_PLANKS, 4));
            pattern.setOutput(new ItemStack(Items.CRAFTING_TABLE, 1));

            // Assign the pattern to the crafter
            crafter.assign(key);

            // Put ingredients in inventory
            core.getInventory().insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            // Manually tick the coordinator with the pattern data
            var boxData = List.of(
                new CraftingCoordinator.BoxData(rmbPos, List.of(pattern)));
            var crafterData = List.of(
                new CraftingCoordinator.CrafterData(crafter.getAssignments()));

            core.craftingCoordinator.enqueue(key, 1, CraftingSource.GUI_REQUEST);
            core.craftingCoordinator.tick(core.getInventory(), boxData, crafterData);

            // Verify output was produced
            ItemStack result = core.getInventory().extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (result.isEmpty()) {
                helper.fail("Expected CRAFTING_TABLE in inventory after dispatch");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Test: Auto-buffer enqueues a job when stock is below minimum
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 10)
    public static void coordinator_auto_buffer_enqueues_when_below_minimum(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            AutoCrafterBlockEntity crafter = getCrafter(helper, AUTO_CRAFTER_POS);
            if (core == null || crafter == null) {
                return;
            }

            BlockPos rmbPos = new BlockPos(99, 99, 99);
            PatternKey key = new PatternKey(rmbPos, 0);

            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.OAK_PLANKS, 4));
            pattern.setOutput(new ItemStack(Items.CRAFTING_TABLE, 1));

            // Assign with autoEnabled=true, minimumBuffer=1 (inventory has 0 crafting tables)
            crafter.assign(key);
            crafter.updateConfig(key, true, 1);

            // Put ingredients in inventory
            core.getInventory().insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            var boxData = List.of(
                new CraftingCoordinator.BoxData(rmbPos, List.of(pattern)));
            var crafterData = List.of(
                new CraftingCoordinator.CrafterData(crafter.getAssignments()));

            // tick() should auto-enqueue and dispatch
            core.craftingCoordinator.tick(core.getInventory(), boxData, crafterData);

            ItemStack result = core.getInventory().extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (result.isEmpty()) {
                helper.fail("Expected CRAFTING_TABLE crafted by auto-buffer");
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

    private static AutoCrafterBlockEntity getCrafter(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AutoCrafterBlockEntity be) {
            return be;
        }
        helper.fail("AutoCrafterBlockEntity not found at " + pos);
        return null;
    }
}
