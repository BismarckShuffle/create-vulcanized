package com.bismarckshuffle.createvulcanized;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = CreateVulcanized.ID, value = Dist.CLIENT)
public class ClientModEvents {

//    @SubscribeEvent
//    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
//        // Maps the texture assets explicitly to your Resin fluid type
//        event.registerFluidType(new IClientFluidTypeExtensions() {
//            @Override
//            public ResourceLocation getStillTexture() {
//                return ResourceLocation.fromNamespaceAndPath(CreateVulcanized.ID, "block/resin_still");
//            }
//
//            @Override
//            public ResourceLocation getFlowingTexture() {
//                return ResourceLocation.fromNamespaceAndPath(CreateVulcanized.ID, "block/resin_flow");
//            }
//        }, AllFluids.RESIN.get().getFluidType());
//    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Binds the solid graphic layer safely to both your source and flowing resin block variants
            ItemBlockRenderTypes.setRenderLayer(AllFluids.RESIN.get().getSource(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(AllFluids.RESIN.get().getFlowing(), RenderType.solid());
        });
    }
}
