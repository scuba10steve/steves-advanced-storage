package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.BlockStorageBlockEntity;
import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class BlockStorageGameTests {

    private static final BlockPos CORE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos RACK_POS = new BlockPos(2, 1, 1);
    private static final ResourceLocation STORAGE_BOX_ID = ResourceLocation.fromNamespaceAndPath("s3", "storage_box");

    private static AdvancedStorageCoreBlockEntity getCore(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity be) return be;
        helper.fail("AdvancedStorageCoreBlockEntity not found at " + pos);
        return null;
    }

    private static BlockStorageBlockEntity getRack(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof BlockStorageBlockEntity be) return be;
        helper.fail("BlockStorageBlockEntity not found at " + pos);
        return null;
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_capacity_increases_with_inserted_storage_box(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            BlockStorageBlockEntity rack = getRack(helper, RACK_POS);
            if (core == null || rack == null) return;

            long capacityBefore = core.getInventory().getMaxItems();

            Item storageBoxItem = BuiltInRegistries.ITEM.getOptional(STORAGE_BOX_ID).orElseThrow();
            rack.handler.insertItem(0, new ItemStack(storageBoxItem), false);

            long capacityAfter = core.getInventory().getMaxItems();
            if (capacityAfter != capacityBefore + 10000) {
                helper.fail("Expected capacity " + (capacityBefore + 10000) + " after inserting s3:storage_box, got " + capacityAfter);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_power_draw_increases_per_occupied_slot(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            BlockStorageBlockEntity rack = getRack(helper, RACK_POS);
            if (core == null || rack == null) return;

            int powerBefore = core.containerData.get(4) | (core.containerData.get(5) << 16);
            int expectedPerSlot = S3AdvancedConfig.BLOCK_STORAGE_1_SLOT_ENERGY_PER_TICK.get();

            Item storageBoxItem = BuiltInRegistries.ITEM.getOptional(STORAGE_BOX_ID).orElseThrow();
            rack.handler.insertItem(0, new ItemStack(storageBoxItem), false);

            int powerAfter = core.containerData.get(4) | (core.containerData.get(5) << 16);
            if (powerAfter != powerBefore + expectedPerSlot) {
                helper.fail("Expected totalPowerDraw " + (powerBefore + expectedPerSlot) + " after 1 occupied slot, got " + powerAfter);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_is_item_valid_rejects_advanced_block_storage(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            BlockStorageBlockEntity rack = getRack(helper, RACK_POS);
            if (rack == null) return;

            ItemStack advancedRackItem = new ItemStack(ModItems.BLOCK_STORAGE_1.get());
            if (rack.handler.isItemValid(0, advancedRackItem)) {
                helper.fail("isItemValid should reject s3_advanced:block_storage_1 — advanced rack must not be inserted into itself");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_is_item_valid_accepts_base_storage_box(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            BlockStorageBlockEntity rack = getRack(helper, RACK_POS);
            if (rack == null) return;

            Item storageBoxItem = BuiltInRegistries.ITEM.getOptional(STORAGE_BOX_ID).orElseThrow();
            if (!rack.handler.isItemValid(0, new ItemStack(storageBoxItem))) {
                helper.fail("isItemValid should accept s3:storage_box in the block storage rack");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_nbt_roundtrip(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            BlockStorageBlockEntity rack = getRack(helper, RACK_POS);
            if (rack == null) return;

            Item storageBoxItem = BuiltInRegistries.ITEM.getOptional(STORAGE_BOX_ID).orElseThrow();
            ItemStack original = new ItemStack(storageBoxItem);
            rack.handler.insertItem(0, original.copy(), false);

            CompoundTag tag = rack.getUpdateTag(helper.getLevel().registryAccess());
            rack.loadAdditional(tag, helper.getLevel().registryAccess());

            ItemStack loaded = rack.handler.getStackInSlot(0);
            if (loaded.isEmpty()) {
                helper.fail("Slot 0 should contain s3:storage_box after NBT roundtrip, was empty");
                return;
            }
            if (!ItemStack.isSameItem(loaded, original)) {
                helper.fail("Slot 0 item mismatch after NBT roundtrip: expected s3:storage_box, got " + loaded);
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_block_storage_1", setupTicks = 5)
    public static void block_storage_1_removed_from_multiblock_on_break(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            BlockStorageBlockEntity rack = getRack(helper, RACK_POS);
            if (core == null || rack == null) return;

            Item storageBoxItem = BuiltInRegistries.ITEM.getOptional(STORAGE_BOX_ID).orElseThrow();
            rack.handler.insertItem(0, new ItemStack(storageBoxItem), false);

            long capacityWithRack = core.getInventory().getMaxItems();
            if (capacityWithRack <= 0) {
                helper.fail("Precondition failed: expected capacity > 0 after inserting storage box, got " + capacityWithRack);
                return;
            }

            helper.setBlock(RACK_POS, Blocks.AIR.defaultBlockState());

            helper.runAfterDelay(1, () -> {
                long capacityAfterBreak = core.getInventory().getMaxItems();
                if (capacityAfterBreak != 0) {
                    helper.fail("Expected capacity 0 after rack removed, got " + capacityAfterBreak);
                    return;
                }
                helper.succeed();
            });
        });
    }
}
