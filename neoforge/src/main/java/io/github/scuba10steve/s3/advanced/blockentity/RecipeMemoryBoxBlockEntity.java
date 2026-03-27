package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import io.github.scuba10steve.s3.advanced.gui.server.RecipeMemoryBoxMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeMemoryBoxBlockEntity extends BaseBlockEntity implements MenuProvider {

    public static final int MAX_PATTERNS = 9;

    private final List<RecipePattern> patterns = new ArrayList<>(MAX_PATTERNS);

    public RecipeMemoryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECIPE_MEMORY_BOX.get(), pos, state);
        for (int i = 0; i < MAX_PATTERNS; i++) {
            patterns.add(new RecipePattern());
        }
    }

    public RecipePattern getPattern(int index) {
        return patterns.get(index);
    }

    public void setPattern(int index, RecipePattern pattern) {
        patterns.set(index, pattern);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public List<RecipePattern> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.recipe_memory_box");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RecipeMemoryBoxMenu(containerId, playerInventory, this);
    }

    // Sync full NBT to client on chunk load and block updates
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < MAX_PATTERNS; i++) {
            CompoundTag entry = patterns.get(i).save(registries);
            entry.putByte("PatternIndex", (byte) i);
            list.add(entry);
        }
        tag.put("Patterns", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Patterns")) {
            ListTag list = tag.getList("Patterns", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int idx = entry.getByte("PatternIndex") & 0xFF;
                if (idx < MAX_PATTERNS) {
                    patterns.set(idx, RecipePattern.load(entry, registries));
                }
            }
        }
    }
}
