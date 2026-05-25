package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockStorageCable extends StorageMultiblock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final VoxelShape CORE     = Block.box(6,  6, 6, 10, 10, 10);
    private static final VoxelShape ARM_UP   = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape ARM_DOWN = Block.box(6,  0, 6, 10,  6, 10);
    private static final VoxelShape ARM_N    = Block.box(6,  6, 0, 10, 10,  6);
    private static final VoxelShape ARM_S    = Block.box(6,  6, 10, 10, 10, 16);
    private static final VoxelShape ARM_E    = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape ARM_W    = Block.box(0,  6, 6,  6, 10, 10);

    public BlockStorageCable() {
        super(Properties.of().strength(1.0f).noOcclusion());
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST,  false).setValue(WEST,  false)
            .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectionState(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos)
                    .setValue(directionProperty(direction), connects(neighborState));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(UP))    shape = Shapes.or(shape, ARM_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, ARM_DOWN);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_N);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_S);
        if (state.getValue(EAST))  shape = Shapes.or(shape, ARM_E);
        if (state.getValue(WEST))  shape = Shapes.or(shape, ARM_W);
        return shape;
    }

    private BlockState connectionState(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(directionProperty(dir),
                connects(level.getBlockState(pos.relative(dir))));
        }
        return state;
    }

    private boolean connects(BlockState neighborState) {
        return neighborState.getBlock() instanceof StorageMultiblock;
    }

    private static BooleanProperty directionProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST  -> EAST;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }
}
