package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import io.github.scuba10steve.s3.gui.server.StorageCoreCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class AdvancedStorageCraftingDisplayMenu extends StorageCoreCraftingMenu {

    // Client constructor — buf contains BlockPos written by S3Platform.openMenu
    public AdvancedStorageCraftingDisplayMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    // Server constructor — called from AdvancedStorageCoreBlockEntity.createMenu()
    public AdvancedStorageCraftingDisplayMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        // StorageCoreCraftingMenu's public constructor sets up all crafting + player inventory slots
        super(containerId, playerInventory, pos);
    }

    /**
     * Overridden to return our custom MenuType so the server tells the client
     * to construct AdvancedStorageCraftingDisplayMenu (not S3's StorageCoreCraftingMenu).
     * StorageCoreCraftingMenu hardcodes S3's menu type in its super() chain with no
     * protected MenuType constructor to intercept, so this override is required.
     */
    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.ADVANCED_STORAGE_CRAFTING_DISPLAY.get();
    }
}
