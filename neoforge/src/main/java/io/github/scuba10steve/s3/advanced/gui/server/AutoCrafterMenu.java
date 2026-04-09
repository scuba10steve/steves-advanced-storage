package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class AutoCrafterMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final PerPatternConfig[] configs;
    private final ItemStack[] outputItems;
    private final String customName;

    // Client constructor (stub — full implementation in Task 5)
    public AutoCrafterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = buf.readBlockPos();
        this.customName = "";
        this.configs = new PerPatternConfig[AutoCrafterBlockEntity.SLOT_COUNT];
        Arrays.fill(this.configs, PerPatternConfig.DEFAULT);
        this.outputItems = new ItemStack[AutoCrafterBlockEntity.SLOT_COUNT];
        Arrays.fill(this.outputItems, ItemStack.EMPTY);
    }

    // Server constructor
    public AutoCrafterMenu(int containerId, Inventory playerInventory, AutoCrafterBlockEntity be) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = be.getBlockPos();
        this.customName = be.getCustomName();
        this.configs = be.getConfigs().clone();
        this.outputItems = new ItemStack[AutoCrafterBlockEntity.SLOT_COUNT];
        Arrays.fill(this.outputItems, ItemStack.EMPTY);
    }

    public BlockPos getBlockPos() { return pos; }
    public PerPatternConfig[] getConfigs() { return configs; }
    public ItemStack[] getOutputItems() { return outputItems; }
    public String getCustomName() { return customName; }

    public void setConfig(int slot, PerPatternConfig config) {
        if (slot >= 0 && slot < AutoCrafterBlockEntity.SLOT_COUNT) {
            configs[slot] = config;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
