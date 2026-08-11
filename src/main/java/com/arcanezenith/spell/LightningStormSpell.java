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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * LIGHTNING STORM — Total War: Warhammer elektromos viharfelhő.
 *
 * Lila-kék elektromos viharfelhő jelenik meg Y+15-n, 30 másodpercig másodpercenként
 * 12 monumentális villámcsapást lő le random célpontokra a 35 blokk sugarú körben.
 * Összesített max sebzés: 12 × 30 × 40 = 14400 dmg (hadseregtörlő).
 * A LIGHTNING_FLASH shader minden csapásnál felvillan.
 */
public class LightningStormSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "lightning_storm");

    private static final float  BOLT_DMG   = 40.0f;
    private static final int    BOLTS_PER_WAVE = 12;
    private static final double STORM_RANGE= 35.0;
    private static final int    DURATION   = 30; // másodperc
    private static final Random RNG        = new Random();

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 75.0f; }
    @Override public int cooldownTicks()      { return 900; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.position();
        double cloudY = center.y + 15;

        // ── VIHARFELHŐ MEGJELENÉS ──────────────────────────────────────
        for (int i = 0; i < 120; i++) {
            double ang = RNG.nextDouble() * Math.PI * 2;
            double r   = RNG.nextDouble() * 12;
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.15f, 0.1f, 0.35f), 3.0f),
                    center.x + Math.cos(ang)*r, cloudY + RNG.nextDouble()*2,
                    center.z + Math.sin(ang)*r,
                    1, 0.2,0.1,0.2, 0.02);
            level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                    center.x + Math.cos(ang)*r*0.7, cloudY + 0.5,
                    center.z + Math.sin(ang)*r*0.7, 1, 0.15,0.05,0.15, 0.04);
        }
        level.playSound(null, center.x, cloudY, center.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 0.3f);
        // VIHAR 30 másodpercre — az ég valóban elsötétül
        SpellEnvironmentFX.startStorm(level, 610);
        // Ködfal körben — az ellenségek elveszítik a látótávolságukat
        SpellEnvironmentFX.castFogZone(level, caster, 40.0, 610);
        // Mélybúgású basszushang induláskor
        SpellEnvironmentFX.playDeepBassImpact(level, center.x, cloudY, center.z);
        ModNetworking.sendEffect(caster, "shake",            0.8f, 20f);
        ModNetworking.sendEffect(caster, "fov_punch",       -18f, 15f);
        ModNetworking.sendEffect(caster, "arcane_overdrive", 1.0f, 25f);
        ModNetworking.sendEffect(caster, "lightning_flash",  1.0f, 6f);

        // ── 30 MÁSODPERC × 20 TICK = 600 TICK, minden 20 tickben 12 csapás ─
        for (int wave = 0; wave < DURATION; wave++) {
            final int w = wave;
            DelayedEffectScheduler.schedule(wave * 20, () -> {

                // Felhő pulzálás
                for (int i = 0; i < 30; i++) {
                    double ang = RNG.nextDouble() * Math.PI * 2;
                    double r   = RNG.nextDouble() * 10;
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(0.3f, 0.15f, 0.65f), 2.0f),
                            center.x + Math.cos(ang)*r, cloudY,
                            center.z + Math.sin(ang)*r,
                            1, 0.1,0.05,0.1, 0.01);
                }

                // 12 villám a felhőből
                AABB searchBox = new AABB(
                        center.x-STORM_RANGE, center.y-5, center.z-STORM_RANGE,
                        center.x+STORM_RANGE, center.y+20, center.z+STORM_RANGE);

                java.util.List<LivingEntity> targets = level.getEntitiesOfClass(
                        LivingEntity.class, searchBox, e -> e instanceof Mob && e.isAlive());
                targets.sort((a, b) -> RNG.nextInt(3) - 1); // random sorrend

                for (int b = 0; b < BOLTS_PER_WAVE; b++) {
                    final int fb = b;
                    // Kis eltolás a csapások között — látványosabb
                    DelayedEffectScheduler.schedule(b, () -> {
                        double tx, ty, tz;
                        LivingEntity target = null;

                        if (fb < targets.size()) {
                            target = targets.get(fb);
                            tx = target.getX(); ty = target.getY(); tz = target.getZ();
                        } else {
                            // Ha nincs elég mob, random pozíció a területen
                            double ang = RNG.nextDouble() * Math.PI * 2;
                            double r   = RNG.nextDouble() * STORM_RANGE;
                            tx = center.x + Math.cos(ang)*r;
                            ty = center.y;
                            tz = center.z + Math.sin(ang)*r;
                        }

                        // Villám FELHŐTŐL a CÉLPONTIG
                        double dx = tx - center.x, dz = tz - center.z;
                        int segments = 14;
                        for (int seg = 0; seg <= segments; seg++) {
                            double t = seg / (double) segments;
                            double px = center.x + dx*t + (seg>0&&seg<segments ? (RNG.nextDouble()-0.5)*1.8 : 0);
                            double py = cloudY - t*(cloudY-ty);
                            double pz = center.z + dz*t + (seg>0&&seg<segments ? (RNG.nextDouble()-0.5)*1.8 : 0);

                            // Lila-kék villám
                            level.sendParticles(new DustParticleOptions(
                                    new Vector3f(0.5f, 0.3f + (float)t*0.4f, 1.0f), 1.4f),
                                    px, py, pz, 1, 0.04,0.04,0.04, 0.0);
                            level.sendParticles(ModParticles.STORM_BOLT.get(),
                                    px, py, pz, 1, 0.03,0.03,0.03, 0.0);
                            level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                                    px, py, pz, 1, 0.06,0.04,0.06, 0.02);
                        }

                        // Impact
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tx,ty+0.5,tz, 15, 0.4,0.4,0.4, 0.1);
                        level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.6f, 0.4f, 1.0f), 2.5f),
                                tx, ty+0.5, tz, 20, 0.5,0.5,0.5, 0.08);

                        if (target != null) {
                            target.hurt(target.damageSources().lightningBolt(), BOLT_DMG);
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                        }

                        level.playSound(null, tx, ty, tz,
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.0f + (float)RNG.nextDouble()*0.4f);

                        // Minden harmadik boltnál flash
                        if (fb % 3 == 0)
                            ModNetworking.sendEffect(caster, "lightning_flash", 0.7f, 3f);
                    });
                }

                // Hullámonként növekvő shake
                if (w % 5 == 0)
                    ModNetworking.sendEffect(caster, "shake", 0.4f + w*0.01f, 8f);
            });
        }

        // Vihar vége
        DelayedEffectScheduler.schedule(602, () -> {
            level.playSound(null, center.x, cloudY, center.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 0.6f);
            ModNetworking.sendEffect(caster, "shake", 0.6f, 10f);
        });
    }
}
