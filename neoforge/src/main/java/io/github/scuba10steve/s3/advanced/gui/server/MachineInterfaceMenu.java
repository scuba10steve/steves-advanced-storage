package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MachineInterfaceMenu extends AbstractContainerMenu {

    public MachineInterfaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player p) { return true; }
}
