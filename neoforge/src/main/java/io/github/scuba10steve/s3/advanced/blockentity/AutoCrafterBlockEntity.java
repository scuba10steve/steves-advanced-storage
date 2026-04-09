package io.github.scuba10steve.s3.advanced.blockentity;

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

import java.util.Arrays;

public class AutoCrafterBlockEntity extends BaseBlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 4;

    private final PerPatternConfig[] configs = new PerPatternConfig[SLOT_COUNT];
    private String customName = "";

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state);
        Arrays.fill(configs, PerPatternConfig.DEFAULT);
    }

    public PerPatternConfig[] getConfigs() {
        return configs;
    }

    public void setConfig(int slot, PerPatternConfig config) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            configs[slot] = config;
            setChanged();
        }
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String name) {
        this.customName = name.length() > 32 ? name.substring(0, 32) : name;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return customName.isEmpty()
            ? Component.translatable("block.s3_advanced.auto_crafter")
            : Component.literal(customName);
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
        for (int i = 0; i < SLOT_COUNT; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.putBoolean("AutoEnabled", configs[i].autoEnabled());
            entry.putInt("MinBuffer", configs[i].minimumBuffer());
            list.add(entry);
        }
        tag.put("Configs", list);
        if (!customName.isEmpty()) {
            tag.putString("CustomName", customName);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Arrays.fill(configs, PerPatternConfig.DEFAULT);
        if (tag.contains("Configs")) {
            ListTag list = tag.getList("Configs", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < SLOT_COUNT) {
                    configs[slot] = new PerPatternConfig(
                        entry.getBoolean("AutoEnabled"),
                        entry.getInt("MinBuffer")
                    );
                }
            }
        }
        customName = tag.contains("CustomName") ? tag.getString("CustomName") : "";
    }
}
