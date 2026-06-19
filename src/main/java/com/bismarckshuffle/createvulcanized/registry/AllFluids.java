package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class AllFluids {

    // Registers "resin" as an official, physical thick fluid type
    public static final FluidEntry<BaseFlowingFluid.Flowing> RESIN = CreateVulcanized.REGISTRATE
            .fluid("resin",
                    ResourceLocation.fromNamespaceAndPath(CreateVulcanized.ID, "block/resin_still"),
                    ResourceLocation.fromNamespaceAndPath(CreateVulcanized.ID, "block/resin_flow")
            )
                    .lang("Resin")
            // Sets physical attributes using SimpleFluidType configurations
            .properties(b -> b.viscosity(2000)
                    .density(1400).descriptionId("fluid_type.createvulcanized.resin"))
            .fluidProperties(p -> p.levelDecreasePerBlock(3)
                    .tickRate(45)
                    .slopeFindDistance(1)
                    .explosionResistance(100f))
            .source(BaseFlowingFluid.Source::new)
            .bucket()
            .build()
            .register();

    public static void register() {
        // Forces class loading to trigger Registrate calls on initialization
    }
}
