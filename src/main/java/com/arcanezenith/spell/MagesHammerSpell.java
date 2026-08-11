package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.combat.TerrainDestruction;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * Mage's Hammer — Sky portal + falling ethereal hammer, 120 crush damage,
 * 7-block crater, 5s damaging lava-geyser aftermath. Multi-ring portal particles,
 * 25m stone debris wave, maximum screen shake on impact.
 */
public class MagesHammerSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "mages_hammer");

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 60.0f; }
    @Override public int cooldownTicks()      { return 600; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 groundTarget = caster.position().add(caster.getLookAngle().multiply(12,0,12));
        Vec3 finalTarget  = new Vec3(groundTarget.x, caster.getY(), groundTarget.z);
        double portalY    = caster.getY() + 30;

        // ── Sky portal formation (3 nested rings, staggered) ─────────────
        for (int ring = 0; ring < 4; ring++) {
            int r = ring;
            DelayedEffectScheduler.schedule(ring * 6, () -> {
                double radius = 5.0 - r * 1.0;
                int pts = 32;
                for (int p = 0; p < pts; p++) {
                    double ang = p * Math.PI * 2 / pts + r * 0.3;
                    level.sendParticles(ModParticles.RUNE_RING.get(),
                            finalTarget.x + Math.cos(ang)*radius, portalY,
                            finalTarget.z + Math.sin(ang)*radius,
                            1, 0,0,0, 0.0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.75f+r*0.05f,0.65f,1.0f),1.8f),
                            finalTarget.x + Math.cos(ang)*radius, portalY,
                            finalTarget.z + Math.sin(ang)*radius,
                            1, 0,0,0, 0.0);
                }
                level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                        finalTarget.x, portalY, finalTarget.z,
                        8, 0.5, 0.2, 0.5, 0.02);
                level.playSound(null, finalTarget.x, portalY, finalTarget.z,
                        SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.6f);
            });
        }

        // ── Hammer descending particle column ─────────────────────────────
        for (int tick = 5; tick <= 28; tick++) {
            int t = tick;
            DelayedEffectScheduler.schedule(t, () -> {
                double progress = (t - 5) / 23.0;
                double hammerY  = portalY - progress * (portalY - caster.getY() - 1);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.8f,0.7f,1.0f),3.0f),
                        finalTarget.x, hammerY, finalTarget.z,
                        6, 0.4, 0.2, 0.4, 0.02);
                level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                        finalTarget.x, hammerY, finalTarget.z,
                        4, 0.25, 0.3, 0.25, 0.03);
            });
        }

        // ── Impact at 1.5s (30 ticks) ─────────────────────────────────────
        DelayedEffectScheduler.schedule(30, () -> {
            // Giant 25m dust wave ring
            for (int i = 0; i < 120; i++) {
                double ang  = i * Math.PI * 2 / 120.0;
                double wave = 12.5;
                level.sendParticles(ParticleTypes.EXPLOSION,
                        finalTarget.x + Math.cos(ang)*wave, finalTarget.y+0.5,
                        finalTarget.z + Math.sin(ang)*wave,
                        1, 0, 0.25, 0, 0.06);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.6f,0.55f,0.7f),2.0f),
                        finalTarget.x + Math.cos(ang)*wave*0.6, finalTarget.y+0.3,
                        finalTarget.z + Math.sin(ang)*wave*0.6,
                        1, 0, 0.2, 0, 0.04);
            }
            // Stone debris flying outward
            for (int i = 0; i < 50; i++) {
                double ang = Math.random()*Math.PI*2;
                level.sendParticles(ParticleTypes.EXPLOSION,
                        finalTarget.x + Math.cos(ang)*3, finalTarget.y+1,
                        finalTarget.z + Math.sin(ang)*3,
                        1, Math.cos(ang)*0.3, 0.4+Math.random()*0.4, Math.sin(ang)*0.3, 0.0);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    finalTarget.x, finalTarget.y+1, finalTarget.z, 5, 0,0,0, 0);
            level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                    finalTarget.x, finalTarget.y+1, finalTarget.z,
                    30, 1.0, 0.5, 1.0, 0.12);
            level.sendParticles(ModParticles.RUNE_RING.get(),
                    finalTarget.x, finalTarget.y+0.5, finalTarget.z,
                    8, 0,0,0, 0.0);

            // Sounds: tearing rift → bone-cracking THUD
            level.playSound(null, finalTarget.x, finalTarget.y, finalTarget.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.25f);
            level.playSound(null, finalTarget.x, finalTarget.y, finalTarget.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.5f, 0.4f);

            // Maximum impact shake + FOV crunch
            ModNetworking.sendEffect(caster, "shake",      2.0f, 25f);
            ModNetworking.sendEffect(caster, "fov_punch",  22f,  18f);

            // 120 crush damage in 10-block radius
            AABB crushBox = new AABB(
                    finalTarget.x-10, finalTarget.y-2, finalTarget.z-10,
                    finalTarget.x+10, finalTarget.y+8, finalTarget.z+10);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, crushBox,
                    en -> en != caster && en.isAlive())) {
                e.hurt(caster.damageSources().magic(), 120.0f);
                e.setDeltaMovement(0, -3.0, 0);
            }

            // Terrain crater 7 blocks
            TerrainDestruction.carveCrater(level,
                    finalTarget.x, finalTarget.y, finalTarget.z, 7.0);
            TerrainDestruction.playCrumbleSound(level,
                    finalTarget.x, finalTarget.y, finalTarget.z);
            // Blokk-robbanás FX + fény + mély basszushang
            SpellEnvironmentFX.blockExplosionFX(level, finalTarget, 5.0);
            SpellEnvironmentFX.spawnLight(level, finalTarget.add(0, 2, 0), 30);
            SpellEnvironmentFX.playDeepBassImpact(level, finalTarget.x, finalTarget.y, finalTarget.z);

            // ── Crater afterburn 5s: lava geyser + periodic damage ────────
            for (int tick = 0; tick < 25; tick++) {
                int tt = tick;
                DelayedEffectScheduler.schedule(tick * 4, () -> {
                    // Lava geyser from crater floor
                    for (int g = 0; g < 6; g++) {
                        double ang = g * Math.PI * 2 / 6.0 + tt * 0.15;
                        double r   = Math.random() * 4.5;
                        level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                                finalTarget.x + Math.cos(ang)*r, finalTarget.y+0.5,
                                finalTarget.z + Math.sin(ang)*r,
                                2, 0.1, 0.3, 0.1, 0.12);
                    }
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.55f,0.5f,0.65f),1.5f),
                            finalTarget.x, finalTarget.y+0.3, finalTarget.z,
                            10, 3.0, 0.1, 3.0, 0.02);
                    // 4 dmg/tick from afterburn
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                            new AABB(finalTarget.x-5, finalTarget.y-1, finalTarget.z-5,
                                     finalTarget.x+5, finalTarget.y+3, finalTarget.z+5),
                            en -> en != caster && en.isAlive())) {
                        e.hurt(e.damageSources().magic(), 4.0f);
                    }
                });
            }
        });
    }
}
