package com.bismarckshuffle.createvulcanized.blockentity;

import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private int progressTimer = 0;
    private int validationTimer = 0;

    // Create an internal FluidTank
    public final FluidTank fluidTank = new FluidTank(2000) {
        @Override

        public boolean isFluidValid(FluidStack stack) {
            return false; // We don't want fluid going back to the receptacle
        }                 // Holds up to 2 buckets
    };

    public TreeSpileBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        // Only validate the structural environment once every 5 seconds (100 ticks)
        validationTimer++;
        if (validationTimer >= 100) {
            validationTimer = 0;
            boolean isLegitTree = checkTreeStructure(level, pos, state);

            // If the environment state changed, dynamically update the block model look
            if (state.getValue(TreeSpileBlock.ATTACHED_TO_TREE) != isLegitTree) {
                level.setBlock(pos, state.setValue(TreeSpileBlock.ATTACHED_TO_TREE, isLegitTree), 3);
            }
        }

        // ONLY tick processing if fully attached to a healthy tree
        if (!state.getValue(TreeSpileBlock.ATTACHED_TO_TREE)) {
            progressTimer = 0; // Reset progress if the tree structure breaks mid-process
            return;
        }

        progressTimer++;
        if (progressTimer >= TICK_INTERVAL) {
            progressTimer = 0;
            fillWithResin();
        }
    }

    private void fillWithResin() {
        // TODO: Tie this directly to your registered Resin fluid instead of null
        // fluidTank.fill(new FluidStack(ModFluids.RESIN.get(), RESIN_PER_CYCLE), FluidAction.EXECUTE);

        this.setChanged(); // Tells Minecraft to save the Block Entity chunk data
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
