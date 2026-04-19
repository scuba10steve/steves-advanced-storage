package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import io.github.scuba10steve.s3.blockentity.StorageCoreBlockEntity;
import io.github.scuba10steve.s3.gui.server.StorageCoreCraftingMenu;
import io.github.scuba10steve.s3.storage.StorageInventory;
import io.github.scuba10steve.s3.storage.StoredItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class AdvancedStorageGameTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedStorageGameTests.class);
    private static final BlockPos CORE_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void core_with_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 10000);
    }

    @GameTest(template = "core_with_condensed_storage_box", setupTicks = 5)
    public static void core_with_condensed_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 40000);
    }

    @GameTest(template = "core_with_compressed_storage_box", setupTicks = 5)
    public static void core_with_compressed_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 80000);
    }

    @GameTest(template = "core_with_super_storage_box", setupTicks = 5)
    public static void core_with_super_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 160000);
    }

    @GameTest(template = "core_with_ultra_storage_box", setupTicks = 5)
    public static void core_with_ultra_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 640000);
    }

    @GameTest(template = "core_with_hyper_storage_box", setupTicks = 5)
    public static void core_with_hyper_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 2560000);
    }

    @GameTest(template = "core_with_ultimate_storage_box", setupTicks = 5)
    public static void core_with_ultimate_storage_box(GameTestHelper helper) {
        assertMultiblockFormsAndStores(helper, 10240000);
    }

    @GameTest(template = "core_with_hyper_storage_box", setupTicks = 5, timeoutTicks = 200)
    public static void hyper_storage_extract_insert_performance(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();

            // Fill with every registered item type (bypassing core to skip sync overhead during setup)
            Item lastItem = null;
            int distinctTypes = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item, 64);
                if (!stack.isEmpty()) {
                    inv.insertItem(stack);
                    lastItem = item;
                    distinctTypes++;
                }
            }

            // Fill remaining capacity with stone
            long remaining = inv.getMaxItems() - inv.getTotalItemCount();
            if (remaining > 0) {
                inv.insertItem(new ItemStack(Items.STONE, (int) Math.min(remaining, Integer.MAX_VALUE)));
            }

            LOGGER.info("Performance test: filled inventory with {} distinct item types, total count: {}/{}",
                distinctTypes, inv.getTotalItemCount(), inv.getMaxItems());

            if (lastItem == null) {
                helper.fail("No items were inserted");
                return;
            }

            // Time extract of the last item (worst case — end of list)
            ItemStack template = new ItemStack(lastItem, 1);

            long extractStart = System.nanoTime();
            ItemStack extracted = core.extractItem(template, 1);
            long extractNanos = System.nanoTime() - extractStart;

            if (extracted.isEmpty()) {
                helper.fail("Failed to extract " + lastItem);
                return;
            }

            // Time insert of the same item back
            long insertStart = System.nanoTime();
            core.insertItem(extracted);
            long insertNanos = System.nanoTime() - insertStart;

            long extractMicros = extractNanos / 1000;
            long insertMicros = insertNanos / 1000;

            LOGGER.info("Performance test results ({} distinct types):", distinctTypes);
            LOGGER.info("  Extract: {} us", extractMicros);
            LOGGER.info("  Insert:  {} us", insertMicros);

            if (extractNanos >= 1_000_000) {
                helper.fail("Extract took " + extractMicros + " us, exceeds 1ms threshold");
                return;
            }
            if (insertNanos >= 1_000_000) {
                helper.fail("Insert took " + insertMicros + " us, exceeds 1ms threshold");
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "large_ultimate_multiblock", setupTicks = 20, timeoutTicks = 400)
    public static void large_ultimate_extract_insert_performance(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(5, 1, 5);

        helper.runAfterDelay(20, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, corePos);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();

            // Fill with 10x of every registered item type
            Item lastItem = null;
            int distinctTypes = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item, 640);
                if (!stack.isEmpty()) {
                    inv.insertItem(stack);
                    lastItem = item;
                    distinctTypes++;
                }
            }

            // Fill remaining capacity split between cobblestone and oak wood
            long remaining = inv.getMaxItems() - inv.getTotalItemCount();
            long halfRemaining = remaining / 2;

            bulkInsert(inv, Items.COBBLESTONE, halfRemaining);
            bulkInsert(inv, Items.OAK_LOG, inv.getMaxItems() - inv.getTotalItemCount());

            LOGGER.info("Large perf test: {} distinct types, total count: {}/{}",
                distinctTypes, inv.getTotalItemCount(), inv.getMaxItems());

            if (lastItem == null) {
                helper.fail("No items were inserted");
                return;
            }

            // Time extract of the last item (worst case)
            ItemStack template = new ItemStack(lastItem, 1);

            long extractStart = System.nanoTime();
            ItemStack extracted = core.extractItem(template, 1);
            long extractNanos = System.nanoTime() - extractStart;

            if (extracted.isEmpty()) {
                helper.fail("Failed to extract " + lastItem);
                return;
            }

            // Time insert of the same item back
            long insertStart = System.nanoTime();
            core.insertItem(extracted);
            long insertNanos = System.nanoTime() - insertStart;

            long extractMicros = extractNanos / 1000;
            long insertMicros = insertNanos / 1000;

            LOGGER.info("Large perf test results ({} distinct types, {} total items):", distinctTypes, inv.getTotalItemCount());
            LOGGER.info("  Extract: {} us", extractMicros);
            LOGGER.info("  Insert:  {} us", insertMicros);

            if (extractNanos >= 1_000_000) {
                helper.fail("Extract took " + extractMicros + " us, exceeds 1ms threshold");
                return;
            }
            if (insertNanos >= 1_000_000) {
                helper.fail("Insert took " + insertMicros + " us, exceeds 1ms threshold");
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void advanced_blocks_registered(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            String[] expectedBlocks = {
                "advanced_storage_core", "solar_generator", "coal_generator", "block_storage_1"
            };

            java.util.List<String> missing = new java.util.ArrayList<>();
            for (String name : expectedBlocks) {
                net.minecraft.resources.ResourceLocation id =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("s3_advanced", name);
                if (BuiltInRegistries.BLOCK.getOptional(id).isEmpty()) {
                    missing.add(name);
                }
            }

            if (!missing.isEmpty()) {
                helper.fail("Missing " + missing.size() + " s3_advanced blocks: " + String.join(", ", missing));
                return;
            }

            LOGGER.info("All {} s3_advanced blocks verified in block registry", expectedBlocks.length);
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void advanced_core_container_data_has_7_slots(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;
            int count = core.containerData.getCount();
            if (count != 7) {
                helper.fail("Expected containerData.getCount() == 7, got " + count);
                return;
            }
            LOGGER.info("advanced_core_container_data_has_7_slots: PASSED");
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void advanced_core_total_power_draw_split_encoded_correctly(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) return;
            int low  = core.containerData.get(4);
            int high = core.containerData.get(5);
            int reconstructed = (high << 16) | (low & 0xFFFF);
            int actual = core.getTotalPowerDraw();
            if (reconstructed != actual) {
                helper.fail("Split-encoded totalPowerDraw mismatch: slot4=" + low
                    + " slot5=" + high + " → " + reconstructed
                    + " but getTotalPowerDraw()=" + actual);
                return;
            }
            LOGGER.info("advanced_core_total_power_draw_split_encoded_correctly: PASSED (totalPowerDraw={})", actual);
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void insert_when_full_returns_original_stack(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();

            // Fill storage to capacity (10000 stone)
            bulkInsert(inv, Items.STONE, inv.getMaxItems());

            long countBefore = inv.getTotalItemCount();
            if (countBefore != 10000) {
                helper.fail("Expected storage full at 10000, got " + countBefore);
                return;
            }

            // Attempt to insert 1 diamond into full storage
            ItemStack remainder = inv.insertItem(new ItemStack(Items.DIAMOND, 1));

            if (remainder.getCount() != 1) {
                helper.fail("Expected remainder count 1, got " + remainder.getCount());
                return;
            }

            if (inv.getTotalItemCount() != countBefore) {
                helper.fail("Total item count changed from " + countBefore + " to " + inv.getTotalItemCount());
                return;
            }

            LOGGER.info("insert_when_full_returns_original_stack: PASSED");
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5, timeoutTicks = 40)
    public static void core_placement_triggers_multiblock_scan(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            // Verify initial capacity from template
            long initialCapacity = core.getInventory().getMaxItems();
            if (initialCapacity != 10000) {
                helper.fail("Expected initial capacity 10000, got " + initialCapacity);
                return;
            }

            // Remove the core block
            helper.destroyBlock(CORE_POS);

            // Re-place the advanced core (triggers onPlace -> attemptMultiblock -> scanMultiblock)
            helper.setBlock(CORE_POS, ModBlocks.ADVANCED_STORAGE_CORE.get().defaultBlockState());

            // Wait a few ticks for the multiblock scan to complete
            helper.runAfterDelay(10, () -> {
                AdvancedStorageCoreBlockEntity newCore = getAdvancedCore(helper, CORE_POS);
                if (newCore == null) {
                    return;
                }

                long restoredCapacity = newCore.getInventory().getMaxItems();
                if (restoredCapacity != 10000) {
                    helper.fail("Expected capacity 10000 after re-placement, got " + restoredCapacity);
                    return;
                }

                LOGGER.info("core_placement_triggers_multiblock_scan: PASSED");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void stored_items_preserve_insertion_order(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();

            // Insert items in a specific order
            Item[] insertionOrder = { Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT, Items.EMERALD };
            for (Item item : insertionOrder) {
                inv.insertItem(new ItemStack(item, 1));
            }

            // Retrieve stored items and verify order matches insertion order
            List<StoredItemStack> storedItems = inv.getStoredItems();
            if (storedItems.size() != insertionOrder.length) {
                helper.fail("Expected " + insertionOrder.length + " stored item types, got " + storedItems.size());
                return;
            }

            for (int i = 0; i < insertionOrder.length; i++) {
                Item expected = insertionOrder[i];
                Item actual = storedItems.get(i).getItemStack().getItem();
                if (!expected.equals(actual)) {
                    helper.fail("Order mismatch at index " + i + ": expected " + expected + ", got " + actual);
                    return;
                }
            }

            LOGGER.info("stored_items_preserve_insertion_order: PASSED");
            helper.succeed();
        });
    }

    /**
     * Verifies that extracting items changes getTotalItemCount() even when getStoredItems().size()
     * stays the same (regression test for #24).
     */
    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void extract_changes_total_count_not_item_types(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();

            // Insert 64 stone — one item type
            inv.insertItem(new ItemStack(Items.STONE, 64));

            int typesBefore = inv.getStoredItems().size();
            long countBefore = inv.getTotalItemCount();

            if (typesBefore != 1) {
                helper.fail("Expected 1 item type before extract, got " + typesBefore);
                return;
            }
            if (countBefore != 64) {
                helper.fail("Expected 64 total count before extract, got " + countBefore);
                return;
            }

            // Extract 1 stone — same number of types, different total count
            inv.extractItem(new ItemStack(Items.STONE), 1);

            int typesAfter = inv.getStoredItems().size();
            long countAfter = inv.getTotalItemCount();

            if (typesAfter != typesBefore) {
                helper.fail("Item type count should stay the same after partial extract, was " + typesBefore + " now " + typesAfter);
                return;
            }
            if (countAfter == countBefore) {
                helper.fail("Total item count should change after extract, still " + countAfter);
                return;
            }
            if (countAfter != 63) {
                helper.fail("Expected 63 total count after extract, got " + countAfter);
                return;
            }

            LOGGER.info("extract_changes_total_count_not_item_types: PASSED (types={}, count {} -> {})",
                typesBefore, countBefore, countAfter);
            helper.succeed();
        });
    }

    /**
     * Verifies that StorageCoreCraftingMenu.removed() clears the crafting grid on the server
     * and returns items to storage (regression test for #25).
     */
    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void crafting_menu_removed_clears_grid_server_side(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();
            long countBefore = inv.getTotalItemCount();

            // Create a crafting menu using a mock server player (server-side, isClientSide=false)
            BlockPos absPos = helper.absolutePos(CORE_POS);
            ServerPlayer mockPlayer = helper.makeMockServerPlayerInLevel();
            StorageCoreCraftingMenu menu = new StorageCoreCraftingMenu(0, mockPlayer.getInventory(), absPos);

            // Place items directly in the crafting grid (slots 1-9)
            menu.getSlot(1).set(new ItemStack(Items.DIAMOND, 1));
            menu.getSlot(2).set(new ItemStack(Items.GOLD_INGOT, 1));
            menu.getSlot(3).set(new ItemStack(Items.IRON_INGOT, 1));

            // Verify grid has items
            if (menu.getSlot(1).getItem().isEmpty()) {
                helper.fail("Crafting grid slot 1 should have diamond");
                return;
            }

            // Call removed() — on the server, this should clear the grid and return items to storage
            menu.removed(mockPlayer);

            // Verify grid is now empty
            for (int i = 1; i <= 9; i++) {
                if (!menu.getSlot(i).getItem().isEmpty()) {
                    helper.fail("Crafting grid slot " + i + " should be empty after removed(), has " + menu.getSlot(i).getItem());
                    return;
                }
            }

            // Verify items were returned to storage
            long countAfter = inv.getTotalItemCount();
            long expectedCount = countBefore + 3; // 3 items placed in grid
            if (countAfter != expectedCount) {
                helper.fail("Expected " + expectedCount + " items in storage after grid clear, got " + countAfter);
                return;
            }

            LOGGER.info("crafting_menu_removed_clears_grid_server_side: PASSED");
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

    private static void bulkInsert(StorageInventory inv, Item item, long amount) {
        while (amount > 0) {
            int batch = (int) Math.min(amount, Integer.MAX_VALUE);
            inv.insertItem(new ItemStack(item, batch));
            amount -= batch;
        }
    }

    private static void assertMultiblockFormsAndStores(GameTestHelper helper, long expectedCapacity) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            StorageInventory inv = core.getInventory();

            if (inv.getMaxItems() != expectedCapacity) {
                helper.fail("Expected capacity " + expectedCapacity + ", got " + inv.getMaxItems());
                return;
            }

            core.insertItem(new ItemStack(Items.DIAMOND, 64));

            long totalCount = inv.getTotalItemCount();
            if (totalCount != 64) {
                helper.fail("Expected 64 total items, got " + totalCount);
                return;
            }

            helper.succeed();
        });
    }
}
