package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageCoreMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedStorageCoreScreen extends AbstractContainerScreen<AdvancedStorageCoreMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("s3_advanced", "textures/gui/info_panel.png");

    public AdvancedStorageCoreScreen(AdvancedStorageCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 96;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Energy bar background
        g.fill(x + 8, y + 32, x + 168, y + 44, 0xFF373737);
        // Energy bar fill — amber colour for a consumer
        int fill = 160 * menu.getEnergyPercent() / 100;
        if (fill > 0) {
            g.fill(x + 8, y + 32, x + 8 + fill, y + 44, 0xFFB87A00);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);

        // Energy values
        String energyText = menu.getEnergyStored() + " / " + menu.getCapacity() + " FE";
        g.drawString(font, energyText, imageWidth / 2 - font.width(energyText) / 2, 48, 0x404040, false);

        // Drain and status
        String drainText = "Consuming: " + menu.getEnergyPerTick() + " FE/t";
        g.drawString(font, drainText, imageWidth / 2 - font.width(drainText) / 2, 62, 0x404040, false);

        boolean powered = menu.isPowered();
        String statusText = powered ? "Status: Powered" : "Status: Unpowered";
        int statusColor = powered ? 0x1A9E3C : 0xCC2222;
        g.drawString(font, statusText, imageWidth / 2 - font.width(statusText) / 2, 76, statusColor, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
