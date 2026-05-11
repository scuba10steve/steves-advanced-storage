package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.ConfigBlockBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ConfigBlockMenu extends AbstractContainerMenu {

    private static final int SLOT_COUNT = 3;
    private final BlockPos pos;

    // Client constructor
    public ConfigBlockMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    // Server constructor
    public ConfigBlockMenu(int containerId, Inventory playerInventory, ConfigBlockBlockEntity be) {
        super(ModMenuTypes.CONFIG_BLOCK.get(), containerId);
        this.pos = be.getBlockPos();

        // 3 config slots in 1 row, centered: startX = (176 - 3*18) / 2 = 61
        for (int col = 0; col < SLOT_COUNT; col++) {
            addSlot(new SlotItemHandler(be.handler, col, 61 + col * 18, 17));
        }

        // Player inventory (3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 53 + row * 18));
            }
        }
        // Hotbar
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 111));
        }
    }

    private static ConfigBlockBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ConfigBlockBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No ConfigBlockBlockEntity at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(slotStack, SLOT_COUNT, SLOT_COUNT + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(slotStack, 0, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
            ContainerLevelAccess.create(player.level(), pos),
            player, ModBlocks.BLOCK_CONFIG_BLOCK.get());
    }
}
