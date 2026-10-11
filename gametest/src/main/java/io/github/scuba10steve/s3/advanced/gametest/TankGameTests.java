package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.block.TankTier;
import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.TankBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import io.github.scuba10steve.s3.advanced.init.ModDataComponents;
import io.github.scuba10steve.s3.advanced.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class TankGameTests {

    // Template core_with_storage_box: core at (1,1,1), storage box at (2,1,1).
    static final BlockPos CORE_POS = new BlockPos(1, 1, 1);
    static final BlockPos ADJACENT_POS = new BlockPos(0, 1, 1);   // touches the core
    static final BlockPos LOOSE_POS = new BlockPos(0, 1, 0);      // touches nothing in the multiblock
    static final BlockPos LOOSE_POS_2 = new BlockPos(1, 1, 2);    // touches the core; use only when joining is irrelevant

    static TankBlockEntity placeTank(GameTestHelper h, BlockPos pos, TankTier tier) {
        h.setBlock(pos, ModBlocks.TANKS.get(tier).get());
        if (h.getBlockEntity(pos) instanceof TankBlockEntity be) {
            return be;
        }
        h.fail("No TankBlockEntity at " + pos);
        return null;
    }

    static IFluidHandler handler(GameTestHelper h, BlockPos pos) {
        IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(fh != null, "No fluid handler capability at " + pos);
        return fh;
    }

    static FluidStack water(int mb) {
        return new FluidStack(Fluids.WATER, mb);
    }

    static FluidStack lava(int mb) {
        return new FluidStack(Fluids.LAVA, mb);
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_capacity_matches_tier(GameTestHelper h) {
        BlockPos[] positions = { LOOSE_POS, new BlockPos(2, 1, 2), new BlockPos(0, 2, 0), new BlockPos(2, 2, 2) };
        TankTier[] tiers = TankTier.values();
        for (int i = 0; i < tiers.length; i++) {
            placeTank(h, positions[i], tiers[i]);
            int filled = handler(h, positions[i]).fill(water(Integer.MAX_VALUE), FluidAction.EXECUTE);
            h.assertTrue(filled == tiers[i].capacityMb(),
                tiers[i] + " accepted " + filled + " mB, expected " + tiers[i].capacityMb());
        }
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_holds_single_fluid(GameTestHelper h) {
        placeTank(h, LOOSE_POS, TankTier.BASIC);
        IFluidHandler fh = handler(h, LOOSE_POS);
        fh.fill(water(1000), FluidAction.EXECUTE);
        h.assertTrue(fh.fill(lava(1000), FluidAction.EXECUTE) == 0, "Tank holding water accepted lava");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_lock_persists_when_empty(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        IFluidHandler fh = handler(h, LOOSE_POS);
        fh.fill(water(1000), FluidAction.EXECUTE);
        be.toggleLock();
        h.assertTrue(be.getLockedFluid() == Fluids.WATER, "Expected lock to water");
        fh.drain(1000, FluidAction.EXECUTE);
        h.assertTrue(be.getTank().isEmpty(), "Expected tank empty after drain");
        h.assertTrue(fh.fill(lava(1000), FluidAction.EXECUTE) == 0, "Locked-to-water empty tank accepted lava");
        h.assertTrue(fh.fill(water(500), FluidAction.EXECUTE) == 500, "Locked-to-water tank refused water");
        be.toggleLock();
        h.assertTrue(be.getLockedFluid() == null, "Expected unlock");
        fh.drain(500, FluidAction.EXECUTE);
        h.assertTrue(fh.fill(lava(1000), FluidAction.EXECUTE) == 1000, "Unlocked empty tank refused lava");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_lock_ignored_when_empty_and_unlocked(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        be.toggleLock();
        h.assertTrue(be.getLockedFluid() == null, "Empty unlocked tank must not lock");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_nbt_round_trip(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        handler(h, LOOSE_POS).fill(water(4321), FluidAction.EXECUTE);
        be.toggleLock();
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = be.saveWithoutMetadata(registries);
        be.getTank().setFluid(FluidStack.EMPTY);
        be.toggleLock(); // unlock
        be.loadWithComponents(tag, registries);
        h.assertTrue(be.getTank().getFluidAmount() == 4321, "Amount lost in NBT round trip");
        h.assertTrue(be.getLockedFluid() == Fluids.WATER, "Lock lost in NBT round trip");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_invalid_lock_id_loads_unlocked(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = be.saveWithoutMetadata(registries);
        tag.putString("LockedFluid", "nonexistent_mod:ghost_fluid");
        be.loadWithComponents(tag, registries);
        h.assertTrue(be.getLockedFluid() == null, "Unknown lock id must load as unlocked");
        h.assertTrue(handler(h, LOOSE_POS).fill(lava(1000), FluidAction.EXECUTE) == 1000,
            "Tank with invalid lock should accept any fluid");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_over_capacity_after_load_keeps_fluid(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        var registries = h.getLevel().registryAccess();
        CompoundTag tankTag = new CompoundTag();
        tankTag.put("Fluid", water(20000).save(registries));
        CompoundTag tag = be.saveWithoutMetadata(registries);
        tag.put("Tank", tankTag);
        be.loadWithComponents(tag, registries);
        IFluidHandler fh = handler(h, LOOSE_POS);
        h.assertTrue(be.getTank().getFluidAmount() == 20000, "Over-capacity contents were truncated");
        h.assertTrue(fh.fill(water(1000), FluidAction.EXECUTE) == 0, "Over-capacity tank accepted more fluid");
        h.assertTrue(fh.drain(1000, FluidAction.EXECUTE).getAmount() == 1000, "Over-capacity tank refused drain");
        h.succeed();
    }

    static AdvancedStorageCoreBlockEntity core(GameTestHelper h) {
        if (h.getBlockEntity(CORE_POS) instanceof AdvancedStorageCoreBlockEntity be) {
            return be;
        }
        h.fail("No AdvancedStorageCoreBlockEntity at " + CORE_POS);
        return null;
    }

    static int powerDraw(AdvancedStorageCoreBlockEntity core) {
        return (core.containerData.get(5) << 16) | (core.containerData.get(4) & 0xFFFF);
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void tank_discovered_and_adds_power(GameTestHelper h) {
        AdvancedStorageCoreBlockEntity core = core(h);
        int before = powerDraw(core);
        TankBlockEntity tank = placeTank(h, ADJACENT_POS, TankTier.SUPER);
        h.runAfterDelay(2, () -> {
            h.assertTrue(core.getTanks().contains(tank), "Core did not discover adjacent tank");
            int delta = powerDraw(core) - before;
            h.assertTrue(delta == TankTier.SUPER.energyPerTick(),
                "Power draw rose by " + delta + ", expected " + TankTier.SUPER.energyPerTick());
            h.succeed();
        });
    }

    @GameTest(template = "core_with_storage_box", setupTicks = 5)
    public static void tank_removed_from_multiblock_drops_power(GameTestHelper h) {
        AdvancedStorageCoreBlockEntity core = core(h);
        int before = powerDraw(core);
        placeTank(h, ADJACENT_POS, TankTier.BASIC);
        h.runAfterDelay(2, () -> {
            h.setBlock(ADJACENT_POS, Blocks.AIR);
            h.runAfterDelay(2, () -> {
                h.assertTrue(core.getTanks().isEmpty(), "Removed tank still listed");
                h.assertTrue(powerDraw(core) == before, "Power draw not restored after tank removal");
                h.succeed();
            });
        });
    }

    static ItemStack dropOf(GameTestHelper h, BlockPos pos) {
        BlockState state = h.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, h.getLevel(), h.absolutePos(pos), h.getBlockEntity(pos));
        h.assertTrue(drops.size() == 1, "Expected exactly one drop, got " + drops.size());
        return drops.get(0);
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_drop_carries_fluid_and_lock(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.ADVANCED);
        handler(h, LOOSE_POS).fill(water(7000), FluidAction.EXECUTE);
        be.toggleLock();
        ItemStack drop = dropOf(h, LOOSE_POS);
        var content = drop.get(ModDataComponents.TANK_FLUID.get());
        h.assertTrue(content != null && content.copy().getAmount() == 7000 && content.copy().is(Fluids.WATER),
            "Drop missing tank_fluid water x7000");
        var locked = drop.get(ModDataComponents.LOCKED_FLUID.get());
        h.assertTrue(locked != null && locked.value() == Fluids.WATER, "Drop missing locked_fluid water");

        TankBlockEntity placed = placeTank(h, new BlockPos(2, 1, 2), TankTier.ADVANCED);
        placed.applyComponentsFromItemStack(drop);
        h.assertTrue(placed.getTank().getFluidAmount() == 7000, "Placed tank did not restore amount");
        h.assertTrue(placed.getLockedFluid() == Fluids.WATER, "Placed tank did not restore lock");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void empty_tank_drop_has_no_components(GameTestHelper h) {
        placeTank(h, LOOSE_POS, TankTier.BASIC);
        ItemStack drop = dropOf(h, LOOSE_POS);
        ItemStack fresh = new ItemStack(ModItems.TANK_ITEMS.get(TankTier.BASIC).get());
        h.assertTrue(ItemStack.isSameItemSameComponents(drop, fresh),
            "Empty unlocked tank drop must stack with a fresh tank item");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void tank_render_step_math(GameTestHelper h) {
        h.assertTrue(TankBlockEntity.renderStep(0, 16000) == 0, "empty -> 0");
        h.assertTrue(TankBlockEntity.renderStep(16000, 16000) == 64, "full -> 64");
        h.assertTrue(TankBlockEntity.renderStep(8000, 16000) == 32, "half -> 32");
        h.assertTrue(TankBlockEntity.renderStep(1_024_000, 1_024_000) == 64, "hyper full -> 64");
        h.assertTrue(TankBlockEntity.renderStep(20000, 16000) == 64, "over capacity clamps to 64");
        h.assertTrue(TankBlockEntity.renderStep(5, 0) == 0, "zero capacity -> 0");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void bucket_slots_empty_water_bucket_into_tank(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        be.bucketSlots.setItem(TankBlockEntity.SLOT_INPUT, new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(be.getTank().getFluidAmount() == 1000, "Tank should hold 1000 mB");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_INPUT).isEmpty(), "Input should be consumed");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_OUTPUT).is(Items.BUCKET), "Output should be an empty bucket");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void bucket_slots_blocked_output_leaves_tank_unchanged(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        handler(h, LOOSE_POS).fill(water(5000), FluidAction.EXECUTE);
        be.bucketSlots.setItem(TankBlockEntity.SLOT_OUTPUT, new ItemStack(Items.DIRT));
        be.bucketSlots.setItem(TankBlockEntity.SLOT_INPUT, new ItemStack(Items.BUCKET));
        h.assertTrue(be.getTank().getFluidAmount() == 5000, "Blocked transfer must not drain the tank");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_INPUT).is(Items.BUCKET), "Input bucket must remain");
        be.bucketSlots.setItem(TankBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        h.assertTrue(be.getTank().getFluidAmount() == 4000, "Clearing output should retry and drain 1000 mB");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_OUTPUT).is(Items.WATER_BUCKET), "Output should hold a water bucket");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void bucket_slots_stacked_input_one_at_a_time(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        handler(h, LOOSE_POS).fill(water(16000), FluidAction.EXECUTE);
        be.bucketSlots.setItem(TankBlockEntity.SLOT_INPUT, new ItemStack(Items.BUCKET, 16));
        h.assertTrue(be.getTank().getFluidAmount() == 15000, "Exactly one bucket should fill (water buckets don't stack)");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_INPUT).getCount() == 15, "15 empty buckets should remain");
        be.bucketSlots.setItem(TankBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        h.assertTrue(be.getTank().getFluidAmount() == 14000, "Taking the output should process the next bucket");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void bucket_slots_rejected_fluid_untouched(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        handler(h, LOOSE_POS).fill(water(1000), FluidAction.EXECUTE);
        be.toggleLock();
        be.bucketSlots.setItem(TankBlockEntity.SLOT_INPUT, new ItemStack(Items.LAVA_BUCKET));
        h.assertTrue(be.getTank().getFluidAmount() == 1000 && be.getTank().getFluid().is(Fluids.WATER),
            "Locked tank contents changed");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_INPUT).is(Items.LAVA_BUCKET), "Lava bucket must stay in input");
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_OUTPUT).isEmpty(), "Nothing should reach output");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void bucket_slots_retry_after_pipe_fill(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        be.bucketSlots.setItem(TankBlockEntity.SLOT_INPUT, new ItemStack(Items.BUCKET));
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_INPUT).is(Items.BUCKET), "Empty tank can't fill a bucket");
        handler(h, LOOSE_POS).fill(water(1000), FluidAction.EXECUTE);
        h.assertTrue(be.bucketSlots.getItem(TankBlockEntity.SLOT_OUTPUT).is(Items.WATER_BUCKET),
            "Tank change should retry the waiting bucket");
        h.assertTrue(be.getTank().isEmpty(), "Tank should be drained into the bucket");
        h.succeed();
    }

    @GameTest(template = "core_with_storage_box")
    public static void bucket_slots_drop_when_tank_broken(GameTestHelper h) {
        TankBlockEntity be = placeTank(h, LOOSE_POS, TankTier.BASIC);
        be.bucketSlots.setItem(TankBlockEntity.SLOT_OUTPUT, new ItemStack(Items.DIRT, 3));
        h.destroyBlock(LOOSE_POS);
        h.assertItemEntityPresent(Items.DIRT, LOOSE_POS, 2.0);
        h.succeed();
    }
}
