package com.bismarckshuffle.createvulcanized.extensions;

import com.bismarckshuffle.createvulcanized.registry.AllDataComponents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public class AndesiteAlloyBarDecorator implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        // 1. Fetch custom Data Component from the stack
        // (Replace 'ModDataComponents.PROGRESSION.get()' with your actual component registry reference)
        var componentValue = stack.get(AllDataComponents.PROGRESSION.get());

        // If the stack lacks the component, safely treat the progression value as 0
        int progressValue = (componentValue != null) ? componentValue.progress() : 0;

        // 2. Conditional visibility filter: Only render for values 1, 2, 3, and 4
        if (progressValue < 1 || progressValue > 4) {
            return false;
        }

        // 3. Mathematical scale translation
        // Maps the progress onto standard vanilla scale parameters (0.0 to 1.0)
        // With a maximum bound of 5, progress ranges from 20% to 80% visibility
        float progressFraction = (float) progressValue / 5.0F;
        int barWidth = Math.round(progressFraction * 13.0F);

        // 4. GUI Layout setup
        int barX = xOffset + 2;
        int barY = yOffset + 13;

        // Render the black backing bar (13x2 pixels)
        guiGraphics.fill(barX, barY, barX + 13, barY + 2, 0xFF000000);

        // Render the dynamic filled line (using a bright color choice like Orange/Yellow)
        int barColor = 0xFFFFAA00;
        guiGraphics.fill(barX, barY, barX + barWidth, barY + 1, barColor);

        return true;
    }
}
