package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdatePatternConfigPacket(
        BlockPos crafterPos, PatternKey patternKey, boolean autoEnabled, int minimumBuffer)
        implements CustomPacketPayload {

    public static final Type<UpdatePatternConfigPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "update_pattern_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePatternConfigPacket> STREAM_CODEC =
        StreamCodec.of(UpdatePatternConfigPacket::encode, UpdatePatternConfigPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, UpdatePatternConfigPacket p) {
        buf.writeBlockPos(p.crafterPos());
        buf.writeBlockPos(p.patternKey().pos());
        buf.writeInt(p.patternKey().index());
        buf.writeBoolean(p.autoEnabled());
        buf.writeInt(p.minimumBuffer());
    }

    private static UpdatePatternConfigPacket decode(RegistryFriendlyByteBuf buf) {
        BlockPos crafterPos = buf.readBlockPos();
        PatternKey key = new PatternKey(buf.readBlockPos(), buf.readInt());
        return new UpdatePatternConfigPacket(crafterPos, key, buf.readBoolean(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
