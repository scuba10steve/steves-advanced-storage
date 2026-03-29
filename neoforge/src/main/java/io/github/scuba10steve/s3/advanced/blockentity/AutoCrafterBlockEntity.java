package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.gui.server.AutoCrafterMenu;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoCrafterBlockEntity extends BaseBlockEntity implements MenuProvider {

    public static final int MAX_ASSIGNMENTS = 4;

    private final Map<PatternKey, PerPatternConfig> assignments = new LinkedHashMap<>();

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state);
    }

    /** Returns an unmodifiable view of the current assignment map. */
    public Map<PatternKey, PerPatternConfig> getAssignments() {
        return Collections.unmodifiableMap(assignments);
    }

    /** Adds patternKey with DEFAULT config if not already assigned and below the slot cap. */
    public void assign(PatternKey key) {
        if (assignments.size() < MAX_ASSIGNMENTS) {
            assignments.putIfAbsent(key, PerPatternConfig.DEFAULT);
            setChanged();
        }
    }

    /** Removes the assignment for patternKey (no-op if not present). */
    public void unassign(PatternKey key) {
        if (assignments.remove(key) != null) {
            setChanged();
        }
    }

    /** Updates config for an existing assignment (no-op if patternKey not assigned). */
    public void updateConfig(PatternKey key, boolean autoEnabled, int minimumBuffer) {
        if (assignments.containsKey(key)) {
            assignments.put(key, new PerPatternConfig(autoEnabled, minimumBuffer));
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.auto_crafter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AutoCrafterMenu(containerId, playerInventory, this);
    }

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
        for (Map.Entry<PatternKey, PerPatternConfig> entry : assignments.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("BoxPos", entry.getKey().pos().asLong());
            entryTag.putInt("PatternIndex", entry.getKey().index());
            entryTag.putBoolean("AutoEnabled", entry.getValue().autoEnabled());
            entryTag.putInt("MinBuffer", entry.getValue().minimumBuffer());
            list.add(entryTag);
        }
        tag.put("Assignments", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        assignments.clear();
        if (tag.contains("Assignments")) {
            ListTag list = tag.getList("Assignments", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                PatternKey key = new PatternKey(BlockPos.of(entry.getLong("BoxPos")), entry.getInt("PatternIndex"));
                assignments.put(key, new PerPatternConfig(entry.getBoolean("AutoEnabled"), entry.getInt("MinBuffer")));
            }
        }
    }
}
