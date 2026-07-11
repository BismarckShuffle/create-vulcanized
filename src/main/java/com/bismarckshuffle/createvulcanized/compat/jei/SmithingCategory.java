package com.bismarckshuffle.createvulcanized.compat.jei;

import com.bismarckshuffle.createvulcanized.registry.AllItems;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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

    private final IDrawableStatic[] hammerFrames;

    public SmithingCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(150, 60);
        icon = guiHelper.createDrawableItemStack(new ItemStack(AllItems.SMITHING_HAMMER.get()));

        hammerFrames = new IDrawableStatic[] {
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_0.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build(), // Generates an IDrawableStatic instead of a standard IDrawable
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_1.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build(),
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_2.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build(),
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_3.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build(),
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_4.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build(),
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_5.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build(),
                guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath("createvulcanized", "textures/gui/jei/hammer_anim_6.png"), 0, 0, 128, 128)
                        .setTextureSize(128, 128)
                        .build()
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
        // Background
        background.draw(guiGraphics, 0, 0);

        // Slots
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 20, 22);
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 110, 22);

        // Arrow
//        AllGuiTextures.JEI_ARROW.render(guiGraphics, 78, 26);

        // Hammer animation frame logic
        long time = System.currentTimeMillis() / 120;
        int frame = (int)(time % hammerFrames.length);

        guiGraphics.pose().pushPose();

// 1. Move directly to your desired on-screen position (X: 57, Y: 14)
        guiGraphics.pose().translate(57, 14, 0);

// 2. Scale the 128x128px frame down to 25% size (making it exactly 32x32px)
        guiGraphics.pose().scale(0.25f, 0.25f, 1.0f);

// 3. Draw at 0, 0 because the translate step already handles the screen positioning
        hammerFrames[frame].draw(guiGraphics, 0, 0);

        guiGraphics.pose().popPose();

        // “×5 hits” text centered under hammer
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                "×5 hits",
                65 - (Minecraft.getInstance().font.width("×5 hits") / 2),
                48,
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
