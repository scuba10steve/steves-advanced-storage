package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStatisticsMenu;
import io.github.scuba10steve.s3.util.CountFormatter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdvancedStatisticsScreen extends AbstractContainerScreen<AdvancedStatisticsMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "textures/gui/advanced_statistics.png");

    private enum Tab { GENERAL, STORAGE, POWER, CRAFTING }

    private Tab activeTab = Tab.GENERAL;

    private static final int LABEL_COLOR = 0x404040;
    private static final int VALUE_COLOR = 0x202020;
    private static final int GREEN       = 0x1A9E3C;
    private static final int ORANGE      = 0xFF8800;
    private static final int RED         = 0xCC2222;
    private static final int BLUE        = 0x2255CC;

    private final List<ComponentIcon> componentIcons = new ArrayList<>();

    public AdvancedStatisticsScreen(AdvancedStatisticsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight + 100;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        drawTab(graphics, x + 4,   y + 14, 40, 12, Tab.GENERAL,  "gui.s3_advanced.stats.tab.general",  mouseX, mouseY);
        drawTab(graphics, x + 46,  y + 14, 40, 12, Tab.STORAGE,  "gui.s3_advanced.stats.tab.storage",  mouseX, mouseY);
        drawTab(graphics, x + 88,  y + 14, 40, 12, Tab.POWER,    "gui.s3_advanced.stats.tab.power",    mouseX, mouseY);
        drawTab(graphics, x + 130, y + 14, 40, 12, Tab.CRAFTING, "gui.s3_advanced.stats.tab.crafting", mouseX, mouseY);
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int w, int h,
                         Tab tab, String langKey, int mouseX, int mouseY) {
        boolean isActive = activeTab == tab;
        boolean hovered  = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = isActive ? 0xFFD0D0D0 : (hovered ? 0xFFBBBBBB : 0xFFAAAAAA);
        graphics.fill(x, y, x + w, y + h, bg);
        Component label = Component.translatable(langKey);
        int textX = x + (w - font.width(label)) / 2;
        graphics.drawString(font, label, textX, y + 2, LABEL_COLOR, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title,
            this.imageWidth / 2 - font.width(this.title) / 2, 4, LABEL_COLOR, false);
        int contentY = 30;
        switch (activeTab) {
            case GENERAL  -> renderGeneralTab(graphics, contentY, mouseX, mouseY);
            case STORAGE  -> renderStorageTab(graphics, contentY);
            case POWER    -> renderPowerTab(graphics, contentY);
            case CRAFTING -> renderCraftingTab(graphics, contentY);
        }
        graphics.drawString(font, "?", imageWidth - 14, 4, 0x808080, false);
    }

    private void renderGeneralTab(GuiGraphics graphics, int startY, int mouseX, int mouseY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;
        boolean exact = hasShiftDown();
        int bottom = imageHeight - 6;

        String blocksStr = String.valueOf(m.blockCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.block_count"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, blocksStr, imageWidth - 6 - font.width(blocksStr), y, VALUE_COLOR, false);
        y += 11;

        for (Map.Entry<String, Integer> entry : m.tierBreakdown.entrySet()) {
            if (y >= bottom) {
                break;
            }
            String line = capitalize(entry.getKey()) + ": " + entry.getValue();
            graphics.drawString(font, line, 16, y, VALUE_COLOR, false);
            y += 10;
        }
        y += 4;

        if (y < bottom) {
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.components"), 6, y, LABEL_COLOR, false);
            y += 11;
        }

        componentIcons.clear();
        if (!m.presentComponents.isEmpty() && y + 16 <= bottom) {
            int iconX = 10;
            for (String comp : m.presentComponents) {
                if (iconX + 16 > imageWidth - 6) {
                    break;
                }
                ItemStack icon = getItemForComponent(comp);
                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, iconX, y);
                    componentIcons.add(new ComponentIcon(iconX, y, icon.getHoverName()));
                    iconX += 18;
                }
            }
        } else if (m.presentComponents.isEmpty() && y < bottom) {
            graphics.drawString(font, "None", 10, y + 2, VALUE_COLOR, false);
        }
    }

    private void renderStorageTab(GuiGraphics graphics, int startY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;
        boolean exact = hasShiftDown();
        int bottom = imageHeight - 6;

        long total = m.totalItems;
        long cap   = m.capacity;
        float ratio = cap > 0 ? (float) total / cap : 0f;
        int pct = (int)(ratio * 100);
        int barColor = pct < 75 ? GREEN : (pct < 90 ? ORANGE : RED);

        String itemsStr = formatVal(total, exact) + " / " + formatVal(cap, exact);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.items_stored"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, itemsStr, imageWidth - 6 - font.width(itemsStr), y, VALUE_COLOR, false);
        y += 10;

        int barW = imageWidth - 12;
        graphics.fill(6, y, 6 + barW, y + 6, 0xFF555555);
        int filled = (int)(barW * Math.min(ratio, 1f));
        if (filled > 0) {
            graphics.fill(6, y, 6 + filled, y + 6, barColor);
        }
        String pctStr = pct + "%";
        graphics.drawString(font, pctStr, 6 + barW - font.width(pctStr), y + 7, VALUE_COLOR, false);
        y += 18;

        if (y < bottom) {
            String types = String.valueOf(m.uniqueTypes);
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.unique_types"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, types, imageWidth - 6 - font.width(types), y, VALUE_COLOR, false);
            y += 11;
        }

        if (y < bottom) {
            String freeStr = formatVal(m.getFreeSpace(), exact);
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.free_space"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, freeStr, imageWidth - 6 - font.width(freeStr), y, VALUE_COLOR, false);
        }
    }

    private void renderPowerTab(GuiGraphics graphics, int startY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;
        boolean exact = hasShiftDown();
        int bottom = imageHeight - 6;

        String genStr = formatVal(m.generationRate, exact) + " FE/t";
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.generation"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, genStr, imageWidth - 6 - font.width(genStr), y, GREEN, false);
        y += 11;

        if (y < bottom) {
            String conStr = formatVal(m.consumptionRate, exact) + " FE/t";
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.consumption"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, conStr, imageWidth - 6 - font.width(conStr), y, RED, false);
            y += 11;
        }

        if (y < bottom) {
            int net = m.getNetBalance();
            String netStr = (net >= 0 ? "+" : "") + net + " FE/t";
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.net_balance"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, netStr, imageWidth - 6 - font.width(netStr), y, net >= 0 ? GREEN : RED, false);
            y += 14;
        }

        if (y < bottom) {
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.energy_stored"), 6, y, LABEL_COLOR, false);
            y += 10;
            long stored = m.energyStored;
            long maxCap = m.energyCapacity;
            float ratio = maxCap > 0 ? (float) stored / maxCap : 0f;
            int barW = imageWidth - 12;
            graphics.fill(6, y, 6 + barW, y + 6, 0xFF555555);
            int filled = (int)(barW * Math.min(ratio, 1f));
            if (filled > 0) {
                graphics.fill(6, y, 6 + filled, y + 6, BLUE);
            }
            y += 9;
            if (y < bottom) {
                String energyStr = formatVal(stored, exact) + " / " + formatVal(maxCap, exact) + " FE";
                graphics.drawString(font, energyStr, 6, y, VALUE_COLOR, false);
            }
        }
    }

    private void renderCraftingTab(GuiGraphics graphics, int startY) {
        int y = startY;
        AdvancedStatisticsMenu m = this.menu;
        int bottom = imageHeight - 6;

        String rmbStr = String.valueOf(m.rmbCount);
        graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.rmb_count"), 6, y, LABEL_COLOR, false);
        graphics.drawString(font, rmbStr, imageWidth - 6 - font.width(rmbStr), y, VALUE_COLOR, false);
        y += 10;

        if (y < bottom) {
            String slotsStr = m.usedPatternSlots + " / " + m.totalPatternSlots;
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.slots_used"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, slotsStr, imageWidth - 6 - font.width(slotsStr), y, VALUE_COLOR, false);
            y += 13;
        }

        if (y < bottom) {
            String acStr = String.valueOf(m.autoCrafterCount);
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.ac_count"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, acStr, imageWidth - 6 - font.width(acStr), y, VALUE_COLOR, false);
            y += 10;
        }

        if (y < bottom) {
            String pairedStr = String.valueOf(m.pairedCrafterCount);
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.paired"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, pairedStr, imageWidth - 6 - font.width(pairedStr), y, VALUE_COLOR, false);
            y += 13;
        }

        if (y < bottom) {
            String miStr = String.valueOf(m.miCount);
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.mi_count"), 6, y, LABEL_COLOR, false);
            graphics.drawString(font, miStr, imageWidth - 6 - font.width(miStr), y, VALUE_COLOR, false);
            y += 10;
        }

        if (y < bottom) {
            String activeVal = String.valueOf(m.activeMiCount);
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.mi_active"), 10, y, LABEL_COLOR, false);
            graphics.drawString(font, activeVal, imageWidth - 6 - font.width(activeVal), y, VALUE_COLOR, false);
            y += 9;
        }

        if (y < bottom) {
            String idleVal = String.valueOf(m.getIdleMiCount());
            graphics.drawString(font, Component.translatable("gui.s3_advanced.stats.mi_idle"), 10, y, LABEL_COLOR, false);
            graphics.drawString(font, idleVal, imageWidth - 6 - font.width(idleVal), y, VALUE_COLOR, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        for (ComponentIcon icon : componentIcons) {
            if (relX >= icon.x && relX < icon.x + 16 && relY >= icon.y && relY < icon.y + 16) {
                graphics.renderTooltip(font, icon.name, mouseX, mouseY);
                return;
            }
        }

        int qX = imageWidth - 14;
        int qY = 4;
        if (relX >= qX && relX < qX + font.width("?") && relY >= qY && relY < qY + font.lineHeight) {
            graphics.renderTooltip(font, Component.literal("Hold Shift for exact counts"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            if (isInTab(mouseX, mouseY, x + 4,   y + 14, 40, 12)) { activeTab = Tab.GENERAL;  return true; }
            if (isInTab(mouseX, mouseY, x + 46,  y + 14, 40, 12)) { activeTab = Tab.STORAGE;  return true; }
            if (isInTab(mouseX, mouseY, x + 88,  y + 14, 40, 12)) { activeTab = Tab.POWER;    return true; }
            if (isInTab(mouseX, mouseY, x + 130, y + 14, 40, 12)) { activeTab = Tab.CRAFTING; return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInTab(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String formatVal(long value, boolean exact) {
        return exact ? CountFormatter.formatExactCount(value) : CountFormatter.formatCount(value);
    }

    private static ItemStack getItemForComponent(String registryPath) {
        // Components are stored as "namespace:path" by the advanced core scan.
        ResourceLocation itemId = registryPath.contains(":")
            ? ResourceLocation.parse(registryPath)
            : ResourceLocation.fromNamespaceAndPath("s3", registryPath);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private record ComponentIcon(int x, int y, Component name) {}
}
