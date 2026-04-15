package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStatisticsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

public class AdvancedStatisticsScreen extends AbstractContainerScreen<AdvancedStatisticsMenu> {

    private enum Tab { STORAGE, POWER, CRAFTING }

    private Tab activeTab = Tab.STORAGE;

    private static final int BG_COLOR     = 0xFF2B2B2B;
    private static final int TAB_INACTIVE = 0xFF444444;
    private static final int TAB_ACTIVE   = 0xFF666666;
    private static final int TAB_TEXT     = 0xFFFFFFFF;
    private static final int LABEL_COLOR  = 0xFFAAAAAA;
    private static final int VALUE_COLOR  = 0xFFFFFFFF;
    private static final int GREEN        = 0xFF55FF55;
    private static final int ORANGE       = 0xFFFFAA00;
    private static final int RED          = 0xFFFF5555;

    public AdvancedStatisticsScreen(AdvancedStatisticsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight + 100; // push player inventory label off-screen
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, BG_COLOR);
        graphics.fill(x, y + 14, x + imageWidth, y + 28, 0xFF1A1A1A);
        drawTab(graphics, x + 2,   y + 15, 58, 12, Tab.STORAGE,  "gui.s3_advanced.stats.tab.storage",  mouseX, mouseY);
        drawTab(graphics, x + 62,  y + 15, 58, 12, Tab.POWER,    "gui.s3_advanced.stats.tab.power",    mouseX, mouseY);
        drawTab(graphics, x + 122, y + 15, 58, 12, Tab.CRAFTING, "gui.s3_advanced.stats.tab.crafting", mouseX, mouseY);
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int w, int h,
                         Tab tab, String langKey, int mouseX, int mouseY) {
        boolean isActive = activeTab == tab;
        boolean hovered  = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = isActive ? TAB_ACTIVE : (hovered ? 0xFF555555 : TAB_INACTIVE);
        graphics.fill(x, y, x + w, y + h, bg);
        Component label = Component.translatable(langKey);
        int textX = x + (w - font.width(label)) / 2;
        graphics.drawString(font, label, textX, y + 2, TAB_TEXT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, 6, 4, TAB_TEXT, false);
        int contentY = 32;
        switch (activeTab) {
            case STORAGE  -> renderStorageTab(graphics, contentY, mouseX, mouseY);
            case POWER    -> renderPowerTab(graphics, contentY);
            case CRAFTING -> renderCraftingTab(graphics, contentY);
        }
    }

    private void renderStorageTab(GuiGraphics graphics, int startY, int mouseX, int mouseY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;
        long total = m.totalItems;
        long cap   = m.capacity;
        float ratio = cap > 0 ? (float) total / cap : 0f;
        int barColor = ratio < 0.75f ? GREEN : (ratio < 0.9f ? ORANGE : RED);
        String itemsStr = total + " / " + cap;
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.items_stored"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, itemsStr, imageWidth - 6 - font.width(itemsStr), y, VALUE_COLOR, false);
        y += 10;
        int barW = imageWidth - 12;
        graphics.fill(6, y, 6 + barW, y + 4, 0xFF111111);
        graphics.fill(6, y, 6 + (int)(barW * Math.min(ratio, 1f)), y + 4, barColor);
        y += 8;

        String types = String.valueOf(m.uniqueTypes);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.unique_types"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, types, imageWidth - 6 - font.width(types), y, VALUE_COLOR, false);
        y += 10;

        String freeStr = String.valueOf(m.getFreeSpace());
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.free_space"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, freeStr, imageWidth - 6 - font.width(freeStr), y, VALUE_COLOR, false);
        y += 10;

        String blocksStr = String.valueOf(m.blockCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.block_count"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, blocksStr, imageWidth - 6 - font.width(blocksStr), y, VALUE_COLOR, false);
        y += 12;

        for (Map.Entry<String, Integer> entry : m.tierBreakdown.entrySet()) {
            String line = entry.getKey() + ": " + entry.getValue();
            graphics.drawString(font, line, 10, y, LABEL_COLOR, false);
            y += 9;
        }
        y += 3;

        if (!m.presentComponents.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.components"), 6, y, LABEL_COLOR, false);
            y += 9;
            for (String comp : m.presentComponents) {
                graphics.drawString(font, "  " + comp, 10, y, 0xFF88AAFF, false);
                y += 9;
            }
        }
    }

    private void renderPowerTab(GuiGraphics graphics, int startY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;

        String genStr = m.generationRate + " FE/t";
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.generation"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, genStr, imageWidth - 6 - font.width(genStr), y, GREEN, false);
        y += 10;

        String conStr = m.consumptionRate + " FE/t";
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.consumption"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, conStr, imageWidth - 6 - font.width(conStr), y, RED, false);
        y += 10;

        int net = m.getNetBalance();
        String netStr = (net >= 0 ? "+" : "") + net + " FE/t";
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.net_balance"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, netStr, imageWidth - 6 - font.width(netStr), y, net >= 0 ? GREEN : RED, false);
        y += 12;

        long stored = m.energyStored;
        long maxCap = m.energyCapacity;
        float ratio = maxCap > 0 ? (float) stored / maxCap : 0f;
        String energyStr = stored + " / " + maxCap + " FE";
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.energy_stored"), 6, y, LABEL_COLOR, false);
        y += 9;
        graphics.fill(6, y, 6 + imageWidth - 12, y + 4, 0xFF111111);
        graphics.fill(6, y, 6 + (int)((imageWidth - 12) * Math.min(ratio, 1f)), y + 4, 0xFF33AAFF);
        y += 6;
        graphics.drawString(font, energyStr, 6, y, VALUE_COLOR, false);
    }

    private void renderCraftingTab(GuiGraphics graphics, int startY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;

        String rmbStr = String.valueOf(m.rmbCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.rmb_count"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, rmbStr, imageWidth - 6 - font.width(rmbStr), y, VALUE_COLOR, false);
        y += 10;

        String slotsStr = m.usedPatternSlots + " / " + m.totalPatternSlots;
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.slots_used"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, slotsStr, imageWidth - 6 - font.width(slotsStr), y, VALUE_COLOR, false);
        y += 12;

        String acStr = String.valueOf(m.autoCrafterCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.ac_count"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, acStr, imageWidth - 6 - font.width(acStr), y, VALUE_COLOR, false);
        y += 10;

        String pairedStr = String.valueOf(m.pairedCrafterCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.paired"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, pairedStr, imageWidth - 6 - font.width(pairedStr), y, VALUE_COLOR, false);
        y += 12;

        String miStr = String.valueOf(m.miCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.mi_count"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, miStr, imageWidth - 6 - font.width(miStr), y, VALUE_COLOR, false);
        y += 10;

        String activeVal = String.valueOf(m.activeMiCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.mi_active"), 10, y, LABEL_COLOR, false);
        graphics.drawString(font, activeVal, imageWidth - 6 - font.width(activeVal), y, VALUE_COLOR, false);
        y += 9;
        String idleVal = String.valueOf(m.getIdleMiCount());
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.mi_idle"), 10, y, LABEL_COLOR, false);
        graphics.drawString(font, idleVal, imageWidth - 6 - font.width(idleVal), y, VALUE_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            if (isInTab(mouseX, mouseY, x + 2,   y + 15, 58, 12)) { activeTab = Tab.STORAGE;  return true; }
            if (isInTab(mouseX, mouseY, x + 62,  y + 15, 58, 12)) { activeTab = Tab.POWER;    return true; }
            if (isInTab(mouseX, mouseY, x + 122, y + 15, 58, 12)) { activeTab = Tab.CRAFTING; return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInTab(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
