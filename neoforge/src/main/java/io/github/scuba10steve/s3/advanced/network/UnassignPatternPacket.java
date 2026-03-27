package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnassignPatternPacket(BlockPos crafterPos, PatternKey patternKey)
        implements CustomPacketPayload {

    public static final Type<UnassignPatternPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "unassign_pattern"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnassignPatternPacket> STREAM_CODEC =
        StreamCodec.of(UnassignPatternPacket::encode, UnassignPatternPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, UnassignPatternPacket p) {
        buf.writeBlockPos(p.crafterPos());
        buf.writeBlockPos(p.patternKey().pos());
        buf.writeInt(p.patternKey().index());
    }

    private static UnassignPatternPacket decode(RegistryFriendlyByteBuf buf) {
        BlockPos crafterPos = buf.readBlockPos();
        return new UnassignPatternPacket(crafterPos,
            new PatternKey(buf.readBlockPos(), buf.readInt()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
