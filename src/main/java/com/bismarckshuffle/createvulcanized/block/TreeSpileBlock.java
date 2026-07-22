package com.bismarckshuffle.createvulcanized.block;

import com.bismarckshuffle.createvulcanized.registry.AllBlockEntities;

import com.bismarckshuffle.createvulcanized.registry.AllFluids;
import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public class TreeSpileBlock extends HorizontalDirectionalBlock implements EntityBlock, IBE<TreeSpileBlockEntity>, IWrenchable  {

    public static final MapCodec<TreeSpileBlock> CODEC = simpleCodec(TreeSpileBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ATTACHED_TO_TREE = BooleanProperty.create("attached_to_tree");

    // Taller outline used while the spile is attached to a tree.
    private static final VoxelShape TALL_ATTACHED_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 24, 16.0D);

    // Normal one-block outline used when the spile is idle.
    private static final VoxelShape SHORT_IDLE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    // Server-side block entity ticker.
    // Cache the ticker instance once instead of creating it per getTicker call
    private static final BlockEntityTicker<TreeSpileBlockEntity> TICKER =
            (lvl, pos, st, be) -> be.tick();

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (BlockEntityTicker<T>) TICKER;
    }

    public TreeSpileBlock(Properties properties) {
        super(properties);
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
        builder.add(FACING, ATTACHED_TO_TREE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!level.isClientSide()) {
            this.withBlockEntityDo(level, pos, spileBe -> spileBe.forceTreeRecheck(level, pos, state));
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

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // Shift-wrench picks the block up with its stored data.
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                ItemStack dropStack = new ItemStack(this.asItem());

                this.withBlockEntityDo(level, pos, spileBe -> {
                    // Drop the bucket slot contents separately.
                    ItemStackHandler inv = spileBe.getInventoryHandler();
                    for (int i = 0; i < inv.getSlots(); i++) {
                        ItemStack slotStack = inv.getStackInSlot(i);
                        if (!slotStack.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slotStack);
                        }
                    }

                    net.minecraft.nbt.CompoundTag blockEntityData = spileBe.saveWithoutMetadata(level.registryAccess());

                    if (!blockEntityData.isEmpty()) {
                        dropStack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(blockEntityData));
                    }
                });

                level.destroyBlock(pos, false, player);

                if (!player.getInventory().add(dropStack)) {
                    player.drop(dropStack, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Normal wrench use rotates the spile.
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

    // Prevent placing through the extended model bounds.
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof WrenchItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TreeSpileBlockEntity spileBe) {
            net.neoforged.neoforge.fluids.capability.IFluidHandler tankHandler = spileBe.getFluidTank();

            // Resin bucket -> tank.
            if (stack.is(AllFluids.RESIN.get().getBucket())) {
                int currentFluid = tankHandler.getFluidInTank(0).getAmount();
                int emptySpace = TreeSpileBlockEntity.MAX_CAPACITY - currentFluid;

                // Only allow the player to dump a bucket if there is at least 800mB of empty space left
                if (emptySpace >= 800) {
                    int actualFillAmount = Math.min(1000, emptySpace);
                    FluidStack topOff = new FluidStack(AllFluids.RESIN.get(), actualFillAmount);
                    tankHandler.fill(topOff, IFluidHandler.FluidAction.EXECUTE);

                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                        if (!player.getInventory().add(emptyBucket)) {
                            player.drop(emptyBucket, false);
                        }
                    }

                    level.blockEntityChanged(pos);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Receptacle is too full to accept a bucket"), true);
                }
                return ItemInteractionResult.SUCCESS;
            }

            // Empty bucket -> resin bucket.
            if (stack.is(Items.BUCKET)) {
                boolean transactionSuccess = FluidUtil.interactWithFluidHandler(player, hand, tankHandler);
                if (transactionSuccess) {
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                    level.blockEntityChanged(pos);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // Non-fluid interaction opens the GUI.
            player.openMenu(spileBe, (net.minecraft.network.RegistryFriendlyByteBuf buffer) -> buffer.writeBlockPos(pos));
            spileBe.startOpen(player);
            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                net.minecraft.world.level.block.entity.BlockEntity entity = level.getBlockEntity(pos);
                if (entity instanceof TreeSpileBlockEntity spileBe) {

                    // Drop inventory contents before the block entity is removed.
                    ItemStackHandler inv = spileBe.getInventoryHandler();
                    for (int i = 0; i < inv.getSlots(); i++) {
                        ItemStack slotStack = inv.getStackInSlot(i);
                        if (!slotStack.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slotStack);
                        }
                    }

                    // Preserve fluid contents on the block item drop.
                    ItemStack spileDrop = new ItemStack(this.asItem());
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    tag.put("FluidTank", spileBe.getFluidTank().writeToNBT(level.registryAccess(), new net.minecraft.nbt.CompoundTag()));

                    if (!spileBe.getFluidTank().isEmpty()) {
                        spileDrop.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                    }

                    net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, spileDrop);

                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);

        // Only the server has reliable block entity data here.
        if (level instanceof Level world && !world.isClientSide()) {
            net.minecraft.world.level.block.entity.BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TreeSpileBlockEntity spileBe) {
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.put("FluidTank", spileBe.getFluidTank().writeToNBT(world.registryAccess(), new net.minecraft.nbt.CompoundTag()));
                stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
            }
        }
        return stack;
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        // Drop handling is done in onRemove so the fluid data can be copied first.
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.createvulcanized.tree_spile.desc")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.createvulcanized.tree_spile.warning")
                .withStyle(ChatFormatting.GOLD));
    }
}
