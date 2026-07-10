package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.components.ProgressionComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AllDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "createvulcanized");

    public static final Supplier<DataComponentType<ProgressionComponent>> PROGRESSION =
            DATA_COMPONENT_TYPES.register("progression", () -> DataComponentType.<ProgressionComponent>builder()
                    .persistent(ProgressionComponent.CODEC)
                    .networkSynchronized(ProgressionComponent.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}

