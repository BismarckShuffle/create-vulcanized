package com.bismarckshuffle.createvulcanized.datagen.recipes;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.registry.AllFluids;
import com.bismarckshuffle.createvulcanized.registry.AllItems;
import com.simibubi.create.api.data.recipe.CompactingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public class VulcanizedCompactingRecipeGen extends CompactingRecipeGen {
    public VulcanizedCompactingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateVulcanized.ID);

        @SuppressWarnings("unused")
        GeneratedRecipe RAW_RUBBER_SHEET = create("raw_rubber_sheet", b -> b
                .require(AllFluids.RESIN.get(), 250)
                .output(AllItems.RAW_RUBBER_SHEET.get()));
    }
}
