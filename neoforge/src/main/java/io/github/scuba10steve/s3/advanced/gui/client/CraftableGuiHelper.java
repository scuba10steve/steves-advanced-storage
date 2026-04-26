package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.crafting.CrafterSlot;
import io.github.scuba10steve.s3.advanced.network.CraftableSyncPacket;
import io.github.scuba10steve.s3.storage.StoredItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stateless helper for craftable-item GUI logic shared by both storage display screens.
 */
public final class CraftableGuiHelper {

    private CraftableGuiHelper() {}

    /** Returns true if any entry's output matches the given item. */
    public static boolean isCraftable(ItemStack item, List<CraftableSyncPacket.Entry> entries) {
        if (item.isEmpty()) {
            return false;
        }
        for (CraftableSyncPacket.Entry e : entries) {
            if (ItemStack.isSameItemSameComponents(e.output(), item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the CrafterSlot for the first entry whose output matches the item,
     * or empty if the item is not craftable.
     */
    public static Optional<CrafterSlot> findCrafterSlot(ItemStack item, List<CraftableSyncPacket.Entry> entries) {
        for (CraftableSyncPacket.Entry e : entries) {
            if (ItemStack.isSameItemSameComponents(e.output(), item)) {
                return Optional.of(e.crafterSlot());
            }
        }
        return Optional.empty();
    }

    /**
     * Appends a "Craftable (Ctrl+click)" tooltip line if the item is craftable.
     * Called from screen subclasses' getTooltipFromContainerItem() override.
     */
    public static void appendCraftableTooltip(ItemStack item,
                                               List<CraftableSyncPacket.Entry> entries,
                                               List<Component> tooltip) {
        if (isCraftable(item, entries)) {
            tooltip.add(Component.translatable("tooltip.s3_advanced.craftable")
                .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Returns true if the mouse click is the craft trigger (left-click + Ctrl).
     * Called from screen subclasses' mouseClicked() override.
     */
    public static boolean isCraftTrigger(int button) {
        return button == 0 && Screen.hasControlDown();
    }

    /**
     * Returns a new list that is {@code base} plus one virtual entry (count 0) for each
     * craftable output that is not already present in {@code base}.
     * Used by screen subclasses to show craftable-but-not-stored items in the display.
     */
    public static List<StoredItemStack> withCraftableItems(List<StoredItemStack> base,
                                                            List<CraftableSyncPacket.Entry> entries) {
        if (entries.isEmpty()) {
            return base;
        }
        List<StoredItemStack> result = new ArrayList<>(base);
        outer:
        for (CraftableSyncPacket.Entry entry : entries) {
            ItemStack output = entry.output();
            if (output.isEmpty()) {
                continue;
            }
            for (StoredItemStack existing : base) {
                if (ItemStack.isSameItemSameComponents(existing.getItemStack(), output)) {
                    continue outer; // already present in storage
                }
            }
            result.add(new StoredItemStack(output.copyWithCount(1), 0));
        }
        return result;
    }
}
