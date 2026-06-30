package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.items.RubberSheetItem;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

import static com.bismarckshuffle.createvulcanized.CreateVulcanized.REGISTRATE;

/**
 * Item registration using Create's Registrate.
 */
public class AllItems {

    public static final ItemEntry<RubberSheetItem> RAW_RUBBER_SHEET =
            REGISTRATE.item("raw_rubber_sheet", RubberSheetItem::new)
                    .properties(p -> p.stacksTo(64))
                    .lang("Raw Rubber Sheet")
                    .register();

    @SuppressWarnings("unused")
    public static final ItemEntry<Item> VULCANIZED_RUBBER_STRIPS =
            REGISTRATE.item("vulcanized_rubber_strips", Item::new)
                    .properties(p -> p.stacksTo(64))
                    .lang("Vulcanized Rubber Strips")
                    .register();


    public static void register() {
        // Force class loading to trigger Registrate calls
    }
}
