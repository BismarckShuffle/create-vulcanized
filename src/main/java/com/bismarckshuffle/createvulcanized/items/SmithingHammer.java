package com.bismarckshuffle.createvulcanized.items;

import com.bismarckshuffle.createvulcanized.registry.AllDataComponents;
import com.bismarckshuffle.createvulcanized.registry.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.block.state.BlockState;

import com.bismarckshuffle.createvulcanized.components.ProgressionComponent;


public class SmithingHammer extends Item {

    public SmithingHammer(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack hammer = ctx.getItemInHand();

        BlockState state = level.getBlockState(pos);

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        // Depot
        if (state.getBlock() instanceof DepotBlock) {
            DepotBlockEntity depot = (DepotBlockEntity) level.getBlockEntity(pos);
            if (depot == null)
                return InteractionResult.PASS;

            ItemStack stack = depot.getHeldItem();
            if (stack.isEmpty())
                return InteractionResult.PASS;

            ItemStack result = addProgress(stack, player, level);

            if (result != null)
                depot.setHeldItem(result);

            // IMPORTANT: prevent depot from running its own right-click logic
            return InteractionResult.CONSUME;
        }

        // Belt
        if (state.getBlock() instanceof BeltBlock) {
            BeltBlockEntity belt = (BeltBlockEntity) level.getBlockEntity(pos);
            if (belt == null)
                return InteractionResult.PASS;

            return hammerBelt(level, belt, player);
        }

        return InteractionResult.PASS;
    }

    // DEPOT HANDLING
    private InteractionResult hammerDepot(Level level, DepotBlockEntity depot, Player player) {
        ItemStack stack = depot.getHeldItem();

        if (stack.isEmpty())
            return InteractionResult.PASS;

        ItemStack result = addProgress(stack, player, level);

        if (result != null) {
            depot.setHeldItem(result);
        }

        return InteractionResult.SUCCESS;
    }

    // BELT HANDLING
    private InteractionResult hammerBelt(Level level, BeltBlockEntity belt, Player player) {
//        var inventory = belt.getInventory();
//
//        for (var transported : inventory.getTransportedItems()) {
//            ItemStack stack = transported.stackBefore;
//            if (stack.isEmpty())
//                continue;
//
//            ItemStack result = addProgress(stack, player, level);
//            if (result == null)
//                continue;
//
//            int segment = (int) transported.beltPosition;
//
//            // Get Create's processing behaviour at this segment
//            TransportedItemStackHandlerBehaviour handler =
//                    BlockEntityBehaviour.get(level,
//                            BeltHelper.getPositionForOffset(belt, segment),
//                            TransportedItemStackHandlerBehaviour.TYPE);
//
//            if (handler == null)
//                continue;
//
//            // Replace the transported item using Create's processing API
//            TransportedItemStackHandlerBehaviour.TransportedResult tr = TransportedItemStackHandlerBehaviour.TransportedResult.convertTo(result.copy());
//
//            handler.handleReceivedItem(transported, tr);
//
//            // Belt will refresh visuals automatically
//            belt.notifyUpdate();
//        }
        return InteractionResult.SUCCESS;
    }

    // PROGRESS SYSTEM
    private ItemStack addProgress(ItemStack stack, Player player, Level level) {

        ProgressionComponent old = stack.get(AllDataComponents.PROGRESSION);
        if (old == null) {
            // default if somehow missing
            old = new ProgressionComponent(0, 5);
        }

        int newProgress = old.progress() + 1;

        // Animation
        player.swing(player.getUsedItemHand(), true);

        // Sound
        level.playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_HIT, SoundSource.PLAYERS,
                0.75f, 1.2f);

        // Completion
        if (newProgress >= old.maxProgress()) {
            return finishRecipe(stack, level);
        }

        // Update progress
        stack.set(AllDataComponents.PROGRESSION,
                new ProgressionComponent(newProgress, old.maxProgress()));

        return null;
    }



    // TRANSFORM ITEM
    private ItemStack finishRecipe(ItemStack stack, Level level) {
        // Remove the input item
        stack.shrink(1);

        ItemStack output = new ItemStack(AllItems.ANDESITE_FASTENER.get());

        // Remove progression component from the output
        output.remove(AllDataComponents.PROGRESSION.get());

        return output;
    }
}
