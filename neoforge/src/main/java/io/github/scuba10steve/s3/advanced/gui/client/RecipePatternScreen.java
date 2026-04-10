package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public class RecipePatternScreen extends AbstractContainerScreen<RecipePatternMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        StevesAdvancedStorage.MOD_ID, "textures/gui/recipe_pattern.png");

    private Button saveButton;
    private Button clearButton;
    private Button prevButton;
    private Button nextButton;
    private Button backButton;
    private Button assignButton;

    /** Whether the crafter-picker overlay is open. */
    private boolean pickerOpen;

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

        // Assign button — top-right area, left of the back button
        assignButton = addRenderableWidget(Button.builder(
            Component.translatable("gui.s3_advanced.assign"),
            btn -> pickerOpen = !pickerOpen)
            .bounds(x + 108, y + 4, 48, 12).build());
        assignButton.visible = false;

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
        assignButton.visible = !menu.getCrafterPositions().isEmpty();
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
        if (pickerOpen) {
            renderCrafterPicker(graphics, mouseX, mouseY);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Renders a floating panel listing available Auto-Crafters.
     * Player clicks one to send an AssignPatternPacket.
     */
    private void renderCrafterPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        List<BlockPos> crafters = menu.getCrafterPositions();
        int panelW = 100;
        int panelH = 12 + crafters.size() * 12;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF1A1A1A);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xFF2D2D2D);
        graphics.drawString(this.font, "Assign to:", panelX + 4, panelY + 2, 0xFFFFFFFF, false);

        for (int i = 0; i < crafters.size(); i++) {
            BlockPos cp = crafters.get(i);
            int entryY = panelY + 12 + i * 12;
            String label = cp.getX() + ", " + cp.getY() + ", " + cp.getZ();
            boolean hovered = isInBounds(mouseX, mouseY, panelX + 2, entryY, panelW - 4, 11);
            if (hovered) {
                graphics.fill(panelX + 2, entryY, panelX + panelW - 2, entryY + 11, 0xFF3A6A3A);
            }
            graphics.drawString(this.font, label, panelX + 4, entryY + 2, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pickerOpen && button == 0) {
            List<BlockPos> crafters = menu.getCrafterPositions();
            int panelW = 100;
            int panelH = 12 + crafters.size() * 12;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;

            for (int i = 0; i < crafters.size(); i++) {
                int entryY = panelY + 12 + i * 12;
                if (isInBounds(mouseX, mouseY, panelX + 2, entryY, panelW - 4, 11)) {
                    // TODO Task 5: send assign packet when AutoCrafterMenu is rewritten
                    pickerOpen = false;
                    return true;
                }
            }
            // Click outside picker — close it.
            pickerOpen = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
