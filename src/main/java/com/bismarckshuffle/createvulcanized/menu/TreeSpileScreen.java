package com.bismarckshuffle.createvulcanized.menu;

import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions; // Essential NeoForge Import
import net.neoforged.neoforge.fluids.FluidStack;

public class TreeSpileScreen extends AbstractSimiContainerScreen<TreeSpileMenu> {

    public TreeSpileScreen(TreeSpileMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        // Core standard window sizing to fit the unified block layout card
        setWindowSize(176, 186);
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. TOP INDEPENDENT READOUT MODULE: Render a clean, standalone light-gray card face (162x84)
        // Left edge starts at x + 7 and ends at x + 169 to perfectly match the width of the lower inventory plate
        graphics.fill(x + 7, y + 4, x + 169, y + 88, 0xFFC6C6C6);

        // Complete standalone dark gray border frame for the top module
        graphics.fill(x + 7, y + 4, x + 169, y + 5, 0xFF555555);         // Top edge
        graphics.fill(x + 7, y + 4, x + 8, y + 88, 0xFF555555);         // Left edge
        graphics.fill(x + 168, y + 4, x + 169, y + 88, 0xFF555555);     // Right edge
        graphics.fill(x + 7, y + 87, x + 169, y + 88, 0xFF555555);     // NEW: Bottom edge closes the shape cleanly

        // Complete standalone interior white highlight rules for a sharp 3D look
        graphics.fill(x + 8, y + 5, x + 168, y + 6, 0xFFFFFFFF);        // Inner top highlight
        graphics.fill(x + 8, y + 5, x + 9, y + 87, 0xFFFFFFFF);        // Inner left highlight

        // 2. BOTTOM PLAYER INVENTORY DECK: Shifted down to y + 92 to create an intentional, clean modular gap
        // This allows the texture's built-in top shadow to render completely unhindered as part of its own shape
        AllGuiTextures.PLAYER_INVENTORY.render(graphics, x, y + 92);

        // 3. TEXT DISPLAY CUTOUT WINDOW WITH INSET DEPTH SHADING BORDERS
        int boxXStart = x + 40;
        int boxYStart = y + 14;
        int boxXEnd = x + 138;
        int boxYEnd = y + 78;

        graphics.fill(boxXStart, boxYStart, boxXEnd, boxYEnd, 0xFF3A3A3A);

        graphics.fill(boxXStart - 1, boxYStart - 1, boxXEnd + 1, boxYStart, 0xFF373737); // Top shadow
        graphics.fill(boxXStart - 1, boxYStart - 1, boxXStart, boxYEnd + 1, 0xFF373737); // Left shadow
        graphics.fill(boxXStart, boxYEnd, boxXEnd + 1, boxYEnd + 1, 0xFFFFFFFF);         // Bottom highlight
        graphics.fill(boxXEnd, boxYStart, boxXEnd + 1, boxYEnd + 1, 0xFFFFFFFF);         // Right highlight

        // 4. DRAW BUCKET INTERACTIVE INPUT SLOT WIREFRAME WITH DEPTH SHADING
        int slotX = x + 18;
        int slotY = y + 37;
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
        graphics.fill(slotX - 1, slotY - 1, slotX + 18, slotY, 0xFF373737);
        graphics.fill(slotX - 1, slotY - 1, slotX, slotY + 18, 0xFF373737);
        graphics.fill(slotX, slotY + 17, slotX + 19, slotY + 18, 0xFFFFFFFF);
        graphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF);

        // 5. GRADUATED CYLINDER FLUID METER WITH INSET DEPTH SHADING BORDERS
        FluidStack storedFluid = this.menu.blockEntity.getFluidTank().getFluid();
        int currentResin = storedFluid.getAmount();
        int maxCapacity = TreeSpileBlockEntity.MAX_CAPACITY;

        int meterXStart = x + 144;
        int meterYStart = y + 14;
        int meterXEnd = x + 162;
        int meterYEnd = y + 78;
        int maxBarHeight = meterYEnd - meterYStart - 2;

        graphics.fill(meterXStart, meterYStart, meterXEnd, meterYEnd, 0xFF1A1A1A);

        graphics.fill(meterXStart - 1, meterYStart - 1, meterXEnd + 1, meterYStart, 0xFF373737);
        graphics.fill(meterXStart - 1, meterYStart - 1, meterXStart, meterYEnd + 1, 0xFF373737);
        graphics.fill(meterXStart, meterYEnd, meterXEnd + 1, meterYEnd + 1, 0xFFFFFFFF);
        graphics.fill(meterXEnd, meterYStart, meterXEnd + 1, meterYEnd + 1, 0xFFFFFFFF);

        if (currentResin > 0) {
            float ratio = (float) currentResin / (float) maxCapacity;
            int fluidHeight = (int) ((float) maxBarHeight * ratio);

            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(storedFluid.getFluid());
            ResourceLocation stillTextureLocation = extensions.getStillTexture(storedFluid);

            TextureAtlasSprite fluidSprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(stillTextureLocation);

            if (fluidSprite != null) {
                graphics.blit(
                        meterXStart + 1,
                        (meterYEnd - 1) - fluidHeight,
                        0,
                        16,
                        fluidHeight,
                        fluidSprite
                );
            }
        }

        int tickInterval = 250;
        for (int volumeMarker = tickInterval; volumeMarker < maxCapacity; volumeMarker += tickInterval) {
            float markerRatio = (float) volumeMarker / (float) maxCapacity;
            int pixelOffsetFromBottom = (int) ((float) maxBarHeight * markerRatio);
            int markerYLine = (meterYEnd - 1) - pixelOffsetFromBottom;

            int tickWidth = 2;
            if (volumeMarker % 1000 == 0) {
                tickWidth = 8;
            } else if (volumeMarker % 500 == 0) {
                tickWidth = 4;
            }

            graphics.fill(meterXStart + 1, markerYLine, meterXStart + 1 + tickWidth, markerYLine + 1, 0xFFFFFFFF);
            graphics.fill(meterXStart + 1, markerYLine + 1, meterXStart + 1 + tickWidth, markerYLine + 2, 0x44000000);
        }

        graphics.drawString(this.font, "Spile Reservoir", boxXStart + 8, boxYStart + 10, 0xCC9933, false);
        graphics.drawString(this.font, currentResin + " / " + maxCapacity + " mB", boxXStart + 12, boxYStart + 36, 0xFFFFFF, false);
    }
}
