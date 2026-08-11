package com.arcanezenith.event;

import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.spell.SpellCastManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ManaRegenHandler {

    private static final int TICKS_PER_SECOND = 20;
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        SpellCastManager.onServerTick();
        DelayedEffectScheduler.tick();
        com.arcanezenith.spell.SpellComboSystem.tickAll();

        tickCounter++;
        if (tickCounter < TICKS_PER_SECOND) return;
        tickCounter = 0;

        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            var mana = player.getData(ModAttachments.MANA);
            if (mana.getMana() < mana.getMaxMana()) {
                mana.tickRegen(1.0f);
                com.arcanezenith.network.ModNetworking.syncMana(player);
            }
        });
    }
}
