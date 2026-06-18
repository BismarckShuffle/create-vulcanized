package com.bismarckshuffle.createvulcanized;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateVulcanized.ID)
public class CreateVulcanized {
    public static final String ID = "createvulcanized";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );

    public CreateVulcanized(IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);

        AllCreativeModeTabs.register();
        REGISTRATE.setCreativeTab(AllCreativeModeTabs.MAIN_TAB);
        AllItems.register();
        AllBlocks.register();
        AllBlockEntities.register();

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::registerCapabilities);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common setup...");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Client setup...");
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(AllBlocks.TREE_SPILE.get(), RenderType.cutoutMipped());
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Bind a fluid capability handler specifically to Tree Spile Block Entity type
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllBlockEntities.TREE_SPILE.get(),
                (spileBe, direction) -> {
                    // CONSTRAINT: Only expose the tank if a pipe is looking at the BOTTOM face
                    // This is where the copper base is
                    if (direction == Direction.DOWN) {
                        return spileBe.fluidTank;
                    }
                    return null; // Ignore connections from the top or sides
                }
        );
    }
}
