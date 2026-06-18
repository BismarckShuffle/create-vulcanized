package com.bismarckshuffle.createvulcanized.blockentity;

import com.bismarckshuffle.createvulcanized.AllFluids;
import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class TreeSpileBlockEntity extends BlockEntity {

    // Adjust these values to balance your mod!
    private static final int TICK_DELAY_SECONDS = 10;
    private static final int RESIN_PER_CYCLE = 50; // Millibuckets (mB). 1000mB = 1 Bucket.
    private static final int TICK_INTERVAL = TICK_DELAY_SECONDS * 20; // 20 ticks = 1 second

    protected final FluidTank fluidTank = new FluidTank(1000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == AllFluids.RESIN.get();
        }
    };

    private int validationTimer = 0;
    private int operationTimer = 0;

    public TreeSpileBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank getFluidTank() {
        return this.fluidTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FluidTank")) {
            this.fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        }
    }

    // Keep your exact tick execution loop body here!
    public static void tick(Level level, BlockPos pos, BlockState state, TreeSpileBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.validationTimer++;
        if (blockEntity.validationTimer >= 100) {
            blockEntity.validationTimer = 0;
            boolean isTreeStillValid = blockEntity.checkTreeStructure(level, pos, state);
            if (state.getValue(TreeSpileBlock.ATTACHED_TO_TREE) != isTreeStillValid) {
                level.setBlock(pos, state.setValue(TreeSpileBlock.ATTACHED_TO_TREE, isTreeStillValid), 3);
            }
        }

        if (state.getValue(TreeSpileBlock.ATTACHED_TO_TREE)) {
            blockEntity.operationTimer++;
            if (blockEntity.operationTimer >= 200) {
                blockEntity.operationTimer = 0;
                FluidStack resinDrip = new FluidStack(AllFluids.RESIN.get(), 20);
                blockEntity.getFluidTank().fill(resinDrip, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
        } else {
            blockEntity.operationTimer = 0;
        }
    }

    private boolean checkTreeStructure(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(TreeSpileBlock.FACING);
        // The tree wood must be located directly behind the block orientation
        BlockPos currentLogPos = pos.relative(facing.getOpposite());

        // 1. Trace down to ground or look up to find a sequence of 3 wood logs
        int logCount = 0;
        for (int i = 0; i < 5; i++) { // Check up to 5 blocks high for safety
            BlockState logState = level.getBlockState(currentLogPos.above(i));
            if (logState.is(BlockTags.LOGS)) {
                logCount++;
            } else {
                break;
            }
        }

        if (logCount < 3) return false;

        // 2. Scan the immediate canopy area above the log tower for leaves
        BlockPos canopyPos = currentLogPos.above(logCount);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (level.getBlockState(canopyPos.offset(x, 0, z)).is(BlockTags.LEAVES)) {
                    return true; // Found a valid log stack capped with leaves!
                }
            }
        }

        return false;
    }

    public void forceTreeRecheck(Level level, BlockPos pos, BlockState state) {
        this.validationTimer = 0; // Reset your optimization timer

        // Run your array log/leaf verification logic immediately
        boolean isLegitTree = this.checkTreeStructure(level, pos, state);

        // Forcefully apply the true/false visual blockstate look to the world grid
        if (state.getValue(TreeSpileBlock.ATTACHED_TO_TREE) != isLegitTree) {
            level.setBlock(pos, state.setValue(TreeSpileBlock.ATTACHED_TO_TREE, isLegitTree), 3);
        }
    }
}
