package com.arcanezenith.block.entity;

import com.arcanezenith.block.ModBlocks;
import com.arcanezenith.menu.ArcaneInfusionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public class ArcaneInfusionBlockEntity extends BlockEntity implements MenuProvider {

    // Slot 0 = Wand, Slot 1 = Spell Scroll
    public final SimpleContainer inventory = new SimpleContainer(2);

    public ArcaneInfusionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.INFUSION_TABLE_BE_TYPE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Arcane Infusion Table");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
        return new ArcaneInfusionMenu(syncId, playerInv, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        inventory.toTag(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag, registries);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity drop =
                        new net.minecraft.world.entity.item.ItemEntity(level,
                                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
                level.addFreshEntity(drop);
            }
        }
        inventory.clearContent();
    }
}
