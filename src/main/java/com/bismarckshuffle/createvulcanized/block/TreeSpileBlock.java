package com.bismarckshuffle.createvulcanized.block;

import com.bismarckshuffle.createvulcanized.AllBlockEntities;

import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

public class TreeSpileBlock extends HorizontalDirectionalBlock implements EntityBlock, IBE<TreeSpileBlockEntity>, IWrenchable {

    public static final MapCodec<TreeSpileBlock> CODEC = simpleCodec(TreeSpileBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ATTACHED_TO_TREE = BooleanProperty.create("attached_to_tree");

    public TreeSpileBlock(Properties properties) {
        super(properties);
        // 2. Register the default state using the FACING property inherited from the parent class
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ATTACHED_TO_TREE, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    // 3. Override this method to capture the player's look vector on placement
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // This grabs the direction the player is facing and assigns it to the block state
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(ATTACHED_TO_TREE, false);
    }

    // 4. Tell the engine that FACING is a valid property for this block configuration
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHED_TO_TREE);
    }

    @Override
    public Class<TreeSpileBlockEntity> getBlockEntityClass() {
        return TreeSpileBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TreeSpileBlockEntity> getBlockEntityType() {
        return AllBlockEntities.TREE_SPILE.get();
    }

    // Assign the server-side logic loop to Block Entity
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null; // Only calculate resin on the server
        return (lvl, pos, st, be) -> {
            if (be instanceof TreeSpileBlockEntity spileBe) {
                spileBe.tick(lvl, pos, st);
            }
        };
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // 1. SNEAK-DISMANTLE HANDLER: Checks if player is crouched with the wrench
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                // Remove the block from the world grid smoothly
                level.destroyBlock(pos, false, player);

                // Manually spawn your block item and insert it directly into the player's inventory slots
                ItemStack dropStack = new ItemStack(this.asItem());
                if (!player.getInventory().add(dropStack)) {
                    player.drop(dropStack, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. STANDARD ROTATION HANDLER: If just right-clicking normally, rotate and check trees
        InteractionResult result = IWrenchable.super.onWrenched(state, context);
        if (result.consumesAction() && !level.isClientSide()) {
            this.withBlockEntityDo(level, pos, spileBe -> {
                spileBe.forceTreeRecheck(level, pos, level.getBlockState(pos));
            });
        }

        return result;
    }
}
