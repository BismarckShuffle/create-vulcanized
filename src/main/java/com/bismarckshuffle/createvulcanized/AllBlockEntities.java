package com.bismarckshuffle.createvulcanized;

import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;
import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
