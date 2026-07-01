package com.bismarckshuffle.createvulcanized.datagen.recipes;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.registry.AllItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;


import java.util.concurrent.CompletableFuture;

public class VulcanizedVanillaRecipeGen extends RecipeProvider {

    public VulcanizedVanillaRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(AllItems.RAW_RUBBER_SHEET.get()),
                        RecipeCategory.MISC,
                        AllItems.VULCANIZED_RUBBER_STRIPS.get(),
                        0.1f,
                        100
                )
                .unlockedBy("has_raw_rubber_sheet", has(AllItems.RAW_RUBBER_SHEET.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(CreateVulcanized.ID, "blasting/vulcanized_rubber_strips"));
    }
}
