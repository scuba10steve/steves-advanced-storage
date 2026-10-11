package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.block.TankTier;
import io.github.scuba10steve.s3.advanced.blockentity.TankBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
}
