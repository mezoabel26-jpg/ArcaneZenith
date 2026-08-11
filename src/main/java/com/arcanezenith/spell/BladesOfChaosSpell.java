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

/**
 * BLADES OF CHAOS — God of War Kratos láncos pengéi.
 *
 * 2 láncos tűzpenge lendül ki a caster kezéből, körbepörög és visszatér.
 * 6 különálló csapás, minden csapásnál izzó narancssárga tűzcsíkok + szikraeső.
 * Az egész sorozat 3 másodperc, 55 dmg/csapás + égés, összesített: 330 dmg.
 */
public class BladesOfChaosSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "blades_of_chaos");

    private static final float  SLASH_DMG  = 55.0f;
    private static final int    SLASH_COUNT = 6;
    private static final double CHAIN_LEN  = 12.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 45.0f; }
    @Override public int cooldownTicks()      { return 240; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 origin = caster.position().add(0, 1.2, 0);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0f, 0.5f);
        ModNetworking.sendEffect(caster, "heat_haze", 0.7f, 65f);
        ModNetworking.sendEffect(caster, "stellar_fire", 0.8f, 65f);
        ModNetworking.sendEffect(caster, "fov_punch", -15f, 10f);

        // 6 csapás, 10 tickenként
        for (int slash = 0; slash < SLASH_COUNT; slash++) {
            final int s = slash;
            DelayedEffectScheduler.schedule(slash * 10, () -> {

                // Penge szöge — alternáló, növekvő ív
                double baseAng = caster.getYRot() * Math.PI / 180.0;
                double swingAng = baseAng + (s % 2 == 0 ? -0.8 : 0.8) + s * 0.25;

                // ── Tűzcsík a penge útján ────────────────────────────
                for (int step = 0; step <= 16; step++) {
                    double t  = step / 16.0;
                    double r  = t * CHAIN_LEN;
                    double arc= swingAng + t * (s % 2 == 0 ? 1.2 : -1.2);
                    Vec3 p = origin.add(Math.cos(arc)*r, Math.sin(t*Math.PI)*0.8, Math.sin(arc)*r);

                    // Narancssárga izzó csík
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.35f + (float)t*0.3f, 0.0f), 1.6f),
                            p.x, p.y, p.z, 2, 0.06,0.06,0.06, 0.02);
                    // Fehér mag
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.9f, 0.7f), 0.8f),
                            p.x, p.y, p.z, 1, 0.03,0.03,0.03, 0.0);
                    // Dedikált chaos blade particle
                    level.sendParticles(ModParticles.CHAOS_BLADE.get(), p.x, p.y, p.z, 1, 0,0,0, 0.0);
                    // Szikrák
                    if (step % 3 == 0)
                        level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0.1,0.1,0.1, 0.0);
                }

                // Lánc trail
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(0.6f, 0.3f, 0.1f), 1.0f),
                        origin.x, origin.y, origin.z,
                        10, 0.2,0.2,0.2, 0.05);

                // ── Hitscan az ív mentén ─────────────────────────────
                AABB sweepBox = origin.add(caster.getLookAngle().scale(CHAIN_LEN/2))
                        .subtract(CHAIN_LEN/2, CHAIN_LEN/2, CHAIN_LEN/2)
                        .toAabbWithOffset(CHAIN_LEN, CHAIN_LEN, CHAIN_LEN);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                        en -> en != caster && en.isAlive())) {
                    if (origin.distanceTo(e.position()) > CHAIN_LEN + 1) continue;
                    e.hurt(caster.damageSources().magic(), SLASH_DMG);
                    e.setRemainingFireTicks(80);

                    // Impact — szikraeső
                    Vec3 ep = e.position().add(0, e.getBbHeight()/2.0, 0);
                    level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                            ep.x, ep.y, ep.z, 20, 0.5,0.5,0.5, 0.15);
                    level.sendParticles(ParticleTypes.LAVA, ep.x, ep.y, ep.z, 15, 0.4,0.4,0.4, 0.0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.5f, 0.0f), 2.5f),
                            ep.x, ep.y, ep.z, 20, 0.6,0.6,0.6, 0.08);
                    level.playSound(null, ep.x, ep.y, ep.z,
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.6f + s*0.06f);
                    level.playSound(null, ep.x, ep.y, ep.z,
                            SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0f, 1.2f);
                }

                // Minden csapásnál kis shake
                ModNetworking.sendEffect(caster, "shake", 0.4f, 5f);
                level.playSound(null, origin.x, origin.y, origin.z,
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f + s*0.08f);
            });
        }

        // Záró nagy csapás
        DelayedEffectScheduler.schedule(62, () -> {
            ModNetworking.sendEffect(caster, "shake",     1.0f, 12f);
            ModNetworking.sendEffect(caster, "fov_punch", 20f,  10f);
            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0f, 0.4f);
        });
    }
}
