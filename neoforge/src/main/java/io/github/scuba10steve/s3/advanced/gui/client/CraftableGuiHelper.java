package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.network.CraftableSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Stateless helper for craftable-item GUI logic shared by both storage display screens.
 */
public class CraftableGuiHelper {

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
     * Returns the PatternKey for the first entry whose output matches the item,
     * or empty if the item is not craftable.
     */
    public static Optional<PatternKey> findPatternKey(ItemStack item, List<CraftableSyncPacket.Entry> entries) {
        for (CraftableSyncPacket.Entry e : entries) {
            if (ItemStack.isSameItemSameComponents(e.output(), item)) {
                return Optional.of(e.patternKey());
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
}
