package com.bismarckshuffle.createvulcanized;

import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;
import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreateVulcanized.ID)
public class ModWorldEvents {

    // A simple queue to hold spile rechecks that need to run on the next tick
    private static final List<Runnable> scheduledTasks = new ArrayList<>();

    @SubscribeEvent
    public static void onTreeStructureChange(BlockEvent.BreakEvent event) {
        if (!event.getState().is(net.minecraft.tags.BlockTags.LOGS) && !event.getState().is(net.minecraft.tags.BlockTags.LEAVES)) {
            return;
        }
        handleBlockChange(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onTreeStructurePlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getState().is(net.minecraft.tags.BlockTags.LOGS) && !event.getState().is(net.minecraft.tags.BlockTags.LEAVES)) {
            return;
        }
        handleBlockChange(event.getLevel(), event.getPos());
    }

    private static void handleBlockChange(net.minecraft.world.level.LevelAccessor levelAccessor, BlockPos changedPos) {
        if (!(levelAccessor instanceof Level level) || level.isClientSide()) return;

        // Perform a quick 5-block horizontal tracking pass to notify neighboring spiles
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int heightOffset = 0; heightOffset < 5; heightOffset++) {
                BlockPos potentialSpilePos = changedPos.below(heightOffset).relative(dir);
                BlockState spileState = level.getBlockState(potentialSpilePos);

                if (spileState.getBlock() instanceof TreeSpileBlock) {
                    if (spileState.getValue(TreeSpileBlock.FACING) == dir) {
                        BlockEntity be = level.getBlockEntity(potentialSpilePos);
                        if (be instanceof TreeSpileBlockEntity spileBe) {

                            // This schedules the check to run on the server thread 1 tick later,
                            // After the log has officially been deleted and turned to air.
                            synchronized (scheduledTasks) {
                                scheduledTasks.add(() -> {
                                    // Re-fetch the state in case it changed during the tick
                                    BlockState freshState = level.getBlockState(potentialSpilePos);
                                    if (freshState.getBlock() instanceof TreeSpileBlock) {
                                        spileBe.forceTreeRecheck(level, potentialSpilePos, freshState);
                                    }
                                });
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
    // This runs automatically every game tick on the server thread to execute the queued checks
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        List<Runnable> tasksToRun;
        synchronized (scheduledTasks) {
            if (scheduledTasks.isEmpty()) return;
            tasksToRun = new ArrayList<>(scheduledTasks);
            scheduledTasks.clear();
        }
        // Execute all the scheduled spile rechecks safely
        for (Runnable task : tasksToRun) {
            task.run();
        }
    }
}
