package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.BlockStorageBlockEntity;
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

public class BlockStorageMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int slotCount;

    // Client constructor
    public BlockStorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    // Server constructor (also used by client constructor)
    public BlockStorageMenu(int containerId, Inventory playerInventory, BlockStorageBlockEntity be) {
        super(ModMenuTypes.BLOCK_STORAGE.get(), containerId);
        this.pos = be.getBlockPos();
        this.slotCount = be.getSlotCount();

        int cols = 4;
        int rows = slotCount / cols;
        int startX = 53;
        int startY = 29;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                addSlot(new SlotItemHandler(be.handler, row * cols + col,
                    startX + col * 18, startY + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, 85 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 143));
        }
    }

    private static BlockStorageBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof BlockStorageBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No BlockStorageBlockEntity at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        if (index < slotCount) {
            // Rack → player inventory
            if (!moveItemStackTo(slotStack, slotCount, slotCount + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory → rack (only if valid)
            if (!moveItemStackTo(slotStack, 0, slotCount, false)) {
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
            player, ModBlocks.BLOCK_STORAGE_1.get());
    }
}
