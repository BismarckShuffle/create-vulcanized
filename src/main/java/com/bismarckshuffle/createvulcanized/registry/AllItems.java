package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.items.RubberSheetItem;
import com.tterrag.registrate.util.entry.ItemEntry;

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


    public static void register() {
        // Force class loading to trigger Registrate calls
    }
}
