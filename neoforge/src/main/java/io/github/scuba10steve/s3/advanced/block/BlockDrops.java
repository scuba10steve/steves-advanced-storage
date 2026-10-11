package io.github.scuba10steve.s3.advanced.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

final class BlockDrops {

    private BlockDrops() {
    }

    /**
     * Spawns every stack in {@code handler} as an item entity at {@code pos}.
     * Reads slots without extracting, so the handler's {@code onContentsChanged}
     * (and any multiblock rescan it triggers) does not fire mid-removal.
     */
    static void dropContents(Level level, BlockPos pos, IItemHandler handler) {
        if (level.isClientSide()) {
            return;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i).copy());
        }
    }
}
