package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client. Delivers the list of craftable output items for a specific
 * Advanced Storage Core. Sent on GUI open and after every scanMultiblock().
 */
public record CraftableSyncPacket(BlockPos corePos, List<Entry> entries)
        implements CustomPacketPayload {

    public record Entry(PatternKey patternKey, ItemStack output) {}

    public static final Type<CraftableSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "craftable_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftableSyncPacket> STREAM_CODEC =
        StreamCodec.of(CraftableSyncPacket::encode, CraftableSyncPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, CraftableSyncPacket p) {
        buf.writeBlockPos(p.corePos());
        buf.writeInt(p.entries().size());
        for (Entry e : p.entries()) {
            buf.writeBlockPos(e.patternKey().pos());
            buf.writeInt(e.patternKey().index());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.output());
        }
    }

    private static CraftableSyncPacket decode(RegistryFriendlyByteBuf buf) {
        BlockPos corePos = buf.readBlockPos();
        int count = buf.readInt();
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            PatternKey key = new PatternKey(buf.readBlockPos(), buf.readInt());
            ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            entries.add(new Entry(key, output));
        }
        return new CraftableSyncPacket(corePos, entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
