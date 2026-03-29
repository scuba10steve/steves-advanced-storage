package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateMachineInterfaceTickPacket(BlockPos pos, int tickInterval)
        implements CustomPacketPayload {

    public static final Type<UpdateMachineInterfaceTickPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "update_mi_tick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateMachineInterfaceTickPacket> STREAM_CODEC =
        StreamCodec.of(UpdateMachineInterfaceTickPacket::encode, UpdateMachineInterfaceTickPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, UpdateMachineInterfaceTickPacket p) {
        buf.writeBlockPos(p.pos());
        buf.writeInt(p.tickInterval());
    }

    private static UpdateMachineInterfaceTickPacket decode(RegistryFriendlyByteBuf buf) {
        return new UpdateMachineInterfaceTickPacket(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
