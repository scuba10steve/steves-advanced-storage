package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
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
    private final boolean hasPairedRmb;

    // Client constructor — reads buf written by BlockAutoCrafter.useWithoutItem
    public AutoCrafterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = buf.readBlockPos();
        this.customName = buf.readUtf(32);
        this.hasPairedRmb = buf.readBoolean();
        this.configs = new PerPatternConfig[AutoCrafterBlockEntity.SLOT_COUNT];
        this.outputItems = new ItemStack[AutoCrafterBlockEntity.SLOT_COUNT];
        for (int i = 0; i < AutoCrafterBlockEntity.SLOT_COUNT; i++) {
            configs[i] = PerPatternConfig.decode(buf);
            outputItems[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        }
    }

    // Server constructor
    public AutoCrafterMenu(int containerId, Inventory playerInventory, AutoCrafterBlockEntity be) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = be.getBlockPos();
        this.customName = be.getCustomName();
        this.configs = be.getConfigs().clone();
        RecipeMemoryBoxBlockEntity rmb = resolveRmb(be);
        this.hasPairedRmb = rmb != null;
        this.outputItems = resolveOutputItems(rmb);
    }

    public static RecipeMemoryBoxBlockEntity resolveRmb(AutoCrafterBlockEntity be) {
        if (be.getLevel() == null) return null;
        AdvancedStorageCoreBlockEntity core =
            AdvancedStorageCoreBlockEntity.findCore(be.getLevel(), be.getBlockPos());
        return core != null ? core.getRmbForCrafter(be) : null;
    }

    /**
     * Returns the 4 output items (one per RMB pattern slot) for the given paired RMB,
     * or an array of EMPTY stacks if rmb is null.
     */
    public static ItemStack[] resolveOutputItems(RecipeMemoryBoxBlockEntity rmb) {
        ItemStack[] items = new ItemStack[AutoCrafterBlockEntity.SLOT_COUNT];
        Arrays.fill(items, ItemStack.EMPTY);
        if (rmb == null) return items;
        for (int i = 0; i < AutoCrafterBlockEntity.SLOT_COUNT; i++) {
            var pattern = rmb.getPattern(i);
            items[i] = (pattern != null && !pattern.isEmpty()) ? pattern.getOutput().copy() : ItemStack.EMPTY;
        }
        return items;
    }

    public BlockPos getBlockPos() { return pos; }
    public PerPatternConfig[] getConfigs() { return configs; }
    public ItemStack[] getOutputItems() { return outputItems; }
    public String getCustomName() { return customName; }
    public boolean hasPairedRmb() { return hasPairedRmb; }

    /** Called by the screen on optimistic config update so the UI stays responsive. */
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
