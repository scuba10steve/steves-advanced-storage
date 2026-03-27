package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RecipePatternScreen extends AbstractContainerScreen<RecipePatternMenu> {

    private Button saveButton;
    private Button prevButton;
    private Button nextButton;
    private Button backButton;

    public RecipePatternScreen(RecipePatternMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Back button — top-right corner of the panel
        backButton = addRenderableWidget(Button.builder(
            Component.literal("×"),
            btn -> sendButtonClick(3))
            .bounds(x + imageWidth - 16, y + 4, 12, 12).build());

        // Save button
        saveButton = addRenderableWidget(Button.builder(
            Component.translatable("gui.s3_advanced.save"),
            btn -> sendButtonClick(0))
            .bounds(x + 108, y + 55, 50, 14).build());

        // Prev/Next recipe buttons (only visible when multiple recipes match)
        prevButton = addRenderableWidget(Button.builder(
            Component.literal("<"),
            btn -> sendButtonClick(1))
            .bounds(x + 108, y + 17, 20, 14).build());

        nextButton = addRenderableWidget(Button.builder(
            Component.literal(">"),
            btn -> sendButtonClick(2))
            .bounds(x + 150, y + 17, 20, 14).build());
    }

    /** Sends a menu button click to the server. */
    private void sendButtonClick(int buttonId) {
        if (this.minecraft != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean multiMatch = menu.getMatchCount() > 1;
        prevButton.visible = multiMatch;
        nextButton.visible = multiMatch;
        saveButton.active = menu.getMatchCount() > 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF8B8B8B);

        // 3×3 ingredient grid slot backgrounds
        for (int i = 0; i < 9; i++) {
            int row = i / 3, col = i % 3;
            int sx = x + 26 + col * 18;
            int sy = y + 17 + row * 18;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }

        // Output slot — dark outer border + white inner border to distinguish from ingredient slots
        graphics.fill(x + 122, y + 33, x + 142, y + 53, 0xFF373737);
        graphics.fill(x + 123, y + 34, x + 141, y + 52, 0xFFFFFFFF);
        graphics.fill(x + 124, y + 35, x + 140, y + 51, 0xFF8B8B8B);

        // Player inventory (3 rows × 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 7 + col * 18;
                int sy = y + 83 + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
            }
        }
        // Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18;
            int sy = y + 141;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }

        // Match count label (when multiple recipes found)
        int matchCount = menu.getMatchCount();
        if (matchCount > 1) {
            String label = (menu.getSelectedIndex() + 1) + "/" + matchCount;
            graphics.drawString(this.font, label, x + 133, y + 6, 0x404040, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
