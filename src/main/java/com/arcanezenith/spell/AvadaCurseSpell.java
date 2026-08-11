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
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * AVADA CURSE — Killing Curse ihletett halálfény.
 * 200 TRUE DMG instant hitscan, 60 blokk hatótáv.
 * Képernyő zöldre villan, a célpont halálakor "lélek-kiszakadás" burst.
 * Mély dörej + üveg-törés hang kombó.
 */
public class AvadaCurseSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "avada_curse");

    private static final float DAMAGE = 200.0f;
    private static final double RANGE  = 60.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 80.0f; }
    @Override public int cooldownTicks()      { return 400; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 eye  = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end  = eye.add(look.scale(RANGE));

        HitResult bh = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDist = bh.getType() != HitResult.Type.MISS
                ? eye.distanceTo(bh.getLocation()) : RANGE;
        Vec3 impact = eye.add(look.scale(maxDist));

        // ── Halálfény trail — élénkzöld, sűrű ────────────────────────
        int steps = (int)(maxDist * 2.5);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = eye.add(impact.subtract(eye).scale(t));
            // Belső mag — sárga-zöld
            level.sendParticles(ModParticles.DEATH_FLASH.get(), p.x, p.y, p.z, 1, 0.02,0.02,0.02, 0.0);
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.6f, 1.0f, 0.1f), 0.6f),
                    p.x, p.y, p.z, 1, 0.02,0.02,0.02, 0.0);
            // Külső aura — sötétzöld
            if (i % 2 == 0)
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.1f, 0.8f, 0.05f), 1.2f),
                        p.x, p.y, p.z, 2, 0.06,0.06,0.06, 0.02);
        }
        // Zöld képernyő-villanás + mély hang
        ModNetworking.sendEffect(caster, "fov_punch", 8f, 6f);
        ModNetworking.sendEffect(caster, "blood_curse", 1.0f, 30f); // green death pulse
        level.playSound(null, eye.x, eye.y, eye.z,
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.35f);
        // Sötét crash hang réteg
        SpellEnvironmentFX.playDarkCrash(level, eye.x, eye.y, eye.z);
        level.playSound(null, eye.x, eye.y, eye.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.8f, 0.5f);

        // ── Hitscan ────────────────────────────────────────────────────
        Entity hit = findTarget(level, caster, eye, end, maxDist);
        if (hit instanceof LivingEntity target) {
            boolean wasAlive = target.isAlive();
            target.hurt(ModDamageTypes.arcaneTrue(level, caster), DAMAGE);

            Vec3 tp = target.position().add(0, target.getBbHeight()/2.0, 0);

            // Halálos hit: lélek-kiszakadás
            if (wasAlive && !target.isAlive()) {
                soulRipEffect(level, tp);
            } else {
                // Túlélte (boss?) — masszív zöld robbanás
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.3f, 1.0f, 0.05f), 3.0f),
                        tp.x, tp.y, tp.z, 80, 1.0,1.0,1.0, 0.12);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        tp.x, tp.y, tp.z, 3, 0,0,0, 0);
                ModNetworking.sendEffect(caster, "shake",     1.5f, 18f);
                ModNetworking.sendEffect(caster, "fov_punch", 25f,  15f);
            }
            level.playSound(null, tp.x, tp.y, tp.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.4f);
        } else {
            // Falba csapódás
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.2f, 0.9f, 0.05f), 2.0f),
                    impact.x, impact.y, impact.z, 30, 0.4,0.4,0.4, 0.06);
        }
    }

    private void soulRipEffect(ServerLevel level, Vec3 pos) {
        // Fehér sziluett kipárologás felfelé
        for (int i = 0; i < 30; i++) {
            double ang = Math.random() * Math.PI * 2;
            double r   = Math.random() * 0.6;
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.95f, 1.0f, 0.95f), 1.5f),
                    pos.x + Math.cos(ang)*r,
                    pos.y + i * 0.08,
                    pos.z + Math.sin(ang)*r,
                    1, (Math.random()-0.5)*0.05, 0.06, (Math.random()-0.5)*0.05, 0.0);
        }
        // Zöld robbanás a testből
        level.sendParticles(new DustParticleOptions(
                new Vector3f(0.3f, 1.0f, 0.1f), 2.5f),
                pos.x, pos.y, pos.z, 60, 0.8,0.8,0.8, 0.10);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z, 20, 0.5,0.5,0.5, 0.15);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 1.2f);
    }

    private Entity findTarget(ServerLevel level, ServerPlayer caster,
                               Vec3 start, Vec3 end, double maxDist) {
        AABB box = new AABB(start, end).inflate(0.8);
        Entity best = null; double bestD = maxDist;
        for (Entity e : level.getEntities(caster, box,
                en -> en instanceof LivingEntity && en.isAlive())) {
            var opt = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (opt.isPresent()) {
                double d = start.distanceTo(opt.get());
                if (d < bestD) { bestD = d; best = e; }
            }
        }
        return best;
    }
}
