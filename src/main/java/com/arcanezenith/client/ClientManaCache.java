package com.arcanezenith.client;

import com.arcanezenith.network.S2CSyncManaPacket;

/**
 * Purely a display cache - the server remains authoritative for mana. This lets the HUD
 * render every frame without polling the server. Updated whenever an S2CSyncManaPacket
 * arrives (on regen tick and on successful spell cast/spend).
 */
public final class ClientManaCache {

    private static float mana = 100.0f;
    private static float maxMana = 100.0f;
    private static float regenPerSecond = 1.5f;

    // Simple client-side smoothing so the HUD bar doesn't visually "pop" between the
    // ~1/sec regen packets - purely cosmetic, never used for any authoritative check.
    private static float displayedMana = 100.0f;

    private ClientManaCache() {}

    public static void update(S2CSyncManaPacket packet) {
        mana = packet.mana();
        maxMana = packet.maxMana();
        regenPerSecond = packet.regenPerSecond();
    }

    /** Call once per client render tick to ease the displayed value toward the true value. */
    public static void tickSmoothing(float partialTicks) {
        float diff = mana - displayedMana;
        if (Math.abs(diff) < 0.01f) {
            displayedMana = mana;
            return;
        }
        displayedMana += diff * Math.min(1.0f, 0.25f * partialTicks + 0.08f);
    }

    public static float getMana() {
        return mana;
    }

    public static float getDisplayedMana() {
        return displayedMana;
    }

    public static float getMaxMana() {
        return maxMana;
    }

    public static float getRegenPerSecond() {
        return regenPerSecond;
    }
}
