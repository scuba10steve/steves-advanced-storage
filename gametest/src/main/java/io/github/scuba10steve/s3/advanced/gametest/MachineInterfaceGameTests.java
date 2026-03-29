package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.CraftingCoordinator;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
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
    public static void machine_interface_pattern_persists_nbt(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            MachineInterfaceBlockEntity mi = getMI(helper, MI_POS);
            if (mi == null) return;

            PatternKey key = new PatternKey(new BlockPos(10, 10, 10), 0);
            mi.setPattern(key);

            net.minecraft.nbt.CompoundTag tag = mi.getUpdateTag(helper.getLevel().registryAccess());
            mi.loadAdditional(tag, helper.getLevel().registryAccess());

            PatternKey loaded = mi.getAssignedPattern();
            if (loaded == null) {
                helper.fail("Assigned pattern lost after NBT round-trip");
                return;
            }
            if (!loaded.pos().equals(key.pos()) || loaded.index() != key.index()) {
                helper.fail("Pattern key mismatch after NBT round-trip");
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

    @GameTest(template = "core_with_machine_interface", setupTicks = 5)
    public static void machine_interface_pushes_ingredients_to_adjacent_furnace(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            AdvancedStorageCoreBlockEntity core = getCore(helper, CORE_POS);
            MachineInterfaceBlockEntity mi = getMI(helper, MI_POS);
            if (core == null || mi == null) return;

            BlockPos rmbPos = new BlockPos(99, 99, 99);
            PatternKey key = new PatternKey(rmbPos, 0);

            RecipePattern pattern = new RecipePattern();
            pattern.setIngredient(0, new ItemStack(Items.RAW_IRON, 1));
            pattern.setOutput(new ItemStack(Items.IRON_INGOT, 1));

            mi.setPattern(key);
            mi.setTickInterval(1); // fire immediately

            core.getInventory().insertItem(new ItemStack(Items.RAW_IRON, 1));

            List<CraftingCoordinator.BoxData> boxes = List.of(
                new CraftingCoordinator.BoxData(rmbPos, List.of(pattern)));

            // Simulate one tick cycle
            mi.tryTick(core.getInventory(), helper.getLevel(), boxes);

            // Raw iron should have left storage (pushed into furnace)
            ItemStack remaining = core.getInventory().extractItem(
                new ItemStack(Items.RAW_IRON), 64);
            if (!remaining.isEmpty()) {
                helper.fail("Expected RAW_IRON to have been pushed into adjacent furnace");
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
