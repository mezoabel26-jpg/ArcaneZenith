package com.arcanezenith.spell;

import com.arcanezenith.client.effect.CameraShake;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * CastAnimationSystem — vizuális előkészítés (charge phase) minden varázslat előtt.
 *
 * A varázslat nem azonnal sül el — a cast() hívás előtt lefut egy rövid
 * animáció (2–12 tick, spell tier-től függően), majd utána sül el a valódi hatás.
 *
 * Ez OPCIONÁLIS — a SpellCastManager meghívhatja ezt, és csak az animáció
 * végén sül el a spell.cast(). Ha a játékos nem akar animációt (config-ban
 * ki van kapcsolva), azonnal sül el.
 *
 * Animáció szintek:
 *   Tier 0–1 → 4 tick: kis szikra burst
 *   Tier 2–3 → 8 tick: spirál + FOV pull
 *   Tier 4   → 12 tick: nagy spirál + FOV pull + shake
 *   Tier 5   → 16 tick: teljes cinematic töltés
 */
public final class CastAnimationSystem {

    // Aktív animációk (server-side)
    private static final Map<UUID, PendingCast> PENDING = new ConcurrentHashMap<>();

    private CastAnimationSystem() {}

    private record PendingCast(Spell spell, int ticksLeft, int totalTicks) {}

    /**
     * Elindítja a töltési animációt, majd a végén meghívja a spell.cast()-ot.
     * @return true ha az animáció elindult, false ha azonnal kell kiszolgálni
     */
    public static boolean startAndSchedule(ServerPlayer caster, Spell spell, int tier) {
        if (!com.arcanezenith.config.ArcaneZenithConfig.fxSpellShaders) {
            return false; // config: ki van kapcsolva
        }

        int chargeTicks = switch (tier) {
            case 0, 1 -> 4;
            case 2, 3 -> 8;
            case 4    -> 12;
            default   -> 16; // tier 5
        };

        ServerLevel level = caster.serverLevel();
        Vec3 eye = caster.getEyePosition();

        // ── Indítási effektek ────────────────────────────────────────────────
        // FOV befelé húzás a töltés alatt
        ModNetworking.sendEffect(caster, "fov_punch", -(5 + tier * 3f), (float)chargeTicks);

        // Hang — mély basszus beindulás
        level.playSound(null, eye.x, eye.y, eye.z,
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                0.8f, 0.3f + tier * 0.1f);

        // Spirális energia összegyűlés a kéz körül
        runChargeAnimation(level, caster, tier, chargeTicks, spell);

        // Spell tényleges elsütése a töltés végén
        DelayedEffectScheduler.schedule(chargeTicks, () -> {
            if (caster.isAlive()) {
                // Release hang
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                        1.0f, 0.6f + tier * 0.15f);
                spell.cast(caster);
            }
        });

        return true;
    }

    private static void runChargeAnimation(ServerLevel level, ServerPlayer caster,
                                            int tier, int totalTicks, Spell spell) {
        for (int tick = 0; tick < totalTicks; tick++) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                if (!caster.isAlive()) return;

                Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.9));
                double progress = t / (double) totalTicks;

                // Spirális energia befelé húzódik a kézbe
                double radius = (2.0 + tier * 0.4) * (1.0 - progress * 0.75);
                int arms = 2 + tier;

                for (int arm = 0; arm < arms; arm++) {
                    double angle = t * (0.3 + tier * 0.05) + arm * (Math.PI * 2 / arms);
                    Vec3 src = hand.add(
                            Math.cos(angle) * radius,
                            Math.sin(t * 0.15) * 0.5,
                            Math.sin(angle) * radius);

                    // Particle befelé mozog
                    Vec3 velocity = hand.subtract(src).normalize().scale(0.08 + progress * 0.1);

                    // Tier-alapú szín
                    Vector3f color = tierColor(tier, (float) progress);
                    level.sendParticles(new DustParticleOptions(color, 0.8f + (float) progress * 0.6f),
                            src.x, src.y, src.z, 1, velocity.x, velocity.y, velocity.z, 0.0);
                }

                // Belső mag — növekvő fény
                if (t % 2 == 0) {
                    int endRodCount = (int)(1 + progress * 6);
                    level.sendParticles(ParticleTypes.END_ROD,
                            hand.x, hand.y, hand.z,
                            endRodCount, 0.1, 0.1, 0.1, 0.02 + progress * 0.06);
                }

                // Tier 4–5: hang buildup + shake
                if (tier >= 4 && t % 4 == 0) {
                    level.playSound(null, hand.x, hand.y, hand.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                            0.3f + (float) progress * 0.8f, 0.8f + (float) progress * 0.6f);
                    if (t > totalTicks / 2) {
                        ModNetworking.sendEffect(caster, "shake", (float)(0.1 * progress), 3f);
                    }
                }

                // Utolsó 2 tick: release burst
                if (t >= totalTicks - 2) {
                    level.sendParticles(new DustParticleOptions(tierColor(tier, 1.0f), 2.0f),
                            hand.x, hand.y, hand.z,
                            (int)(8 + tier * 4), 0.4, 0.4, 0.4, 0.1 + tier * 0.03);
                }
            });
        }
    }

    private static Vector3f tierColor(int tier, float progress) {
        // Tier-alapú szín, progresszel fehér felé tolódik
        float[] base = switch (tier) {
            case 0, 1 -> new float[]{0.6f, 0.3f, 1.0f};  // lila
            case 2, 3 -> new float[]{0.3f, 0.6f, 1.0f};  // kék
            case 4    -> new float[]{0.8f, 0.4f, 1.0f};  // lila-arany
            default   -> new float[]{1.0f, 0.7f, 0.2f};  // arany (tier 5)
        };
        float white = progress * 0.4f;
        return new Vector3f(
                Math.min(1, base[0] + white),
                Math.min(1, base[1] + white),
                Math.min(1, base[2] + white));
    }
}
