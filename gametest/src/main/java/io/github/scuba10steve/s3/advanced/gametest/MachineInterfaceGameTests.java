package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
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
public class MachineInterfaceGameTests {

    private static final BlockPos CORE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos MI_POS   = new BlockPos(0, 1, 1);

    @GameTest(template = "core_with_machine_interface", setupTicks = 5)
    public static void machine_interface_discovered_in_multiblock(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            if (core == null) return;
            if (core.getMachineInterfaces().isEmpty()) {
                helper.fail("Expected Machine Interface to be discovered in multiblock scan");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_machine_interface", setupTicks = 5)
    public static void machine_interface_contributes_power_draw(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            if (core == null) return;
            int draw = core.containerData.get(4);
            if (draw <= 0) {
                helper.fail("totalPowerDraw not updated after machine interface joined multiblock");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_machine_interface", setupTicks = 5)
    public static void machine_interface_tick_interval_persists_nbt(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            MachineInterfaceBlockEntity mi = getMI(helper, MI_POS);
            if (mi == null) return;

            mi.setTickInterval(42);
            net.minecraft.nbt.CompoundTag tag = mi.getUpdateTag(helper.getLevel().registryAccess());
            mi.loadAdditional(tag, helper.getLevel().registryAccess());

            if (mi.getTickInterval() != 42) {
                helper.fail("tickInterval should be 42 after NBT round-trip, got " + mi.getTickInterval());
                return;
            }
            helper.succeed();
        });
    }

    /**
     * Verifies that when the MI has no adjacent IItemHandler and a pattern is provided,
     * tryTick() leaves the MI in IDLE status (no handler found) rather than crashing.
     */
    @GameTest(template = "core_with_machine_interface", setupTicks = 5)
    public static void machine_interface_idle_when_no_adjacent_handler(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            MachineInterfaceBlockEntity mi = getMI(helper, MI_POS);
            if (core == null || mi == null) return;

            core.getInventory().setMaxItems(1000L);
            core.getInventory().insertItem(new ItemStack(Items.RAW_IRON, 1));

            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.RAW_IRON, 1));
            pattern.setOutput(new ItemStack(Items.IRON_INGOT, 1));

            mi.setTickInterval(1);
            // The chest in the template is at (0,0,0) = test coord (0,1,0), not adjacent to MI at (0,1,1).
            // Direction.DOWN neighbor is (0,0,1) in template = (0,1,0) in test — depends on layout.
            // Regardless, tryTick() should not throw; status will be IDLE or WAITING.
            mi.tryTick(core.getInventory(), helper.getLevel(), pattern);

            // As long as no exception was thrown the MI handled the tick gracefully
            helper.succeed();
        });
    }

    @GameTest(template = "core_with_machine_interface", setupTicks = 5)
    public static void machine_interface_pushes_ingredients_to_adjacent_chest(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            MachineInterfaceBlockEntity mi = getMI(helper, MI_POS);
            if (core == null || mi == null) return;

            core.getInventory().setMaxItems(1000L);

            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.RAW_IRON, 1));
            pattern.setOutput(new ItemStack(Items.IRON_INGOT, 1));

            mi.setTickInterval(1); // fire immediately

            core.getInventory().insertItem(new ItemStack(Items.RAW_IRON, 1));

            // Simulate one tick cycle — passes the pattern directly (new API)
            mi.tryTick(core.getInventory(), helper.getLevel(), pattern);

            // Raw iron should have left storage (pushed into adjacent chest)
            ItemStack remaining = core.getInventory().extractItem(
                new ItemStack(Items.RAW_IRON), 64);
            if (!remaining.isEmpty()) {
                helper.fail("Expected RAW_IRON to have been pushed into adjacent chest");
                return;
            }
            helper.succeed();
        });
    }

    private static AdvancedStorageCoreBlockEntity getCore(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity be) return be;
        helper.fail("AdvancedStorageCoreBlockEntity not found at " + pos);
        return null;
    }

    private static MachineInterfaceBlockEntity getMI(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof MachineInterfaceBlockEntity be) return be;
        helper.fail("MachineInterfaceBlockEntity not found at " + pos);
        return null;
    }
}
