package com.arcanezenith.client;

import com.arcanezenith.network.S2CSyncProgressionPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Purely a display cache - the server remains authoritative for everything. This just lets
 * the spell menu / HUD render without needing to ask the server every frame.
 */
public final class ClientProgressionCache {

    private static int points = 0;
    private static final Set<ResourceLocation> unlocked = new LinkedHashSet<>();
    private static ResourceLocation selected;

    private ClientProgressionCache() {}

    public static void update(S2CSyncProgressionPacket packet) {
        points = packet.points();
        unlocked.clear();
        unlocked.addAll(packet.unlocked());
        selected = packet.selected();
    }

    public static int getPoints() {
        return points;
    }

    public static boolean isUnlocked(ResourceLocation id) {
        return unlocked.contains(id);
    }

    public static ResourceLocation getSelected() {
        return selected;
    }
}
