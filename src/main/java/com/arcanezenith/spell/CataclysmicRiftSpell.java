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

import java.util.Random;
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * Cataclysmic Rift — 50m tectonic fissure that tears open progressively,
 * then erupts with 15m lava geysers every 0.5s for 10s. Real terrain destruction.
 * Earthquake shake for the full 10s, heat haze over the rift, 30 damage/geyser.
 */
public class CataclysmicRiftSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "cataclysmic_rift");
    private static final Random RNG = new Random();

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 75.0f; }
    @Override public int cooldownTicks()      { return 800; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 look    = caster.getLookAngle();
        Vec3 riftDir = new Vec3(look.x, 0, look.z).normalize();
        Vec3 origin  = caster.position();

        // ── Earthquake rumble + heat haze on cast ─────────────────────────
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 3.0f, 0.25f);
        // VIHAR + KÖD — a hasadék előhívja a vihart
        SpellEnvironmentFX.startStorm(level, 210);
        SpellEnvironmentFX.castFogZone(level, caster, 20.0, 200);
        SpellEnvironmentFX.playDeepBassImpact(level, origin.x, origin.y, origin.z);
        ModNetworking.sendEffect(caster, "heat_haze", 0.7f, 210f);
        ModNetworking.sendEffect(caster, "shake",      0.55f, 210f);
        ModNetworking.sendEffect(caster, "fov_punch", -14f,  12f);

        // ── Rift crack spreading progressively over 0.8s ──────────────────
        for (int seg = 0; seg <= 50; seg++) {
            int s = seg;
            DelayedEffectScheduler.schedule(seg / 4, () -> {
                Vec3 pt = origin.add(riftDir.scale(s));

                // Glowing ground crack — red/orange fissure glow
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.9f,0.25f,0.0f), 2.0f),
                        pt.x, pt.y+0.15, pt.z, 3, 0.35,0.05,0.35, 0.01);
                level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                        pt.x, pt.y+0.2, pt.z, 1, 0.15,0.1,0.15, 0.04);
                level.sendParticles(ParticleTypes.LAVA,
                        pt.x, pt.y+0.1, pt.z, 1, 0.2,0,0.2, 0.0);
            });
        }

        // ── Terrain tear after crack spread ──────────────────────────────
        DelayedEffectScheduler.schedule(14, () -> {
            TerrainDestruction.carveFissure(level,
                    origin.x + riftDir.x*25, origin.y-1, origin.z + riftDir.z*25,
                    riftDir.x, riftDir.z, 50.0, 3.0, 5.0);
            TerrainDestruction.playCrumbleSound(level, origin.x, origin.y, origin.z);

            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.35f);
            ModNetworking.sendEffect(caster, "shake",     1.0f, 15f);
            ModNetworking.sendEffect(caster, "fov_punch", 20f,  12f);
        });

        // ── Geyser eruptions for 10s: every 10 ticks, multiple spouts ────
        for (int wave = 0; wave < 20; wave++) {
            int w = wave;
            DelayedEffectScheduler.schedule(16 + wave * 10, () -> {
                // 5 geyser spouts per wave, spaced along the rift
                for (int g = 0; g < 5; g++) {
                    double t   = (g + RNG.nextDouble()) / 5.0;
                    double gx  = origin.x + riftDir.x * (t * 48);
                    double gz  = origin.z + riftDir.z * (t * 48);
                    double gy  = origin.y;

                    // Geyser column — 15 blocks high
                    for (int h = 0; h < 20; h++) {
                        double spread = h * 0.12;
                        level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                                gx, gy+h*0.75, gz,
                                2, spread, 0.08, spread, 0.15);
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(1.0f,0.35f+h*0.02f,0.0f),1.8f),
                                gx, gy+h*0.75, gz,
                                1, spread*0.5, 0.05, spread*0.5, 0.05);
                    }
                    // Dark smoke plume at peak
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            gx, gy+15, gz, 3, 0.5,0.3,0.5, 0.02);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.15f,0.15f,0.15f),2.5f),
                            gx, gy+14, gz, 4, 0.6,0.4,0.6, 0.04);

                    // Plasma base flash
                    level.sendParticles(ModParticles.PLASMA_SPIRAL.get(),
                            gx, gy+0.3, gz, 5, 0.3,0.2,0.3, 0.08);

                    // 30 damage/geyser in 2.5m radius
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                            new AABB(gx-2.5,gy-1,gz-2.5, gx+2.5,gy+12,gz+2.5),
                            en -> en != caster && en.isAlive())) {
                        e.hurt(e.damageSources().magic(), 30.0f);
                        e.setDeltaMovement(e.getDeltaMovement().x, 1.5, e.getDeltaMovement().z);
                    }

                    // Sound per spout
                    if (g == 0) {
                        level.playSound(null, gx, gy, gz,
                                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.6f);
                        level.playSound(null, gx, gy, gz,
                                SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.2f, 0.8f);
                    }
                }
                // Periodic ground rumble
                if (w % 4 == 0) {
                    level.playSound(null, origin.x, origin.y, origin.z,
                            SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.2f, 0.3f);
                    ModNetworking.sendEffect(caster, "shake", 0.35f, 8f);
                }
            });
        }
    }
}
