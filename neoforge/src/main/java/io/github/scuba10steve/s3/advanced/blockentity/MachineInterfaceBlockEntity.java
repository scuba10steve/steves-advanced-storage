package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class MachineInterfaceBlockEntity extends BaseBlockEntity implements MenuProvider {

    public MachineInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_INTERFACE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.s3_advanced.machine_interface");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
