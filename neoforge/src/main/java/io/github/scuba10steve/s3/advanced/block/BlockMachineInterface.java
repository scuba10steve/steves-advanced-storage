package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMachineInterface extends StorageMultiblock implements EntityBlock {

    public BlockMachineInterface() {
        super(Properties.of().strength(2.0f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineInterfaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
