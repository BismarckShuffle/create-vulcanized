package com.bismarckshuffle.createvulcanized.compat.jei;

import com.bismarckshuffle.createvulcanized.registry.AllItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
@JeiPlugin
public class VulcanizedJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("createvulcanized", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        System.out.println("JEI: Registering SmithingCategory");
        registration.addRecipeCategories(
                new SmithingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {

        registration.addRecipes(
                SmithingRecipe.TYPE,
                java.util.List.of(
                        new SmithingRecipe(
                                new ItemStack(com.simibubi.create.AllItems.ANDESITE_ALLOY.get()),
                                new ItemStack(AllItems.ANDESITE_FASTENER.get())
                        )
                )
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(AllItems.SMITHING_HAMMER.get()),
                SmithingRecipe.TYPE
        );
    }
}
