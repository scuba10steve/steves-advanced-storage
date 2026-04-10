package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MachineInterfaceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    /** The BlockPos of the RMB paired to this MI, or null if unpaired. */
    @Nullable private final BlockPos pairedRmbPos;
    public final ContainerData containerData;

    // Client constructor
    public MachineInterfaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), containerId);
        this.pos = buf.readBlockPos();
        boolean hasPair = buf.readBoolean();
        this.pairedRmbPos = hasPair ? buf.readBlockPos() : null;
        this.containerData = new SimpleContainerData(2);
        addDataSlots(this.containerData);
    }

    // Server constructor
    public MachineInterfaceMenu(int containerId, Inventory playerInventory, MachineInterfaceBlockEntity be) {
        super(ModMenuTypes.MACHINE_INTERFACE.get(), containerId);
        this.pos = be.getBlockPos();
        this.pairedRmbPos = resolvePairedRmbPos(be);
        this.containerData = be.containerData;
        addDataSlots(this.containerData);
    }

    @Nullable
    public static BlockPos resolvePairedRmbPos(MachineInterfaceBlockEntity be) {
        if (be.getLevel() == null) return null;
        AdvancedStorageCoreBlockEntity core =
            AdvancedStorageCoreBlockEntity.findCore(be.getLevel(), be.getBlockPos());
        if (core == null) return null;
        RecipeMemoryBoxBlockEntity rmb = core.getRmbForMachineInterface(be);
        return rmb != null ? rmb.getBlockPos() : null;
    }

    public BlockPos getBlockPos() { return pos; }
    @Nullable public BlockPos getPairedRmbPos() { return pairedRmbPos; }

    public int getTickInterval() { return containerData.get(0); }

    public MachineInterfaceBlockEntity.Status getStatus() {
        int ordinal = containerData.get(1);
        MachineInterfaceBlockEntity.Status[] values = MachineInterfaceBlockEntity.Status.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MachineInterfaceBlockEntity.Status.IDLE;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
