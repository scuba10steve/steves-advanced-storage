package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.SolarGeneratorBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SolarGeneratorMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData containerData;

    // Client constructor
    public SolarGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, buf.readBlockPos(), new SimpleContainerData(5));
    }

    // Server constructor
    public SolarGeneratorMenu(int containerId, Inventory playerInventory, SolarGeneratorBlockEntity be) {
        this(containerId, be.getBlockPos(), be.containerData);
    }

    private SolarGeneratorMenu(int containerId, BlockPos pos, ContainerData data) {
        super(ModMenuTypes.SOLAR_GENERATOR.get(), containerId);
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

    public int getCurrentRate() {
        return containerData.get(4);
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
