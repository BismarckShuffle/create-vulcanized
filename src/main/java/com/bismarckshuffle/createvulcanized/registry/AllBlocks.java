package com.bismarckshuffle.createvulcanized.registry;

import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;


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
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false))
            .transform(axeOrPickaxe())
            .loot((provider, block) -> provider.dropOther(block, net.minecraft.world.item.Items.AIR))
            .simpleItem()
            .register();

    public static void register() {
        // Force class loading to trigger Registrate calls
    }
}
