package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.combat.ModDamageTypes;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * EXCALIBUR BEAM — Fate/Zero "Sword of Promised Victory" ihlette arany lézersugár.
 *
 * 0.8s charge-up alatt az egész képernyő aranyra ragyog, majd egyetlen hatalmas
 * arany lézerkard-vágás hasítja ketté a csatateret — 80 blokk hatótáv, 5 blokk
 * széles sugár, 320 true dmg. A sugár útján lévő blokkok porrá válnak.
 *
 * VFX: töltés alatt pulzáló HOLY_BLOOM shader, majd egyetlen vakító fehér-arany
 * villanás, utána golden dust settles le az egész úton.
 */
public class ExcaliburBeamSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "excalibur_beam");

    private static final float  DAMAGE     = 320.0f;
    private static final double RANGE      = 80.0;
    private static final double BEAM_WIDTH = 2.5;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 85.0f; }
    @Override public int cooldownTicks()      { return 600; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();

        // ── TÖLTÉS (16 tick = 0.8s) ───────────────────────────────────
        ModNetworking.sendEffect(caster, "holy_bloom", 1.0f, 20f);
        // LEBEGŐ TÖLTÉS — lovagi energia összegyűlés
        SpellEnvironmentFX.chargeFloatAnimation(level, caster, 16, ModParticles.EXCALIBUR_BEAM.get());
        SpellEnvironmentFX.playMysticCharge(level, caster.getX(), caster.getY(), caster.getZ());
        ModNetworking.sendEffect(caster, "arcane_overdrive", 1.0f, 22f);
        ModNetworking.sendEffect(caster, "fov_punch", -20f, 15f);

        // Töltési aura — arany gyűrűk összehúzódnak a caster kezébe
        for (int t = 0; t < 16; t++) {
            final int ft = t;
            DelayedEffectScheduler.schedule(t, () -> {
                double shrink = 1.0 - ft/16.0;
                Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.8));
                for (int i = 0; i < 12; i++) {
                    double ang = i * Math.PI * 2 / 12.0 + ft * 0.3;
                    double r   = shrink * 3.0;
                    level.sendParticles(ModParticles.EXCALIBUR_BEAM.get(),
                            hand.x + Math.cos(ang)*r, hand.y + Math.sin(ang)*r*0.5,
                            hand.z + Math.sin(ang)*r,
                            1, 0, 0, 0, 0.0);
                    level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                            hand.x + Math.cos(ang)*r, hand.y + Math.sin(ang)*r*0.5,
                            hand.z + Math.sin(ang)*r,
                            1, 0, 0, 0, 0.0);
                }
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.92f, 0.3f), 2.0f),
                        hand.x, hand.y, hand.z,
                        6, 0.1,0.1,0.1, 0.04);
                if (ft == 8)
                    level.playSound(null, hand.x, hand.y, hand.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 1.8f);
            });
        }

        // ── TŰZELÉS 16 tick után ──────────────────────────────────────
        DelayedEffectScheduler.schedule(16, () -> {
            Vec3 eye  = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            Vec3 end  = eye.add(look.scale(RANGE));

            HitResult bh = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            double maxDist = bh.getType() != HitResult.Type.MISS
                    ? eye.distanceTo(bh.getLocation()) : RANGE;
            Vec3 impact = eye.add(look.scale(maxDist));

            // ── VAKÍTÓ FLASH ───────────────────────────────────────────
            // Teljes arany-fehér beam az egész úton
            int steps = (int)(maxDist * 3);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                Vec3 p   = eye.add(impact.subtract(eye).scale(t));
                // Mag — fehér
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.98f, 0.9f), 1.4f),
                        p.x, p.y, p.z, 3, 0.08,0.08,0.08, 0.0);
                // Szélek — arany
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.82f, 0.1f), 2.2f),
                        p.x, p.y, p.z, 4, BEAM_WIDTH*0.2, BEAM_WIDTH*0.15, BEAM_WIDTH*0.2, 0.02);
                // HEAVEN_BEAM overlay
                if (i % 3 == 0)
                    level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                            p.x, p.y, p.z, 1, BEAM_WIDTH*0.15, 0.3, BEAM_WIDTH*0.15, 0.01);
            }
            // Impact robbanás
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 6, 0,0,0,0);
            level.sendParticles(ModParticles.HEAVEN_BEAM.get(), impact.x, impact.y, impact.z, 30, 1.5,1.5,1.5, 0.12);
            level.sendParticles(new DustParticleOptions(new Vector3f(1.0f,0.95f,0.5f),4.0f),
                    impact.x, impact.y, impact.z, 50, 2.0,2.0,2.0, 0.08);

            // Hangok
            level.playSound(null, eye.x, eye.y, eye.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 1.2f);
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.6f);
            level.playSound(null, eye.x, eye.y, eye.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 2.0f);

            ModNetworking.sendEffect(caster, "shake",      2.0f, 20f);
            ModNetworking.sendEffect(caster, "fov_punch",  40f,  18f);
            ModNetworking.sendEffect(caster, "holy_bloom", 0.8f, 30f);

            // ── SEBZÉS — 5 blokk széles sáv ──────────────────────────
            Vec3 perp1 = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 perp2 = look.cross(perp1).normalize();
            AABB broadBox = new AABB(eye, impact).inflate(BEAM_WIDTH + 1);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, broadBox,
                    en -> en != caster && en.isAlive())) {
                Vec3 toE = e.position().subtract(eye);
                double along = toE.dot(look);
                if (along < 0 || along > maxDist) continue;
                Vec3 proj = eye.add(look.scale(along));
                if (proj.distanceTo(e.position()) > BEAM_WIDTH + 0.5) continue;
                e.hurt(ModDamageTypes.arcaneTrue(level, caster), DAMAGE);
                e.setDeltaMovement(look.x*3.0, 0.5, look.z*3.0);
            }

            // Blokkok porrá válnak a sugár útján
            TerrainDestruction.carveFissure(level,
                    eye.x + look.x*(maxDist*0.5), eye.y, eye.z + look.z*(maxDist*0.5),
                    look.x, look.z, maxDist, BEAM_WIDTH, 2.0);

            // Utó-ragyogás — arany dust settling
            DelayedEffectScheduler.schedule(5, () -> {
                for (int i = 0; i <= steps/3; i++) {
                    double t = i / (double)(steps/3);
                    Vec3 p = eye.add(impact.subtract(eye).scale(t));
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.85f, 0.2f), 1.0f),
                            p.x, p.y, p.z, 2, 0.5,0.3,0.5, 0.01);
                }
            });
        });
    }
}
