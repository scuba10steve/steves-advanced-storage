package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.gui.server.AutoCrafterMenu;
import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.world.item.ItemStack;
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

public class BlockAutoCrafter extends StorageMultiblock implements EntityBlock {

    public BlockAutoCrafter() {
        super(Properties.of().strength(2.0f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoCrafterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null; // Core drives all logic; this block does not tick independently.
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AutoCrafterBlockEntity be) {
                serverPlayer.openMenu(be, buf -> {
                    RecipeMemoryBoxBlockEntity rmb = AutoCrafterMenu.resolveRmb(be);
                    buf.writeBlockPos(be.getBlockPos());
                    buf.writeUtf(be.getCustomName(), 32);
                    buf.writeBoolean(rmb != null);
                    ItemStack[] outputs = AutoCrafterMenu.resolveOutputItems(rmb);
                    PerPatternConfig[] configs = be.getConfigs();
                    for (int i = 0; i < AutoCrafterBlockEntity.SLOT_COUNT; i++) {
                        configs[i].encode(buf);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, outputs[i]);
                    }
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
