package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server. Player edits the Auto-Crafter's custom name.
 */
public record RenameAutoCrafterPacket(BlockPos crafterPos, String name)
        implements CustomPacketPayload {

    public static final Type<RenameAutoCrafterPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "rename_auto_crafter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameAutoCrafterPacket> STREAM_CODEC =
        StreamCodec.of(RenameAutoCrafterPacket::encode, RenameAutoCrafterPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, RenameAutoCrafterPacket p) {
        buf.writeBlockPos(p.crafterPos());
        buf.writeUtf(p.name(), 32);
    }

    private static RenameAutoCrafterPacket decode(RegistryFriendlyByteBuf buf) {
        return new RenameAutoCrafterPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
