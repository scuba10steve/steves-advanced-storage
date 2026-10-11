package io.github.scuba10steve.s3.advanced.item;

import io.github.scuba10steve.s3.advanced.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

public class TankBlockItem extends BlockItem {

    public TankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        SimpleFluidContent content = stack.get(ModDataComponents.TANK_FLUID.get());
        if (content != null && !content.isEmpty()) {
            FluidStack fluid = content.copy();
            tooltip.add(Component.translatable("tooltip.s3_advanced.tank.contents",
                fluid.getHoverName(), fluid.getAmount()).withStyle(ChatFormatting.GRAY));
        }
        Holder<Fluid> locked = stack.get(ModDataComponents.LOCKED_FLUID.get());
        if (locked != null && locked.value() != Fluids.EMPTY) {
            tooltip.add(Component.translatable("gui.s3_advanced.tank.locked",
                locked.value().getFluidType().getDescription()).withStyle(ChatFormatting.GRAY));
        }
    }
}
