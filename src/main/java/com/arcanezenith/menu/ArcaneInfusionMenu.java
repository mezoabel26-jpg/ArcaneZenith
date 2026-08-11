package com.arcanezenith.menu;

import com.arcanezenith.block.ModBlocks;
import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.item.ModItems;
import com.arcanezenith.item.SpellScrollItem;
import com.arcanezenith.item.WandItem;
import com.arcanezenith.network.ModNetworking;
import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/**
 * Two-slot menu: Slot 0 = Wand, Slot 1 = Spell Scroll.
 * A "Learn Spell" output slot shows a confirmation item when both are valid.
 * Clicking the output slot consumes the scroll and adds the spell to the wand (progression).
 *
 * Layout:
 *   [Wand Slot] [Scroll Slot] -> [LEARN button / result slot]
 *   + standard player inventory
 */
public class ArcaneInfusionMenu extends AbstractContainerMenu {

    private final Container infusionInv;
    private final ResultContainer resultContainer = new ResultContainer();

    public ArcaneInfusionMenu(int syncId, Inventory playerInv, Container infusionInv) {
        super(ModBlocks.INFUSION_MENU_TYPE.get(), syncId);
        this.infusionInv = infusionInv;
        checkContainerSize(infusionInv, 2);
        infusionInv.startOpen(playerInv.player);

        // Slot 0: Wand
        this.addSlot(new Slot(infusionInv, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof WandItem;
            }
        });

        // Slot 1: Spell Scroll
        this.addSlot(new Slot(infusionInv, 1, 84, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SpellScrollItem;
            }
        });

        // Slot 2: Result / Learn button
        this.addSlot(new Slot(resultContainer, 0, 152, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
            @Override
            public boolean mayPickup(Player player) { return canLearn(); }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (player instanceof ServerPlayer sp) {
                    performLearn(sp);
                }
                updateResult();
            }
        });

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        // Hotbar
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
    }

    private boolean canLearn() {
        ItemStack wand = infusionInv.getItem(0);
        ItemStack scroll = infusionInv.getItem(1);
        if (wand.isEmpty() || !(wand.getItem() instanceof WandItem)) return false;
        if (scroll.isEmpty() || !(scroll.getItem() instanceof SpellScrollItem)) return false;
        return SpellScrollItem.getSpellId(scroll) != null;
    }

    private void updateResult() {
        if (canLearn()) {
            ItemStack scroll = infusionInv.getItem(1);
            ResourceLocation spellId = SpellScrollItem.getSpellId(scroll);
            SpellRegistry.Definition def = SpellRegistry.get(spellId);
            if (def != null) {
                // Show a glowing wand as the result placeholder
                ItemStack result = new ItemStack(ModItems.ARCANE_WAND.get());
                result.setHoverName(net.minecraft.network.chat.Component.literal(
                        "Click to learn: " + def.displayName()));
                resultContainer.setItem(0, result);
            }
        } else {
            resultContainer.setItem(0, ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    private void performLearn(ServerPlayer player) {
        ItemStack scroll = infusionInv.getItem(1);
        ResourceLocation spellId = SpellScrollItem.getSpellId(scroll);
        if (spellId == null) return;

        SpellRegistry.Definition def = SpellRegistry.get(spellId);
        if (def == null) return;

        var progress = player.getData(ModAttachments.SPELL_PROGRESS);
        if (!progress.isUnlocked(spellId)) {
            progress.tryUnlock(spellId, 0); // scroll-based learning is free (no point cost)
        }
        // Consume the scroll
        infusionInv.removeItem(1, 1);
        ModNetworking.syncProgression(player);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§aLearned spell: " + def.displayName()), true);

        updateResult();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return infusionInv.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int tableSlots = 3;
            int invSize = this.slots.size();
            if (index < tableSlots) {
                if (!this.moveItemStackTo(stack, tableSlots, invSize, true)) return ItemStack.EMPTY;
            } else {
                // Try placing into wand slot first, then scroll slot
                if (stack.getItem() instanceof WandItem) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (stack.getItem() instanceof SpellScrollItem) {
                    if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        infusionInv.stopOpen(player);
    }
}
