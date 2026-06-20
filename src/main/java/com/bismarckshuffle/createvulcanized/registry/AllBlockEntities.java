package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;

public class AllBlockEntities extends Block {
    public AllBlockEntities(Properties properties) {
        super(properties);
    }

    public static final BlockEntityEntry<TreeSpileBlockEntity> TREE_SPILE = CreateVulcanized.REGISTRATE
            .blockEntity("tree_spile", TreeSpileBlockEntity::new)
            .validBlocks(AllBlocks.TREE_SPILE)
            .register();

    public static void register() {}
}
