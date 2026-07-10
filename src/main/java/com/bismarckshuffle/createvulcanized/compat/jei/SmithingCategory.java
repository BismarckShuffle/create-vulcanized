package com.bismarckshuffle.createvulcanized.compat.jei;

import com.bismarckshuffle.createvulcanized.registry.AllItems;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SmithingCategory implements IRecipeCategory<SmithingRecipe> {

    private final IDrawable background;
    private final IDrawable icon;

    public SmithingCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(150, 60);
        icon = guiHelper.createDrawableItemStack(new ItemStack(AllItems.SMITHING_HAMMER.get()));
    }

    @Override
    public RecipeType<SmithingRecipe> getRecipeType() {
        return SmithingRecipe.TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Smithing");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(SmithingRecipe recipe,
                     IRecipeSlotsView view,
                     GuiGraphics guiGraphics,
                     double mouseX,
                     double mouseY)
    {
        background.draw(guiGraphics, 0, 0);

        AllGuiTextures.JEI_SLOT.render(guiGraphics, 20, 22);
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 110, 22);
        AllGuiTextures.JEI_ARROW.render(guiGraphics, 66, 28);


        guiGraphics.renderItem(new ItemStack(AllItems.SMITHING_HAMMER.get()), 66, 22);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                "×5 hits",
                55, 40,
                0x404040,
                false
        );


    }


    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SmithingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 22)
                .addItemStack(recipe.input);

        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 22)
                .addItemStack(recipe.output);
    }
}
