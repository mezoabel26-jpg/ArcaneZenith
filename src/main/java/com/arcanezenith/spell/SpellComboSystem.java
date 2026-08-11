package com.arcanezenith.spell;

import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import com.arcanezenith.progression.SpellRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spell Combo Rendszer — ha egymás után 3 különböző elemet sütsz el,
 * COMBO aktiválódik: +30% sebzés + vizuális aura 10 másodpercig.
 *
 * Logika:
 *  - Minden varázslatnál feljegyezzük az elemet (IGNIS, FULGUR, UMBRA...)
 *  - Ha az utolsó 3 elem mind különböző → COMBO aktiválás
 *  - Combo alatt: arany-lila aura + Speed I + combo üzenet a chatben
 *  - A combo damage bónusz a SpellCastManager.cast()-ban van bekötve
 *
 * Elemek: IGNIS(tűz), FULGUR(villám), UMBRA(sötétség),
 *         SYLVA(természet), CRYO(jég), CHRONO(idő), SANCTUM(szentség)
 */
public final class SpellComboSystem {

    public static final float COMBO_DAMAGE_BONUS = 0.30f; // +30%
    public static final int   COMBO_DURATION_TICKS = 200; // 10s

    // UUID → combo state
    private static final Map<UUID, ComboState> STATES = new ConcurrentHashMap<>();

    private SpellComboSystem() {}

    private record ComboState(
        SpellRegistry.Element[] lastElements, // legutóbbi 3 elem
        int comboTicksRemaining               // 0 = nincs aktív combo
    ) {}

    /** Hívd minden cast után. Visszatér a sebzésszorzóval (1.0 = normál, 1.3 = combo). */
    public static float onSpellCast(ServerPlayer caster, SpellRegistry.Element element) {
        UUID uid = caster.getUUID();
        ComboState prev = STATES.getOrDefault(uid,
                new ComboState(new SpellRegistry.Element[0], 0));

        // Utolsó 3 elem frissítése
        SpellRegistry.Element[] last = prev.lastElements();
        SpellRegistry.Element[] updated = new SpellRegistry.Element[Math.min(3, last.length + 1)];
        if (last.length >= 3) {
            updated[0] = last[1]; updated[1] = last[2]; updated[2] = element;
        } else {
            System.arraycopy(last, 0, updated, 0, last.length);
            updated[last.length] = element;
        }

        // Combo check: 3 különböző elem
        int ticksLeft = Math.max(0, prev.comboTicksRemaining() - 1);
        boolean newCombo = false;

        if (updated.length == 3
                && updated[0] != updated[1]
                && updated[1] != updated[2]
                && updated[0] != updated[2]
                && ticksLeft == 0) { // ne triggerelj ha már aktív
            newCombo = true;
            ticksLeft = COMBO_DURATION_TICKS;
            activateCombo(caster, element);
        }

        STATES.put(uid, new ComboState(updated, ticksLeft));
        return ticksLeft > 0 ? (1.0f + COMBO_DAMAGE_BONUS) : 1.0f;
    }

    /** Hívd minden server tick-ben a cooldown csökkentéséhez. */
    public static void tickAll() {
        STATES.replaceAll((uid, state) -> {
            if (state.comboTicksRemaining() > 0)
                return new ComboState(state.lastElements(), state.comboTicksRemaining() - 1);
            return state;
        });
    }

    /** Igaz ha a játékosnak aktív combója van. */
    public static boolean hasActiveCombo(UUID uid) {
        ComboState state = STATES.get(uid);
        return state != null && state.comboTicksRemaining() > 0;
    }

    public static void clearPlayer(UUID uid) {
        STATES.remove(uid);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private static void activateCombo(ServerPlayer caster, SpellRegistry.Element lastElement) {
        ServerLevel level = caster.serverLevel();
        Vec3 pos = caster.position().add(0, 1.0, 0);

        // Chat üzenet
        caster.sendSystemMessage(
                Component.literal("§5§l✦ ELEMENTAL COMBO! §7+30% sebzés 10 másodpercig ✦"));

        // Gyorsaság buff
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, COMBO_DURATION_TICKS, 0, false, false));

        // Combo aktiválás VFX — nagy multicolor burst
        // Minden elem saját színnel jelenik meg a comboban
        float[] r = elementColor(lastElement);
        for (int i = 0; i < 36; i++) {
            double ang = i * Math.PI * 2 / 36.0;
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(r[0], r[1], r[2]), 2.5f),
                    pos.x + Math.cos(ang)*2.0, pos.y, pos.z + Math.sin(ang)*2.0,
                    1, 0, 0.08, 0, 0.0);
        }
        level.sendParticles(ModParticles.RUNE_RING.get(), pos.x, pos.y, pos.z, 8, 0.1, 0.1, 0.1, 0.0);
        level.sendParticles(ModParticles.GOLDEN_LIGHT.get(), pos.x, pos.y, pos.z, 20, 0.8, 0.8, 0.8, 0.12);
        level.sendParticles(ModParticles.HEAVEN_BEAM.get(), pos.x, pos.y+1, pos.z, 6, 0.3, 0.5, 0.3, 0.06);

        // Hang
        level.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);

        // Kamera effektek
        ModNetworking.sendEffect(caster, "arcane_overdrive", 0.8f, 15f);
        ModNetworking.sendEffect(caster, "fov_punch",        -12f,  10f);
        ModNetworking.sendEffect(caster, "shake",              0.4f,  8f);

        // Combo aura 10 másodpercig — minden 10 tickben rúna-gyűrű
        for (int tick = 10; tick <= COMBO_DURATION_TICKS; tick += 10) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                if (!hasActiveCombo(caster.getUUID())) return;
                Vec3 cp = caster.position().add(0, 1.0, 0);
                double angle = t * 4.5;
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(angle + i * 30.0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(r[0], r[1], r[2]), 1.0f),
                            cp.x + Math.cos(a)*1.5, cp.y, cp.z + Math.sin(a)*1.5,
                            1, 0, 0.02, 0, 0.0);
                }
                level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                        cp.x, cp.y, cp.z, 3, 0.6, 0.6, 0.6, 0.05);
            });
        }

        // Lejárati jelzés
        DelayedEffectScheduler.schedule(COMBO_DURATION_TICKS, () -> {
            if (!caster.isAlive()) return;
            caster.sendSystemMessage(Component.literal("§8§o✦ Elemental Combo lejárt."));
            Vec3 ep = caster.position().add(0, 1.0, 0);
            level.sendParticles(ModParticles.ARCANE_SPARK.get(), ep.x, ep.y, ep.z, 15, 0.8, 0.8, 0.8, 0.08);
        });
    }

    private static float[] elementColor(SpellRegistry.Element e) {
        return switch (e) {
            case IGNIS  -> new float[]{1.0f, 0.3f, 0.0f};   // narancs-vörös
            case FULGUR -> new float[]{0.5f, 0.7f, 1.0f};   // kék-fehér
            case UMBRA  -> new float[]{0.4f, 0.0f, 0.8f};   // mély lila
            case SYLVA  -> new float[]{0.2f, 0.9f, 0.2f};   // zöld
            case CRYO   -> new float[]{0.6f, 0.9f, 1.0f};   // cián-fehér
            case CHRONO -> new float[]{0.9f, 0.1f, 0.5f};   // crimson
            case SANCTUM-> new float[]{1.0f, 0.9f, 0.4f};   // arany
        };
    }
}
