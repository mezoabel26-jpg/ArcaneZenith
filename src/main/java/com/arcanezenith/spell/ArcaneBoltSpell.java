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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Arcane Bolt — rapid hitscan laser with stacking Arcane Mark system.
 * 3rd consecutive hit on same target detonates the mark for AoE force damage.
 *
 * VISUAL UPGRADES:
 * - 3-layer particle trail: core white streak + blue-purple glow + outer dust
 * - Impact: radial 360° spark burst + screen micro-recoil
 * - Mark stacks show as pulsing rune rings around target
 * - Detonation: full explosion particle set + heavy shake + FOV punch
 */
public class ArcaneBoltSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "arcane_bolt");

    private static final float MANA_COST    = 4.0f;
    private static final int   COOLDOWN     = 6;
    private static final double RANGE        = 40.0;
    private static final float DAMAGE        = 12.0f;
    private static final float DETONATE_DMG  = 22.0f;

    @Override public ResourceLocation id()       { return ID; }
    @Override public float manaCost()             { return MANA_COST; }
    @Override public int cooldownTicks()          { return COOLDOWN; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 eye  = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end  = eye.add(look.scale(RANGE));

        HitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDist = blockHit.getType() != HitResult.Type.MISS
                ? eye.distanceTo(blockHit.getLocation()) : RANGE;
        Vec3 impact = eye.add(look.scale(maxDist));

        // ── Triple-layer bolt trail ────────────────────────────────────
        int steps = (int)(maxDist * 2);
        Vec3 delta = impact.subtract(eye);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = eye.add(delta.scale(t));
            // Layer 1: bright white core
            level.sendParticles(new DustParticleOptions(new Vector3f(0.95f,0.92f,1.0f),0.55f),
                    p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            // Layer 2: blue-purple glow (every other step)
            if (i % 2 == 0)
                level.sendParticles(new DustParticleOptions(new Vector3f(0.55f,0.3f,1.0f),1.0f),
                        p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
        // Layer 3: END_ROD sparks along path
        level.sendParticles(ParticleTypes.END_ROD, eye.x, eye.y, eye.z,
                (int)(maxDist*0.5), look.x*maxDist*0.5, look.y*maxDist*0.5, look.z*maxDist*0.5, 0.0);

        // Micro camera recoil on cast
        ModNetworking.sendEffect(caster, "fov_punch", 5f, 4f);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.8f);

        // ── Hitscan entity check ───────────────────────────────────────
        Entity hit = raytraceEntity(level, caster, eye, end, maxDist);
        if (hit instanceof LivingEntity target) {
            target.hurt(caster.damageSources().indirectMagic(caster, caster), DAMAGE);
            int stacks = ArcaneMarkTracker.addMark(target.getUUID());

            // Impact burst — density scales with mark stacks
            int burstCount = 18 + stacks * 10;
            Vec3 hp = target.position().add(0, target.getBbHeight()/2.0, 0);
            level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                    hp.x, hp.y, hp.z, burstCount, 0.4, 0.4, 0.4, 0.12);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.7f,0.3f,1.0f),1.6f),
                    hp.x, hp.y, hp.z, burstCount/2, 0.35, 0.35, 0.35, 0.06);
            level.sendParticles(ParticleTypes.CRIT, hp.x, hp.y, hp.z,
                    12, 0.3, 0.3, 0.3, 0.05);

            // Mark rune rings orbiting target
            for (int s = 0; s < stacks; s++) {
                int fs = s;
                DelayedEffectScheduler.schedule(s * 2, () -> {
                    if (target.isAlive()) {
                        Vec3 tp = target.position().add(0, target.getBbHeight()/2.0, 0);
                        level.sendParticles(ModParticles.RUNE_RING.get(),
                                tp.x, tp.y, tp.z, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                });
            }

            level.playSound(null, hp.x, hp.y, hp.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                    0.5f + stacks * 0.1f, 1.6f - stacks * 0.1f);

            if (stacks >= 3) {
                ArcaneMarkTracker.clear(target.getUUID());
                detonateMark(level, caster, target, hp);
            }
        } else {
            // Miss — small spark burst at impact point
            level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                    impact.x, impact.y, impact.z, 8, 0.15, 0.15, 0.15, 0.08);
        }
    }

    private void detonateMark(ServerLevel level, ServerPlayer caster,
                               LivingEntity target, Vec3 hp) {
        // ── DETONATION VFX ────────────────────────────────────────────
        level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                hp.x, hp.y, hp.z, 120, 1.4, 1.4, 1.4, 0.22);
        level.sendParticles(ModParticles.RUNE_RING.get(),
                hp.x, hp.y, hp.z, 8, 0.05, 0.05, 0.05, 0.0);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.85f,0.45f,1.0f),3.0f),
                hp.x, hp.y, hp.z, 60, 1.2, 1.2, 1.2, 0.08);
        level.sendParticles(ParticleTypes.EXPLOSION,
                hp.x, hp.y, hp.z, 5, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.END_ROD,
                hp.x, hp.y, hp.z, 30, 1.0, 1.0, 1.0, 0.18);

        level.playSound(null, hp.x, hp.y, hp.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1f, 1.4f);
        level.playSound(null, hp.x, hp.y, hp.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.6f);

        ModNetworking.sendEffect(caster, "shake",     0.55f, 10f);
        ModNetworking.sendEffect(caster, "fov_punch", 14f,   10f);

        // AoE damage in 4.5-block radius
        AABB aoe = new AABB(hp.x-4.5, hp.y-4.5, hp.z-4.5,
                            hp.x+4.5, hp.y+4.5, hp.z+4.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aoe,
                en -> en != target && en.isAlive())) {
            e.hurt(caster.damageSources().indirectMagic(caster, caster), DETONATE_DMG);
            Vec3 push = e.position().subtract(hp).normalize().scale(1.1);
            e.push(push.x, 0.4, push.z);
        }
    }

    private Entity raytraceEntity(ServerLevel level, ServerPlayer caster,
                                   Vec3 start, Vec3 end, double maxDist) {
        AABB box = new AABB(start, end).inflate(1.0);
        Entity best = null; double bestD = maxDist;
        for (Entity e : level.getEntities(caster, box, en -> en instanceof LivingEntity && en.isAlive())) {
            var opt = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (opt.isPresent()) {
                double d = start.distanceTo(opt.get());
                if (d < bestD) { bestD = d; best = e; }
            }
        }
        return best;
    }
}
