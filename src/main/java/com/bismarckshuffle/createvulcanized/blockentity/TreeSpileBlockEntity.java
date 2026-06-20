package com.bismarckshuffle.createvulcanized.blockentity;

import com.bismarckshuffle.createvulcanized.registry.AllFluids;
import com.bismarckshuffle.createvulcanized.CreateVulcanized;
import com.bismarckshuffle.createvulcanized.block.TreeSpileBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import com.bismarckshuffle.createvulcanized.menu.TreeSpileMenu;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.lang.Lang;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class TreeSpileBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, MenuProvider {

    // Internal timer to control how often the spile extracts resin
    private int extractionTimer = 0;

    public static final int MAX_CAPACITY = 2000;      // Receptacle fluid volume cap in mB
    public static final int RESIN_PER_CYCLE = 20;     // Amount of fluid generated per successful drip cycle in mB
    public static final int TICK_DELAY = 300;          // Production frequency (100 ticks = 5 seconds)

    protected final FluidTank fluidTank = new FluidTank(MAX_CAPACITY) {
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
    public void tick(Level level, BlockState state, TreeSpileBlockEntity be) {
        if (level.isClientSide()) return;

        // Is the tree attached?
        if (!state.getValue(TreeSpileBlock.ATTACHED_TO_TREE)) {
            be.extractionTimer = 0;
            return;
        }

        // If so, increment resin timer
        be.extractionTimer++;
        if (be.extractionTimer >= TICK_DELAY) {
            be.extractionTimer = 0; // Reset the cycle

            // Generate 20mB of resin and deposit it into the internal tank
            net.neoforged.neoforge.fluids.FluidStack resinDroplet =
                    new net.neoforged.neoforge.fluids.FluidStack(AllFluids.RESIN.get(), RESIN_PER_CYCLE);

            // The fluid accumulates inside the block entity up to the 1000mB limit
            be.getFluidTank().fill(resinDroplet, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private boolean checkTreeStructure(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(TreeSpileBlock.FACING);
        BlockPos startLogPos = pos.relative(facing.getOpposite());

        // 1. Core Pillar Validation: Count straight logs directly above the tap point
        int logCount = 0;
        for (int i = 0; i < 6; i++) {
            BlockState logState = level.getBlockState(startLogPos.above(i));
            if (logState.is(net.minecraft.tags.BlockTags.LOGS)) {
                logCount++;
            } else {
                break; // Stop counting the moment a block type breaks vertical continuity
            }
        }

        // Minimum structural requirement of 3 blocks to form a stable tree pillar baseline
        if (logCount < 3) return false;

        // 2. Adaptive Acacia-Proof Canopy Scanning
        // If the straight log pillar finishes, look for leaves starting right there.
        // To support crooked, diagonal trunks (like Acacia), we expand the check window
        // up to 4 blocks higher and 2 blocks outward from the top of our log count.
        BlockPos canopyBasePos = startLogPos.above(logCount);

        for (int yOffset = 0; yOffset <= 4; yOffset++) {
            // Expand the search radius as we go higher to catch wide umbrella branches
            int searchRadius = (yOffset >= 2) ? 2 : 1;

            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = canopyBasePos.offset(x, yOffset, z);
                    if (level.getBlockState(checkPos).is(net.minecraft.tags.BlockTags.LEAVES)) {
                        return true; // Valid live canopy discovered, structure approved!
                    }
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

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 1. Use Create's native Lang builder to add a cleanly formatted, gold-colored title header row
        Lang.builder(CreateVulcanized.ID)
                .text("Tree Spile Contents")
                .forGoggles(tooltip);

        // 2. Safely call the fluid utility. By using a clean list index, it will render perfectly right below the title!
        boolean containsFluid = containedFluidTooltip(tooltip, isPlayerSneaking, this.getFluidTank());

        // 3. Clean, gray-colored fallback if the reservoir is completely dry
        if (!containsFluid) {
            Lang.builder(CreateVulcanized.ID)
                    .translate("tooltip.tree_spile.empty", MAX_CAPACITY)
                    .forGoggles(tooltip, 1);
        }

        return true;
    }

    // Declare this right alongside your fluidTank variable:
    protected final ItemStackHandler inventoryHandler = new ItemStackHandler(1) {
        private boolean isProcessing = false; // The recursion safety lock flag

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            // 1. RECURSION REJECTION: If the flag is true, this change was made by the machine itself,
            // so break out immediately and stop the StackOverflow loop!
            if (isProcessing) {
                return;
            }

            if (level != null && !level.isClientSide) {
                ItemStack stack = getStackInSlot(slot);
                int fluidAmount = fluidTank.getFluidAmount();

                // 2. EXTRACTION OPERATION: Empty bucket -> Filled Resin Bucket
                if (stack.is(Items.BUCKET) && fluidAmount >= 1000) {
                    isProcessing = true;
                    try {
                        fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                        stack.shrink(1);

                        ItemStack filledBucket = new ItemStack(AllFluids.RESIN.get().getBucket());

                        if (stack.isEmpty()) {
                            setStackInSlot(slot, filledBucket);
                        } else {
                            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 0.5, worldPosition.getZ(), filledBucket);
                        }

                        // Play fill audio inside the screen container slot loop
                        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    } finally {
                        isProcessing = false;
                    }
                }
                else if (stack.is(AllFluids.RESIN.get().getBucket())) {
                    int emptySpace = MAX_CAPACITY - fluidAmount;

                    if (emptySpace >= 800) {
                        isProcessing = true;
                        try {
                            int actualFillAmount = Math.min(1000, emptySpace);
                            fluidTank.fill(new FluidStack(AllFluids.RESIN.get(), actualFillAmount), IFluidHandler.FluidAction.EXECUTE);

                            setStackInSlot(slot, new ItemStack(Items.BUCKET));

                            // Play empty audio inside the screen container slot loop
                            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                        } finally {
                            isProcessing = false;
                        }
                    }
                }
            }
        }

        // REINFORCED CAPACITY RESTRAINTS: Tells NeoForge that this specific slot index
        // is strictly built to process 1 item configuration context frame at a time
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public ItemStackHandler getInventoryHandler() {
        return this.inventoryHandler;
    }

    // Tracks how many players currently have this specific block entity GUI open
    private final net.minecraft.world.level.block.entity.ContainerOpenersCounter openersCounter = new net.minecraft.world.level.block.entity.ContainerOpenersCounter() {
        @Override
        protected void onOpen(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            // Play a mechanical wood chest click or door opening noise when opened
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F);
        }

        @Override
        protected void onClose(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            // Play a solid wood click latch closing noise when closed
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 0.8F);
        }

        @Override
        protected void openerCountChanged(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int count, int openCount) {
            // Framework tracking sync stub
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof TreeSpileMenu;
        }
    };

    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            assert this.getLevel() != null;
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createvulcanized.tree_spile");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TreeSpileMenu(containerId, playerInventory, this);
    }
}
