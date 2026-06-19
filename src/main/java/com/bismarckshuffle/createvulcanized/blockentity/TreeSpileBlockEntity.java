package com.bismarckshuffle.createvulcanized.blockentity;

import com.bismarckshuffle.createvulcanized.AllFluids;
import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class TreeSpileBlockEntity extends SmartBlockEntity {

//    // Adjust these values to balance your mod!
//    private static final int TICK_DELAY_SECONDS = 10;
//    private static final int RESIN_PER_CYCLE = 50; // Millibuckets (mB). 1000mB = 1 Bucket.
//    private static final int TICK_INTERVAL = TICK_DELAY_SECONDS * 20; // 20 ticks = 1 second
    // Internal timer to control how often the spile extracts resin
    private int extractionTimer = 0;
    private static final int TICK_DELAY = 100; // Extracts resin every 5 seconds (20 ticks = 1 second)

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

    public TreeSpileBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank getFluidTank() {
        return this.fluidTank;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // This is where you can add Create behaviors (like scroll values or progress bars) later
    }


    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        if (tag.contains("FluidTank")) {
            this.fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        // Create handles client/server filtering automatically.
        // We save the tank data regardless of whether it's a world save or network sync packet.
        tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    // This is the server-side tick loop called by your TreeSpileBlock getTicker method
    public void tick(Level level, BlockPos pos, BlockState state, TreeSpileBlockEntity be) {
        // STEP 1: If the spout model is unattached (tree is missing), STOP everything instantly!
        if (!state.getValue(TreeSpileBlock.ATTACHED_TO_TREE)) {
            be.extractionTimer = 0; // Reset progress
            return;
        }

        // STEP 2: Progress the timer if securely attached
        be.extractionTimer++;
        if (be.extractionTimer >= TICK_DELAY) {
            be.extractionTimer = 0; // Reset cycle

            // STEP 3: Find the block space directly underneath the spile snout
            Direction forward = state.getValue(TreeSpileBlock.FACING);
            BlockPos basinPos = pos.relative(forward).below();

            // STEP 4: Look for an interactive container/receptacle basin directly below
            BlockEntity targetEntity = level.getBlockEntity(basinPos);
            if (targetEntity != null) {
                // Fetch the fluid storage capabilities of the basin underneath
                var fluidCap = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, basinPos, Direction.UP);

                if (fluidCap != null) {
                    // Create a small droplet stack of your custom Resin fluid (20 mB)
                    FluidStack resinDroplet = new FluidStack(AllFluids.RESIN.get().getSource(), 20);

                    // Forcefully pump/fill the droplet straight into the basin below!
                    fluidCap.fill(resinDroplet, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    private boolean checkTreeStructure(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(TreeSpileBlock.FACING);
        BlockPos startLogPos = pos.relative(facing.getOpposite());

        // 1. Core Pillar Validation: Ensure all 3 required log spaces are completely solid wood
        int logCount = 0;
        for (int i = 0; i < 6; i++) {
            BlockState logState = level.getBlockState(startLogPos.above(i));
            if (logState.is(net.minecraft.tags.BlockTags.LOGS)) {
                logCount++;
            } else {
                break; // Stop counting the moment a block type breaks continuity
            }
        }

        // Minimum requirement of 3 blocks to form a stable tree pillar
        if (logCount < 3) return false;

        // 2. Fixed Canopy Scanning: Check for leaves right above our required 3-log pillar
        BlockPos canopyPos = startLogPos.above(logCount);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (level.getBlockState(canopyPos.offset(x, 0, z)).is(net.minecraft.tags.BlockTags.LEAVES)) {
                    return true; // Dynamic structure validation complete!
                }
            }

        }

        return false;
    }

    public void forceTreeRecheck(Level level, BlockPos pos, BlockState state) {

        // Run array log/leaf verification logic immediately
        boolean isLegitTree = this.checkTreeStructure(level, pos, state);

        // Forcefully apply the true/false visual block state look to the world grid
        if (state.getValue(TreeSpileBlock.ATTACHED_TO_TREE) != isLegitTree) {
            level.setBlock(pos, state.setValue(TreeSpileBlock.ATTACHED_TO_TREE, isLegitTree), 3);
        }
    }
}
