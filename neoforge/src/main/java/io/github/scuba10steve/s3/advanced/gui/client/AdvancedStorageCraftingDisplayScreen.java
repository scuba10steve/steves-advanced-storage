package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.crafting.CrafterSlot;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageCraftingDisplayMenu;
import io.github.scuba10steve.s3.advanced.network.CraftableSyncPacket;
import io.github.scuba10steve.s3.gui.client.StorageCoreCraftingScreen;
import io.github.scuba10steve.s3.storage.StoredItemStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdvancedStorageCraftingDisplayScreen extends StorageCoreCraftingScreen {

    private CraftQuantityOverlay quantityOverlay;

    public AdvancedStorageCraftingDisplayScreen(AdvancedStorageCraftingDisplayMenu menu,
                                                 Inventory playerInventory,
                                                 Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public List<StoredItemStack> getDisplayItems() {
        return CraftableGuiHelper.withCraftableItems(
            super.getDisplayItems(), CraftableClientData.get(menu.getPos()));
    }

    @Override
    protected void updateFilteredItems() {
        super.updateFilteredItems();
        filteredItems = CraftableGuiHelper.withCraftableItems(
            filteredItems, CraftableClientData.get(menu.getPos()));
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        CraftableGuiHelper.appendCraftableTooltip(
            stack, CraftableClientData.get(menu.getPos()), tooltip);
        return tooltip;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (quantityOverlay != null) {
            quantityOverlay.render(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (quantityOverlay != null) {
            return quantityOverlay.mouseClicked(mouseX, mouseY, button);
        }
        if (CraftableGuiHelper.isCraftTrigger(button) && menu.getCarried().isEmpty()) {
            Integer slotIdx = getSlotAt((int) mouseX, (int) mouseY);
            if (slotIdx != null) {
                List<StoredItemStack> items = getDisplayItems();
                if (slotIdx < items.size()) {
                    ItemStack stack = items.get(slotIdx).getItemStack();
                    List<CraftableSyncPacket.Entry> entries = CraftableClientData.get(menu.getPos());
                    Optional<CrafterSlot> key = CraftableGuiHelper.findCrafterSlot(stack, entries);
                    if (key.isPresent()) {
                        quantityOverlay = new CraftQuantityOverlay(
                            font, width, height, stack, key.get(), menu.getPos(),
                            () -> quantityOverlay = null);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (quantityOverlay != null) {
            return quantityOverlay.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (quantityOverlay != null) {
            return quantityOverlay.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }
}
