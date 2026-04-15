package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStatisticsBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStatisticsMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockAdvancedStatistics extends StorageMultiblock implements EntityBlock {

    public BlockAdvancedStatistics() {
        super(Properties.of().strength(2.0f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedStatisticsBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        // MultiblockBlockEntity.tick() periodically re-links this BE to the core.
        return type == ModBlockEntities.ADVANCED_STATISTICS.get()
            ? (BlockEntityTicker<T>) (BlockEntityTicker<AdvancedStatisticsBlockEntity>)
              (lvl, pos, st, be) -> be.tick()
            : null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AdvancedStatisticsBlockEntity be) {
                serverPlayer.openMenu(be, buf -> {
                    buf.writeBlockPos(be.getBlockPos());
                    AdvancedStorageCoreBlockEntity core =
                        AdvancedStorageCoreBlockEntity.findCore(level, pos);
                    buf.writeBoolean(core != null);
                    if (core != null) {
                        AdvancedStatisticsMenu.writeSnapshot(buf, core);
                    }
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
