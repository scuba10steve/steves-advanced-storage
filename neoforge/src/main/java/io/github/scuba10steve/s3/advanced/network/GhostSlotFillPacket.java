package io.github.scuba10steve.s3.advanced.network;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record GhostSlotFillPacket(List<ItemStack> items) implements CustomPacketPayload {

    public static final Type<GhostSlotFillPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "ghost_slot_fill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GhostSlotFillPacket> STREAM_CODEC = StreamCodec.of(
        GhostSlotFillPacket::encode,
        GhostSlotFillPacket::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, GhostSlotFillPacket packet) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < packet.items.size() ? packet.items.get(i) : ItemStack.EMPTY;
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static GhostSlotFillPacket decode(RegistryFriendlyByteBuf buf) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new GhostSlotFillPacket(items);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
