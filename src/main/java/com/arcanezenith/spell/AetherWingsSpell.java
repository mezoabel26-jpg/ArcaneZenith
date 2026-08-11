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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aether Wings — 15s supersonic flight with collision physics.
 *
 * VFX: dual 24-point wing geometry from HOLY_STAR particles, expanding as speed
 * increases. Sonic boom ring detonates at speed threshold. Collision launches mobs
 * with a visible knockback particle burst.
 */
public class AetherWingsSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "aether_wings");

    public static final Map<UUID, Long> ACTIVE_FLIERS   = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SONIC_BOOM = new ConcurrentHashMap<>();
    private static final float COLLISION_BASE = 16.0f;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 30.0f; }
    @Override public int cooldownTicks()      { return 400; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        UUID uid = caster.getUUID();

        caster.getAbilities().mayfly = true;
        caster.getAbilities().flying = true;
        caster.onUpdateAbilities();
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,  305, 4, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.JUMP,            305, 3, false, false));
        ACTIVE_FLIERS.put(uid, System.currentTimeMillis());

        // ── ACTIVATION BURST ──────────────────────────────────────────
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5f, 1.3f);
        ModNetworking.sendEffect(caster, "fov_punch", 28f, 22f);
        ModNetworking.sendEffect(caster, "shake",      0.55f, 8f);

        // 360° cyan burst ring
        for (int i = 0; i < 48; i++) {
            double ang = i * Math.PI * 2 / 48.0;
            level.sendParticles(new DustParticleOptions(new Vector3f(0.45f,0.88f,1.0f),1.6f),
                    caster.getX()+Math.cos(ang)*2.0, caster.getY()+1.2, caster.getZ()+Math.sin(ang)*2.0,
                    1, Math.cos(ang)*0.10, 0.08, Math.sin(ang)*0.10, 0.0);
        }
        level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                caster.getX(), caster.getY(), caster.getZ(), 15, 0.3,1.0,0.3, 0.08);

        // ── FLIGHT TICK LOOP ───────────────────────────────────────────
        for (int tick = 1; tick <= 300; tick++) {
            int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                if (!ACTIVE_FLIERS.containsKey(uid)) return;

                Vec3 vel = caster.getDeltaMovement();
                double spd = vel.horizontalDistance();

                Vec3 look  = caster.getLookAngle();
                Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

                // ── Wing geometry: 24-point arc each side ───────────────
                for (int side = -1; side <= 1; side += 2) {
                    double span = 1.8 + spd * 1.8;
                    // Upper wing surface (12 points)
                    for (int f = 0; f < 12; f++) {
                        double progress = f / 11.0;
                        double outward  = progress * span;
                        double vertical = Math.sin(progress * Math.PI) * 2.5;
                        double sweep    = Math.cos(progress * Math.PI * 0.5) * 0.5;
                        Vec3 wingPt = caster.position()
                                .add(0, caster.getBbHeight() * 0.75, 0)
                                .add(right.scale(side * outward))
                                .add(new Vec3(look.x * sweep, vertical, look.z * sweep));
                        level.sendParticles(ModParticles.HOLY_STAR.get(),
                                wingPt.x, wingPt.y, wingPt.z, 1, 0,0,0, 0.0);
                        // Cyan glow dust on wing
                        if (f % 3 == 0)
                            level.sendParticles(new DustParticleOptions(
                                    new Vector3f(0.5f,0.9f,1.0f),0.6f),
                                    wingPt.x, wingPt.y, wingPt.z, 1, 0,0,0, 0.0);
                    }
                    // Wingtip trail
                    Vec3 tip = caster.position()
                            .add(right.scale(side * span))
                            .add(0, caster.getBbHeight() * 0.75, 0);
                    if (spd > 0.15) {
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.6f,0.95f,1.0f),0.8f),
                                tip.x - vel.x*0.6, tip.y, tip.z - vel.z*0.6,
                                2, 0,0,0, 0.0);
                    }
                }

                // ── Sonic boom at speed threshold ───────────────────────
                if (spd > 0.52) {
                    long now = System.currentTimeMillis();
                    if (now - LAST_SONIC_BOOM.getOrDefault(uid, 0L) > 750) {
                        LAST_SONIC_BOOM.put(uid, now);
                        // Full 32-point circular shockwave ring
                        for (int i = 0; i < 32; i++) {
                            double ang = i * Math.PI * 2 / 32.0;
                            level.sendParticles(new DustParticleOptions(
                                    new Vector3f(0.85f,0.98f,1.0f),2.2f),
                                    caster.getX()+Math.cos(ang)*2.8, caster.getY()+1.0,
                                    caster.getZ()+Math.sin(ang)*2.8,
                                    1, 0,0,0, 0.0);
                        }
                        level.sendParticles(ParticleTypes.CLOUD,
                                caster.getX(), caster.getY()+1, caster.getZ(),
                                8, 1.0, 0.3, 1.0, 0.04);
                        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 2.0f);
                        ModNetworking.sendEffect(caster, "fov_punch", 22f, 6f);
                    }
                }

                // ── Collision damage ──────────────────────────────────────
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                        caster.getBoundingBox().inflate(1.8),
                        e -> e != caster && e.isAlive())) {
                    float dmg = COLLISION_BASE + (float)(spd * 14.0);
                    mob.hurt(caster.damageSources().playerAttack(caster), dmg);
                    Vec3 knock = mob.position().subtract(caster.position()).normalize().scale(2.4);
                    mob.setDeltaMovement(knock.x, 0.9, knock.z);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.45f,0.88f,1.0f),2.0f),
                            mob.getX(), mob.getY()+1, mob.getZ(),
                            25, 0.7,0.7,0.7, 0.10);
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                            SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            });
        }

        // ── END OF FLIGHT ──────────────────────────────────────────────
        DelayedEffectScheduler.schedule(302, () -> {
            ACTIVE_FLIERS.remove(uid);
            LAST_SONIC_BOOM.remove(uid);
            if (!caster.isCreative() && !caster.isSpectator()) {
                caster.getAbilities().mayfly = false;
                caster.getAbilities().flying = false;
                caster.onUpdateAbilities();
            }
            level.sendParticles(new DustParticleOptions(new Vector3f(0.45f,0.88f,1.0f),1.5f),
                    caster.getX(), caster.getY()+1, caster.getZ(), 30, 1.0,0.5,1.0, 0.06);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 0.6f);
        });
    }
}
