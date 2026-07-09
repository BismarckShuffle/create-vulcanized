package com.bismarckshuffle.createvulcanized.items;

import com.bismarckshuffle.createvulcanized.registry.AllDataComponents;
import com.bismarckshuffle.createvulcanized.registry.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.item.ItemEntity;
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

    private void applyCooldown(Player player) {
        if (player.getCooldowns().isOnCooldown(this))
            return;

        player.getCooldowns().addCooldown(this, 20); // 20 ticks
    }

    private void playEffects(Level level, Player player, boolean recipeFinisher, DepotBlockEntity depot) {

        player.swing(player.getUsedItemHand(), true);


        if (recipeFinisher)
            level.playSound(null, player.blockPosition(),
                    AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.getMainEvent(), SoundSource.BLOCKS,
                    .85f, 1.5f);
        else
            level.playSound(null, player.blockPosition(),
                    AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.getMainEvent(), SoundSource.BLOCKS,
                    .85f, 1.0f);

        // Sparks / metal impact effect
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = depot.getBlockPos();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;

            // Sparks shooting outward from the depot
            for (int i = 0; i < 12; i++) {

                double vx = (level.random.nextDouble() - 0.5) * 0.4;
                double vy = level.random.nextDouble() * 0.2 + 0.1;
                double vz = (level.random.nextDouble() - 0.5) * 0.4;

                serverLevel.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,   // metal spark particle
                        x, y, z,
                        0,
                        vx, vy, vz, 4.0
                );
            }
        }
    }

    // Only want to process one item from a stack, return the in-progress item if not finished, or the output item if finished
    public ItemStack hammerOne(ItemStack stack, Player player, Level level) {

        // Shared cooldown
        if (player.getCooldowns().isOnCooldown(this))
            return stack;

        // Split off ONE item
        ItemStack single = stack.split(1);

        // Shared progress logic
        ItemStack result = addProgress(single, player, level);

        applyCooldown(player);

        // If recipe completed, result is the output item
        // If not completed, result is null and 'single' now has updated progress
        return (result != null) ? result : single;
    }

    private ItemStack addProgress(ItemStack stack, Player player, Level level) {

        ProgressionComponent old = stack.get(AllDataComponents.PROGRESSION);
        if (old == null)
            old = new ProgressionComponent(0, 5);

        int newProgress = old.progress() + 1;

        if (newProgress >= old.maxProgress())
        {
//            playEffects(level, player, true, null);
            return finishRecipe(stack);
        }
        else
        {
//            playEffects(level, player, false, null);
        }

        stack.set(AllDataComponents.PROGRESSION,
                new ProgressionComponent(newProgress, old.maxProgress()));

        return null;
    }

    private ItemStack finishRecipe(ItemStack stack) {
        stack.shrink(1);

        ItemStack output = new ItemStack(AllItems.ANDESITE_FASTENER.get());
        output.remove(AllDataComponents.PROGRESSION.get());

        return output;
    }

    // DEPOT hammering (called by depot behavior intercept mixin in DepotBlockMixin.java)
    public InteractionResult hammerDepot(Level level, DepotBlockEntity depot, Player player) {

        ItemStack stack = depot.getHeldItem();
        if (stack.isEmpty())
            return InteractionResult.PASS;

        // Cooldown
        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResult.PASS;

        // Read or initialize progress
        ProgressionComponent prog = stack.get(AllDataComponents.PROGRESSION);
        if (prog == null)
            prog = new ProgressionComponent(0, 5);

        int newProgress = prog.progress() + 1;

        // Effects
        player.swing(player.getUsedItemHand(), true);
        playEffects(level, player, newProgress >= prog.maxProgress(), depot);

        // Apply cooldown AFTER successful hit
        applyCooldown(player);

        // COMPLETION
        if (newProgress >= prog.maxProgress()) {

            // Consume ONE sheet
            stack.shrink(1);

            // Reset progress on remaining stack
            if (!stack.isEmpty()) {
                stack.set(AllDataComponents.PROGRESSION,
                        new ProgressionComponent(0, prog.maxProgress()));
                depot.setHeldItem(stack);
            } else {
                depot.setHeldItem(ItemStack.EMPTY);
            }

            // Create output item
            ItemStack output = new ItemStack(AllItems.ANDESITE_FASTENER.get());

            // Drop the output item into the world
            level.addFreshEntity(new ItemEntity(
                    level,
                    depot.getBlockPos().getX() + 0.5,
                    depot.getBlockPos().getY() + 0.75,
                    depot.getBlockPos().getZ() + 0.5,
                    output
            ));

            player.causeFoodExhaustion(0.2F);
            return InteractionResult.SUCCESS;
        }

        // NOT COMPLETE YET — update progress
        stack.set(AllDataComponents.PROGRESSION,
                new ProgressionComponent(newProgress, prog.maxProgress()));

        depot.setHeldItem(stack);
        return InteractionResult.SUCCESS;
    }


    // BELT hammering (called by useOn)
    private InteractionResult hammerBelt(Level level, BeltBlockEntity belt, Player player) {

        var inventory = belt.getInventory();

        for (TransportedItemStack transported : inventory.getTransportedItems()) {

            ItemStack stack = transported.stack;
            if (stack.isEmpty())
                continue;

            // Hammer ONE item
            ItemStack processed = hammerOne(stack, player, level);
            if (processed.isEmpty())
                continue;

            // Replace the transported item
            transported.stack = processed;

            // Belt will update visuals
            belt.notifyUpdate();

            break; // Only hammer ONE transported item
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        BlockState state = level.getBlockState(pos);

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        // BELT ONLY
        if (state.getBlock() instanceof BeltBlock) {
            BeltBlockEntity belt = (BeltBlockEntity) level.getBlockEntity(pos);
            if (belt == null)
                return InteractionResult.PASS;

            return hammerBelt(level, belt, player);
        }

        return InteractionResult.PASS;
    }


}
