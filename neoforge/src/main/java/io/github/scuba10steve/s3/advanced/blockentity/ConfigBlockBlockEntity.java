package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.gui.server.ConfigBlockMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.block.BlockCraftingBox;
import io.github.scuba10steve.s3.block.BlockSearchBox;
import io.github.scuba10steve.s3.block.BlockSortBox;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConfigBlockBlockEntity extends BaseBlockEntity implements MenuProvider {

    public final ItemStackHandler handler;

    public ConfigBlockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONFIG_BLOCK.get(), pos, state);
        this.handler = new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof BlockItem bi && (
                    bi.getBlock() instanceof BlockCraftingBox ||
                    bi.getBlock() instanceof BlockSearchBox   ||
                    bi.getBlock() instanceof BlockSortBox
                );
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (level != null && !level.isClientSide) {
                    AdvancedStorageCoreBlockEntity core =
                        AdvancedStorageCoreBlockEntity.findCore(level, worldPosition);
                    if (core != null) {
                        core.scanMultiblock();
                    }
                }
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.s3_advanced.config_block");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ConfigBlockMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", handler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            handler.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }
}
