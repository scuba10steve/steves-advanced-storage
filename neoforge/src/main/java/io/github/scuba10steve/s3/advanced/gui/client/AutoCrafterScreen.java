package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.gui.server.AutoCrafterMenu;
import io.github.scuba10steve.s3.advanced.network.UnassignPatternPacket;
import io.github.scuba10steve.s3.advanced.network.UpdatePatternConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {

    /** Visible rows in the assignment list area. */
    private static final int VISIBLE_ROWS = 4;
    private static final int ROW_HEIGHT = 22;
    /** Y offset of the first row within the GUI background (below title). */
    private static final int LIST_TOP = 18;
    /** X of the list area within the GUI background. */
    private static final int LIST_LEFT = 8;

    private int scrollOffset = 0;

    /** Snapshot of assignments, updated from menu and on optimistic client updates. */
    private final List<Map.Entry<PatternKey, PerPatternConfig>> rows = new ArrayList<>();

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        refreshRows();
    }

    private void refreshRows() {
        rows.clear();
        rows.addAll(menu.getAssignments().entrySet());
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - VISIBLE_ROWS)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderRows(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int dataIndex = i + scrollOffset;
            if (dataIndex >= rows.size()) break;

            Map.Entry<PatternKey, PerPatternConfig> entry = rows.get(dataIndex);
            PerPatternConfig cfg = entry.getValue();
            int rowX = guiX + LIST_LEFT;
            int rowY = guiY + LIST_TOP + i * ROW_HEIGHT;

            // Row background
            graphics.fill(rowX, rowY, rowX + 160, rowY + ROW_HEIGHT - 1, 0x22FFFFFF);

            // Output item icon placeholder
            graphics.fill(rowX + 2, rowY + 3, rowX + 18, rowY + 19, 0xFF888888);

            // Auto-buffer toggle
            String autoLabel = cfg.autoEnabled() ? "Auto: ON" : "Auto: OFF";
            int autoColor = cfg.autoEnabled() ? 0xFF00FF00 : 0xFFAAAAAA;
            graphics.drawString(this.font, autoLabel, rowX + 22, rowY + 3, autoColor, false);

            // Min buffer display
            graphics.drawString(this.font, "Min: " + cfg.minimumBuffer(), rowX + 22, rowY + 13, 0xFFFFFFFF, false);

            // Remove indicator
            graphics.drawString(this.font, "[X]", rowX + 145, rowY + 7, 0xFFFF4444, false);
        }

        // Scroll indicator (if list overflows)
        if (rows.size() > VISIBLE_ROWS) {
            int total = rows.size();
            int scrollbarHeight = (int) ((float) VISIBLE_ROWS / total * (VISIBLE_ROWS * ROW_HEIGHT));
            int scrollbarY = guiY + LIST_TOP + (int) ((float) scrollOffset / total * (VISIBLE_ROWS * ROW_HEIGHT));
            graphics.fill(guiX + 168, guiY + LIST_TOP, guiX + 170, guiY + LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT, 0xFF444444);
            graphics.fill(guiX + 168, scrollbarY, guiX + 170, scrollbarY + scrollbarHeight, 0xFFCCCCCC);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Solid dark background (no custom texture needed yet)
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2D2D2D);
        // Player inventory area — lighter fill
        graphics.fill(x + 7, y + 83, x + 169, y + 161, 0xFF3A3A3A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xAAAAAA, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int dataIndex = i + scrollOffset;
            if (dataIndex >= rows.size()) break;

            Map.Entry<PatternKey, PerPatternConfig> entry = rows.get(dataIndex);
            PatternKey key = entry.getKey();
            PerPatternConfig cfg = entry.getValue();
            int rowX = guiX + LIST_LEFT;
            int rowY = guiY + LIST_TOP + i * ROW_HEIGHT;

            // Auto toggle — click anywhere on the auto label area
            if (isInBounds(mouseX, mouseY, rowX + 22, rowY + 3, 80, 10)) {
                boolean newAuto = !cfg.autoEnabled();
                // Optimistic update
                rows.set(dataIndex, Map.entry(key, new PerPatternConfig(newAuto, cfg.minimumBuffer())));
                PacketDistributor.sendToServer(new UpdatePatternConfigPacket(
                    menu.getBlockPos(), key, newAuto, cfg.minimumBuffer()));
                return true;
            }

            // Min buffer decrement (click on "Min: N" label region left half)
            if (isInBounds(mouseX, mouseY, rowX + 22, rowY + 13, 20, 10)) {
                int newMin = Math.max(0, cfg.minimumBuffer() - 1);
                rows.set(dataIndex, Map.entry(key, new PerPatternConfig(cfg.autoEnabled(), newMin)));
                PacketDistributor.sendToServer(new UpdatePatternConfigPacket(
                    menu.getBlockPos(), key, cfg.autoEnabled(), newMin));
                return true;
            }

            // Min buffer increment (click on region right of "Min: N")
            if (isInBounds(mouseX, mouseY, rowX + 22 + 40, rowY + 13, 20, 10)) {
                int newMin = cfg.minimumBuffer() + 1;
                rows.set(dataIndex, Map.entry(key, new PerPatternConfig(cfg.autoEnabled(), newMin)));
                PacketDistributor.sendToServer(new UpdatePatternConfigPacket(
                    menu.getBlockPos(), key, cfg.autoEnabled(), newMin));
                return true;
            }

            // Remove button — [X] label area
            if (isInBounds(mouseX, mouseY, rowX + 145, rowY + 3, 18, 16)) {
                rows.remove(dataIndex);
                scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - VISIBLE_ROWS)));
                PacketDistributor.sendToServer(new UnassignPatternPacket(menu.getBlockPos(), key));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, rows.size() - VISIBLE_ROWS);
        if (maxScroll > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
