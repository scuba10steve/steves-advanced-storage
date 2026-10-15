package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.TankBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

public class TankMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE_LOCK = 0;
    public static final int PLAYER_INV_START = 2;
    public static final int PLAYER_HOTBAR_START = 29;
    public static final int SLOT_END = 38;

    private final TankBlockEntity blockEntity;
    private final ContainerData data;

    // Client constructor (via IMenuTypeExtension)
    public TankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(4));
    }

    // Server constructor
    public TankMenu(int containerId, Inventory playerInventory, TankBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private TankMenu(int containerId, Inventory playerInventory, TankBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.TANK.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        Container slots = blockEntity.bucketSlots;
        addSlot(new Slot(slots, TankBlockEntity.SLOT_INPUT, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
            }
        });
        addSlot(new Slot(slots, TankBlockEntity.SLOT_OUTPUT, 26, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        addDataSlots(data);
    }

    private static TankBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof TankBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No TankBlockEntity at " + pos);
    }

    public TankBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getAmount() {
        return (data.get(1) << 16) | (data.get(0) & 0xFFFF);
    }

    public int getCapacity() {
        return (data.get(3) << 16) | (data.get(2) & 0xFFFF);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_LOCK) {
            blockEntity.toggleLock();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        ItemStack result = slotStack.copy();

        if (index < PLAYER_INV_START) {
            if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotStack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            if (!this.moveItemStackTo(slotStack, TankBlockEntity.SLOT_INPUT, TankBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
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
            player, blockEntity.getBlockState().getBlock());
    }
}
