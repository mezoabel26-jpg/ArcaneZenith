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

import java.util.List;

/**
 * CRIMSON BANDS OF CYTTORAK — Marvel Dr. Strange skarlát kötelékek.
 *
 * 8 skarlát kötelék lövellik ki 20 blokkon belüli ellenségekre. 6 másodpercig
 * rögzíti (NoAI + Slowness 255) és folyamatosan szorítja (20 dmg/sec).
 * A 6. másodpercben az összes kötelék egyszerre húzza egy pontba → 150 dmg
 * collision + masszív vörös singularity összevonás VFX.
 */
public class CrimsonBandsSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "crimson_bands");

    private static final float  CRUSH_DMG_PER_SEC = 20.0f;
    private static final float  COLLISION_DMG     = 150.0f;
    private static final double RANGE             = 20.0;
    private static final int    DURATION_TICKS    = 120; // 6s

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 65.0f; }
    @Override public int cooldownTicks()      { return 360; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 origin = caster.position().add(0, 1.0, 0);

        // ── KÖTELÉK KILÖVÉS VIZUÁL ─────────────────────────────────────
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0f, 0.5f);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.CHAIN, SoundSource.PLAYERS, 1.5f, 0.6f);
        ModNetworking.sendEffect(caster, "fov_punch", 18f, 12f);
        ModNetworking.sendEffect(caster, "blood_curse", 0.0f, 125f);
        ModNetworking.sendEffect(caster, "shake",     0.6f, 10f);

        // Kilövési burst
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI * 2 / 8.0;
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.8f, 0.05f, 0.1f), 2.0f),
                    origin.x + Math.cos(ang)*1.5, origin.y, origin.z + Math.sin(ang)*1.5,
                    4, Math.cos(ang)*0.2, 0.05, Math.sin(ang)*0.2, 0.04);
        }
        level.sendParticles(ModParticles.CRIMSON_CHAIN.get(), origin.x, origin.y, origin.z, 12, 0.8,0.1,0.1, 0.06);
        level.sendParticles(ModParticles.RUNE_RING.get(), origin.x, origin.y, origin.z, 6, 0.05,0.05,0.05, 0.0);

        // ── CÉLPONTOK MEGFOGÁSA ───────────────────────────────────────
        AABB searchBox = new AABB(origin.x-RANGE, origin.y-3, origin.z-RANGE,
                                   origin.x+RANGE, origin.y+8, origin.z+RANGE);
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != caster && e.isAlive());

        if (caught.isEmpty()) return;

        // Minden elkapott célponthoz NoAI + Slowness
        java.util.Map<java.util.UUID, Boolean> prevNoAi = new java.util.concurrent.ConcurrentHashMap<>();
        for (LivingEntity target : caught) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS + 10,
                    255, false, false));
            if (target instanceof Mob mob) {
                prevNoAi.put(target.getUUID(), mob.isNoAi());
                mob.setNoAi(true);
            }

            // Kötelék trail a caster-tól a célpontig
            Vec3 tp = target.position().add(0, target.getBbHeight()/2.0, 0);
            Vec3 dir = tp.subtract(origin).normalize();
            double dist = origin.distanceTo(tp);
            for (int s = 0; s <= (int)(dist*2); s++) {
                Vec3 p = origin.add(dir.scale(s * 0.5));
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.85f, 0.04f, 0.08f), 1.2f),
                        p.x, p.y, p.z, 1, 0.04,0.04,0.04, 0.0);
            }
        }

        // ── 6s CRUSH LOOP ─────────────────────────────────────────────
        for (int tick = 20; tick <= DURATION_TICKS; tick += 20) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                for (LivingEntity target : caught) {
                    if (!target.isAlive()) continue;
                    target.setDeltaMovement(0, 0, 0);
                    target.hurt(caster.damageSources().magic(), CRUSH_DMG_PER_SEC);

                    Vec3 tp = target.position().add(0, target.getBbHeight()/2.0, 0);
                    // Pulsáló vörös kötelék gyűrűk
                    for (int i = 0; i < 16; i++) {
                        double ang = i * Math.PI * 2 / 16.0 + t * 0.1;
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.9f, 0.05f, 0.1f), 1.0f),
                                tp.x + Math.cos(ang)*0.8, tp.y, tp.z + Math.sin(ang)*0.8,
                                1, 0,0.02,0, 0.0);
                    }
                    // Kötelék a caster-tól
                    Vec3 dir = tp.subtract(origin).normalize();
                    double dist = origin.distanceTo(tp);
                    for (int s = 0; s <= (int)(dist); s++) {
                        Vec3 p = origin.add(dir.scale(s));
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.7f, 0.03f, 0.05f), 0.8f),
                                p.x, p.y + Math.sin(s*0.8)*0.2, p.z,
                                1, 0,0,0, 0.0);
                    }
                }
                if (t % 40 == 0) {
                    level.playSound(null, origin.x, origin.y, origin.z,
                            SoundEvents.CHAIN, SoundSource.PLAYERS, 0.8f, 0.9f);
                    ModNetworking.sendEffect(caster, "shake", 0.3f, 5f);
                }
            });
        }

        // ── ÖSSZEVONÁS A 6. MÁSODPERCBEN ──────────────────────────────
        DelayedEffectScheduler.schedule(DURATION_TICKS, () -> {
            // AI visszaállítás
            for (LivingEntity e : caught) {
                if (e instanceof Mob mob)
                    mob.setNoAi(prevNoAi.getOrDefault(e.getUUID(), false));
            }

            // Minden célpont a caster elé húzódik
            Vec3 collisionPoint = origin.add(caster.getLookAngle().scale(3));

            for (LivingEntity target : caught) {
                if (!target.isAlive()) continue;
                Vec3 toCenter = collisionPoint.subtract(target.position());
                target.setDeltaMovement(toCenter.normalize().scale(3.0));

                // Collision damage
                target.hurt(caster.damageSources().magic(), COLLISION_DMG);
            }

            // Singularity összevonás VFX
            level.sendParticles(ModParticles.SINGULARITY_NOVA.get(),
                    collisionPoint.x, collisionPoint.y+1, collisionPoint.z,
                    15, 0.5,0.5,0.5, 0.06);
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(1.0f, 0.1f, 0.15f), 4.0f),
                    collisionPoint.x, collisionPoint.y+1, collisionPoint.z,
                    80, 1.5,1.5,1.5, 0.12);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    collisionPoint.x, collisionPoint.y+1, collisionPoint.z, 5, 0,0,0, 0);

            level.playSound(null, collisionPoint.x, collisionPoint.y, collisionPoint.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 0.5f);
            level.playSound(null, collisionPoint.x, collisionPoint.y, collisionPoint.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.6f);

            ModNetworking.sendEffect(caster, "shake",     1.8f, 20f);
            ModNetworking.sendEffect(caster, "fov_punch", 25f,  15f);
        });
    }
}
