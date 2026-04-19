package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class AdvancedStorageCoreMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData containerData;

    // Client constructor
    public AdvancedStorageCoreMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, buf.readBlockPos(), new SimpleContainerData(7));
    }

    // Server constructor
    public AdvancedStorageCoreMenu(int containerId, Inventory playerInventory, AdvancedStorageCoreBlockEntity be) {
        this(containerId, be.getBlockPos(), be.containerData);
    }

    private AdvancedStorageCoreMenu(int containerId, BlockPos pos, ContainerData data) {
        super(ModMenuTypes.ADVANCED_STORAGE_CORE.get(), containerId);
        this.pos = pos;
        this.containerData = data;
        addDataSlots(data);
    }

    public int getEnergyStored() {
        return (containerData.get(1) << 16) | (containerData.get(0) & 0xFFFF);
    }

    public int getCapacity() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getEnergyPerTick() {
        return (containerData.get(5) << 16) | (containerData.get(4) & 0xFFFF);
    }

    public boolean isPowered() {
        return containerData.get(6) != 0;
    }

    public int getEnergyPercent() {
        int capacity = getCapacity();
        if (capacity == 0) {
            return 0;
        }
        return getEnergyStored() * 100 / capacity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
