package io.github.scuba10steve.s3.advanced.gui.client;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.gui.server.MachineInterfaceMenu;
import io.github.scuba10steve.s3.advanced.network.UpdateMachineInterfaceTickPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class MachineInterfaceScreen extends AbstractContainerScreen<MachineInterfaceMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        StevesAdvancedStorage.MOD_ID, "textures/gui/machine_interface.png");

    public MachineInterfaceScreen(MachineInterfaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 80;
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
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderContent(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Pairing status
        BlockPos rmbPos = menu.getPairedRmbPos();
        if (rmbPos != null) {
            String label = "RMB: " + rmbPos.getX() + ", " + rmbPos.getY() + ", " + rmbPos.getZ();
            graphics.drawString(this.font, label, x + 8, y + 17, 0xFF88FF88, false);
        } else {
            graphics.drawString(this.font,
                Component.translatable("gui.s3_advanced.no_paired_rmb"),
                x + 8, y + 17, 0xFFFF8888, false);
        }

        // Status label
        MachineInterfaceBlockEntity.Status status = menu.getStatus();
        String statusKey = switch (status) {
            case PUSHING -> "gui.s3_advanced.status.pushing";
            case WAITING -> "gui.s3_advanced.status.waiting";
            default      -> "gui.s3_advanced.status.idle";
        };
        graphics.drawString(this.font,
            Component.translatable(statusKey), x + 30, y + 20, 0xFFAAAAAA, false);

        // Tick interval: "Every: [-] N [+]"
        graphics.drawString(this.font,
            Component.translatable("gui.s3_advanced.tick_interval"), x + 30, y + 35, 0xFFAAAAAA, false);

        int interval = menu.getTickInterval();
        boolean decHovered = isInBounds(mouseX, mouseY, x + 70, y + 33, 14, 10);
        boolean incHovered = isInBounds(mouseX, mouseY, x + 110, y + 33, 14, 10);
        graphics.drawString(this.font, "[-]", x + 70, y + 35, decHovered ? 0xFFFFFFFF : 0xFFFF8888, false);
        graphics.drawString(this.font, String.valueOf(interval), x + 88, y + 35, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "[+]", x + 110, y + 35, incHovered ? 0xFFFFFFFF : 0xFF88FF88, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int interval = menu.getTickInterval();

        if (isInBounds(mouseX, mouseY, x + 70, y + 33, 14, 10)) {
            int newVal = Math.max(1, interval - 1);
            PacketDistributor.sendToServer(
                new UpdateMachineInterfaceTickPacket(menu.getBlockPos(), newVal));
            return true;
        }
        if (isInBounds(mouseX, mouseY, x + 110, y + 33, 14, 10)) {
            int newVal = Math.min(1200, interval + 1);
            PacketDistributor.sendToServer(
                new UpdateMachineInterfaceTickPacket(menu.getBlockPos(), newVal));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
