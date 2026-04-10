package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RecipePatternScreen extends AbstractContainerScreen<RecipePatternMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        StevesAdvancedStorage.MOD_ID, "textures/gui/recipe_pattern.png");

    private Button saveButton;
    private Button clearButton;
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
            .bounds(x + 160, y + 4, 12, 12).build());

        // Prev/Next recipe buttons (only visible when multiple recipes match)
        prevButton = addRenderableWidget(Button.builder(
            Component.literal("<"),
            btn -> sendButtonClick(1))
            .bounds(x + 108, y + 17, 20, 14).build());
        prevButton.visible = false;

        nextButton = addRenderableWidget(Button.builder(
            Component.literal(">"),
            btn -> sendButtonClick(2))
            .bounds(x + 150, y + 17, 20, 14).build());
        nextButton.visible = false;

        // Save and Clear buttons — side by side, compact height
        saveButton = addRenderableWidget(Button.builder(
            Component.translatable("gui.s3_advanced.save"),
            btn -> sendButtonClick(0))
            .bounds(x + 108, y + 55, 30, 12).build());

        clearButton = addRenderableWidget(Button.builder(
            Component.translatable("gui.s3_advanced.clear"),
            btn -> sendButtonClick(4))
            .bounds(x + 140, y + 55, 36, 12).build());
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
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Match count label (dynamic — not part of the static texture)
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

    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
