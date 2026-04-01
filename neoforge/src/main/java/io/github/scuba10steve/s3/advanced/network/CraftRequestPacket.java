package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server. Player requests a craft from the storage GUI.
 */
public record CraftRequestPacket(BlockPos corePos, PatternKey patternKey, int quantity)
        implements CustomPacketPayload {

    public static final Type<CraftRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "craft_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequestPacket> STREAM_CODEC =
        StreamCodec.of(CraftRequestPacket::encode, CraftRequestPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, CraftRequestPacket p) {
        buf.writeBlockPos(p.corePos());
        buf.writeBlockPos(p.patternKey().pos());
        buf.writeInt(p.patternKey().index());
        buf.writeInt(p.quantity());
    }

    private static CraftRequestPacket decode(RegistryFriendlyByteBuf buf) {
        BlockPos corePos = buf.readBlockPos();
        PatternKey key = new PatternKey(buf.readBlockPos(), buf.readInt());
        int qty = buf.readInt();
        return new CraftRequestPacket(corePos, key, qty);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
