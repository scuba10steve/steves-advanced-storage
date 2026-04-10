package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class AutoCrafterGameTests {

    // core_with_auto_crafter template: Core at (1,0,1), AutoCrafter at (0,0,1).
    // Game test helper adds 1 to Y (floor is Y=0 in template, Y=1 in test coords).
    private static final BlockPos CORE_POS         = new BlockPos(1, 1, 1);
    private static final BlockPos AUTO_CRAFTER_POS = new BlockPos(0, 1, 1);

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

            // totalPowerDraw must be greater than zero (core base draw + auto-crafter draw).
            int draw = core.containerData.get(4);
            if (draw <= 0) {
                helper.fail("totalPowerDraw not updated after auto-crafter joined multiblock");
                return;
            }
            helper.succeed();
        });
    }

    // -----------------------------------------------------------------------
    // Test: PerPatternConfig persists across NBT round-trip
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 5)
    public static void auto_crafter_configs_persist_nbt(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AutoCrafterBlockEntity crafter = getCrafter(helper, AUTO_CRAFTER_POS);
            if (crafter == null) {
                return;
            }

            crafter.setConfig(0, new PerPatternConfig(true, 5));

            // Simulate save/load via NBT round-trip
            net.minecraft.nbt.CompoundTag tag = crafter.getUpdateTag(helper.getLevel().registryAccess());
            crafter.loadAdditional(tag, helper.getLevel().registryAccess());

            PerPatternConfig loaded = crafter.getConfigs()[0];
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
    // Test: Custom name persists across NBT round-trip
    // -----------------------------------------------------------------------
    @GameTest(template = "core_with_auto_crafter", setupTicks = 5)
    public static void auto_crafter_custom_name_persists_nbt(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AutoCrafterBlockEntity crafter = getCrafter(helper, AUTO_CRAFTER_POS);
            if (crafter == null) {
                return;
            }

            crafter.setCustomName("FurnaceCrafter");

            net.minecraft.nbt.CompoundTag tag = crafter.getUpdateTag(helper.getLevel().registryAccess());
            crafter.loadAdditional(tag, helper.getLevel().registryAccess());

            if (!"FurnaceCrafter".equals(crafter.getCustomName())) {
                helper.fail("customName should be 'FurnaceCrafter' after NBT round-trip, got: "
                    + crafter.getCustomName());
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
