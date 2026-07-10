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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SmithingCategory implements IRecipeCategory<SmithingRecipe> {

    private final IDrawable background;
    private final IDrawable icon;

    private final IDrawable[] hammerFrames;

    public SmithingCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(150, 60);
        icon = guiHelper.createDrawableItemStack(new ItemStack(AllItems.SMITHING_HAMMER.get()));

        hammerFrames = new IDrawable[] {
                guiHelper.createDrawable(
                        ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_0.png"),
                        0, 0, 16, 16
                ),
                guiHelper.createDrawable(
                        ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_1.png"),
                        0, 0, 16, 16
                ),
                guiHelper.createDrawable(
                        ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_2.png"),
                        0, 0, 16, 16
                ),
                guiHelper.createDrawable(
                        ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_3.png"),
                        0, 0, 16, 16
                ),
                guiHelper.createDrawable(
                        ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_4.png"),
                        0, 0, 16, 16
                )
        };
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

        // Slots
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 20, 22);
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 110, 22);

        // Create arrow
        AllGuiTextures.JEI_ARROW.render(guiGraphics, 78, 26);

        // Hammer animation
        long time = System.currentTimeMillis() / 120;
        int frame = (int)(time % hammerFrames.length);

        hammerFrames[frame].draw(guiGraphics, 57, 22); // centered between slots

        // “×5 hits” centered under hammer
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                "×5 hits",
                65 - (Minecraft.getInstance().font.width("×5 hits") / 2),
                42,
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
