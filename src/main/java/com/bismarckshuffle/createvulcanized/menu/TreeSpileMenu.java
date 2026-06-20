package com.bismarckshuffle.createvulcanized.menu;

import com.bismarckshuffle.createvulcanized.blockentity.TreeSpileBlockEntity;
import com.bismarckshuffle.createvulcanized.registry.AllMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.SlotItemHandler;

public class TreeSpileMenu extends AbstractContainerMenu {

    public final TreeSpileBlockEntity blockEntity;

    public TreeSpileMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, (TreeSpileBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public TreeSpileMenu(int containerId, Inventory playerInv, TreeSpileBlockEntity entity) {
        super(AllMenuTypes.TREE_SPILE_MENU.get(), containerId);
        this.blockEntity = entity;

        // 1. INPUT BUCKET SLOT: Perfect center alignment inside the upper wireframe box at X: 19, Y: 38
        this.addSlot(new SlotItemHandler(entity.getInventoryHandler(), 0, 19, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET) || stack.is(com.bismarckshuffle.createvulcanized.registry.AllFluids.RESIN.get().getBucket());
            }
        });

        // 2. PLAYER INVENTORY STORAGE MATRIX: Synced perfectly to match the background shift
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, 110 + y * 18));
            }
        }

        // 3. MAIN PLAYER HOTBAR ROW: Synced perfectly so items sit dead-center in the boxes
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInv, x, 8 + x * 18, 168));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 0) {
                // Shift click from the bucket slot moves it down to the main inventory squares
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Shift click from inventory moves it up to the bucket slot if valid
                if (itemstack1.is(Items.BUCKET) || itemstack1.is(com.bismarckshuffle.createvulcanized.registry.AllFluids.RESIN.get().getBucket())) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.blockEntity.isRemoved() && player.distanceToSqr(this.blockEntity.getBlockPos().getCenter()) <= 64.0;
    }
}
