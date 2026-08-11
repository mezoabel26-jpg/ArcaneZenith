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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class GodsSpearSpell implements Spell {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "gods_spear");

    @Override public ResourceLocation id() { return ID; }
    @Override public float manaCost() { return 35.0f; }
    @Override public int cooldownTicks() { return 140; }

    private static final float PRIMARY_DAMAGE  = 55.0f;   // was 14 → 55 (armor bypassing)
    private static final float PIERCE_DAMAGE   = 22.0f;   // was 6 → 22
    private static final float DETONATE_DAMAGE = 40.0f;   // was 8 → 40
    private static final double RANGE = 40.0;

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 start = caster.getEyePosition();
        Vec3 look  = caster.getLookAngle();
        Vec3 end   = start.add(look.scale(RANGE));

        // Cinematic: 6 gold rune rings collapse onto wand tip (pre-launch)
        for (int i = 0; i < 6; i++) {
            int fi = i;
            DelayedEffectScheduler.schedule(i*2, () -> {
                double spread = 3.0 - fi*0.4;
                double ang = fi * Math.PI / 3.0;
                Vec3 ringPos = start.add(Math.cos(ang)*spread, 0, Math.sin(ang)*spread);
                level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                        ringPos.x, ringPos.y, ringPos.z, 6, 0.2,0.2,0.2, 0.03);
                level.sendParticles(ModParticles.RUNE_RING.get(),
                        ringPos.x, ringPos.y, ringPos.z, 1, 0.0,0.0,0.0, 0.0);
            });
        }

        HitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDist = blockHit.getType() != HitResult.Type.MISS
                ? start.distanceTo(blockHit.getLocation()) : RANGE;

        AABB searchBox = new AABB(start, end).inflate(1.2);
        List<LivingEntity> inPath = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != caster && e.isAlive() && e.getBoundingBox().clip(start, end).isPresent());
        inPath.sort((a, b) -> Double.compare(start.distanceToSqr(a.position()), start.distanceToSqr(b.position())));

        // Vapor-cone trail
        spawnSpearTrail(level, start, blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation() : end);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.5f, 0.6f);
        ModNetworking.sendEffect(caster, "shake",     0.8f, 10f);
        ModNetworking.sendEffect(caster, "fov_punch", 18f,  12f);

        LivingEntity primary = null;
        for (LivingEntity target : inPath) {
            if (start.distanceTo(target.position()) > maxDist) continue;
            boolean isPrimary = primary == null;
            target.hurt(ModDamageTypes.arcanePierce(level, caster),
                        isPrimary ? PRIMARY_DAMAGE : PIERCE_DAMAGE);
            if (isPrimary) { primary = target; pinAndDetonate(level, caster, target); }
        }
    }

    private void pinAndDetonate(ServerLevel level, ServerPlayer caster, LivingEntity target) {
        // MOVEMENT_SLOWDOWN 255 zárja a normal mozgást
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 65, 255, false, false, true));
        target.setDeltaMovement(0, 0, 0);

        // Ha Mob példány: NoAI flag on a 3s pin alatt, AI nem fogja felülírni a velocity-t
        boolean wasMobAiDisabled = false;
        if (target instanceof net.minecraft.world.entity.Mob mob) {
            wasMobAiDisabled = mob.isNoAi();
            mob.setNoAi(true);
        }
        final boolean finalWasNoAi = wasMobAiDisabled;

        double x = target.getX(), y = target.getY() + target.getBbHeight() / 2.0, z = target.getZ();
        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 1.8f);

        // Pin tick loop — minden 5. ticken velocity nulázás + particle
        for (int tick = 0; tick < 12; tick++) {
            final int ft = tick * 5;
            DelayedEffectScheduler.schedule(ft, () -> {
                if (!target.isAlive()) return;
                target.setDeltaMovement(0, 0, 0);
                double tx = target.getX(), ty = target.getY() + target.getBbHeight() / 2.0, tz = target.getZ();
                level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                        tx, ty, tz, 12, 0.4, 0.4, 0.4, 0.02);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.85f, 0.1f), 1.8f),
                        tx, ty, tz, 8, 0.3, 0.3, 0.3, 0.01);
            });
        }

        // AI visszaállítása + detonáció 3s-nél (60 tick)
        DelayedEffectScheduler.schedule(60, () -> {
            // AI visszaad
            if (target instanceof net.minecraft.world.entity.Mob mob) {
                mob.setNoAi(finalWasNoAi);
            }
            if (!target.isAlive()) return;

            double tx = target.getX(), ty = target.getY() + target.getBbHeight() / 2.0, tz = target.getZ();
            target.hurt(ModDamageTypes.arcanePierce(level, caster), DETONATE_DAMAGE);

            // Masszív arany robbanás
            level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),   tx, ty, tz, 100, 1.5, 1.5, 1.5, 0.2);
            level.sendParticles(ModParticles.HEAVEN_BEAM.get(),    tx, ty, tz,  12, 0.2, 0.5, 0.2, 0.02);
            level.sendParticles(ParticleTypes.END_ROD,             tx, ty, tz,  50, 1.2, 1.2, 1.2, 0.15);
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(1.0f, 0.8f, 0.1f), 3.0f),
                    tx, ty, tz, 40, 1.0, 1.0, 1.0, 0.08);
            level.playSound(null, tx, ty, tz,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.9f);

            ModNetworking.sendEffect(caster, "shake",     1.0f, 14f);
            ModNetworking.sendEffect(caster, "fov_punch", 20f,  10f);

            // AoE detonáció
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(tx - 4, ty - 4, tz - 4, tx + 4, ty + 4, tz + 4),
                    en -> en.isAlive())) {
                e.hurt(ModDamageTypes.arcanePierce(level, caster), 20.0f);
            }
        });
    }

    private void spawnSpearTrail(ServerLevel level, Vec3 start, Vec3 end) {
        int steps = 30;
        Vec3 delta = end.subtract(start);
        for (int i = 0; i <= steps; i++) {
            double t = i/(double)steps;
            Vec3 p = start.add(delta.scale(t));
            level.sendParticles(ModParticles.GOLDEN_LIGHT.get(), p.x,p.y,p.z, 2, 0.08,0.08,0.08, 0.02);
            level.sendParticles(new DustParticleOptions(new Vector3f(1.0f,0.85f,0.2f),1.2f),
                    p.x,p.y,p.z, 1, 0.05,0.05,0.05, 0.0);
        }
        level.sendParticles(ParticleTypes.END_ROD, end.x,end.y,end.z, 15, 0.4,0.4,0.4, 0.04);
    }
}
