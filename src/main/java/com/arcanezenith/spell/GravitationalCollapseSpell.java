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

public class GravitationalCollapseSpell implements Spell {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "gravitational_collapse");

    @Override public ResourceLocation id() { return ID; }
    @Override public float manaCost() { return 40.0f; }
    @Override public int cooldownTicks() { return 240; }

    private static final float IMPLOSION_DAMAGE = 60.0f;   // was 25 → 60

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.getEyePosition().add(caster.getLookAngle().scale(12));

        level.playSound(null, center.x,center.y,center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.25f);

        // Gravity lens + FOV pull
        ModNetworking.sendEffect(caster, "gravity",    0.25f, 130f);
        ModNetworking.sendEffect(caster, "fov_punch", -20f,   80f);
        ModNetworking.sendEffect(caster, "shake",      0.3f,  20f);

        // Pull phase: 3s (60 ticks), every 2 ticks
        for (int tick = 1; tick <= 60; tick++) {
            int t = tick;
            DelayedEffectScheduler.schedule(tick*2, () -> {
                // 1200 inward dust streams (design spec: 1200 streams)
                for (int i = 0; i < 20; i++) {
                    double ang   = i * Math.PI * 2 / 20.0 + t*0.08;
                    double dist  = Math.max(1.5, 14 - t*0.2);
                    double hAng  = ang * 1.3;
                    Vec3 src = center.add(
                            Math.cos(ang)*dist, Math.sin(hAng)*dist*0.6, Math.sin(ang)*dist);
                    Vec3 dir = center.subtract(src).normalize().scale(0.4);
                    level.sendParticles(ModParticles.GRAVITY_DUST.get(),
                            src.x,src.y,src.z, 2, dir.x,dir.y,dir.z, 0.0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.35f,0.0f,0.6f),1.0f), src.x,src.y,src.z,
                            1, dir.x,dir.y,dir.z, 0.0);
                }
                // Void core pulses
                level.sendParticles(ModParticles.VOID_CORE.get(),
                        center.x,center.y,center.z, 5, 0.05,0.05,0.05, 0.0);

                // Inverse-square pull
                AABB pullBox = new AABB(center.x-15,center.y-15,center.z-15,
                                         center.x+15,center.y+15,center.z+15);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pullBox,
                        en -> en != caster && en.isAlive())) {
                    Vec3 toCenter = center.subtract(e.position());
                    double dist2  = toCenter.lengthSqr();
                    if (dist2 < 0.01) continue;
                    double force  = Math.min(0.9, 18.0 / (dist2+1));
                    Vec3 pull = toCenter.normalize().scale(force);
                    e.setDeltaMovement(e.getDeltaMovement().add(pull));
                    e.hurtMarked = true;
                }
            });
        }

        // Detonation at 3s
        DelayedEffectScheduler.schedule(122, () -> {
            // Terrain carve: blocks pulled into void
            TerrainDestruction.carveCrater(level, center.x,center.y,center.z, 4.5);
            TerrainDestruction.playCrumbleSound(level, center.x,center.y,center.z);

            // Expanding dark purple shockwave ring
            for (int i = 0; i < 80; i++) {
                double ang = i * Math.PI * 2 / 80.0;
                level.sendParticles(new DustParticleOptions(new Vector3f(0.15f,0.0f,0.35f),2.5f),
                        center.x+Math.cos(ang)*9, center.y, center.z+Math.sin(ang)*9,
                        1, 0,0.2,0, 0.03);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x,center.y,center.z, 3, 0,0,0, 0);
            level.sendParticles(ModParticles.VOID_CORE.get(), center.x,center.y,center.z, 30, 1.5,1.5,1.5, 0.2);
            level.playSound(null, center.x,center.y,center.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 0.4f);

            ModNetworking.sendEffect(caster, "shake",      1.2f, 20f);
            ModNetworking.sendEffect(caster, "fov_punch",  25f,  12f);

            AABB blastBox = new AABB(center.x-12,center.y-12,center.z-12,
                                      center.x+12,center.y+12,center.z+12);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, blastBox,
                    en -> en != caster && en.isAlive())) {
                e.hurt(caster.damageSources().magic(), IMPLOSION_DAMAGE);
                Vec3 knock = e.position().subtract(center).normalize().scale(2.5);
                e.setDeltaMovement(knock.x, 1.5, knock.z);
            }
        });
    }
}
