package com.arcanezenith.spell;

import com.arcanezenith.event.DelayedEffectScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;

/**
 * SpellEnvironmentFX — Központi rendszer a varázslatok környezeti hatásaihoz.
 *
 * Tartalmaz:
 *   - égbolt sötétítés (időjárás manipuláció: thunderstorm bekapcsolása)
 *   - lebegő töltés animáció (caster levitation + energia összegyűlés)
 *   - vihar / köd / villám effektek
 *   - fény-kibocsátás a varázslatmag körül
 *   - mély basszus / dörgés / üveg hang layerek
 *   - automatikus visszaállítás a varázslat után
 */
public final class SpellEnvironmentFX {

    private static final Random RNG = new Random();

    private SpellEnvironmentFX() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  1. ÉG SÖTÉTÍTÉS + VIHAR
    //     Az égbolt elsötétül (setRaining + setThundering), majd a varázslat
    //     után durationTicks tickkel visszaáll az eredeti állapotra.
    // ─────────────────────────────────────────────────────────────────────────
    public static void startStorm(ServerLevel level, int durationTicks) {
        boolean wasRaining   = level.isRaining();
        boolean wasThundering= level.isThundering();

        // Azonnali vihar bekapcsolás
        level.setWeatherParameters(0, durationTicks + 20, true, true);

        // Visszaállítás a varázslat után
        DelayedEffectScheduler.schedule(durationTicks, () -> {
            if (!wasRaining && !wasThundering) {
                level.setWeatherParameters(6000, 0, false, false);
            }
        });
    }

