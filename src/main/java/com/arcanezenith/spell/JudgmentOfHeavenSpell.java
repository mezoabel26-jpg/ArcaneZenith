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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * Judgment of Heaven — 20m golden holy sword from orbit, 100 impact damage,
 * then 8s of 30m holy energy spike eruptions every 0.5s (15 damage/spike).
 * Orbital beam shader, blinding white flash on impact, HEAVEN_BEAM + HOLY_STAR particles.
 */
public class JudgmentOfHeavenSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "judgment_of_heaven");
    private static final Random RNG = new Random();

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 70.0f; }
    @Override public int cooldownTicks()      { return 800; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = new Vec3(
                caster.getX() + caster.getLookAngle().x * 15,
                caster.getY(),
                caster.getZ() + caster.getLookAngle().z * 15);

        // ── Orbital descent column (2s pre-impact) ────────────────────────
        level.playSound(null, center.x, center.y+60, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 1.8f);
        // FÉNY AZ ÉGBŐL — a leszálló kard fényt bocsát ki
        SpellEnvironmentFX.spawnLight(level, center.add(0, 10, 0), 45);
        SpellEnvironmentFX.playMysticCharge(level, center.x, center.y, center.z);
        ModNetworking.sendEffect(caster, "fov_punch",  -15f, 20f);
        ModNetworking.sendEffect(caster, "holy_bloom",   1.0f, 200f);

        for (int tick = 0; tick < 40; tick++) {
            int t = tick;
            DelayedEffectScheduler.schedule(t, () -> {
                double progress = t / 40.0;
                double colY = center.y + 65 - progress * 65;

                // Core beam — thick gold pillar descending
                level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                        center.x, colY, center.z,
                        8, 1.0, 0.5, 1.0, 0.03);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f,0.95f,0.45f),2.5f),
                        center.x, colY, center.z,
                        6, 1.2, 0.3, 1.2, 0.02);
                level.sendParticles(ModParticles.HOLY_STAR.get(),
                        center.x, colY, center.z,
                        4, 0.8, 0.2, 0.8, 0.02);
                level.sendParticles(ParticleTypes.END_ROD,
                        center.x, colY, center.z,
                        3, 0.7, 0.1, 0.7, 0.0);

                // Orbit ring at beam tip
                for (int i = 0; i < 12; i++) {
                    double ang = i * Math.PI * 2 / 12.0 + t * 0.15;
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f,0.9f,0.3f),1.2f),
                            center.x + Math.cos(ang)*2.5, colY,
                            center.z + Math.sin(ang)*2.5,
                            1, 0,0,0, 0.0);
                }
            });
        }

        // ── Impact at 2s ──────────────────────────────────────────────────
        DelayedEffectScheduler.schedule(40, () -> {
            // Blinding white flash — flood with white particles
            for (int i = 0; i < 120; i++) {
                double ang = RNG.nextDouble() * Math.PI * 2;
                double r   = RNG.nextDouble() * 15;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f,1.0f,0.9f),3.5f),
                        center.x + Math.cos(ang)*r,
                        center.y + RNG.nextDouble()*10,
                        center.z + Math.sin(ang)*r,
                        1, 0,0,0, 0.0);
            }
            // Massive golden shockwave ring
            for (int i = 0; i < 80; i++) {
                double ang = i * Math.PI * 2 / 80.0;
                level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                        center.x + Math.cos(ang)*12, center.y+0.5,
                        center.z + Math.sin(ang)*12,
                        1, Math.cos(ang)*0.05, 0.05, Math.sin(ang)*0.05, 0.0);
            }
            // Impact pillars (6 shockwave columns)
            for (int p = 0; p < 6; p++) {
                double ang = p * Math.PI / 3.0;
                for (int h = 0; h < 8; h++) {
                    level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                            center.x + Math.cos(ang)*p*1.5, center.y+h*1.5,
                            center.z + Math.sin(ang)*p*1.5,
                            2, 0.2,0.1,0.2, 0.02);
                }
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    center.x, center.y+1, center.z, 8, 0,0,0, 0);

            // Sounds: orbital hum → heavy impact
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.5f, 0.25f);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.5f, 0.3f);

            ModNetworking.sendEffect(caster, "shake",      2.0f, 22f);
            ModNetworking.sendEffect(caster, "fov_punch",  28f,  18f);

            // 100 damage in 10-block radius on sword landing
            AABB impactBox = new AABB(center.x-10,center.y-2,center.z-10,
                                       center.x+10,center.y+12,center.z+10);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, impactBox,
                    en -> en != caster && en.isAlive())) {
                e.hurt(caster.damageSources().magic(), 100.0f);
                e.setDeltaMovement(0, 2.5, 0);
            }

            // ── Holy energy spikes for 8s, every 10 ticks (16 waves) ─────
            for (int wave = 1; wave <= 16; wave++) {
                int w = wave;
                DelayedEffectScheduler.schedule(wave * 10, () -> {
                    // 8 random spike positions in 30m zone
                    for (int spike = 0; spike < 8; spike++) {
                        double ang  = RNG.nextDouble() * Math.PI * 2;
                        double dist = 3.0 + RNG.nextDouble() * 28;
                        double sx   = center.x + Math.cos(ang)*dist;
                        double sz   = center.z + Math.sin(ang)*dist;

                        // Spike particle column
                        for (int h = 0; h < 10; h++) {
                            level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                                    sx, center.y+h*1.5, sz,
                                    2, 0.25, 0.15, 0.25, 0.04);
                            level.sendParticles(ModParticles.HOLY_STAR.get(),
                                    sx, center.y+h*1.5, sz,
                                    1, 0.15, 0.1, 0.15, 0.02);
                        }
                        // Ground crack line
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(1.0f,0.9f,0.4f),2.0f),
                                sx, center.y+0.1, sz,
                                5, 0.5, 0.05, 0.5, 0.01);
                        level.playSound(null, sx, center.y, sz,
                                SoundEvents.AMETHYST_BLOCK_CHIME,
                                SoundSource.PLAYERS, 0.8f, 1.2f+RNG.nextFloat()*0.4f);

                        // 15 damage per spike
                        AABB spikeBox = new AABB(sx-2,center.y-1,sz-2,sx+2,center.y+8,sz+2);
                        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, spikeBox,
                                en -> en != caster && en.isAlive())) {
                            e.hurt(e.damageSources().magic(), 15.0f);
                        }
                    }
                    // Spike wave shake pulse every other wave
                    if (w % 2 == 0) ModNetworking.sendEffect(caster, "shake", 0.3f, 5f);
                });
            }
        });
    }
}
