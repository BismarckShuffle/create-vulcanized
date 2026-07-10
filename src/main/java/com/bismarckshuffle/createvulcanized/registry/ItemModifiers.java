package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.components.ProgressionComponent;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = CreateVulcanized.ID)
public class ItemModifiers {

    @SubscribeEvent
    public static void modifyItems(ModifyDefaultComponentsEvent event) {

        Item andesiteAlloy = com.simibubi.create.AllItems.ANDESITE_ALLOY.get();

        if (andesiteAlloy != BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getDefaultKey())) {
            event.modify(andesiteAlloy, builder -> builder.set(AllDataComponents.PROGRESSION.get(), new ProgressionComponent(0, 5)));
        }
    }
}
