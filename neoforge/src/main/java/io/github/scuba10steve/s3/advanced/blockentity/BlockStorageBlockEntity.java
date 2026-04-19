package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.gui.server.BlockStorageMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.block.BlockStorage;
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

public class BlockStorageBlockEntity extends BaseBlockEntity implements MenuProvider {

    private final int slotCount;
    public final ItemStackHandler handler;

    public BlockStorageBlockEntity(BlockPos pos, BlockState state, int slotCount) {
        super(ModBlockEntities.BLOCK_STORAGE.get(), pos, state);
        this.slotCount = slotCount;
        this.handler = new ItemStackHandler(slotCount) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof BlockStorage
                    && !(bi.getBlock() instanceof io.github.scuba10steve.s3.advanced.block.BlockStorage);
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

    public int getSlotCount() {
        return slotCount;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.block_storage_1");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlockStorageMenu(containerId, playerInventory, this);
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
