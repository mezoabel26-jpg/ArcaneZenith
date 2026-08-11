package com.arcanezenith.item;

import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.spell.ArcaneBoltSpell;
import com.arcanezenith.spell.Spell;
import com.arcanezenith.spell.SpellCastManager;
import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WandItem extends Item {

    private static final Spell STARTER_SPELL = new ArcaneBoltSpell();

    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Spell spellToCast;

            if (player.isShiftKeyDown()) {
                // Shift+right-click always casts the free starter spell - a reliable fallback attack.
                spellToCast = STARTER_SPELL;
            } else {
                var progress = serverPlayer.getData(ModAttachments.SPELL_PROGRESS);
                spellToCast = SpellRegistry.getSpell(progress.getSelected());
                if (spellToCast == null) {
                    spellToCast = STARTER_SPELL;
                }
            }

            boolean cast = SpellCastManager.tryCast(serverPlayer, spellToCast);
            if (!cast) {
                serverPlayer.displayClientMessage(
                        Component.literal("§5Not enough mana or spell on cooldown"), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
