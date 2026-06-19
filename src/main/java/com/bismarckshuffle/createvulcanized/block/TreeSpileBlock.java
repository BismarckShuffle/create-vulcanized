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
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TreeSpileBlock extends HorizontalDirectionalBlock implements EntityBlock, IBE<TreeSpileBlockEntity>, IWrenchable {

    public static final MapCodec<TreeSpileBlock> CODEC = simpleCodec(TreeSpileBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ATTACHED_TO_TREE = BooleanProperty.create("attached_to_tree");

    // TALL WORKING HITBOX: Expanded full-width bounds for when the spile is active (16x16 wide, 24 high)
    private static final VoxelShape TALL_ATTACHED_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 24, 16.0D);

    // SHORT IDLE HITBOX: Standard single-block boundaries for when the spile is unattached/placed anywhere else
    private static final VoxelShape SHORT_IDLE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);


    public TreeSpileBlock(Properties properties) {
        super(properties);
        // Register the default state using the FACING property inherited from the parent class
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
        builder.add(FACING, ATTACHED_TO_TREE);
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction placementFacing = context.getHorizontalDirection();

        if (clickedFace.getAxis().isHorizontal()) {
            placementFacing = clickedFace;
        }

        // Checks if placed against a tag-valid log directly upon world initialization
        boolean isTree = context.getLevel().getBlockState(context.getClickedPos().relative(placementFacing.getOpposite())).is(net.minecraft.tags.BlockTags.LOGS);

        return this.defaultBlockState()
                .setValue(FACING, placementFacing)
                .setValue(ATTACHED_TO_TREE, isTree);
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

            this.withBlockEntityDo(level, pos, spileBe -> spileBe.forceTreeRecheck(level, pos, newState));
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ATTACHED_TO_TREE) ? TALL_ATTACHED_SHAPE : SHORT_IDLE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(ATTACHED_TO_TREE)) {
            // Returns a full 2-block-tall solid physical wall to the building engine
            return Block.box(0.0D, 4.0D, 0.0D, 16.0D, 32.0D, 16.0D);
        }
        return SHORT_IDLE_SHAPE;
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
    // Tells the placement system that if something tries to click the top area of the block space,
    // it cannot overwrite or clip through this block's extended boundaries.
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }
}
