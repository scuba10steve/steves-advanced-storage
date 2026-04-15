package io.github.scuba10steve.s3.advanced.jei;

import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageCraftingDisplayMenu;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import io.github.scuba10steve.s3.jei.StorageRecipeTransferHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.constants.RecipeTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * JEI recipe transfer handler for AdvancedStorageCraftingDisplayMenu.
 *
 * The base S3 mod registers its handler against STORAGE_CORE_CRAFTING. Our menu
 * overrides getType() to return ADVANCED_STORAGE_CRAFTING_DISPLAY so the client
 * constructs the right screen class — but that means JEI can no longer match the
 * base handler to our open menu. This wrapper registers for our menu type and
 * delegates the actual transfer logic back to the base StorageRecipeTransferHandler.
 */
public class AdvancedStorageCraftingTransferHandler
        implements IRecipeTransferHandler<AdvancedStorageCraftingDisplayMenu, RecipeHolder<CraftingRecipe>> {

    private final StorageRecipeTransferHandler delegate;

    public AdvancedStorageCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.delegate = new StorageRecipeTransferHandler(helper);
    }

    @Override
    public Class<AdvancedStorageCraftingDisplayMenu> getContainerClass() {
        return AdvancedStorageCraftingDisplayMenu.class;
    }

    @Override
    public Optional<MenuType<AdvancedStorageCraftingDisplayMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.ADVANCED_STORAGE_CRAFTING_DISPLAY.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            AdvancedStorageCraftingDisplayMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        // AdvancedStorageCraftingDisplayMenu extends StorageCoreCraftingMenu, so the
        // delegate's transferRecipe accepts it without issue.
        return delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
    }
}
