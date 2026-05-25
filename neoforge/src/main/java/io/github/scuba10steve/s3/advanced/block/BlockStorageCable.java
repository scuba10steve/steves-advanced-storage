package io.github.scuba10steve.s3.advanced.block;

import io.github.scuba10steve.s3.block.StorageMultiblock;

public class BlockStorageCable extends StorageMultiblock {
    public BlockStorageCable() {
        super(Properties.of().strength(1.0f).noOcclusion());
    }
}
