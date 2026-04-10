package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdatePatternConfigPacket(
        BlockPos crafterPos, int slotIndex, boolean autoEnabled, int minimumBuffer)
        implements CustomPacketPayload {

    public static final Type<UpdatePatternConfigPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "update_pattern_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePatternConfigPacket> STREAM_CODEC =
        StreamCodec.of(UpdatePatternConfigPacket::encode, UpdatePatternConfigPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, UpdatePatternConfigPacket p) {
        buf.writeBlockPos(p.crafterPos());
        buf.writeInt(p.slotIndex());
        buf.writeBoolean(p.autoEnabled());
        buf.writeInt(p.minimumBuffer());
    }

    private static UpdatePatternConfigPacket decode(RegistryFriendlyByteBuf buf) {
        return new UpdatePatternConfigPacket(
            buf.readBlockPos(), buf.readInt(), buf.readBoolean(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
