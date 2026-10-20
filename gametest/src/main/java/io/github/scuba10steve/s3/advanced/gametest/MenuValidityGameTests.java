package io.github.scuba10steve.s3.advanced.gametest;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStatisticsBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.SolarGeneratorBlockEntity;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStatisticsMenu;
import io.github.scuba10steve.s3.advanced.gui.server.AutoCrafterMenu;
import io.github.scuba10steve.s3.advanced.gui.server.MachineInterfaceMenu;
import io.github.scuba10steve.s3.advanced.gui.server.RecipeMemoryBoxMenu;
import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import io.github.scuba10steve.s3.advanced.gui.server.SolarGeneratorMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Function;

import static io.github.scuba10steve.s3.advanced.gametest.TankGameTests.LOOSE_POS;

/**
 * Menus must become invalid once their block is gone (#52). These assert {@code stillValid}
 * directly: the mock player is a plain, un-ticked {@link Player}, so the server-side
 * close path ({@code ServerPlayer.tick} -> {@code closeContainer}) is not exercised.
 */
@GameTestHolder("s3_advanced")
@PrefixGameTestTemplate(false)
public class MenuValidityGameTests {

    // Template y=0 maps to test y=1.
    private static final BlockPos AUTO_CRAFTER_POS      = new BlockPos(0, 1, 1); // core_with_auto_crafter
    private static final BlockPos MACHINE_INTERFACE_POS = new BlockPos(0, 1, 1); // core_with_machine_interface
    private static final BlockPos RMB_POS               = new BlockPos(2, 1, 1); // core_with_rmb_and_crafter

    @GameTest(template = "core_with_auto_crafter")
    public static void auto_crafter_menu_invalid_after_block_removed(GameTestHelper h) {
        assertInvalidAfterBlockRemoved(h, AUTO_CRAFTER_POS, player -> new AutoCrafterMenu(0, player.getInventory(),
            (AutoCrafterBlockEntity) h.getBlockEntity(AUTO_CRAFTER_POS)));
    }

    @GameTest(template = "core_with_machine_interface")
    public static void machine_interface_menu_invalid_after_block_removed(GameTestHelper h) {
        assertInvalidAfterBlockRemoved(h, MACHINE_INTERFACE_POS, player -> new MachineInterfaceMenu(0, player.getInventory(),
            (MachineInterfaceBlockEntity) h.getBlockEntity(MACHINE_INTERFACE_POS)));
    }

    @GameTest(template = "core_with_rmb_and_crafter")
    public static void recipe_memory_box_menu_invalid_after_block_removed(GameTestHelper h) {
        assertInvalidAfterBlockRemoved(h, RMB_POS, player -> new RecipeMemoryBoxMenu(0, player.getInventory(),
            (RecipeMemoryBoxBlockEntity) h.getBlockEntity(RMB_POS)));
    }

    @GameTest(template = "core_with_rmb_and_crafter")
    public static void recipe_pattern_menu_invalid_after_block_removed(GameTestHelper h) {
        assertInvalidAfterBlockRemoved(h, RMB_POS, player -> new RecipePatternMenu(0, player.getInventory(),
            (RecipeMemoryBoxBlockEntity) h.getBlockEntity(RMB_POS), 0));
    }

    @GameTest(template = "core_with_storage_box")
    public static void solar_generator_menu_invalid_after_block_removed(GameTestHelper h) {
        h.setBlock(LOOSE_POS, ModBlocks.SOLAR_GENERATOR.get());
        assertInvalidAfterBlockRemoved(h, LOOSE_POS, player -> new SolarGeneratorMenu(0, player.getInventory(),
            (SolarGeneratorBlockEntity) h.getBlockEntity(LOOSE_POS)));
    }

    @GameTest(template = "core_with_storage_box")
    public static void advanced_statistics_menu_invalid_after_block_removed(GameTestHelper h) {
        h.setBlock(LOOSE_POS, ModBlocks.ADVANCED_STATISTICS.get());
        assertInvalidAfterBlockRemoved(h, LOOSE_POS, player -> new AdvancedStatisticsMenu(0, player.getInventory(),
            (AdvancedStatisticsBlockEntity) h.getBlockEntity(LOOSE_POS)));
    }

    private static void assertInvalidAfterBlockRemoved(GameTestHelper h, BlockPos pos,
                                                       Function<Player, AbstractContainerMenu> menuFactory) {
        // makeMockPlayer spawns at BlockPos.ZERO; stand on top of the block so it is within reach.
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 standAt = Vec3.atBottomCenterOf(h.absolutePos(pos.above()));
        player.moveTo(standAt.x, standAt.y, standAt.z);
        AbstractContainerMenu menu = menuFactory.apply(player);

        h.assertTrue(menu.stillValid(player), "Menu should be valid while its block is present");
        h.setBlock(pos, Blocks.AIR);
        h.assertFalse(menu.stillValid(player), "Menu should be invalid after its block is removed");
        h.succeed();
    }
}
