package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.crafting.CraftingCoordinator;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import io.github.scuba10steve.s3.advanced.gui.server.MachineInterfaceMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import io.github.scuba10steve.s3.storage.StorageInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class MachineInterfaceBlockEntity extends BaseBlockEntity implements MenuProvider {

    public enum Status { IDLE, PUSHING, WAITING }

    private PatternKey assignedPattern = null;
    private int tickInterval;
    private int ticksElapsed = 0;
    private Status status = Status.IDLE;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tickInterval;
                case 1 -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) tickInterval = Math.max(1, value);
        }

        @Override
        public int getCount() { return 2; }
    };

    public MachineInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_INTERFACE.get(), pos, state);
        this.tickInterval = S3AdvancedConfig.MACHINE_INTERFACE_TICK_INTERVAL.get();
    }

    public PatternKey getAssignedPattern() { return assignedPattern; }

    public void setPattern(PatternKey key) {
        this.assignedPattern = key;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearPattern() {
        this.assignedPattern = null;
        setChanged();
    }

    public int getTickInterval() { return tickInterval; }

    public void setTickInterval(int interval) {
        this.tickInterval = Math.max(1, interval);
        setChanged();
    }

    public Status getStatus() { return status; }

    /**
     * Called each server tick by AdvancedStorageCoreBlockEntity.
     * Counts up; when tickInterval is reached, pulls outputs from adjacent machines
     * then pushes pattern ingredients into the first adjacent item handler found.
     */
    public void tryTick(StorageInventory inventory, Level level,
                        List<CraftingCoordinator.BoxData> boxes) {
        ticksElapsed++;
        if (ticksElapsed < tickInterval) {
            return;
        }
        ticksElapsed = 0;

        // Pull any completed outputs from adjacent machines back into storage
        for (Direction dir : Direction.values()) {
            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler != null) {
                pullFrom(handler, inventory);
            }
        }

        // Push ingredients if a pattern is assigned
        if (assignedPattern == null) {
            status = Status.IDLE;
            return;
        }
        RecipePattern pattern = resolvePattern(boxes);
        if (pattern == null || pattern.isEmpty()) {
            status = Status.IDLE;
            return;
        }

        for (Direction dir : Direction.values()) {
            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler != null) {
                boolean pushed = pushTo(handler, inventory, pattern);
                status = pushed ? Status.PUSHING : Status.WAITING;
                return;
            }
        }
        status = Status.IDLE;
    }

    private void pullFrom(IItemHandler handler, StorageInventory inventory) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (inSlot.isEmpty()) continue;
            ItemStack extracted = handler.extractItem(i, inSlot.getCount(), false);
            if (!extracted.isEmpty()) {
                inventory.insertItem(extracted);
            }
        }
    }

    private boolean pushTo(IItemHandler handler, StorageInventory inventory, RecipePattern pattern) {
        List<ItemStack> extracted = new ArrayList<>();
        for (ItemStack ingredient : pattern.getGrid()) {
            if (ingredient.isEmpty()) continue;
            ItemStack got = inventory.extractItem(ingredient, ingredient.getCount());
            if (got.getCount() < ingredient.getCount()) {
                if (!got.isEmpty()) inventory.insertItem(got);
                for (ItemStack e : extracted) inventory.insertItem(e);
                return false;
            }
            extracted.add(got);
        }
        for (ItemStack stack : extracted) {
            ItemStack remaining = stack;
            for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
                remaining = handler.insertItem(i, remaining, false);
            }
            if (!remaining.isEmpty()) {
                inventory.insertItem(remaining);
            }
        }
        return true;
    }

    private RecipePattern resolvePattern(List<CraftingCoordinator.BoxData> boxes) {
        for (CraftingCoordinator.BoxData box : boxes) {
            if (box.pos().equals(assignedPattern.pos())) {
                return box.get(assignedPattern.index());
            }
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.machine_interface");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineInterfaceMenu(id, inv, this);
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
        tag.putInt("TickInterval", tickInterval);
        if (assignedPattern != null) {
            tag.putLong("PatternPos", assignedPattern.pos().asLong());
            tag.putInt("PatternIndex", assignedPattern.index());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickInterval = tag.contains("TickInterval")
                ? Math.max(1, tag.getInt("TickInterval"))
                : S3AdvancedConfig.MACHINE_INTERFACE_TICK_INTERVAL.get();
        if (tag.contains("PatternPos")) {
            assignedPattern = new PatternKey(
                    BlockPos.of(tag.getLong("PatternPos")), tag.getInt("PatternIndex"));
        } else {
            assignedPattern = null;
        }
    }
}
