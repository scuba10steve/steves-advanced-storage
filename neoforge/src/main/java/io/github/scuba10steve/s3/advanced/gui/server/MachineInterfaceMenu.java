package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MachineInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    // Client constructor
    public MachineInterfaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), id);
        this.pos = buf.readBlockPos();
    }

    // Server constructor
    public MachineInterfaceMenu(int id, Inventory inv, MachineInterfaceBlockEntity be) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), id);
        this.pos = be.getBlockPos();
    }

    public BlockPos getBlockPos() {
        return pos;
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
