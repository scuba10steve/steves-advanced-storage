package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MachineInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final PatternKey assignedPattern;
    private final ItemStack outputItem;
    public final ContainerData containerData;

    // Client constructor
    public MachineInterfaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), containerId);
        this.pos = buf.readBlockPos();
        boolean hasPattern = buf.readBoolean();
        if (hasPattern) {
            this.assignedPattern = new PatternKey(buf.readBlockPos(), buf.readInt());
            this.outputItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        } else {
            this.assignedPattern = null;
            this.outputItem = ItemStack.EMPTY;
        }
        this.containerData = new SimpleContainerData(2);
        addDataSlots(this.containerData);
    }

    // Server constructor
    public MachineInterfaceMenu(int containerId, Inventory playerInventory, MachineInterfaceBlockEntity be) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), containerId);
        this.pos = be.getBlockPos();
        this.assignedPattern = be.getAssignedPattern();
        this.outputItem = resolveOutput(be);
        this.containerData = be.containerData;
        addDataSlots(this.containerData);
    }

    private static ItemStack resolveOutput(MachineInterfaceBlockEntity be) {
        PatternKey key = be.getAssignedPattern();
        if (key == null) return ItemStack.EMPTY;
        Level level = be.getLevel();
        if (level == null) return ItemStack.EMPTY;
        if (level.getBlockEntity(key.pos()) instanceof RecipeMemoryBoxBlockEntity rmbBe) {
            return rmbBe.getPattern(key.index()).getOutput().copy();
        }
        return ItemStack.EMPTY;
    }

    public BlockPos getBlockPos() { return pos; }
    public PatternKey getAssignedPattern() { return assignedPattern; }
    public ItemStack getOutputItem() { return outputItem; }

    /** Tick interval synced via ContainerData slot 0. */
    public int getTickInterval() { return containerData.get(0); }

    /** Status ordinal synced via ContainerData slot 1. */
    public MachineInterfaceBlockEntity.Status getStatus() {
        int ordinal = containerData.get(1);
        MachineInterfaceBlockEntity.Status[] values = MachineInterfaceBlockEntity.Status.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MachineInterfaceBlockEntity.Status.IDLE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
