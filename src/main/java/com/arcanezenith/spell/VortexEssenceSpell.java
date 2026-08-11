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
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * VORTEX ESSENCE — Iron's Spells ihlette gravitációs szingularitás.
 *
 * Sötétlila örvénylő szingularitás jelenik meg a caster előtt. 5 másodpercig
 * 1200 particle-stream húzza be az ellenségeket, majd robban: 260 dmg 18m körben.
 * Az accretion disk animáció 3 koncentrikus gyűrűből áll, forgó sebességük különböző.
 */
public class VortexEssenceSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "vortex_essence");

    private static final float  IMPLOSION_DMG = 260.0f;
    private static final double PULL_RANGE    = 22.0;
    private static final double DETONATE_RANGE= 18.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 70.0f; }
    @Override public int cooldownTicks()      { return 350; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.getEyePosition().add(caster.getLookAngle().scale(10));

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.15f);
        ModNetworking.sendEffect(caster, "gravity",    0.3f, 115f);
        // KÖDFAL — a gravitáció eltorzítja a látást
        SpellEnvironmentFX.castFogZone(level, caster, 25.0, 115);
        // FÉNY — a szingularitás középpontja fényes
        SpellEnvironmentFX.spawnLight(level, center, 105);
        ModNetworking.sendEffect(caster, "void_rift",   0.0f, 115f);
        ModNetworking.sendEffect(caster, "fov_punch", -25f,  110f);
        ModNetworking.sendEffect(caster, "shake",       0.3f, 110f);

        // ── 5s PULL PHASE ─────────────────────────────────────────────
        for (int tick = 1; tick <= 100; tick++) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                double growth = 1.0 + t * 0.025;

                // 3 koncentrikus accretion gyűrű különböző forgással
                int[][] rings = {{30, 2, 0}, {22, 3, 1}, {15, 5, 2}};
                for (int[] ring : rings) {
                    int pts   = ring[0];
                    double r  = (ring[1] + ring[2] * 0.5) * growth;
                    double rot= ring[2] * 0.4;
                    float hue = 0.15f + ring[2] * 0.12f;
                    for (int p = 0; p < pts; p++) {
                        double ang = p * Math.PI * 2 / pts + t * (0.15 + rot);
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(hue, 0.0f, 0.6f + ring[2]*0.12f), 1.0f),
                                center.x + Math.cos(ang)*r,
                                center.y + Math.sin(ang*2)*0.3,
                                center.z + Math.sin(ang)*r,
                                1, 0.03,0.03,0.03, 0.0);
                    }
                }
                // Event horizon
                level.sendParticles(ModParticles.VORTEX_RING.get(), center.x, center.y, center.z, 8, 0.5,0.1,0.5, 0.02);
                level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 3, 0.2,0.2,0.2, 0.0);
                level.sendParticles(ModParticles.VOID_CORE.get(),  center.x, center.y, center.z, 2, 0.05,0.05,0.05, 0.0);

                // Inward dust streams — 24 irány
                for (int s = 0; s < 24; s++) {
                    double ang  = s * Math.PI * 2 / 24.0 + t * 0.06;
                    double dist = Math.max(2.0, 20 - t * 0.18);
                    double vAng = ang * 0.7;
                    Vec3 src = center.add(Math.cos(ang)*dist, Math.sin(vAng)*dist*0.4, Math.sin(ang)*dist);
                    Vec3 dir = center.subtract(src).normalize().scale(0.5);
                    level.sendParticles(ModParticles.GRAVITY_DUST.get(),
                            src.x, src.y, src.z, 1, dir.x,dir.y,dir.z, 0.0);
                }

                // Gravitációs húzás — inverse-square
                AABB pullBox = new AABB(center.x-PULL_RANGE, center.y-PULL_RANGE, center.z-PULL_RANGE,
                                         center.x+PULL_RANGE, center.y+PULL_RANGE, center.z+PULL_RANGE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pullBox,
                        en -> en != caster && en.isAlive())) {
                    Vec3 toC = center.subtract(e.position());
                    double d2 = Math.max(1, toC.lengthSqr());
                    double force = Math.min(1.1, 20.0 / (d2 + 1));
                    e.setDeltaMovement(e.getDeltaMovement().add(toC.normalize().scale(force)));
                    e.hurtMarked = true;
                }
                if (t % 25 == 0)
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.1f + t*0.002f);
            });
        }

        // ── DETONÁCIÓ ─────────────────────────────────────────────────
        DelayedEffectScheduler.schedule(102, () -> {
            // Shockwave gyűrű
            for (int i = 0; i < 100; i++) {
                double ang = i * Math.PI * 2 / 100.0;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.2f, 0.0f, 0.5f), 2.8f),
                        center.x + Math.cos(ang)*DETONATE_RANGE*0.6, center.y,
                        center.z + Math.sin(ang)*DETONATE_RANGE*0.6,
                        1, 0,0.15,0, 0.04);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 8, 0,0,0,0);
            level.sendParticles(ModParticles.VOID_CORE.get(), center.x, center.y, center.z, 40, 2.0,2.0,2.0, 0.2);
            level.sendParticles(ModParticles.SINGULARITY_NOVA.get(), center.x, center.y, center.z, 20, 1.5,1.5,1.5, 0.06);

            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.25f);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0f, 0.4f);

            ModNetworking.sendEffect(caster, "shake",     1.5f, 22f);
            ModNetworking.sendEffect(caster, "fov_punch", 30f,  15f);

            AABB detonateBox = new AABB(center.x-DETONATE_RANGE, center.y-DETONATE_RANGE, center.z-DETONATE_RANGE,
                                         center.x+DETONATE_RANGE, center.y+DETONATE_RANGE, center.z+DETONATE_RANGE);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, detonateBox,
                    en -> en != caster && en.isAlive())) {
                e.hurt(caster.damageSources().magic(), IMPLOSION_DMG);
                Vec3 knock = e.position().subtract(center).normalize().scale(3.0);
                e.setDeltaMovement(knock.x, 2.0, knock.z);
            }
            com.arcanezenith.combat.TerrainDestruction.carveCrater(level, center.x, center.y, center.z, 5.0);
        });
    }
}
