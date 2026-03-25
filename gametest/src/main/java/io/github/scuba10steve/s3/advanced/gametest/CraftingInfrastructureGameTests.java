package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.CraftingSource;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.storage.StorageInventory;
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
public class CraftingInfrastructureGameTests {

    private static final BlockPos CORE_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void crafting_engine_succeeds_with_sufficient_ingredients(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;

            StorageInventory inv = core.getInventory();
            inv.insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            List<ItemStack> ingredients = List.of(new ItemStack(Items.OAK_PLANKS, 4));
            ItemStack output = new ItemStack(Items.CRAFTING_TABLE, 1);
            boolean result = core.craftingEngine.execute(ingredients, output, inv);

            if (!result) {
                helper.fail("execute() returned false with sufficient ingredients");
                return;
            }
            ItemStack extracted = inv.extractItem(new ItemStack(Items.CRAFTING_TABLE), 1);
            if (extracted.isEmpty()) {
                helper.fail("Crafting table not found in storage after successful execute");
                return;
            }
            if (inv.getTotalItemCount() != 0) {
                helper.fail("Expected 0 planks remaining, got " + inv.getTotalItemCount());
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void crafting_engine_fails_and_rolls_back_with_insufficient_ingredients(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;

            StorageInventory inv = core.getInventory();
            inv.insertItem(new ItemStack(Items.OAK_PLANKS, 2)); // need 4

            List<ItemStack> ingredients = List.of(new ItemStack(Items.OAK_PLANKS, 4));
            ItemStack output = new ItemStack(Items.CRAFTING_TABLE, 1);
            boolean result = core.craftingEngine.execute(ingredients, output, inv);

            if (result) {
                helper.fail("execute() returned true with insufficient ingredients");
                return;
            }
            long count = inv.getTotalItemCount();
            if (count != 2) {
                helper.fail("Expected 2 planks after rollback, got " + count);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void crafting_engine_rolls_back_partial_extractions(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;

            StorageInventory inv = core.getInventory();
            inv.insertItem(new ItemStack(Items.OAK_PLANKS, 4));

            List<ItemStack> ingredients = List.of(
                new ItemStack(Items.OAK_PLANKS, 4),
                new ItemStack(Items.STICK, 2)
            );
            ItemStack output = new ItemStack(Items.WOODEN_AXE, 1);
            boolean result = core.craftingEngine.execute(ingredients, output, inv);

            if (result) {
                helper.fail("execute() returned true when sticks were unavailable");
                return;
            }
            long count = inv.getTotalItemCount();
            if (count != 4) {
                helper.fail("Expected 4 planks restored after rollback, got " + count);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void crafting_coordinator_auto_stock_deduplicated(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;

            PatternKey key = new PatternKey(new BlockPos(5, 5, 5), 0);

            core.craftingCoordinator.enqueue(key, 1, CraftingSource.AUTO_STOCK);
            core.craftingCoordinator.enqueue(key, 1, CraftingSource.AUTO_STOCK); // duplicate

            int size = core.craftingCoordinator.getQueueSize();
            if (size != 1) {
                helper.fail("Expected 1 job after AUTO_STOCK deduplication, got " + size);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void crafting_coordinator_gui_request_not_deduplicated(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;

            PatternKey key = new PatternKey(new BlockPos(5, 5, 5), 0);

            core.craftingCoordinator.enqueue(key, 1, CraftingSource.AUTO_STOCK);
            core.craftingCoordinator.enqueue(key, 1, CraftingSource.GUI_REQUEST); // not deduplicated

            int size = core.craftingCoordinator.getQueueSize();
            if (size != 2) {
                helper.fail("Expected 2 jobs (AUTO_STOCK + GUI_REQUEST), got " + size);
                return;
            }
            helper.succeed();
        });
    }

    private static AdvancedStorageCoreBlockEntity getAdvancedCore(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity be) {
            return be;
        }
        helper.fail("AdvancedStorageCoreBlockEntity not found at " + pos);
        return null;
    }
}
