package io.github.scuba10steve.s3.advanced.item;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModBlocks;
import io.github.scuba10steve.s3.blockentity.StorageCoreBlockEntity;
import io.github.scuba10steve.s3.item.BaseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ItemAdvancedStorageCoreUpgrade extends BaseItem {

    public ItemAdvancedStorageCoreUpgrade() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof StorageCoreBlockEntity) || be instanceof AdvancedStorageCoreBlockEntity) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            performUpgrade(context, level, pos, (StorageCoreBlockEntity) be);
        }

        return InteractionResult.SUCCESS;
    }

    private void performUpgrade(UseOnContext context, Level level, BlockPos pos, StorageCoreBlockEntity core) {
        CompoundTag storedData = core.saveWithFullMetadata(level.registryAccess());

        BlockState newState = ModBlocks.ADVANCED_STORAGE_CORE.get().defaultBlockState();
        level.removeBlockEntity(pos);
        level.setBlock(pos, newState, 3);

        BlockEntity newBE = level.getBlockEntity(pos);
        if (newBE instanceof AdvancedStorageCoreBlockEntity advancedCore) {
            storedData.putInt("x", pos.getX());
            storedData.putInt("y", pos.getY());
            storedData.putInt("z", pos.getZ());
            newBE.loadWithComponents(storedData, level.registryAccess());
            advancedCore.scanMultiblock();
        }

        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
    }
}
