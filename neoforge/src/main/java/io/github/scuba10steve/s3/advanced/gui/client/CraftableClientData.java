package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.network.CraftableSyncPacket;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache of craftable entries per Advanced Storage Core position.
 * Updated by CraftableSyncPacket; read by storage display screens.
 */
public class CraftableClientData {

    private static final Map<BlockPos, List<CraftableSyncPacket.Entry>> DATA = new HashMap<>();

    public static void put(BlockPos corePos, List<CraftableSyncPacket.Entry> entries) {
        DATA.put(corePos, List.copyOf(entries));
    }

    public static List<CraftableSyncPacket.Entry> get(BlockPos corePos) {
        return DATA.getOrDefault(corePos, List.of());
    }

    public static void clear(BlockPos corePos) {
        DATA.remove(corePos);
    }
}
