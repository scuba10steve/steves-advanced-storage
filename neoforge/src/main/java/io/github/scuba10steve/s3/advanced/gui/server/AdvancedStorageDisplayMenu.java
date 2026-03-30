package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import io.github.scuba10steve.s3.gui.server.StorageCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AdvancedStorageDisplayMenu extends StorageCoreMenu {

    // Client constructor — buf contains BlockPos written by S3Platform.openMenu
    public AdvancedStorageDisplayMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    // Server constructor — called from AdvancedStorageCoreBlockEntity.createMenu()
    public AdvancedStorageDisplayMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.ADVANCED_STORAGE_DISPLAY.get(), containerId, playerInventory, pos);
        // The protected StorageCoreMenu constructor does NOT add player inventory slots;
        // the public one does. We replicate that here.
        addPlayerInventory(playerInventory, 140, 198);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return super.quickMoveStack(player, index);
    }
}
