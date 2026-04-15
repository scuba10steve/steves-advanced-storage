package io.github.scuba10steve.s3.advanced.jei;

import io.github.scuba10steve.s3.advanced.StevesAdvancedStorage;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class S3AdvancedJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(StevesAdvancedStorage.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            new RecipePatternTransferHandler(registration.getTransferHelper()),
            RecipeTypes.CRAFTING
        );
        registration.addRecipeTransferHandler(
            new AdvancedStorageCraftingTransferHandler(registration.getTransferHelper()),
            RecipeTypes.CRAFTING
        );
    }
}
