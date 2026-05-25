package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.block.BlockBlankBox;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockStorageCable extends BlockBlankBox {
    public BlockStorageCable() {
        super(BlockBehaviour.Properties.of().strength(1.0f));
    }
}
