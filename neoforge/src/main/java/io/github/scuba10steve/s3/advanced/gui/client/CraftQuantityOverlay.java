package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.crafting.CrafterSlot;
import io.github.scuba10steve.s3.advanced.network.CraftRequestPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * In-screen overlay for entering a craft quantity.
 * Rendered and driven by input delegates from the parent storage screen.
 * Does NOT extend Screen — opening it does not trigger screen replacement,
 * so the container menu stays open on the server.
 */
public class CraftQuantityOverlay {

    private static final int PANEL_W = 140;
    private static final int PANEL_H = 72;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;

    private final Font font;
    private final int screenWidth;
    private final int screenHeight;
    private final int panelX;
    private final int panelY;
    private final ItemStack item;
    private final CrafterSlot crafterSlot;
    private final BlockPos corePos;
    private final Runnable onClose;

    private final EditBox quantityBox;
    private final Button confirmButton;
    private final Button cancelButton;

    public CraftQuantityOverlay(Font font, int screenWidth, int screenHeight,
                                 ItemStack item, CrafterSlot crafterSlot, BlockPos corePos,
                                 Runnable onClose) {
        this.font = font;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.item = item.copy();
        this.crafterSlot = crafterSlot;
        this.corePos = corePos;
        this.onClose = onClose;

        this.panelX = (screenWidth - PANEL_W) / 2;
        this.panelY = (screenHeight - PANEL_H) / 2;

        this.quantityBox = new EditBox(font, panelX + 10, panelY + 34, 80, 14,
            Component.translatable("gui.s3_advanced.craft_quantity"));
        this.quantityBox.setMaxLength(4);
        this.quantityBox.setValue("1");
        this.quantityBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        this.quantityBox.setFocused(true);

        this.confirmButton = Button.builder(
            Component.translatable("gui.s3_advanced.craft_confirm"),
            btn -> confirm())
            .bounds(panelX + 10, panelY + 52, 55, 14).build();

        this.cancelButton = Button.builder(
            Component.translatable("gui.s3_advanced.craft_cancel"),
            btn -> onClose.run())
            .bounds(panelX + 75, panelY + 52, 55, 14).build();
    }

    private void confirm() {
        int qty = parseQuantity();
        PacketDistributor.sendToServer(new CraftRequestPacket(corePos, crafterSlot, qty));
        onClose.run();
    }

    private int parseQuantity() {
        try {
            return Math.max(1, Math.min(9999, Integer.parseInt(quantityBox.getValue())));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dark overlay
        g.fill(0, 0, screenWidth, screenHeight, 0x80000000);

        // Panel background
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF1A1A1A);
        g.fill(panelX + 1, panelY + 1, panelX + PANEL_W - 1, panelY + PANEL_H - 1, 0xFF2D2D2D);

        // Item icon + name
        g.renderItem(item, panelX + 10, panelY + 10);
        g.drawString(font, item.getHoverName(), panelX + 30, panelY + 14, 0xFFFFFF, false);

        // Quantity label
        g.drawString(font, Component.translatable("gui.s3_advanced.craft_quantity"),
            panelX + 10, panelY + 24, 0xAAAAAA, false);

        quantityBox.render(g, mouseX, mouseY, partialTick);
        confirmButton.render(g, mouseX, mouseY, partialTick);
        cancelButton.render(g, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (quantityBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (confirmButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (cancelButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        // Click outside panel dismisses
        if (mouseX < panelX || mouseX > panelX + PANEL_W || mouseY < panelY || mouseY > panelY + PANEL_H) {
            onClose.run();
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            onClose.run();
            return true;
        }
        if (keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER) {
            confirm();
            return true;
        }
        return quantityBox.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char c, int modifiers) {
        return quantityBox.charTyped(c, modifiers);
    }
}
