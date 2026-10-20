package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.blockentity.TankBlockEntity;
import io.github.scuba10steve.s3.advanced.gui.server.TankMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class TankScreen extends AbstractContainerScreen<TankMenu> {

    private static final int BAR_X = 80;
    private static final int BAR_Y = 18;
    private static final int BAR_W = 16;
    private static final int BAR_H = 52;

    private Button lockButton;

    public TankScreen(TankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // imageHeight stays at the default 166; non-standard heights break slot rendering
    }

    @Override
    protected void init() {
        super.init();
        lockButton = addRenderableWidget(Button.builder(Component.translatable("gui.s3_advanced.tank.lock"),
                b -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, TankMenu.BUTTON_TOGGLE_LOCK))
            .bounds(leftPos + 110, topPos + 52, 56, 16)
            .build());
        updateLockButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLockButton();
    }

    private void updateLockButton() {
        TankBlockEntity be = menu.getBlockEntity();
        boolean locked = be.getLockedFluid() != null;
        lockButton.setMessage(Component.translatable(locked ? "gui.s3_advanced.tank.unlock" : "gui.s3_advanced.tank.lock"));
        lockButton.active = locked || !be.getTank().isEmpty();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF555555);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF373737);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
        }
        g.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, 0xFF373737);
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF1E1E1E);

        FluidStack fluid = menu.getBlockEntity().getTank().getFluid();
        int capacity = menu.getCapacity();
        if (!fluid.isEmpty() && capacity > 0) {
            int filled = (int) Math.min(BAR_H, (long) menu.getAmount() * BAR_H / capacity);
            drawFluid(g, fluid, x + BAR_X, y + BAR_Y + BAR_H - filled, filled);
        }
    }

    private void drawFluid(GuiGraphics g, FluidStack fluid, int left, int top, int height) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        float a = ((tint >> 24) & 0xFF) / 255f;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float gr = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        int drawn = 0;
        while (drawn < height) {
            int h = Math.min(16, height - drawn);
            g.blit(left, top + height - drawn - h, 0, BAR_W, h, sprite, r, gr, b, a == 0 ? 1f : a);
            drawn += h;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);

        TankBlockEntity be = menu.getBlockEntity();
        FluidStack fluid = be.getTank().getFluid();
        Fluid locked = be.getLockedFluid();
        Fluid shown = fluid.isEmpty() ? locked : fluid.getFluid();
        Component name = shown == null
            ? Component.translatable("gui.s3_advanced.tank.empty")
            : shown.getFluidType().getDescription();
        g.drawString(font, name, 104, 22, fluid.isEmpty() ? 0x888888 : 0x404040, false);
        if (locked != null) {
            g.drawString(font, Component.translatable("gui.s3_advanced.tank.locked_flag"), 104, 34, 0x888888, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("gui.s3_advanced.tank.amount",
                menu.getAmount(), menu.getCapacity()), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
