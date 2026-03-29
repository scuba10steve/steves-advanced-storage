package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class RecipeMemoryBoxGameTests {

    private static final BlockPos CORE_POS = new BlockPos(1, 1, 1);

    /**
     * Places a Recipe Memory Box adjacent to the core and verifies the core's
     * scanMultiblock() discovers it and adds it to getRecipeMemoryBoxes().
     */
    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void recipe_memory_box_discovered_in_multiblock(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            // Place a Recipe Memory Box adjacent to the core (east side)
            BlockPos rmbPos = CORE_POS.east();
            helper.setBlock(rmbPos, ModBlocks.RECIPE_MEMORY_BOX.get().defaultBlockState());

            // Wait for the onPlace scan to complete (1 tick)
            helper.runAfterDelay(1, () -> {
                int count = core.getRecipeMemoryBoxes().size();
                if (count != 1) {
                    helper.fail("Expected 1 RecipeMemoryBox in multiblock after placement, got " + count);
                    return;
                }
                // Verify power draw increased by RECIPE_MEMORY_BOX_ENERGY_PER_TICK
                int basePower = io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();
                int rmbPower = io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig.RECIPE_MEMORY_BOX_ENERGY_PER_TICK.get();
                int expectedPower = basePower + rmbPower;
                // Access totalPowerDraw via containerData slot 4 (synced to client)
                int actualPower = core.containerData.get(4);
                if (actualPower != expectedPower) {
                    helper.fail("Expected totalPowerDraw=" + expectedPower + " after RMB placement, got " + actualPower);
                    return;
                }
                helper.succeed();
            });
        });
    }

    /**
     * Verifies that RecipePattern NBT save/load round-trips correctly.
     * Uses the core block entity's registry access for ItemStack serialization.
     */
    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void recipe_pattern_nbt_roundtrip(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            // Build a pattern with some ingredients and an output
            RecipePattern original = new RecipePattern();
            original.setIngredient(0, new ItemStack(Items.OAK_PLANKS));
            original.setIngredient(4, new ItemStack(Items.STICK));
            original.setOutput(new ItemStack(Items.WOODEN_SWORD));
            original.setPinnedRecipeId(net.minecraft.resources.ResourceLocation.parse("minecraft:wooden_sword"));

            var registries = core.getLevel().registryAccess();
            var tag = original.save(registries);
            RecipePattern loaded = RecipePattern.load(tag, registries);

            if (!ItemStack.isSameItemSameComponents(loaded.getIngredient(0), original.getIngredient(0))) {
                helper.fail("Slot 0 ingredient mismatch after NBT roundtrip");
                return;
            }
            if (!ItemStack.isSameItemSameComponents(loaded.getIngredient(4), original.getIngredient(4))) {
                helper.fail("Slot 4 ingredient mismatch after NBT roundtrip");
                return;
            }
            if (!ItemStack.isSameItemSameComponents(loaded.getOutput(), original.getOutput())) {
                helper.fail("Output mismatch after NBT roundtrip");
                return;
            }
            if (!loaded.getPinnedRecipeId().equals(original.getPinnedRecipeId())) {
                helper.fail("PinnedRecipeId mismatch after NBT roundtrip");
                return;
            }
            // Slots that were not set should be empty
            if (!loaded.getIngredient(1).isEmpty()) {
                helper.fail("Slot 1 should be empty after roundtrip");
                return;
            }
            helper.succeed();
        });
    }

    /**
     * Places a Recipe Memory Box, sets a pattern on its BE, breaks the box,
     * re-scans, and verifies the core's list is now empty.
     */
    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void recipe_memory_box_removed_from_multiblock_on_break(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getAdvancedCore(helper, CORE_POS);
            if (core == null) {
                return;
            }

            BlockPos rmbPos = CORE_POS.east();
            helper.setBlock(rmbPos, ModBlocks.RECIPE_MEMORY_BOX.get().defaultBlockState());

            helper.runAfterDelay(1, () -> {
                // Confirm it was added
                if (core.getRecipeMemoryBoxes().size() != 1) {
                    helper.fail("Expected 1 RMB before removal, got " + core.getRecipeMemoryBoxes().size());
                    return;
                }

                // Break the block (replace with air)
                helper.setBlock(rmbPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

                helper.runAfterDelay(1, () -> {
                    int count = core.getRecipeMemoryBoxes().size();
                    if (count != 0) {
                        helper.fail("Expected 0 RMBs after removal, got " + count);
                        return;
                    }
                    helper.succeed();
                });
            });
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
