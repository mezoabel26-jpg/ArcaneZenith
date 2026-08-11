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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Plasma Annihilator — 4-second continuous channelled devastation.
 * Damage ramps from 8 → 28 per tick, heat haze shader activates instantly,
 * continuous wand recoil shake, triple particle layers on the beam.
 */
public class PlasmaAnnihilatorSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "plasma_annihilator");

    @Override public ResourceLocation id()        { return ID; }
    @Override public float manaCost()             { return 55.0f; }
    @Override public int cooldownTicks()          { return 300; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();

        // Charge-up sound + instant heat haze
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0f, 0.2f);
        ModNetworking.sendEffect(caster, "heat_haze", 0.9f, 90f);
        ModNetworking.sendEffect(caster, "fov_punch", -12f, 10f);
        ModNetworking.sendEffect(caster, "shake",      0.25f, 85f);

        // 4 s = 80 ticks, fire every 2 ticks → 40 beam pulses
        for (int tick = 2; tick <= 80; tick += 2) {
            int t = tick;
            DelayedEffectScheduler.schedule(t, () -> {
                float ramp = (float) t / 80f; // 0 → 1 over channel duration
                fireBeam(level, caster, ramp);
            });
        }

        // Rising crescendo sounds mid-beam
        DelayedEffectScheduler.schedule(20, () -> level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.8f));
        DelayedEffectScheduler.schedule(50, () -> level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.8f, 1.4f));
        // Final scream
        DelayedEffectScheduler.schedule(80, () -> {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.6f);
            ModNetworking.sendEffect(caster, "shake", 0.5f, 8f);
        });
    }

    private void fireBeam(ServerLevel level, ServerPlayer caster, float ramp) {
        // Damage ramps 8 → 28 per firing pulse (every 2 ticks = 10× per second)
        float damage = 8f + ramp * 20f;

        Vec3 eye  = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end  = eye.add(look.scale(40.0));

        HitResult bh = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDist = bh.getType() != HitResult.Type.MISS
                ? eye.distanceTo(bh.getLocation()) : 40.0;
        Vec3 beamEnd = eye.add(look.scale(maxDist));

        // ── Layer 1: white-hot core ──────────────────────────────────
        int steps = (int) (maxDist * 1.5);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = eye.add(beamEnd.subtract(eye).scale(t));
            level.sendParticles(ModParticles.PLASMA_BEAM.get(),
                    p.x, p.y, p.z, 1, 0.015, 0.015, 0.015, 0.05);
        }

        // ── Layer 2: hot-pink plasma spirals around beam ─────────────
        double step = maxDist / 18.0;
        for (int i = 0; i <= 18; i++) {
            Vec3 base = eye.add(look.scale(i * step));
            double spiralAng = i * 0.55 + ramp * 12.0;
            Vec3 perp = new Vec3(-look.z, 0, look.x).normalize().scale(0.3);
            Vec3 up   = look.cross(perp).normalize().scale(0.3);
            Vec3 spiral = base
                    .add(perp.scale(Math.cos(spiralAng)))
                    .add(up.scale(Math.sin(spiralAng)));
            level.sendParticles(ModParticles.PLASMA_SPIRAL.get(),
                    spiral.x, spiral.y, spiral.z, 1, 0.04, 0.04, 0.04, 0.02);
        }

        // ── Layer 3: outer hot-orange dust for width ─────────────────
        for (int i = 0; i <= steps / 2; i++) {
            double t = i / (double) (steps / 2);
            Vec3 p = eye.add(beamEnd.subtract(eye).scale(t));
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(1.0f, 0.45f + ramp * 0.2f, 0.1f), 1.6f),
                    p.x, p.y, p.z, 1, 0.12, 0.08, 0.12, 0.0);
        }

        // ── Damage all pierced entities ──────────────────────────────
        AABB box = new AABB(eye, beamEnd).inflate(1.25);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive());
        for (LivingEntity target : targets) {
            if (target.getBoundingBox().inflate(0.5).clip(eye, beamEnd).isPresent()) {
                target.hurt(caster.damageSources().indirectMagic(caster, caster), damage);
                // Lava sizzle drips at hit entity
                level.sendParticles(ParticleTypes.DRIPPING_LAVA,
                        target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                        3, 0.3, 0.3, 0.3, 0.0);
            }
        }

        // ── Block-surface impact splatter ────────────────────────────
        if (bh.getType() != HitResult.Type.MISS) {
            level.sendParticles(ModParticles.PLASMA_SPIRAL.get(),
                    beamEnd.x, beamEnd.y, beamEnd.z, 4, 0.2, 0.2, 0.2, 0.06);
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(1.0f, 0.6f, 0.1f), 2.2f),
                    beamEnd.x, beamEnd.y, beamEnd.z, 6, 0.25, 0.25, 0.25, 0.04);
            level.sendParticles(ParticleTypes.DRIPPING_LAVA,
                    beamEnd.x, beamEnd.y, beamEnd.z, 2, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
