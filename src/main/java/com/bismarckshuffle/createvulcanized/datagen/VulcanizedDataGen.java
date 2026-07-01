package com.bismarckshuffle.createvulcanized.datagen;

import com.bismarckshuffle.createvulcanized.datagen.recipes.VulcanizedCompactingRecipeGen;
import com.bismarckshuffle.createvulcanized.datagen.recipes.VulcanizedVanillaRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public class VulcanizedDataGen {
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        gen.addProvider(event.includeServer(),
                new VulcanizedCompactingRecipeGen(output, registries));
        gen.addProvider(event.includeServer(),
                new VulcanizedVanillaRecipeGen(output, registries));
    }
}
