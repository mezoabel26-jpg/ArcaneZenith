package com.arcanezenith.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The "Codex of the Astral": right-click opens ArcaneCodexScreen, a real interactive
 * multi-tab GUI with a clickable skill tree (laid out from SpellRegistry's element/tier/
 * prerequisite data), a bestiary, and a crafting reference.
 *
 * See ArcaneCodexScreenOpener (client-only) for the actual screen-opening call, kept out of
 * this class so this Item class stays loadable on a dedicated server (no client-only imports here).
 */
public class ArcaneCodexItem extends Item {

    public ArcaneCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.arcanezenith.client.ArcaneCodexScreenOpener.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