    /**
     * Vihar közeleg — villám csap le a caster körül random pozíciókba.
     * durationTicks időn át, intervalTicks-enként.
     */
    public static void strikeAroundCaster(ServerLevel level, ServerPlayer caster,
                                           double radius, int intervalTicks,
                                           int durationTicks, float dmg) {
        int waves = durationTicks / intervalTicks;
        for (int w = 0; w < waves; w++) {
            final int fw = w;
            DelayedEffectScheduler.schedule(fw * intervalTicks, () -> {
                double ang = RNG.nextDouble() * Math.PI * 2;
                double r   = RNG.nextDouble() * radius;
                double x   = caster.getX() + Math.cos(ang) * r;
                double z   = caster.getZ() + Math.sin(ang) * r;
                double y   = caster.getY();

                // Tényleges villám entitás
                var lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT
                        .create(level);
                if (lightning != null) {
                    lightning.moveTo(x, y, z);
                    lightning.setDamage(dmg);
                    level.addFreshEntity(lightning);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. LEBEGŐ TÖLTÉS ANIMÁCIÓ
    //     A caster levitál (Levitation effekt), köré gyűlik az energia
     //    (spirál particle + mélyzúgás), majd az effekt végén visszaesik.
    // ─────────────────────────────────────────────────────────────────────────
    public static void chargeFloatAnimation(ServerLevel level, ServerPlayer caster,
                                              int chargeTicks,
                                              net.minecraft.core.particles.SimpleParticleType particle) {
        // Levitation — felemelkedés a töltés alatt
        caster.addEffect(new MobEffectInstance(
                MobEffects.LEVITATION, chargeTicks + 5, 1, false, false));

        // Energia összegyűlés hang — mély basszus zúgás
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.8f, 0.25f);

        // Spirális energia-összegyűlés particlek
        for (int tick = 0; tick < chargeTicks; tick++) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                double progress = t / (double) chargeTicks;
                // Külső spirál → befelé szorul ahogy a töltés halad
                double radius  = 4.0 * (1.0 - progress * 0.7);
                double speed   = 0.15 + progress * 0.25;
                double angOff  = t * speed;
                Vec3 pos       = caster.getEyePosition();

                for (int arm = 0; arm < 3; arm++) {
                    double a = angOff + arm * Math.PI * 2 / 3.0;
                    double px = pos.x + Math.cos(a) * radius;
                    double py = pos.y + Math.sin(t * 0.12) * 0.8;
                    double pz = pos.z + Math.sin(a) * radius;
                    // Befelé mozgó particle
                    Vec3 dir = pos.subtract(px, py, pz).normalize().scale(0.08 + progress * 0.12);
                    level.sendParticles(particle, px, py, pz,
                            1, dir.x, dir.y, dir.z, 0.0);
                }

                // Belső mag fény-burst növekvő intenzitással
                if (t % 4 == 0) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pos.x, pos.y, pos.z,
                            (int)(3 + progress * 8), 0.3, 0.3, 0.3, 0.05 + progress * 0.1);
                }

                // Hang intenzitás növekedése
                if (t % 8 == 0) {
                    level.playSound(null, pos.x, pos.y, pos.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                            0.4f + (float) progress * 1.2f,
                            0.5f + (float) progress * 0.8f);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. KÖDFAL — sűrű köd / darkness effekt a caster körül
    //     Mobs és játékosok Darkness/Blindness debuffot kapnak
    //     a varázslat hatókörén belül.
    // ─────────────────────────────────────────────────────────────────────────
    public static void castFogZone(ServerLevel level, ServerPlayer caster,
                                    double radius, int durationTicks) {
        net.minecraft.world.phys.AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                en -> en != caster && en.isAlive())) {
            // Darkness effekt — látótávolság csökken
            e.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS, durationTicks, 0, false, true));
            // Slowness is — a köd lassít
            e.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 1, false, false));
        }

        // Sűrű, sötét particle köd
        for (int tick = 0; tick < durationTicks; tick += 5) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                Vec3 cp = caster.position();
                for (int i = 0; i < 12; i++) {
                    double ang = RNG.nextDouble() * Math.PI * 2;
                    double r   = RNG.nextDouble() * radius;
                    double y   = cp.y + RNG.nextDouble() * 3;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            cp.x + Math.cos(ang) * r, y, cp.z + Math.sin(ang) * r,
                            1, 0.05, 0.08, 0.05, 0.01);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4. FÉNY-KIBOCSÁTÁS
    //     Ideiglenes fényforrás létrehozása a varázslatmagnál.
    //     Minecraft-ban ezt light block elhelyezésével + eltávolításával tesszük.
    // ─────────────────────────────────────────────────────────────────────────
    public static void spawnLight(ServerLevel level, Vec3 pos, int durationTicks) {
        BlockPos bp = BlockPos.containing(pos);

        // Csak légüres helyen helyezünk el fényelemet
        if (!level.getBlockState(bp).isAir()) return;

        level.setBlock(bp,
                Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15),
                3);

        // Eltávolítás a varázslat után
        DelayedEffectScheduler.schedule(durationTicks, () -> {
            if (level.getBlockState(bp).getBlock() == Blocks.LIGHT) {
                level.removeBlock(bp, false);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  5. MÉLY BASSZUS HANG RÉTEGEK
    //     Több hang egymásra rétegezve mélységérzetet ad.
    // ─────────────────────────────────────────────────────────────────────────
    public static void playDeepBassImpact(ServerLevel level, double x, double y, double z) {
        // Réteg 1: mély basszus "THUD"
        level.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.5f, 0.2f);
        // Réteg 2: dörgés
        level.playSound(null, x, y, z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5f, 0.3f);
        // Réteg 3: üvegcsörömpölés
        DelayedEffectScheduler.schedule(3, () ->
            level.playSound(null, x, y, z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0f, 0.4f));
        // Réteg 4: fém-zúgás utózúgás
        DelayedEffectScheduler.schedule(8, () ->
            level.playSound(null, x, y, z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.35f));
    }

    public static void playMysticCharge(ServerLevel level, double x, double y, double z) {
        // Mágikus töltés — emelkedő zúgás
        level.playSound(null, x, y, z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.3f);
        DelayedEffectScheduler.schedule(5, () ->
            level.playSound(null, x, y, z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.5f));
        DelayedEffectScheduler.schedule(12, () ->
            level.playSound(null, x, y, z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.8f, 1.2f));
    }

    public static void playDarkCrash(ServerLevel level, double x, double y, double z) {
        // Sötét robbanás — nyomás érzése
        level.playSound(null, x, y, z,
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 0.4f);
        DelayedEffectScheduler.schedule(2, () ->
            level.playSound(null, x, y, z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.25f));
        DelayedEffectScheduler.schedule(6, () ->
            level.playSound(null, x, y, z,
                    SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.6f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  6. BLOKK-ROBBANÁS + repülő törmelék
    //     A becsapódás helyén törnek szét a blokkok és repülnek a levegőbe.
    //     Minecraft limitáció: igazi blokk-repülés nincs, de TNT + particle
    //     combo hiteles hatást ad.
    // ─────────────────────────────────────────────────────────────────────────
    public static void blockExplosionFX(ServerLevel level, Vec3 impact, double radius) {
        // TerrainDestruction kráter
        com.arcanezenith.combat.TerrainDestruction.carveCrater(
                level, impact.x, impact.y, impact.z, radius);

        // Repülő kődarab VFX — FALLING DUST particlek különböző irányokba
        for (int i = 0; i < 40; i++) {
            double ang   = RNG.nextDouble() * Math.PI * 2;
            double incl  = RNG.nextDouble() * Math.PI / 2;
            double spd   = 0.3 + RNG.nextDouble() * 0.5;
            double vx    = Math.cos(ang) * Math.cos(incl) * spd;
            double vy    = Math.sin(incl) * spd;
            double vz    = Math.sin(ang) * Math.cos(incl) * spd;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    impact.x, impact.y + 1, impact.z, 1, vx, vy, vz, 0.0);
        }

        // Kőpor-felhő
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                impact.x, impact.y + 1, impact.z, 20, radius*0.4, 0.5, radius*0.4, 0.04);
    }
}
