package io.github.scuba10steve.s3.advanced.crafting;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Per-pattern auto-buffer configuration stored in AutoCrafterBlockEntity.
 * autoEnabled: whether this pattern participates in automatic buffering.
 * minimumBuffer: target quantity; coordinator tops up to this level.
 */
public record PerPatternConfig(boolean autoEnabled, int minimumBuffer) {

    public static final PerPatternConfig DEFAULT = new PerPatternConfig(false, 0);

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoEnabled);
        buf.writeInt(minimumBuffer);
    }

    public static PerPatternConfig decode(FriendlyByteBuf buf) {
        return new PerPatternConfig(buf.readBoolean(), buf.readInt());
    }
}
