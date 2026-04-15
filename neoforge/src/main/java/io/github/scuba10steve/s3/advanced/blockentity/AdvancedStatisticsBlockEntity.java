package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStatisticsMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.MultiblockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedStatisticsBlockEntity extends MultiblockBlockEntity implements MenuProvider {

    public AdvancedStatisticsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_STATISTICS.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.advanced_statistics");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AdvancedStatisticsMenu(containerId, playerInventory, this);
    }
}
