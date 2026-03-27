package io.github.scuba10steve.s3.advanced.jei;

import io.github.scuba10steve.s3.advanced.gui.server.RecipePatternMenu;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import io.github.scuba10steve.s3.advanced.network.GhostSlotFillPacket;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JEI recipe transfer handler for RecipePatternMenu.
 * Ghost slots don't require the player to hold the items — the handler just
 * sends the recipe ingredients to fill the pattern grid.
 */
public class RecipePatternTransferHandler implements IRecipeTransferHandler<RecipePatternMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    public RecipePatternTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<RecipePatternMenu> getContainerClass() {
        return RecipePatternMenu.class;
    }

    @Override
    public Optional<MenuType<RecipePatternMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.RECIPE_PATTERN.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            RecipePatternMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {

        Map<Integer, Ingredient> slotMap = helper.getGuiSlotIndexToIngredientMap(recipe);

        ItemStack[] items = new ItemStack[9];
        Arrays.fill(items, ItemStack.EMPTY);

        for (var entry : slotMap.entrySet()) {
            int slot = entry.getKey();
            Ingredient ingredient = entry.getValue();
            if (slot >= 0 && slot < 9 && !ingredient.isEmpty()) {
                ItemStack[] matches = ingredient.getItems();
                if (matches.length > 0) {
                    items[slot] = matches[0].copyWithCount(1);
                }
            }
        }

        if (doTransfer) {
            PacketDistributor.sendToServer(new GhostSlotFillPacket(new ArrayList<>(Arrays.asList(items))));
        }

        return null;
    }
}
