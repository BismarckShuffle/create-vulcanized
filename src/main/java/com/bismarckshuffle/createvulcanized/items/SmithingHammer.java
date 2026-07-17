package com.bismarckshuffle.createvulcanized.items;

import com.bismarckshuffle.createvulcanized.compat.jei.SmithingRecipe;
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
import org.jetbrains.annotations.NotNull;


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

    private SmithingRecipe findRecipe(ItemStack input) {
        ItemStack testStack = input.copyWithCount(1);
        
        if (testStack.is(com.simibubi.create.AllItems.ANDESITE_ALLOY.get())) {
            return new SmithingRecipe(
                    new ItemStack(com.simibubi.create.AllItems.ANDESITE_ALLOY.get()),
                    new ItemStack(AllItems.ANDESITE_FASTENER.get())
            );
        }
        
        return null;
    }

    private HammerResult processOne(ItemStack stack) {

        ProgressionComponent prog = stack.get(AllDataComponents.PROGRESSION.get());

        if (prog == null)
            prog = new ProgressionComponent(0, 5);

        int newProgress = prog.progress() + 1;

        // COMPLETED
        if (newProgress >= prog.maxProgress()) {

            SmithingRecipe recipe = findRecipe(stack);
            ItemStack output = recipe != null ? recipe.output.copy() : new ItemStack(AllItems.ANDESITE_FASTENER.get());

            return new HammerResult(stack, output, true, 0, prog.maxProgress());
        }

        // IN PROGRESS
        stack.set(AllDataComponents.PROGRESSION,
                new ProgressionComponent(newProgress, prog.maxProgress()));

        return new HammerResult(stack, null, false, newProgress, prog.maxProgress());
    }

    // DEPOT hammering (called by depot behavior intercept mixin in DepotBlockMixin.java)
    public InteractionResult hammerDepot(Level level, DepotBlockEntity depot, Player player) {

        ItemStack stack = depot.getHeldItem();
        if (stack.isEmpty())
            return InteractionResult.PASS;

        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResult.PASS;

        HammerResult result = processOne(stack);

        // Effects
        playEffects(level, player, result.finished, depot);
        applyCooldown(player);
        player.causeFoodExhaustion(0.2F);

        // COMPLETED
        if (result.finished) {

            depot.setHeldItem(result.remaining.isEmpty() ? ItemStack.EMPTY : result.remaining);

            level.addFreshEntity(new ItemEntity(
                    level,
                    depot.getBlockPos().getX() + 0.5,
                    depot.getBlockPos().getY() + 0.75,
                    depot.getBlockPos().getZ() + 0.5,
                    result.output
            ));

            return InteractionResult.SUCCESS;
        }

        // NOT COMPLETED
        depot.setHeldItem(result.remaining);
        return InteractionResult.SUCCESS;
    }



    // BELT hammering (called by useOn)
    private InteractionResult hammerBelt(Level level, BeltBlockEntity belt, Player player) {

        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResult.PASS;

        var inventory = belt.getInventory();

        for (TransportedItemStack transported : inventory.getTransportedItems()) {

            ItemStack stack = transported.stack;
            if (stack.isEmpty())
                continue;

            HammerResult result = processOne(stack);

            transported.stack = result.remaining;
            belt.notifyUpdate();

            playEffects(level, player, result.finished, null);
            applyCooldown(player);
            player.causeFoodExhaustion(0.2F);

            if (result.finished) {
                transported.stack = result.output;
            }

            break;
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
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

            if (player != null) {
                return hammerBelt(level, belt, player);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * @param remaining stack after removing 1 item
     * @param output    finished item (null if not finished)
     * @param finished  true if recipe completed
     */
    private record HammerResult(ItemStack remaining, ItemStack output, boolean finished, int newProgress,
                                int maxProgress) {
    }


}
