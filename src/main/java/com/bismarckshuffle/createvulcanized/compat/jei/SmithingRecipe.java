package com.bismarckshuffle.createvulcanized.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

public class SmithingRecipe {

    public static final RecipeType<SmithingRecipe> TYPE =
            RecipeType.create("createvulcanized", "smithing", SmithingRecipe.class);

    public final ItemStack input;
    public final ItemStack output;

    public SmithingRecipe(ItemStack input, ItemStack output) {
        this.input = input;
        this.output = output;
    }
}
