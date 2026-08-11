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

import java.util.List;

/**
 * Horde of Blades — 12 spectral ice blades orbit the caster.
 *
 * VFX: every orbit tick renders full blade geometry from frost shard particles,
 * with motion-blur trails. Launch phase fires 3 blades at 3 separate targets with
 * visible hitscan trails + glass-shatter impacts. Parry window creates a
 * 360° ice-blue shockwave ring on deflect.
 */
public class HordeOfBladesSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "horde_of_blades");

    private static final int   BLADE_COUNT    = 12;
    private static final float BLADE_DMG      = 24.0f;
    private static final float PARRY_DMG      = 14.0f;
    private static final float ORBIT_RADIUS   = 1.6f;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 40.0f; }
    @Override public int cooldownTicks()      { return 320; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 pos = caster.position();

        // ── SUMMON BLAST ───────────────────────────────────────────────
        // 360° horizontal frost shard ring erupting outward
        for (int i = 0; i < BLADE_COUNT * 5; i++) {
            double ang = i * Math.PI * 2 / (BLADE_COUNT * 5);
            double r   = ORBIT_RADIUS + 0.5;
            level.sendParticles(ModParticles.FROST_SHARD.get(),
                    pos.x + Math.cos(ang)*r, pos.y + 1.0, pos.z + Math.sin(ang)*r,
                    1, Math.cos(ang)*0.06, 0.04, Math.sin(ang)*0.06, 0.0);
        }
        level.sendParticles(ModParticles.RUNE_RING.get(), pos.x, pos.y+1, pos.z, 8, 0.0,0.0,0.0, 0.0);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.5f,0.85f,1.0f),2.5f),
                pos.x, pos.y+1, pos.z, 30, 1.5,0.2,1.5, 0.06);
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y+1, pos.z, 25, 1.2,0.3,1.2, 0.04);

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 1.5f, 0.35f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.8f, 1.6f);

        ModNetworking.sendEffect(caster, "fov_punch", -22f, 18f);
        ModNetworking.sendEffect(caster, "shake",      0.55f, 10f);

        // ── ORBIT PHASE: 60 ticks ──────────────────────────────────────
        for (int tick = 1; tick <= 60; tick++) {
            int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                Vec3 cp = caster.position();
                double angOffset = t * 0.20;

                // Render all 12 blades as 3-pixel shard bodies
                for (int b = 0; b < BLADE_COUNT; b++) {
                    double ang = b * Math.PI * 2.0 / BLADE_COUNT + angOffset;
                    double bx  = cp.x + Math.cos(ang) * ORBIT_RADIUS;
                    double bz  = cp.z + Math.sin(ang) * ORBIT_RADIUS;
                    double by  = cp.y + 0.9 + Math.sin(t*0.18 + b) * 0.28;

                    // Blade body — frost shard
                    level.sendParticles(ModParticles.FROST_SHARD.get(),
                            bx, by, bz, 1, 0.03,0.03,0.03, 0.0);
                    // Motion-blur trail — ice dust behind each blade
                    double trailAng = ang - 0.25;
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.45f,0.82f,1.0f),0.7f),
                            cp.x + Math.cos(trailAng)*ORBIT_RADIUS,
                            by,
                            cp.z + Math.sin(trailAng)*ORBIT_RADIUS,
                            1, 0,0,0, 0.0);
                }

                // ── PARRY: any mob in 3.5m takes reflected damage ────────
                AABB parryBox = caster.getBoundingBox().inflate(3.5);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, parryBox,
                        en -> en != caster && en.isAlive())) {
                    if ((t % 10) == 0) { // check every 0.5s to avoid spam
                        e.hurt(e.damageSources().magic(), PARRY_DMG);
                        // Parry shockwave ring
                        Vec3 ep = e.position();
                        for (int i = 0; i < 20; i++) {
                            double ra = i * Math.PI * 2 / 20.0;
                            level.sendParticles(new DustParticleOptions(
                                    new Vector3f(0.7f,0.95f,1.0f),1.4f),
                                    ep.x+Math.cos(ra)*2.0, ep.y+0.5, ep.z+Math.sin(ra)*2.0,
                                    1, 0,0.04,0, 0.02);
                        }
                        level.sendParticles(ParticleTypes.SNOWFLAKE, ep.x, ep.y+1, ep.z, 12, 0.4,0.3,0.4, 0.05);
                        level.playSound(null, ep.x, ep.y, ep.z,
                                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.3f, 1.9f);
                    }
                }
            });
        }

        // ── LAUNCH PHASE at 3s ─────────────────────────────────────────
        DelayedEffectScheduler.schedule(62, () -> {
            Vec3 launchOrigin = caster.position();
            AABB targetBox = new AABB(launchOrigin.x-22,launchOrigin.y-6,launchOrigin.z-22,
                                       launchOrigin.x+22,launchOrigin.y+8,launchOrigin.z+22);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, targetBox,
                    e -> e != caster && e.isAlive());
            targets.sort((a,b) -> Double.compare(
                    launchOrigin.distanceToSqr(a.position()),launchOrigin.distanceToSqr(b.position())));

            int launches = Math.min(3, targets.size());
            for (int i = 0; i < launches; i++) {
                LivingEntity target = targets.get(i);
                int fi = i;
                DelayedEffectScheduler.schedule(i * 3, () -> {
                    target.hurt(caster.damageSources().indirectMagic(caster,caster), BLADE_DMG);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));

                    // Hitscan trail — FROST_SHARD streak to target
                    Vec3 dir = target.position().subtract(launchOrigin).normalize();
                    int dist = (int)launchOrigin.distanceTo(target.position());
                    for (int s = 0; s <= dist; s++) {
                        Vec3 p = launchOrigin.add(dir.scale(s));
                        level.sendParticles(ModParticles.FROST_SHARD.get(),
                                p.x, p.y+1, p.z, 1, 0.06,0.06,0.06, 0.08);
                    }
                    // Glass-shatter impact
                    Vec3 tp = target.position();
                    level.sendParticles(ModParticles.FROST_SHARD.get(),
                            tp.x,tp.y+1,tp.z, 35, 0.6,0.6,0.6, 0.18);
                    level.sendParticles(ParticleTypes.SNOWFLAKE,
                            tp.x,tp.y+1,tp.z, 20, 0.5,0.5,0.5, 0.06);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.6f,0.95f,1.0f),2.0f),
                            tp.x,tp.y+1,tp.z, 15, 0.4,0.4,0.4, 0.04);
                    level.playSound(null, tp.x, tp.y, tp.z,
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 1.5f + fi*0.1f);
                });
            }

            // Final launch shake
            ModNetworking.sendEffect(caster, "shake",     0.7f, 10f);
            ModNetworking.sendEffect(caster, "fov_punch", 18f,  10f);
        });
    }
}
