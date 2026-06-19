package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.menu.TreeSpileMenu;
import com.bismarckshuffle.createvulcanized.menu.TreeSpileScreen;
import com.tterrag.registrate.util.entry.MenuEntry;

public class AllMenuTypes {

    public static final MenuEntry<TreeSpileMenu> TREE_SPILE_MENU = CreateVulcanized.REGISTRATE
            .menu("tree_spile",
                    (type, windowId, inv, buf) -> {
                        assert buf != null;
                        return new TreeSpileMenu(windowId, inv, buf);
                    },
                    () -> TreeSpileScreen::new
            )
            .register();

    public static void register() {}
}
