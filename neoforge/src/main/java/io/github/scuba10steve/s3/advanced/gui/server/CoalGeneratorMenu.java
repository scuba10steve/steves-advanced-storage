package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.CoalGeneratorBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CoalGeneratorMenu extends AbstractContainerMenu {

    private final CoalGeneratorBlockEntity blockEntity;
    public final ContainerData containerData;

    // Client constructor (via IMenuTypeExtension)
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            getBlockEntity(playerInventory, buf.readBlockPos()),
            new SimpleContainerData(7));
    }

    // Server constructor
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, CoalGeneratorBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private CoalGeneratorMenu(int containerId, Inventory playerInventory,
                               CoalGeneratorBlockEntity blockEntity, ContainerData containerData) {
        super(ModMenuTypes.COAL_GENERATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;

        // Fuel slot (index 0), centered horizontally
        // slot.x/y must point to the inner 16x16 fill of the 18x18 background graphic (+1 offset)
        addSlot(new SlotItemHandler(blockEntity.fuelHandler, 0, 81, 43));

        // Player inventory (3 rows), indices 1-27
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 9 + col * 18, 85 + row * 18));
            }
        }

        // Player hotbar, indices 28-36
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 9 + i * 18, 143));
        }

        addDataSlots(containerData);
    }

    private static CoalGeneratorBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof CoalGeneratorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No CoalGeneratorBlockEntity at " + pos);
    }

    public boolean isLit() {
        return containerData.get(0) > 0;
    }

    /** Returns 0-13 for the flame burn animation height. */
    public int getBurnProgress() {
        int litTime = containerData.get(0);
        int litDuration = containerData.get(1);
        if (litDuration == 0) {
            return 0;
        }
        return litTime * 13 / litDuration;
    }

    /** Returns 0-100 for the energy bar fill percentage. */
    public int getEnergyPercent() {
        int capacity = getCapacity();
        if (capacity == 0) {
            return 0;
        }
        return getEnergyStored() * 100 / capacity;
    }

    public int getEnergyStored() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getCapacity() {
        return (containerData.get(5) << 16) | (containerData.get(4) & 0xFFFF);
    }

    public int getGenerationRate() {
        return containerData.get(6);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        if (index == 0) {
            // Fuel slot -> player inventory
            if (!this.moveItemStackTo(slotStack, 1, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> fuel slot if valid
            if (blockEntity.fuelHandler.isItemValid(0, slotStack)) {
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
            ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
            player, ModBlocks.COAL_GENERATOR.get());
    }
}
