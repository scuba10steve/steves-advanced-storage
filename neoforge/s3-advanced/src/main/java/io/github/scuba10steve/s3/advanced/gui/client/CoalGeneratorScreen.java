package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.gui.server.CoalGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("s3_advanced", "textures/gui/coal_generator.png");

    // Sprite positions in the texture sheet (to the right of the 176x166 panel)
    private static final int FLAME_U = 176;
    private static final int FLAME_V = 0;
    private static final int FLAME_W = 14;
    private static final int FLAME_H = 13;

    private static final int ENERGY_BAR_U = 176;
    private static final int ENERGY_BAR_V = 14;
    private static final int ENERGY_BAR_W = 6;
    private static final int ENERGY_BAR_H = 50;

    // Positions relative to the container panel
    private static final int FUEL_SLOT_X = 80;
    private static final int FLAME_X = 60; // left of fuel slot, same row
    private static final int FLAME_Y = 43;
    private static final int ENERGY_BAR_X = 152;
    private static final int ENERGY_BAR_Y = 18;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // imageHeight stays at default 166 — non-standard heights break slot rendering
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Draw main panel background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Draw flame burn progress (14 wide, up to 13 tall)
        if (menu.isLit()) {
            int progress = menu.getBurnProgress();
            guiGraphics.blit(TEXTURE, x + FLAME_X, y + FLAME_Y + (FLAME_H - progress),
                FLAME_U, FLAME_V + (FLAME_H - progress), FLAME_W, progress);
        }

        // Draw energy bar (filled from bottom up)
        int energyPercent = menu.getEnergyPercent();
        int filledHeight = ENERGY_BAR_H * energyPercent / 100;
        guiGraphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y + (ENERGY_BAR_H - filledHeight),
            ENERGY_BAR_U, ENERGY_BAR_V + (ENERGY_BAR_H - filledHeight), ENERGY_BAR_W, filledHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(this.font, this.title,
            this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0x404040, false);

        // Energy stored — above the fuel slot (slot is now at y=50)
        String energyText = menu.getEnergyStored() + " / " + menu.getCapacity() + " FE";
        guiGraphics.drawString(this.font, energyText,
            this.imageWidth / 2 - this.font.width(energyText) / 2, 18, 0x404040, false);

        // Burn status — one line below energy text, both comfortably above slot
        boolean lit = menu.isLit();
        String statusText = lit ? "Burning: " + menu.getGenerationRate() + " FE/t" : "Idle";
        int statusColor = lit ? 0xFFA500 : 0x888888;
        guiGraphics.drawString(this.font, statusText,
            this.imageWidth / 2 - this.font.width(statusText) / 2, 29, statusColor, false);

        // Player inventory label — standard position, now uncontested
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
            this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
