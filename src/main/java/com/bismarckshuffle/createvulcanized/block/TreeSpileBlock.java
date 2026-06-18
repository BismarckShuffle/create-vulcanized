package com.bismarckshuffle.createvulcanized.block;

import com.bismarckshuffle.createvulcanized.AllBlockEntities;

import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
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

        // 1. CROUCH + WRENCH DISMANTLE HANDLER (WITH INVENTORY RETENTION)
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                // Prepare the item drop stack frame
                ItemStack dropStack = new ItemStack(this.asItem());

                // Fetch the active Block Entity before erasing it from the grid world
                this.withBlockEntityDo(level, pos, spileBe -> {
                    // Instruct the Block Entity to write its full data (including its fluid tank) into a transient NBT compound
                    net.minecraft.nbt.CompoundTag blockEntityData = spileBe.saveWithFullMetadata(level.registryAccess());

                    // Minecraft 1.21 uses Data Components! Remove world-specific coordinate tags so it stacks cleanly
                    blockEntityData.remove("id");
                    blockEntityData.remove("x");
                    blockEntityData.remove("y");
                    blockEntityData.remove("z");

                    // Bake the compound tag directly into the item's BLOCK_ENTITY_DATA component layer
                    if (!blockEntityData.isEmpty()) {
                        dropStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityData));
                    }
                });

                // Clear the block from the world grid safely (false avoids triggering standard loot drops)
                level.destroyBlock(pos, false, player);

                // Insert the custom cargo-loaded item directly into the player's inventory bar
                if (!player.getInventory().add(dropStack)) {
                    player.drop(dropStack, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. ANY-SIDE HORIZONTAL ROTATION OVERRIDE
        if (!level.isClientSide()) {
            Direction currentFacing = state.getValue(FACING);
            Direction nextFacing = currentFacing.getClockWise();
            BlockState newState = state.setValue(FACING, nextFacing);
            level.setBlock(pos, newState, 3);
            IWrenchable.playRotateSound(level, pos);

            this.withBlockEntityDo(level, pos, spileBe -> {
                spileBe.forceTreeRecheck(level, pos, newState);
            });
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
