package com.bismarckshuffle.createvulcanized;

import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Blocks;

/**
 * Block registration using Create's Registrate.
 * <p>
 * Example:
 * <pre>
 * public static final BlockEntry&lt;Block&gt; EXAMPLE_BLOCK = ExampleMod.REGISTRATE
 *         .block("example_block", Block::new)
 *         .simpleItem()
 *         .register();
 * </pre>
 */
public class AllBlocks {

    public static final BlockEntry<TreeSpileBlock> TREE_SPILE = CreateVulcanized.REGISTRATE
            .block("tree_spile", TreeSpileBlock::new)
            .initialProperties(() -> Blocks.STONE)
            .simpleItem()
            .register();

    public static void register() {
        // Force class loading to trigger Registrate calls
    }
}
