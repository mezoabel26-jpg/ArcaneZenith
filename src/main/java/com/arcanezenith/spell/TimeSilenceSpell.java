package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Time Silence — 30-second global time stop. Buffered damage detonates on resume
 * with 3× multiplier. Clockwork rune floor disc (30m), crimson damage-chain particles
 * on frozen entities, Time-Stop desaturation shader, chromatic aberration blast on unfreeze.
 */
public class TimeSilenceSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "time_silence");

    private static final Map<UUID, Float>  DAMAGE_BUFFER  = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3>   FROZEN_ENTITIES = new ConcurrentHashMap<>();

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 80.0f; }
    @Override public int cooldownTicks()      { return 1200; }

    /** Called by TimeSilenceDamageHandler — buffers damage dealt during stop. */
    public static boolean isFrozen(UUID id)   { return FROZEN_ENTITIES.containsKey(id); }
    public static void bufferDamage(UUID id, float amount) {
        DAMAGE_BUFFER.merge(id, amount, Float::sum);
    }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 castPos = caster.position();

        // ── Stop sound + time-stop desaturation shader ────────────────────
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0f, 0.2f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.5f, 0.5f);
        ModNetworking.sendEffect(caster, "time_stop",  1.0f, 630f);
        ModNetworking.sendEffect(caster, "fov_punch", -18f,  15f);
        ModNetworking.sendEffect(caster, "shake",       0.6f, 12f);

        // ── Clockwork floor rune disc — 30m radius, 3 concentric rings ───
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 10.0;
            int    pts    = ring * 36;
            for (int p = 0; p < pts; p++) {
                double ang = p * Math.PI * 2 / pts;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.05f, 0.02f, 0.15f), 1.8f),
                        castPos.x + Math.cos(ang)*radius,
                        castPos.y + 0.05,
                        castPos.z + Math.sin(ang)*radius,
                        1, 0,0,0, 0.0);
            }
        }
        // Radial spokes
        for (int spoke = 0; spoke < 12; spoke++) {
            double ang = spoke * Math.PI * 2 / 12.0;
            for (int r = 2; r <= 30; r += 2) {
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.2f,0.05f,0.45f),1.0f),
                        castPos.x + Math.cos(ang)*r, castPos.y+0.05,
                        castPos.z + Math.sin(ang)*r,
                        1, 0,0,0, 0.0);
            }
        }

        // ── Freeze all mobs within 100 blocks ────────────────────────────
        DAMAGE_BUFFER.clear();
        FROZEN_ENTITIES.clear();

        AABB freezeBox = caster.getBoundingBox().inflate(100);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, freezeBox,
                e -> e instanceof Mob && e != caster && e.isAlive());

        // UUID → volt-e már NoAi a mob, hogy visszaállíthassuk pontosan
        final Map<UUID, Boolean> prevNoAi = new java.util.concurrent.ConcurrentHashMap<>();

        for (LivingEntity mob : targets) {
            FROZEN_ENTITIES.put(mob.getUUID(), mob.getDeltaMovement());
            DAMAGE_BUFFER.put(mob.getUUID(), 0.0f);
            // Damage Resistance hogy a mi damage buffer-ünkön kívül ne kapjon sebzést
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 660, 255, false, false));
            // NoAI: megakadályozza hogy az AI felülírja a velocity-t
            if (mob instanceof Mob m) {
                prevNoAi.put(mob.getUUID(), m.isNoAi());
                m.setNoAi(true);
            }
        }

        // ── 30s tick loop — every 4 ticks: freeze position + crimson chains ──
        for (int tick = 4; tick <= 600; tick += 4) {
            int t = tick;
            DelayedEffectScheduler.schedule(t, () -> {
                // Rhythmic tick sound
                if (t % 20 == 0) {
                    level.playSound(null, castPos.x, castPos.y, castPos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                            0.4f, 0.3f + (t / 600f) * 0.3f);
                }

                for (Map.Entry<UUID, Vec3> entry : FROZEN_ENTITIES.entrySet()) {
                    level.getEntitiesOfClass(LivingEntity.class,
                            caster.getBoundingBox().inflate(100),
                            e -> e.getUUID().equals(entry.getKey()))
                        .forEach(mob -> {
                            mob.setDeltaMovement(0, 0, 0);
                            mob.setPos(mob.getX(), mob.getY(), mob.getZ());

                            // Crimson damage-accumulation chain around frozen entity
                            float buffered = DAMAGE_BUFFER.getOrDefault(entry.getKey(), 0f);
                            float chainIntensity = Math.min(1.0f, buffered / 50f);
                            int chainPts = 8 + (int)(chainIntensity * 16);
                            for (int cp = 0; cp < chainPts; cp++) {
                                double ang = cp * Math.PI * 2 / chainPts + t * 0.05;
                                level.sendParticles(new DustParticleOptions(
                                        new Vector3f(0.9f,0.1f+chainIntensity*0.1f,0.1f),
                                        0.6f+chainIntensity*0.8f),
                                        mob.getX() + Math.cos(ang)*0.7,
                                        mob.getY() + mob.getBbHeight()*0.5,
                                        mob.getZ() + Math.sin(ang)*0.7,
                                        1, 0,0,0, 0.0);
                            }
                        });
                }

                // Rotating rune floor pulses inward every 5s
                if (t % 100 == 0) {
                    for (int i = 0; i < 36; i++) {
                        double ang = i * Math.PI * 2 / 36.0 + t * 0.01;
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.6f,0.1f,0.9f),2.0f),
                                castPos.x + Math.cos(ang)*20, castPos.y+0.1,
                                castPos.z + Math.sin(ang)*20,
                                1, -Math.cos(ang)*0.05, 0, -Math.sin(ang)*0.05, 0.0);
                    }
                }
            });
        }

        // ── Resume at 30s: detonation + chromatic aberration ─────────────
        DelayedEffectScheduler.schedule(602, () -> {
            // Visszaállítjuk az AI-t minden korábban fagyasztott mobnál
            for (Map.Entry<UUID, Boolean> entry : prevNoAi.entrySet()) {
                level.getEntitiesOfClass(LivingEntity.class,
                        caster.getBoundingBox().inflate(100),
                        e -> e.getUUID().equals(entry.getKey()))
                    .forEach(mob -> {
                        if (mob instanceof Mob m) m.setNoAi(entry.getValue());
                        mob.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                    });
            }
            // Glass-shatter + massive explosion sound
            level.playSound(null, castPos.x, castPos.y, castPos.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0f, 0.5f);
            level.playSound(null, castPos.x, castPos.y, castPos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.35f);

            // Chromatic aberration blast shader
            ModNetworking.sendEffect(caster, "time_stop",  0.0f, 1f);
            ModNetworking.sendEffect(caster, "shake",       1.5f, 20f);
            ModNetworking.sendEffect(caster, "fov_punch",  30f,  15f);

            // Detonate all buffered damage with 3× multiplier
            for (Map.Entry<UUID, Float> entry : DAMAGE_BUFFER.entrySet()) {
                float burst = entry.getValue() * 3.0f;
                if (burst < 1f) continue;
                level.getEntitiesOfClass(LivingEntity.class,
                        caster.getBoundingBox().inflate(100),
                        e -> e.getUUID().equals(entry.getKey()) && e.isAlive())
                    .forEach(mob -> {
                        mob.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                        mob.hurt(caster.damageSources().magic(), burst);
                        // Crimson explosion on each enemy
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(1.0f,0.1f,0.1f),2.5f),
                                mob.getX(), mob.getY()+mob.getBbHeight()/2.0, mob.getZ(),
                                25, 0.8,0.8,0.8, 0.1);
                        level.sendParticles(ParticleTypes.EXPLOSION,
                                mob.getX(), mob.getY()+1, mob.getZ(),
                                3, 0.3,0.3,0.3, 0.0);
                    });
            }

            // World shatter: giant shockwave ring from caster
            for (int i = 0; i < 80; i++) {
                double ang = i * Math.PI * 2 / 80.0;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.7f,0.1f,0.95f),2.8f),
                        castPos.x+Math.cos(ang)*25, castPos.y+0.2,
                        castPos.z+Math.sin(ang)*25,
                        1, 0, 0.1, 0, 0.03);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    castPos.x, castPos.y+1, castPos.z, 5, 0,0,0, 0);

            DAMAGE_BUFFER.clear();
            FROZEN_ENTITIES.clear();
        });
    }
}
