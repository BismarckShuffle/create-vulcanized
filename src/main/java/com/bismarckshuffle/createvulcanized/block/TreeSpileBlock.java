package com.bismarckshuffle.createvulcanized.block;

import com.bismarckshuffle.createvulcanized.AllBlockEntities;

import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // This tells Minecraft the block actually accepts the horizontal facing property
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ATTACHED_TO_TREE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        // Ensure we only run structural checks on the logical server thread to prevent network desyncs
        if (!level.isClientSide()) {
            this.withBlockEntityDo(level, pos, spileBe -> {
                // Forcefully invoke the tree check loop immediately upon world instantiation!
                spileBe.forceTreeRecheck(level, pos, state);
            });
        }
    }

    // 3. Override this method to capture the player's look vector on placement
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 1. Grab the exact physical block face the player's crosshairs right-clicked!
        Direction clickedFace = context.getClickedFace();

        // 2. Default fallback: If they placed it from inside a block or from the top/bottom face,
        // fallback cleanly to their standard player looking direction.
        Direction placementFacing = context.getHorizontalDirection();

        // 3. SMART ROTATION: If they clicked the side of a log, lock the spile's facing
        // directly to that wall surface, completely ignoring diagonal player angles!
        if (clickedFace.getAxis().isHorizontal()) {
            placementFacing = clickedFace;
        }

        // 4. Match the layout orientation cleanly to your model's Blockbench facing.
        // If your spile places backward after this, swap `placementFacing` with `placementFacing.getOpposite()`
        return this.defaultBlockState()
                .setValue(FACING, placementFacing)
                .setValue(ATTACHED_TO_TREE, false);
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
                spileBe.tick(lvl, pos, st, spileBe);
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

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        if (!level.isClientSide()) {
            // 1. Find the exact coordinate where the tree bark log is supposed to be
            Direction facing = state.getValue(FACING);
            BlockPos logPos = pos.relative(facing.getOpposite());

            // 2. If the block update happened to that specific log position...
            if (neighborPos.equals(logPos)) {
                BlockState currentLogState = level.getBlockState(logPos);

                // 3. Check if the block is no longer a valid log (e.g., turned to air or broken)
                if (!currentLogState.is(net.minecraft.tags.BlockTags.LOGS)) {
                    // Instantly break the spile smoothly and drop it as an item frame onto the floor!
                    level.destroyBlock(pos, true);
                    return;
                }

                // 4. Optimization: If the log is still there but changed types, wake up the scanner
                // to verify the canopy leaf count is still valid
                this.withBlockEntityDo(level, pos, spileBe -> {
                    spileBe.forceTreeRecheck(level, pos, state);
                });
            }
        }
    }

    // 5. Tell the placement engine that this block requires a valid wall attachment surface to survive
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos logPos = pos.relative(facing.getOpposite());
        return level.getBlockState(logPos).is(net.minecraft.tags.BlockTags.LOGS);
    }
}
