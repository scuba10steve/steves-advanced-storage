package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

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
                    buf.writeBlockPos(pos);
                    Map<PatternKey, PerPatternConfig> assignments = be.getAssignments();
                    buf.writeInt(assignments.size());
                    RegistryFriendlyByteBuf registryBuf = (RegistryFriendlyByteBuf) buf;
                    for (Map.Entry<PatternKey, PerPatternConfig> entry : assignments.entrySet()) {
                        buf.writeBlockPos(entry.getKey().pos());
                        buf.writeInt(entry.getKey().index());
                        entry.getValue().encode(buf);
                        ItemStack output = ItemStack.EMPTY;
                        if (level.getBlockEntity(entry.getKey().pos()) instanceof RecipeMemoryBoxBlockEntity rmbBe) {
                            output = rmbBe.getPattern(entry.getKey().index()).getOutput();
                        }
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf, output);
                    }
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
