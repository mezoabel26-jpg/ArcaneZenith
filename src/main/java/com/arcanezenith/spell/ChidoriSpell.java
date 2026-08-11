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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * CHIDORI — Naruto villámenergia-roham.
 *
 * 1s töltés alatt kék elektromos szikrák sűrűsödnek a kézben (ezer madár csicsergése
 * hangeffekt). Majd villámgyors roham a caster nézetirányában 20 blokkon —
 * az első eltalált célpont kap 180 true dmg + masszív visszalökés.
 *
 * VFX: kék THUNDER_SPARK spirál a kéz körül töltés közben, vakító kék-fehér
 * fénycsík a roham útján, ELECTRIC_SPARK burst az impactnél.
 */
public class ChidoriSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "chidori");

    private static final float  DAMAGE   = 180.0f;
    private static final double DASH_LEN = 20.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 60.0f; }
    @Override public int cooldownTicks()      { return 300; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 handPos = caster.getEyePosition().add(caster.getLookAngle().scale(1.0));

        // ── TÖLTÉS (20 tick) ──────────────────────────────────────────
        ModNetworking.sendEffect(caster, "fov_punch", -25f, 18f);
        level.playSound(null, handPos.x, handPos.y, handPos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 2.0f);

        for (int t = 0; t < 20; t++) {
            final int ft = t;
            DelayedEffectScheduler.schedule(t, () -> {
                Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.9));
                double charge = ft / 19.0;

                // Kék villám szikrák spirálban sűrűsödnek
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI * 2 / 8.0 + ft * 0.5;
                    double r   = (1.0 - charge) * 1.5;
                    level.sendParticles(ModParticles.CHIDORI_SPARK.get(),
                            hand.x + Math.cos(ang)*r, hand.y + Math.sin(ang)*r*0.4,
                            hand.z + Math.sin(ang)*r,
                            1, 0, 0, 0, 0.0);
                    level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                            hand.x + Math.cos(ang)*r, hand.y + Math.sin(ang)*r*0.4,
                            hand.z + Math.sin(ang)*r,
                            1, 0, 0, 0, 0.0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.4f, 0.7f, 1.0f), 1.0f),
                            hand.x + Math.cos(ang)*r*0.5, hand.y,
                            hand.z + Math.sin(ang)*r*0.5,
                            1, 0, 0, 0, 0.0);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z,
                        (int)(charge*8)+2, 0.12,0.08,0.12, 0.04);

                // Ezer madár csicsergése — növekvő hangintenzitás
                if (ft % 4 == 0)
                    level.playSound(null, hand.x, hand.y, hand.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                            0.3f + (float)charge*1.2f, 1.5f + (float)charge*0.5f);
            });
        }

        // ── ROHAM 20 tick után ────────────────────────────────────────
        DelayedEffectScheduler.schedule(20, () -> {
            Vec3 eye  = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            Vec3 end  = eye.add(look.scale(DASH_LEN));

            // Akadály ellenőrzés
            HitResult bh = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            double maxDist = bh.getType() != HitResult.Type.MISS
                    ? eye.distanceTo(bh.getLocation()) : DASH_LEN;

            // Caster teleportálódik a cél felé (roham szimulálása)
            Vec3 dashEnd = eye.add(look.scale(Math.max(0, maxDist - 1.5)));
            caster.teleportTo(dashEnd.x, caster.getY(), dashEnd.z);

            // Vakító kék-fehér fénycsík a teljes úton
            int steps = (int)(maxDist * 3);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                Vec3 p = eye.add(look.scale(t * maxDist));
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.6f, 0.85f, 1.0f), 1.2f),
                        p.x, p.y, p.z, 2, 0.06,0.06,0.06, 0.0);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.9f, 0.97f, 1.0f), 0.6f),
                        p.x, p.y, p.z, 1, 0.02,0.02,0.02, 0.0);
                if (i % 3 == 0)
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            p.x, p.y, p.z, 1, 0.08,0.08,0.08, 0.05);
            }

            // Roham hang — elektromos zúgás
            level.playSound(null, eye.x, eye.y, eye.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5f, 1.6f);

            // Hitscan
            Entity hit = findTarget(level, caster, eye, end, maxDist);
            if (hit instanceof LivingEntity target) {
                target.hurt(ModDamageTypes.arcaneTrue(level, caster), DAMAGE);
                Vec3 knock = look.scale(5.0);
                target.setDeltaMovement(knock.x, 2.5, knock.z);

                // IMPACT — masszív elektromos robbanás
                Vec3 tp = target.position().add(0, target.getBbHeight()/2.0, 0);
                for (int i = 0; i < 60; i++) {
                    double ang = Math.random() * Math.PI * 2;
                    double r   = Math.random() * 2.0;
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.5f, 0.8f, 1.0f), 2.0f),
                            tp.x + Math.cos(ang)*r, tp.y + Math.random()*2,
                            tp.z + Math.sin(ang)*r,
                            1, 0, 0.1, 0, 0.0);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tp.x, tp.y, tp.z, 40, 0.8,0.8,0.8, 0.15);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tp.x, tp.y, tp.z, 2, 0,0,0, 0);
                level.playSound(null, tp.x, tp.y, tp.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 1.4f);
                level.playSound(null, tp.x, tp.y, tp.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 1.8f);

                ModNetworking.sendEffect(caster, "shake",            1.8f, 15f);
                ModNetworking.sendEffect(caster, "fov_punch",        30f,  12f);
                ModNetworking.sendEffect(caster, "lightning_flash",  1.0f,  5f);
            } else {
                // Falba csapódás
                ModNetworking.sendEffect(caster, "shake",     0.8f, 8f);
                ModNetworking.sendEffect(caster, "fov_punch", 15f,  6f);
            }
        });
    }

    private Entity findTarget(ServerLevel level, ServerPlayer caster,
                               Vec3 start, Vec3 end, double maxDist) {
        AABB box = new AABB(start, end).inflate(1.2);
        Entity best = null; double bestD = maxDist;
        for (Entity e : level.getEntities(caster, box,
                en -> en instanceof LivingEntity && en.isAlive())) {
            var opt = e.getBoundingBox().inflate(0.5).clip(start, end);
            if (opt.isPresent()) {
                double d = start.distanceTo(opt.get());
                if (d < bestD) { bestD = d; best = e; }
            }
        }
        return best;
    }
}
