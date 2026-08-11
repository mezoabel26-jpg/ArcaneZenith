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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.arcanezenith.spell.SpellEnvironmentFX;

public class ThunderWarSpell implements Spell {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "thunder_war");
    private static final Random RNG = new Random();

    @Override public ResourceLocation id() { return ID; }
    @Override public float manaCost() { return 45.0f; }
    @Override public int cooldownTicks() { return 400; }

    private static final float DAMAGE_PER_STRIKE = 22.0f;   // was 5 → 22
    private static final int CHAIN_TARGETS = 10;

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.position();

        // Storm cloud formation — dense thunder particles high above
        level.playSound(null, center.x, center.y+12, center.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.35f);
        // VIHAR bekapcsolás — az ég elsötétül
        SpellEnvironmentFX.startStorm(level, 100);
        // Villámok csapnak le a caster körül a vihar alatt
        SpellEnvironmentFX.strikeAroundCaster(level, caster, 25.0, 20, 80, 3.0f);
        // Mély basszus hang rétegek
        SpellEnvironmentFX.playMysticCharge(level, center.x, center.y, center.z);
        ModNetworking.sendEffect(caster, "fov_punch",        -15f, 20f);
        ModNetworking.sendEffect(caster, "shake",              0.5f, 12f);
        ModNetworking.sendEffect(caster, "lightning_flash",    1.0f, 6f);

        // Storm cloud particles at Y+12
        for (int i = 0; i < 80; i++) {
            double ang = RNG.nextDouble()*Math.PI*2;
            double r   = RNG.nextDouble()*12;
            level.sendParticles(new DustParticleOptions(new Vector3f(0.25f,0.25f,0.3f), 2.5f),
                    center.x+Math.cos(ang)*r, center.y+12, center.z+Math.sin(ang)*r,
                    1, 0.5, 0.2, 0.5, 0.0);
            level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                    center.x+Math.cos(ang)*r*0.7, center.y+11+RNG.nextDouble()*2,
                    center.z+Math.sin(ang)*r*0.7, 1, 0.3, 0.1, 0.3, 0.05);
        }

        // Chain lightning every 4 ticks for 4 seconds (20 strikes total)
        for (int tick = 4; tick <= 80; tick += 4) {
            int t = tick;
            DelayedEffectScheduler.schedule(t, () -> {
                AABB searchBox = new AABB(center.x-25, center.y-5, center.z-25,
                                          center.x+25, center.y+20, center.z+25);
                List<LivingEntity> mobs = new ArrayList<>(level.getEntitiesOfClass(
                        LivingEntity.class, searchBox, e -> e instanceof Mob && e.isAlive()));
                if (mobs.isEmpty()) return;

                // Strike up to CHAIN_TARGETS
                mobs.sort((a, b) -> Double.compare(
                        center.distanceToSqr(a.position()), center.distanceToSqr(b.position())));
                int hits = Math.min(CHAIN_TARGETS, mobs.size());
                for (int i = 0; i < hits; i++) {
                    LivingEntity mob = mobs.get(i);
                    mob.hurt(mob.damageSources().lightningBolt(), DAMAGE_PER_STRIKE);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false));

                    // Lightning bolt column from cloud to target
                    double mx = mob.getX(), my = mob.getY(), mz = mob.getZ();
                    int segments = 12;
                    for (int s = 0; s <= segments; s++) {
                        double ty = my + (center.y+12 - my) * (s/(double)segments);
                        double jitter = (s>0&&s<segments) ? (RNG.nextDouble()-0.5)*1.5 : 0;
                        level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                                mx+jitter, ty, mz+jitter, 2, 0.15, 0.1, 0.15, 0.02);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                mx+jitter, ty, mz+jitter, 1, 0.1, 0.05, 0.1, 0.0);
                    }
                    // Impact flash
                    level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                            mx, my+1, mz, 20, 0.5, 0.5, 0.5, 0.15);
                    level.playSound(null, mx, my, mz,
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.2f);
                }
                // Screen flash white on first strike of each wave
                if (hits > 0) {
                    ModNetworking.sendEffect(caster, "fov_punch",        8f, 3f);
                    ModNetworking.sendEffect(caster, "lightning_flash",  1.0f, 4f);
                }
            });
        }
        // Post-storm lingering rumble
        DelayedEffectScheduler.schedule(85, () ->
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.5f));
    }
}
