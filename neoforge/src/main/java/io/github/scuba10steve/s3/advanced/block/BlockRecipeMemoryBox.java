package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BlockRecipeMemoryBox extends StorageMultiblock implements EntityBlock {

    public BlockRecipeMemoryBox() {
        super(Properties.of().strength(2.0f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecipeMemoryBoxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof RecipeMemoryBoxBlockEntity be) {
                AdvancedStorageCoreBlockEntity core = findCore(level, pos);
                List<BlockPos> crafterPositions = core != null
                    ? core.getAutoCrafters().stream()
                          .map(AutoCrafterBlockEntity::getBlockPos)
                          .toList()
                    : List.of();
                serverPlayer.openMenu(be, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeInt(crafterPositions.size());
                    for (BlockPos cp : crafterPositions) {
                        buf.writeBlockPos(cp);
                    }
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * BFS from {@code start} following StorageMultiblock and AdvancedStorageCore blocks
     * to find the AdvancedStorageCoreBlockEntity for this multiblock.
     * Returns null if not found (e.g. block placed outside a multiblock).
     */
    private static AdvancedStorageCoreBlockEntity findCore(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            Block block = level.getBlockState(pos).getBlock();
            if (block instanceof BlockAdvancedStorageCore
                    && level.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity core) {
                return core;
            }
            if (block instanceof StorageMultiblock || block instanceof BlockAdvancedStorageCore) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return null;
    }
}
