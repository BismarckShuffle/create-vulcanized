package com.bismarckshuffle.createvulcanized;

import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

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
