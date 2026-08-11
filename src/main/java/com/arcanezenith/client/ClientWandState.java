package com.arcanezenith.client;

import com.arcanezenith.network.S2CWandStatePacket;
import net.minecraft.resources.ResourceLocation;

/**
 * Display cache for the diegetic wand-tip HUD. The server only pushes a new packet right
 * after a cast (see ModNetworking#syncWandState); between packets, the client ticks the
 * remaining-cooldown value down locally in real time purely for smooth rendering. This can
 * drift by a tick or two from the true server state, which is fine for a HUD display - the
 * server independently re-validates cooldown/mana on every actual cast attempt regardless.
 */
public final class ClientWandState {

    private static ResourceLocation spellId;
    private static int cooldownTotalTicks = 0;
    private static int cooldownRemainingTicks = 0;
    private static long lastUpdateRealtimeMs = 0L;

    private ClientWandState() {}

    public static void update(S2CWandStatePacket packet) {
        spellId = packet.spellId();
        cooldownTotalTicks = packet.cooldownTotalTicks();
        cooldownRemainingTicks = packet.cooldownRemainingTicks();
        lastUpdateRealtimeMs = System.currentTimeMillis();
    }

    /** 0 = fully on cooldown just cast, 1 = fully ready. Never authoritative - display only. */
    public static float getCooldownProgress() {
        if (cooldownTotalTicks <= 0) return 1.0f;
        long elapsedMs = System.currentTimeMillis() - lastUpdateRealtimeMs;
        float elapsedTicks = elapsedMs / 50.0f; // 20 ticks/sec = 50ms/tick
        float remaining = Math.max(0f, cooldownRemainingTicks - elapsedTicks);
        return 1.0f - Math.min(1.0f, remaining / cooldownTotalTicks);
    }

    public static boolean isReady() {
        return getCooldownProgress() >= 1.0f;
    }

    public static ResourceLocation getSpellId() {
        return spellId;
    }
}
