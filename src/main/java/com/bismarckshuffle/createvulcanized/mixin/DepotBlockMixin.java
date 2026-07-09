package com.bismarckshuffle.createvulcanized.mixin;

import com.bismarckshuffle.createvulcanized.registry.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DepotBlock.class)
public abstract class DepotBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void vulcanized$hammerUse(ItemStack stack, BlockState state, Level level,
                                      BlockPos pos, Player player, InteractionHand hand,
                                      BlockHitResult hitResult,
                                      CallbackInfoReturnable<ItemInteractionResult> cir) {

        // Only intercept hammer
        if (!stack.is(AllItems.SMITHING_HAMMER.get()))
            return;

        // Get depot block entity
        DepotBlockEntity depot = (DepotBlockEntity) level.getBlockEntity(pos);
        if (depot == null)
            return;

        // Only hammer if depot has an item
        if (!depot.getHeldItem().isEmpty()) {

            InteractionResult result = AllItems.SMITHING_HAMMER.get().hammerDepot(level, depot, player);

            if (result == InteractionResult.SUCCESS) {
                cir.setReturnValue(ItemInteractionResult.sidedSuccess(level.isClientSide()));
                cir.cancel();
            } else {
                // PASS and do NOT swing hammer
                cir.setReturnValue(ItemInteractionResult.CONSUME);
                cir.cancel();
            }

        }
    }
}


