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

import java.util.List;

/**
 * GLINTSTONE PHALANX — Elden Ring neonkék mágikus tőrök.
 *
 * 16 ragyogó neonkék mágikus tőr ívben jelenik meg a caster feje felett.
 * Automatikusan lőnek ki a legközelebbi ellenfelekre, elegáns fénycsíkot húzva.
 * Minden tőr 70 dmg, egyszerre 16 → max 1120 dmg. 8 másodpercig tartanak.
 */
public class GlintstonePhalanxSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "glintstone_phalanx");

    private static final int   DAGGER_COUNT = 16;
    private static final float DAGGER_DMG   = 70.0f;
    private static final double TRACK_RANGE = 25.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 50.0f; }
    @Override public int cooldownTicks()      { return 280; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 castPos = caster.position().add(0, 2.5, 0);

        // ── MEGJELENÉS — tőrök ívben materializálódnak ────────────────
        for (int i = 0; i < DAGGER_COUNT; i++) {
            final int fi = i;
            DelayedEffectScheduler.schedule(i, () -> {
                double progress = fi / (double)(DAGGER_COUNT - 1);
                double arcAng   = -Math.PI/2 + progress * Math.PI; // -90° → +90°
                double x = castPos.x + Math.cos(arcAng) * 3.5;
                double y = castPos.y + Math.sin(arcAng) * 1.2 + 0.5;
                double z = castPos.z;

                // Neonkék materializálódás
                level.sendParticles(ModParticles.GLINT_DAGGER.get(),
                        x, y, z, 4, 0.15,0.15,0.15, 0.04);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.1f, 0.6f, 1.0f), 1.4f),
                        x, y, z, 8, 0.2,0.2,0.2, 0.04);
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.6f, 0.9f, 1.0f), 0.7f),
                        x, y, z, 4, 0.08,0.08,0.08, 0.02);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 3, 0.1,0.1,0.1, 0.02);
                level.playSound(null, x, y, z,
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.8f + fi*0.02f);
            });
        }

        ModNetworking.sendEffect(caster, "fov_punch", -12f, 15f);
        level.playSound(null, castPos.x, castPos.y, castPos.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.8f);

        // ── ORBIT + AUTO-FIRE PHASE (8s = 160 tick) ──────────────────
        for (int tick = 5; tick <= 160; tick++) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                Vec3 cp = caster.position().add(0, 2.5, 0);
                double rotSpeed = t * 0.04;

                // Tőrök orbiting — neonkék ívben forognak
                for (int i = 0; i < DAGGER_COUNT; i++) {
                    double progress = i / (double)(DAGGER_COUNT - 1);
                    double arcAng   = -Math.PI/2 + progress * Math.PI + rotSpeed * 0.15;
                    double dagX = cp.x + Math.cos(arcAng) * 3.5;
                    double dagY = cp.y + Math.sin(arcAng) * 1.2 + 0.5;
                    double dagZ = cp.z + Math.sin(rotSpeed + i * 0.3) * 0.4;

                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.1f, 0.55f, 1.0f), 1.0f),
                            dagX, dagY, dagZ, 1, 0,0,0, 0.0);
                    // Kék ragyogás
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.4f, 0.8f, 1.0f), 0.5f),
                            dagX, dagY, dagZ, 1, 0.04,0.04,0.04, 0.0);
                }

                // Automatikus tüzelés — minden 10 tickben 1 tőr lő
                if (t % 10 == 0) {
                    // Legközelebbi célpont keresés
                    AABB trackBox = new AABB(cp.x-TRACK_RANGE, cp.y-5, cp.z-TRACK_RANGE,
                                             cp.x+TRACK_RANGE, cp.y+10, cp.z+TRACK_RANGE);
                    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, trackBox,
                            e -> e != caster && e.isAlive());
                    if (targets.isEmpty()) return;

                    targets.sort((a, b) -> Double.compare(
                            cp.distanceToSqr(a.position()), cp.distanceToSqr(b.position())));

                    // Az aktuális tőr indexe
                    int daggerIdx = (t / 10 - 1) % DAGGER_COUNT;
                    LivingEntity target = targets.get(0);

                    double progress = daggerIdx / (double)(DAGGER_COUNT - 1);
                    double arcAng   = -Math.PI/2 + progress * Math.PI + rotSpeed * 0.15;
                    Vec3 daggerOrigin = cp.add(Math.cos(arcAng)*3.5, Math.sin(arcAng)*1.2+0.5, 0);
                    Vec3 targetPos = target.position().add(0, target.getBbHeight()/2.0, 0);

                    // Elegáns fénycsík — gyors hitscan trail
                    Vec3 dir = targetPos.subtract(daggerOrigin).normalize();
                    double dist = daggerOrigin.distanceTo(targetPos);
                    int steps = (int)(dist * 2);
                    for (int s = 0; s <= steps; s++) {
                        double tt = s / (double) steps;
                        Vec3 p = daggerOrigin.add(dir.scale(tt * dist));
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.15f, 0.65f, 1.0f), 0.9f),
                                p.x, p.y, p.z, 1, 0.02,0.02,0.02, 0.0);
                    }

                    // Impact
                    target.hurt(caster.damageSources().indirectMagic(caster, caster), DAGGER_DMG);
                    Vec3 tp = target.position().add(0, target.getBbHeight()/2.0, 0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.2f, 0.7f, 1.0f), 2.2f),
                            tp.x, tp.y, tp.z, 20, 0.4,0.4,0.4, 0.08);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            tp.x, tp.y, tp.z, 10, 0.3,0.3,0.3, 0.06);
                    level.playSound(null, tp.x, tp.y, tp.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.6f);
                }
            });
        }

        // Lejárat
        DelayedEffectScheduler.schedule(162, () -> {
            Vec3 cp = caster.position().add(0, 2.5, 0);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.1f,0.5f,1.0f),1.5f),
                    cp.x, cp.y, cp.z, 40, 2.0,1.0,2.0, 0.06);
            level.playSound(null, cp.x, cp.y, cp.z,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f);
        });
    }
}
