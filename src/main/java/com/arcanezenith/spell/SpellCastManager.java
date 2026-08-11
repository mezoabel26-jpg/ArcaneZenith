package com.arcanezenith.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.arcanezenith.network.ModNetworking;

/**
 * Lightweight, in-memory (not persisted) per-player spell cooldown tracker.
 * Good enough for a working vertical slice; swap for a saved-data attachment
 * if cooldowns need to survive server restarts.
 */
public final class SpellCastManager {

    private static final Map<UUID, Map<ResourceLocation, Long>> COOLDOWN_END_TICK = new HashMap<>();
    private static long serverTick = 0L;

    private SpellCastManager() {}

    public static void onServerTick() {
        serverTick++;
    }

    public static boolean isOnCooldown(ServerPlayer player, Spell spell) {
        Map<ResourceLocation, Long> map = COOLDOWN_END_TICK.get(player.getUUID());
        if (map == null) return false;
        Long end = map.get(spell.id());
        return end != null && serverTick < end;
    }

    public static int ticksRemaining(ServerPlayer player, Spell spell) {
        Map<ResourceLocation, Long> map = COOLDOWN_END_TICK.get(player.getUUID());
        if (map == null) return 0;
        Long end = map.get(spell.id());
        if (end == null) return 0;
        return (int) Math.max(0, end - serverTick);
    }

    public static void startCooldown(ServerPlayer player, Spell spell) {
        COOLDOWN_END_TICK
                .computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .put(spell.id(), serverTick + spell.cooldownTicks());
    }

    /**
     * Attempts to cast a spell: validates cooldown + mana, spends mana, starts cooldown, then executes.
     * @return true if the spell was actually cast.
     */
    public static boolean tryCast(ServerPlayer player, Spell spell) {
        if (isOnCooldown(player, spell)) {
            return false;
        }
        var mana = player.getData(com.arcanezenith.capability.ModAttachments.MANA);
        if (!mana.spend(spell.manaCost())) {
            return false;
        }
        startCooldown(player, spell);
        com.arcanezenith.network.ModNetworking.syncMana(player);
        com.arcanezenith.network.ModNetworking.syncWandState(player, spell);

        // Cast Animation System — tier-alapú töltési animáció
        var defOpt = com.arcanezenith.progression.SpellRegistry.getById(spell.id());
        int tier = defOpt != null ? defOpt.tier() : 0;

        // Spell Combo tracking
        float comboBonusMult = 1.0f;
        if (defOpt != null) {
            comboBonusMult = SpellComboSystem.onSpellCast(player, defOpt.element());
            ModNetworking.sendEffect(player, "dark_fantasy", comboBonusMult - 1.0f, 0f);
        }

        // Animált töltés tier >= 2-nél, közvetlen elsütés tier 0–1-nél
        if (tier >= 2) {
            boolean animated = CastAnimationSystem.startAndSchedule(player, spell, tier);
            if (!animated) spell.cast(player);
        } else {
            spell.cast(player);
        }
        return true;
    }
}
