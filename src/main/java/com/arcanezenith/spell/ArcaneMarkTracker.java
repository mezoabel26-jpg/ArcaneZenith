package com.arcanezenith.spell;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory stack tracker for the Arcane Bolt "Arcane Mark" mechanic. */
final class ArcaneMarkTracker {
    private static final Map<UUID, Integer> STACKS = new HashMap<>();

    private ArcaneMarkTracker() {}

    static int addMark(UUID entityId) {
        int next = STACKS.merge(entityId, 1, Integer::sum);
        return next;
    }

    static void clear(UUID entityId) {
        STACKS.remove(entityId);
    }
}
