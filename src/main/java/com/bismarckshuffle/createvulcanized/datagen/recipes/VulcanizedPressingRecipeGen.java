package com.bismarckshuffle.createvulcanized.datagen.recipes;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.registry.AllItems;
import com.simibubi.create.api.data.recipe.PressingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public class VulcanizedPressingRecipeGen extends PressingRecipeGen {
    public VulcanizedPressingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateVulcanized.ID);

        @SuppressWarnings("unused")
        GeneratedRecipe ANDESITE_FASTENER = create("andesite_fastener", b -> b
                .require(com.simibubi.create.AllItems.ANDESITE_ALLOY.get())
                .output(AllItems.ANDESITE_FASTENER.get()));
    }
}
