package com.arcanezenith.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Common contract for a castable spell. Keep spell logic server-authoritative:
 * the client only ever *requests* a cast; the server validates mana/cooldown and executes it.
 */
public interface Spell {

    ResourceLocation id();

    float manaCost();

    /** Cooldown in ticks (20 ticks = 1 second). */
    int cooldownTicks();

    /**
     * Executes the spell on the server. Implementations are responsible for their own
     * particle/sound broadcast (use S2C packets or PacketDistributor.sendToPlayersTrackingEntity).
     */
    void cast(ServerPlayer caster);
}
