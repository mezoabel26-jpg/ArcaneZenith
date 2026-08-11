package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.combat.ModDamageTypes;
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
 * Singularity Collapse — the most visually spectacular spell in the mod.
 * 5s black hole phase: gravity lensing shader, 1200-stream accretion disk,
 * full suction, void terrain carve. Then: 0.5s absolute silence, then a
 * reality-shattering supernova that fills the screen white and deals 100 TRUE DAMAGE
 * in a 40m radius. SINGULARITY_NOVA particles expand outward.
 */
public class SingularityCollapseSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "singularity_collapse");

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 100.0f; }
    @Override public int cooldownTicks()      { return 2400; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.getEyePosition().add(caster.getLookAngle().scale(16));

        // ── Black hole forms: gravity lensing + FOV pull + deep suction ──
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.5f, 0.15f);
        // LEBEGŐ TÖLTÉS — a caster felemelkedik és köré gyűlik az energia
        SpellEnvironmentFX.chargeFloatAnimation(level, caster, 60, ModParticles.GRAVITY_DUST.get());
        // FÉNY — a fekete lyuk mag helye fény-blokkot kap
        SpellEnvironmentFX.spawnLight(level, center, 120);
        // Mély misztikus töltéshang
        SpellEnvironmentFX.playMysticCharge(level, center.x, center.y, center.z);
        ModNetworking.sendEffect(caster, "gravity",    0.28f, 115f);
        ModNetworking.sendEffect(caster, "fov_punch", -28f,  110f);
        ModNetworking.sendEffect(caster, "shake",       0.35f, 110f);

        // ── 5s black hole phase: 100 ticks ──────────────────────────────
        for (int tick = 1; tick <= 100; tick++) {
            int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                double growth = 1.0 + t * 0.04;  // disk grows over time

                // ── Accretion disk: 60 particles/tick, 3 rings ──────────
                for (int ring = 0; ring < 3; ring++) {
                    double ringR  = (1.8 + ring * 1.0) * growth;
                    double tilt   = ring * 0.2;
                    int    ringPts = 20 + ring * 8;
                    for (int p = 0; p < ringPts; p++) {
                        double ang = p * Math.PI * 2 / ringPts + t * (0.18 - ring * 0.04);
                        double px  = center.x + Math.cos(ang) * ringR;
                        double py  = center.y + Math.sin(ang * 2) * tilt;
                        double pz  = center.z + Math.sin(ang) * ringR;
                        float hue  = 0.25f + ring * 0.15f;
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(hue, 0.0f, 0.5f + ring * 0.15f), 1.2f),
                                px, py, pz, 1, 0.04, 0.04, 0.04, 0.0);
                    }
                }

                // ── Event horizon core: pitch-black squid ink void ────────
                level.sendParticles(ParticleTypes.SQUID_INK,
                        center.x, center.y, center.z, 5, 0.25,0.25,0.25, 0.0);
                level.sendParticles(ModParticles.VOID_CORE.get(),
                        center.x, center.y, center.z, 3, 0.1,0.1,0.1, 0.0);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.0f,0.0f,0.0f),2.5f),
                        center.x, center.y, center.z, 8, 0.35,0.35,0.35, 0.0);

                // ── Inward dust streams (the "1200 streams" from design doc) ─
                for (int stream = 0; stream < 24; stream++) {
                    double ang  = stream * Math.PI * 2 / 24.0 + t * 0.07;
                    double dist = Math.max(1.5, 16 - t * 0.14);
                    double vAng = ang * 0.8;
                    Vec3 src = center.add(
                            Math.cos(ang)*dist,
                            Math.sin(vAng)*dist*0.5,
                            Math.sin(ang)*dist);
                    Vec3 dir = center.subtract(src).normalize().scale(0.45);
                    level.sendParticles(ModParticles.GRAVITY_DUST.get(),
                            src.x, src.y, src.z, 1, dir.x, dir.y, dir.z, 0.0);
                }

                // ── Inverse-square gravitational pull ─────────────────────
                AABB pullBox = new AABB(center.x-30,center.y-30,center.z-30,
                                         center.x+30,center.y+30,center.z+30);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pullBox,
                        en -> en != caster && en.isAlive())) {
                    Vec3 toCenter = center.subtract(e.position());
                    double d2     = toCenter.lengthSqr();
                    if (d2 < 0.01) continue;
                    double force  = Math.min(1.2, 22.0 / (d2+1));
                    e.setDeltaMovement(e.getDeltaMovement().add(
                            toCenter.normalize().scale(force)));
                    e.hurtMarked = true;
                }

                // Tick sounds: deepening suction hum
                if (t % 25 == 0) {
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                            0.8f, 0.15f + t*0.002f);
                }
            });
        }

        // ── Void terrain carve mid-phase (tick 80) ────────────────────────
        DelayedEffectScheduler.schedule(80, () -> {
            com.arcanezenith.combat.TerrainDestruction.carveCrater(
                    level, center.x, center.y, center.z, 5.0);
            com.arcanezenith.combat.TerrainDestruction.playCrumbleSound(
                    level, center.x, center.y, center.z);
        });

        // ── 0.5s absolute silence gap ────────────────────────────────────
        // (no particles, no sounds at tick 101-110)

        // ── SUPERNOVA at 5.5s ─────────────────────────────────────────────
        DelayedEffectScheduler.schedule(111, () -> {
            // First: blink of pure darkness — 0-particle frame
            // Then: immediate white flood
            for (int i = 0; i < 200; i++) {
                double ang  = Math.random() * Math.PI * 2;
                double incl = Math.random() * Math.PI;
                double r    = Math.random() * 40;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f,1.0f,1.0f),4.0f),
                        center.x + Math.cos(ang)*Math.sin(incl)*r,
                        center.y + Math.cos(incl)*r,
                        center.z + Math.sin(ang)*Math.sin(incl)*r,
                        1, 0,0,0, 0.0);
            }
            // Expanding supernova ring (SINGULARITY_NOVA handles animation)
            for (int i = 0; i < 16; i++) {
                double ang = i * Math.PI * 2 / 16.0;
                level.sendParticles(ModParticles.SINGULARITY_NOVA.get(),
                        center.x + Math.cos(ang)*2, center.y,
                        center.z + Math.sin(ang)*2,
                        2, 0.3,0.1,0.3, 0.0);
            }
            // Core white burst
            level.sendParticles(ModParticles.SINGULARITY_NOVA.get(),
                    center.x, center.y, center.z, 30, 2.0,2.0,2.0, 0.05);
            // Implosion debris (dark purple shards)
            for (int i = 0; i < 60; i++) {
                double ang  = Math.random()*Math.PI*2;
                double spd  = 0.4 + Math.random()*0.4;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.3f,0.0f,0.55f),2.0f),
                        center.x, center.y, center.z,
                        1, Math.cos(ang)*spd, (Math.random()-0.5)*spd*0.5, Math.sin(ang)*spd, 0.0);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    center.x, center.y, center.z, 15, 0,0,0, 0);

            // Sound: cinematic supernova blast
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.2f);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.5f, 0.35f);

            // Max cinematic effects — screen goes white, massive shake
            ModNetworking.sendEffect(caster, "shake",      2.5f, 30f);
            ModNetworking.sendEffect(caster, "fov_punch",  40f,  20f);

            // 100 TRUE DAMAGE (arcane_true bypasses armor/resistance/enchantments)
            AABB novaBox = new AABB(center.x-40,center.y-40,center.z-40,
                                     center.x+40,center.y+40,center.z+40);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, novaBox,
                    en -> en != caster && en.isAlive())) {
                e.hurt(ModDamageTypes.arcaneTrue(level, caster), 100.0f);
                // Launch surviving entities outward
                Vec3 knock = e.position().subtract(center).normalize().scale(3.5);
                e.setDeltaMovement(knock.x, 2.5, knock.z);
                // Individual death particles
                level.sendParticles(ModParticles.SINGULARITY_NOVA.get(),
                        e.getX(), e.getY()+e.getBbHeight()/2.0, e.getZ(),
                        4, 0.4,0.4,0.4, 0.04);
            }

            // Afterglow: fading purple nebula for 3s
            for (int afterTick = 5; afterTick <= 60; afterTick += 5) {
                int at = afterTick;
                DelayedEffectScheduler.schedule(afterTick, () -> {
                    double fade = 1.0 - at/65.0;
                    for (int i = 0; i < 12; i++) {
                        double ang  = Math.random()*Math.PI*2;
                        double r    = 5 + Math.random()*20;
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.5f,0.1f,0.9f),(float)(3.0*fade)),
                                center.x+Math.cos(ang)*r, center.y+(Math.random()-0.5)*8,
                                center.z+Math.sin(ang)*r,
                                1, 0,0,0, 0.0);
                    }
                });
            }
        });
    }
}
