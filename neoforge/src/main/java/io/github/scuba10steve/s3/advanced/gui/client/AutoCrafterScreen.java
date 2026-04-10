package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.gui.server.AutoCrafterMenu;
import io.github.scuba10steve.s3.advanced.network.RenameAutoCrafterPacket;
import io.github.scuba10steve.s3.advanced.network.UpdatePatternConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {

    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 28;
    private static final int LIST_LEFT = 8;

    private EditBox nameField;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 120;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        nameField = new EditBox(this.font, x + 8, y + 14, 160, 12,
            Component.translatable("gui.s3_advanced.auto_crafter_name"));
        nameField.setMaxLength(32);
        nameField.setValue(menu.getCustomName());
        nameField.setResponder(text -> { /* sent on blur/enter below */ });
        addRenderableWidget(nameField);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Send rename on Enter
        if (keyCode == 257 /* Enter */ && nameField.isFocused()) {
            PacketDistributor.sendToServer(
                new RenameAutoCrafterPacket(menu.getBlockPos(), nameField.getValue()));
            nameField.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        // Send rename when GUI closes
        PacketDistributor.sendToServer(
            new RenameAutoCrafterPacket(menu.getBlockPos(), nameField.getValue()));
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSlots(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;
        ItemStack[] outputs = menu.getOutputItems();

        boolean anyPaired = false;
        for (ItemStack s : outputs) { if (!s.isEmpty()) { anyPaired = true; break; } }

        if (!anyPaired) {
            graphics.drawString(this.font,
                Component.translatable("gui.s3_advanced.no_paired_rmb"),
                guiX + LIST_LEFT, guiY + LIST_TOP + 10, 0xFFAAAAAA, false);
            return;
        }

        for (int i = 0; i < 4; i++) {
            PerPatternConfig cfg = menu.getConfigs()[i];
            int rowX = guiX + LIST_LEFT;
            int rowY = guiY + LIST_TOP + i * ROW_HEIGHT;

            graphics.fill(rowX, rowY, rowX + 160, rowY + ROW_HEIGHT - 1, 0x22FFFFFF);

            // Output item
            ItemStack output = outputs[i];
            if (!output.isEmpty()) {
                graphics.renderItem(output, rowX + 2, rowY + 3);
            } else {
                graphics.fill(rowX + 2, rowY + 3, rowX + 18, rowY + 19, 0xFF555555);
            }

            // Auto toggle
            String autoLabel = cfg.autoEnabled() ? "Auto: ON" : "Auto: OFF";
            int autoColor = cfg.autoEnabled() ? 0xFF00FF00 : 0xFFAAAAAA;
            graphics.drawString(this.font, autoLabel, rowX + 22, rowY + 3, autoColor, false);

            // Min buffer: [-] N [+]
            graphics.drawString(this.font, "Min:", rowX + 22, rowY + 13, 0xFFAAAAAA, false);
            boolean decHov = isInBounds(mouseX, mouseY, rowX + 48, rowY + 11, 14, 10);
            boolean incHov = isInBounds(mouseX, mouseY, rowX + 92, rowY + 11, 14, 10);
            graphics.drawString(this.font, "[-]", rowX + 48, rowY + 13,
                decHov ? 0xFFFFFFFF : 0xFFFF8888, false);
            graphics.drawString(this.font, String.valueOf(cfg.minimumBuffer()),
                rowX + 66, rowY + 13, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "[+]", rowX + 92, rowY + 13,
                incHov ? 0xFFFFFFFF : 0xFF88FF88, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2D2D2D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int guiX = (this.width - this.imageWidth) / 2;
        int guiY = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < 4; i++) {
            PerPatternConfig cfg = menu.getConfigs()[i];
            int rowX = guiX + LIST_LEFT;
            int rowY = guiY + LIST_TOP + i * ROW_HEIGHT;

            // Auto toggle
            if (isInBounds(mouseX, mouseY, rowX + 22, rowY + 3, 80, 10)) {
                boolean newAuto = !cfg.autoEnabled();
                PerPatternConfig newCfg = new PerPatternConfig(newAuto, cfg.minimumBuffer());
                menu.setConfig(i, newCfg);
                PacketDistributor.sendToServer(new UpdatePatternConfigPacket(
                    menu.getBlockPos(), i, newAuto, cfg.minimumBuffer()));
                return true;
            }

            // Decrement
            if (isInBounds(mouseX, mouseY, rowX + 48, rowY + 11, 14, 10)) {
                ItemStack output = menu.getOutputItems()[i];
                int amount = Screen.hasShiftDown() && !output.isEmpty()
                    ? output.getMaxStackSize() : 1;
                int newMin = Math.max(0, cfg.minimumBuffer() - amount);
                PerPatternConfig newCfg = new PerPatternConfig(cfg.autoEnabled(), newMin);
                menu.setConfig(i, newCfg);
                PacketDistributor.sendToServer(new UpdatePatternConfigPacket(
                    menu.getBlockPos(), i, cfg.autoEnabled(), newMin));
                return true;
            }

            // Increment
            if (isInBounds(mouseX, mouseY, rowX + 92, rowY + 11, 14, 10)) {
                ItemStack output = menu.getOutputItems()[i];
                int amount = Screen.hasShiftDown() && !output.isEmpty()
                    ? output.getMaxStackSize() : 1;
                int newMin = Math.min(cfg.minimumBuffer() + amount, 9999);
                PerPatternConfig newCfg = new PerPatternConfig(cfg.autoEnabled(), newMin);
                menu.setConfig(i, newCfg);
                PacketDistributor.sendToServer(new UpdatePatternConfigPacket(
                    menu.getBlockPos(), i, cfg.autoEnabled(), newMin));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
