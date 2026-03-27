package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.gui.server.RecipeMemoryBoxMenu;
import io.github.scuba10steve.s3.advanced.network.AssignPatternPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RecipeMemoryBoxScreen extends AbstractContainerScreen<RecipeMemoryBoxMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        StevesAdvancedStorage.MOD_ID, "textures/gui/recipe_memory_box.png");

    /** Index of slot whose Assign button was clicked; -1 means picker is hidden. */
    private int assigningSlot = -1;

    public RecipeMemoryBoxScreen(RecipeMemoryBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
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
        renderAssignButtons(graphics, mouseX, mouseY);
        if (assigningSlot >= 0) {
            renderCrafterPicker(graphics, mouseX, mouseY);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Renders a small green "A" indicator in the bottom-right corner of each
     * populated pattern slot, when the multiblock contains at least one Auto-Crafter.
     */
    private void renderAssignButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCrafterPositions().isEmpty()) return;
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;
        for (int i = 0; i < RecipeMemoryBoxMenu.PATTERN_SLOT_COUNT; i++) {
            var slot = this.menu.slots.get(i);
            if (slot.getItem().isEmpty()) continue;
            int btnX = guiX + slot.x + 9;
            int btnY = guiY + slot.y + 9;
            int bgColor = isInBounds(mouseX, mouseY, btnX, btnY, 7, 7) ? 0xFF00CC00 : 0xFF007700;
            graphics.fill(btnX, btnY, btnX + 7, btnY + 7, bgColor);
            graphics.drawString(this.font, "A", btnX + 1, btnY + 1, 0xFFFFFFFF, false);
        }
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
        graphics.drawString(this.font, "Assign to:", panelX + 4, panelY + 2, 0xFFFFFF, false);

        for (int i = 0; i < crafters.size(); i++) {
            BlockPos cp = crafters.get(i);
            int entryY = panelY + 12 + i * 12;
            String label = cp.getX() + ", " + cp.getY() + ", " + cp.getZ();
            boolean hovered = isInBounds(mouseX, mouseY, panelX + 2, entryY, panelW - 4, 11);
            if (hovered) graphics.fill(panelX + 2, entryY, panelX + panelW - 2, entryY + 11, 0xFF3A6A3A);
            graphics.drawString(this.font, label, panelX + 4, entryY + 2, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;

        // If picker is open, handle its clicks before anything else.
        if (assigningSlot >= 0) {
            List<BlockPos> crafters = menu.getCrafterPositions();
            int panelW = 100;
            int panelH = 12 + crafters.size() * 12;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;

            for (int i = 0; i < crafters.size(); i++) {
                int entryY = panelY + 12 + i * 12;
                if (isInBounds(mouseX, mouseY, panelX + 2, entryY, panelW - 4, 11)) {
                    PatternKey key = new PatternKey(menu.getRmbPos(), assigningSlot);
                    PacketDistributor.sendToServer(new AssignPatternPacket(crafters.get(i), key));
                    assigningSlot = -1;
                    return true;
                }
            }
            // Click outside picker — close it.
            assigningSlot = -1;
            return true;
        }

        // Check Assign button hits (consume before slot click reaches the menu).
        if (!menu.getCrafterPositions().isEmpty()) {
            for (int i = 0; i < RecipeMemoryBoxMenu.PATTERN_SLOT_COUNT; i++) {
                var slot = this.menu.slots.get(i);
                if (slot.getItem().isEmpty()) continue;
                int btnX = guiX + slot.x + 9;
                int btnY = guiY + slot.y + 9;
                if (isInBounds(mouseX, mouseY, btnX, btnY, 7, 7)) {
                    assigningSlot = i;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
