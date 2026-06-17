package com.bismarckshuffle.createvulcanized.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class TreeSpileBlock extends HorizontalDirectionalBlock {

    public TreeSpileBlock(Properties properties) {
        super(properties);
        // 2. Register the default state using the FACING property inherited from the parent class
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }

    // 3. You MUST override this method to capture the player's look vector on placement!
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // This grabs the direction the player is facing and assigns it to the block state
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    // 4. Tell the engine that FACING is a valid property for this block configuration
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
