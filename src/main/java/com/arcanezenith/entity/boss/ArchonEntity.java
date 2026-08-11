package com.arcanezenith.entity.boss;

import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.entity.ArcaneZealotEntity;
import com.arcanezenith.entity.ModEntities;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.arcanezenith.spell.SpellEnvironmentFX;

import java.util.List;

/**
 * Archon of the Shattered Sky — the 3-phase ultimate boss.
 *
 * PHASE 1 (100–66% HP): Hovers with a Lightning Shield. Fires 180° sweeping beam rays
 * (simulated as a AABB sweep), summons Arcane Zealot waves every 30s.
 * The Lightning Shield reflects damage back if not broken with magic (anti-melee).
 *
 * PHASE 2 (65–33% HP): Phase transition — dramatic gravity inversion effect (FOV pull,
 * gravity_lens shader), arena floor shatters (TerrainDestruction), summons floating
 * platform clusters. Rains meteor particles at random positions every 3s.
 *
 * PHASE 3 (32–0% HP): Every 45s triggers a 15s Time Silence window (same damage-buffer
 * system as TimeSilenceSpell — reuses that infrastructure). Between silences: rapid-fire
 * multi-bolt attacks, and the boss becomes increasingly aggressive.
 *
 * Boss bar: standard ServerBossEvent, displayed with a purple bar.
 */
public class ArchonEntity extends Monster {

    public enum Phase { ONE, TWO, THREE }

    private Phase currentPhase = Phase.ONE;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Archon of the Shattered Sky"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    // Phase 1 timers
    private int beamCooldown = 100;
    private int zealotSummonCooldown = 600; // 30s

    // Phase 2 timers
    private int meteorCooldown = 60;
    private boolean phase2Transitioned = false;

    // Phase 3 timers
    private int timeSilenceCooldown = 900; // 45s
    private int rapidBoltCooldown = 40;
    private boolean timeSilenceActive = false;

    // Shield
    private int shieldHits = 5; // hits to break per phase

    public ArchonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 30.0f));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);

        // Csak az első látásnál triggereljük az intrót
        if (this.distanceTo(player) < 30 && !introTriggered) {
            introTriggered = true;
            triggerBossIntro(player);
        }
    }

    private boolean introTriggered = false;

    private void triggerBossIntro(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level();
        Vec3 bossPos = this.position();

        // 1. Azonnali: ég elsötétül, vihar kezdődik
        level.setWeatherParameters(0, 1200, true, true);

        // 2. Azonnali kamera-effektek — minden közeli játékosnál
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceTo(this) < 60) {
                ModNetworking.sendEffect(nearby, "shake",      1.2f, 20f);
                ModNetworking.sendEffect(nearby, "fov_punch", -20f,  15f);
                ModNetworking.sendEffect(nearby, "void_rift",  0.8f, 40f);
            }
        }

        // 3. Archon VFX megjelenés burst
        for (int i = 0; i < 80; i++) {
            double ang = Math.random() * Math.PI * 2;
            double r   = Math.random() * 5.0;
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.4f, 0.1f, 0.8f), 3.0f),
                    bossPos.x + Math.cos(ang)*r, bossPos.y + Math.random()*4,
                    bossPos.z + Math.sin(ang)*r,
                    1, 0, 0.1, 0, 0.0);
        }
        level.sendParticles(ModParticles.VOID_CORE.get(),
                bossPos.x, bossPos.y+2, bossPos.z, 30, 1.5, 1.5, 1.5, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                bossPos.x, bossPos.y+2, bossPos.z, 5, 0, 0, 0, 0);

        // 4. Hang: mély dörej + ender dragon
        level.playSound(null, bossPos.x, bossPos.y, bossPos.z,
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.5f);
        level.playSound(null, bossPos.x, bossPos.y, bossPos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 0.3f);

        // 5. Boss "szól" — chat üzenet drámai beúszással
        DelayedEffectScheduler.schedule(20, () -> {
            for (ServerPlayer nearby : level.players()) {
                if (nearby.distanceTo(this) < 80) {
                    nearby.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§5§l[Archon] §4§o\"A csillagok tanúi lesznek bukásodnak.\""));
                }
            }
        });

        // 6. Második üzenet + boss bar animáció
        DelayedEffectScheduler.schedule(50, () -> {
            for (ServerPlayer nearby : level.players()) {
                if (nearby.distanceTo(this) < 80) {
                    nearby.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§5§l[Archon] §4§o\"Az ég szétreped. A végzet elérkezett.\""));
                    ModNetworking.sendEffect(nearby, "shake", 0.8f, 12f);
                    ModNetworking.sendEffect(nearby, "dark_fantasy", 1.0f, 0);
                }
            }
            // Nagy villám a boss felett
            var lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(bossPos.x, bossPos.y + 10, bossPos.z);
                lightning.setDamage(0);
                level.addFreshEntity(lightning);
            }
        });

        // 7. Final dramatic pause — 3 másodperccel később minden elcsendesedik
        DelayedEffectScheduler.schedule(80, () -> {
            for (ServerPlayer nearby : level.players()) {
                if (nearby.distanceTo(this) < 80) {
                    ModNetworking.sendEffect(nearby, "fov_punch", 10f, 8f);
                }
            }
        });
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (level().isClientSide()) return;
        ServerLevel level = (ServerLevel) this.level();

        // Hovering motion
        LivingEntity target = this.getTarget();
        if (target != null && this.getY() < target.getY() + 4) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04, 0));
        }

        // Ambient particles — constant rain of arcane sparks from body
        if (tickCount % 3 == 0) {
            level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                    this.getX() + (random.nextDouble() - 0.5) * 2,
                    this.getY() + random.nextDouble() * 3,
                    this.getZ() + (random.nextDouble() - 0.5) * 2,
                    2, 0.1, 0.1, 0.1, 0.05);
        }

        checkPhaseTransition(level);

        switch (currentPhase) {
            case ONE  -> tickPhaseOne(level);
            case TWO  -> tickPhaseTwo(level);
            case THREE -> tickPhaseThree(level);
        }
    }

    private void checkPhaseTransition(ServerLevel level) {
        float hpPct = this.getHealth() / this.getMaxHealth();
        Phase newPhase = hpPct > 0.65f ? Phase.ONE : (hpPct > 0.32f ? Phase.TWO : Phase.THREE);

        if (newPhase != currentPhase) {
            onPhaseChange(level, currentPhase, newPhase);
            currentPhase = newPhase;
        }
    }

    private void onPhaseChange(ServerLevel level, Phase from, Phase to) {
        Vec3 pos = this.position();

        // Dramatic transition: big particle burst + screen shake to all nearby players
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 2, pos.z, 5, 0, 0, 0, 0);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.7f);

        for (ServerPlayer sp : level.players()) {
            if (sp.distanceTo(this) < 80) {
                ModNetworking.sendEffect(sp, "shake", 1.8f, 25f);
                ModNetworking.sendEffect(sp, "fov_punch", 25f, 20f);
            }
        }

        if (to == Phase.TWO) {
            transitionToPhaseTwo(level, pos);
        } else if (to == Phase.THREE) {
            transitionToPhaseThree(level, pos);
        }

        // Heal 5% on phase change (brief invulnerability window)
        this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + this.getMaxHealth() * 0.05f));
        this.invulnerableTime = 60;
        shieldHits = 5;
    }

    // ── PHASE 1 ───────────────────────────────────────────────────────────────

    private void tickPhaseOne(ServerLevel level) {
        LivingEntity target = this.getTarget();

        // Lightning shield visual — orbiting thunder sparks
        if (tickCount % 4 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2.0 + tickCount * 0.08;
                level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                        this.getX() + Math.cos(a) * 2.2,
                        this.getY() + 1.5,
                        this.getZ() + Math.sin(a) * 2.2,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // 180-degree sweep beam
        if (beamCooldown > 0) beamCooldown--;
        if (beamCooldown == 0 && target != null) {
            sweepBeam(level, target);
            beamCooldown = 200; // 10s
        }

        // Zealot summons
        if (zealotSummonCooldown > 0) zealotSummonCooldown--;
        if (zealotSummonCooldown == 0) {
            summonZealots(level, 3);
            zealotSummonCooldown = 600;
        }
    }

    private void sweepBeam(ServerLevel level, LivingEntity target) {
        Vec3 center = this.getEyePosition();
        Vec3 toTarget = target.position().subtract(center).normalize();

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 0.8f);

        // 180° arc of beam hits
        for (int step = 0; step < 19; step++) {
            int s = step;
            DelayedEffectScheduler.schedule(step * 2, () -> {
                double angle = Math.toRadians(-90 + s * 10);
                Vec3 beamDir = rotateYaw(toTarget, angle).normalize();
                Vec3 beamEnd = center.add(beamDir.scale(25));

                // Particle trail
                for (int i = 1; i <= 15; i++) {
                    Vec3 p = center.add(beamDir.scale(i));
                    level.sendParticles(ModParticles.THUNDER_SPARK.get(), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);
                }

                // Hit entities along beam
                AABB beamBox = new AABB(center, beamEnd).inflate(0.8);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, beamBox,
                        en -> en != this && en.isAlive())) {
                    e.hurt(e.damageSources().lightningBolt(), 14.0f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                }
                // Screen flash per beam step to visible players
                for (ServerPlayer sp : level.players()) {
                    if (sp.distanceTo(this) < 60) {
                        ModNetworking.sendEffect(sp, "lightning_flash", 0.8f, 3f);
                    }
                }
            });
        }
    }

    private Vec3 rotateYaw(Vec3 v, double radians) {
        double cos = Math.cos(radians), sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    private void summonZealots(ServerLevel level, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 6 + random.nextDouble() * 4;
            double sx = this.getX() + Math.cos(angle) * dist;
            double sz = this.getZ() + Math.sin(angle) * dist;

            ArcaneZealotEntity zealot = ModEntities.ARCANE_ZEALOT.get().create(level);
            if (zealot == null) continue;
            zealot.moveTo(sx, this.getY(), sz, random.nextFloat() * 360, 0);
            level.addFreshEntity(zealot);

            level.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.2f, 1.0f), 1.5f),
                    sx, this.getY() + 1, sz, 20, 0.3, 0.3, 0.3, 0.05);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    // ── PHASE 2 ───────────────────────────────────────────────────────────────

    private void transitionToPhaseTwo(ServerLevel level, Vec3 pos) {
        // Gravity inversion effect for all players in 60-block range
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceTo(this) < 60) {
                ModNetworking.sendEffect(sp, "gravity", 0.2f, 80f);
                ModNetworking.sendEffect(sp, "fov_punch", -25f, 30f);
            }
        }
        // Shatter arena floor around Archon
        com.arcanezenith.combat.TerrainDestruction.carveCrater(level, pos.x, pos.y - 2, pos.z, 12.0);
        com.arcanezenith.combat.TerrainDestruction.playCrumbleSound(level, pos.x, pos.y, pos.z);

        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 20, 3.0, 0.5, 3.0, 0.2);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 3.0f, 0.3f);
    }

    private void tickPhaseTwo(ServerLevel level) {
        // Meteor rain every 3s
        if (meteorCooldown > 0) meteorCooldown--;
        if (meteorCooldown == 0) {
            spawnMeteors(level, 4);
            meteorCooldown = 60;
        }

        // Intensified lightning shield
        if (tickCount % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0 + tickCount * 0.12;
                level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                        this.getX() + Math.cos(a) * 3.0,
                        this.getY() + 1.5 + Math.sin(tickCount * 0.1) * 0.5,
                        this.getZ() + Math.sin(a) * 3.0,
                        1, 0.05, 0.05, 0.05, 0.03);
            }
        }
    }

    private void spawnMeteors(ServerLevel level, int count) {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        for (int i = 0; i < count; i++) {
            double rx = target.getX() + (random.nextDouble() - 0.5) * 20;
            double rz = target.getZ() + (random.nextDouble() - 0.5) * 20;
            double highY = target.getY() + 20;

            // Descending trail
            int steps = 20;
            for (int s = 0; s < steps; s++) {
                int fs = s;
                DelayedEffectScheduler.schedule(s, () -> {
                    double curY = highY - fs * 1.0;
                    level.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.1f), 1.5f),
                            rx, curY, rz, 2, 0.15, 0.1, 0.15, 0.02);
                });
            }
            // Impact
            DelayedEffectScheduler.schedule(steps, () -> {
                double impY = target.getY();
                level.sendParticles(ParticleTypes.EXPLOSION, rx, impY, rz, 2, 0.3, 0.1, 0.3, 0.1);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(rx - 3, impY - 2, rz - 3, rx + 3, impY + 4, rz + 3),
                        en -> en != this && en.isAlive())) {
                    e.hurt(e.damageSources().magic(), 20.0f);
                }
                for (ServerPlayer sp : level.players()) {
                    if (sp.distanceTo(this) < 60) ModNetworking.sendEffect(sp, "shake", 0.5f, 8f);
                }
            });
        }
    }

    // ── PHASE 3 ───────────────────────────────────────────────────────────────

    private void transitionToPhaseThree(ServerLevel level, Vec3 pos) {
        // World-wide desaturation + massive shake
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceTo(this) < 80) {
                ModNetworking.sendEffect(sp, "time_stop", 1.0f, 30f);
                ModNetworking.sendEffect(sp, "shake", 2.0f, 30f);
            }
        }
        level.sendParticles(ModParticles.SINGULARITY_NOVA.get(), pos.x, pos.y + 2, pos.z, 20, 1.0, 1.0, 1.0, 0.06);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.5f, 0.5f);
    }

    private void tickPhaseThree(ServerLevel level) {
        // Rapid multi-bolt attack
        if (rapidBoltCooldown > 0) rapidBoltCooldown--;
        if (rapidBoltCooldown == 0 && this.getTarget() != null) {
            fireRapidBolts(level);
            rapidBoltCooldown = 40;
        }

        // Time silence every 45s
        if (timeSilenceCooldown > 0) timeSilenceCooldown--;
        if (timeSilenceCooldown == 0 && !timeSilenceActive) {
            triggerTimeSilenceWindow(level);
            timeSilenceCooldown = 900;
        }

        // Increasingly aggressive ambient — swirling void+lightning mix
        if (tickCount % 2 == 0) {
            double a = tickCount * 0.15;
            for (int arm = 0; arm < 2; arm++) {
                double armAngle = a + arm * Math.PI;
                level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                        this.getX() + Math.cos(armAngle) * 2.5,
                        this.getY() + 1.5 + Math.sin(tickCount * 0.08),
                        this.getZ() + Math.sin(armAngle) * 2.5,
                        1, 0.04, 0.04, 0.04, 0.02);
                level.sendParticles(ModParticles.VOID_CORE.get(),
                        this.getX() + Math.cos(armAngle + 0.5) * 1.5,
                        this.getY() + 1.0,
                        this.getZ() + Math.sin(armAngle + 0.5) * 1.5,
                        1, 0.03, 0.03, 0.03, 0.0);
            }
        }
    }

    private void fireRapidBolts(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        // 5 bolts with 4-tick spread
        for (int b = 0; b < 5; b++) {
            int fb = b;
            DelayedEffectScheduler.schedule(b * 4, () -> {
                if (!this.isAlive() || !target.isAlive()) return;
                Vec3 start = this.getEyePosition();
                Vec3 dir = target.getEyePosition().subtract(start).normalize();
                // Slight random spread
                dir = dir.add(
                        (random.nextDouble() - 0.5) * 0.15,
                        (random.nextDouble() - 0.5) * 0.10,
                        (random.nextDouble() - 0.5) * 0.15).normalize();

                for (int s = 1; s <= 15; s++) {
                    Vec3 p = start.add(dir.scale(s));
                    level.sendParticles(ModParticles.ARCANE_SPARK.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
                }
                // Hitscan check
                AABB hitBox = target.getBoundingBox().inflate(0.3);
                Vec3 end = start.add(dir.scale(20));
                if (hitBox.clip(start, end).isPresent()) {
                    target.hurt(this.damageSources().indirectMagic(this, this), 10.0f);
                }
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.6f, 1.5f + fb * 0.08f);
            });
        }
    }

    private void triggerTimeSilenceWindow(ServerLevel level) {
        timeSilenceActive = true;
        // Reuse the global TimeSilenceSpell infrastructure
        // Approximate: freeze all players and nearby mobs for 15s via NoAI + Resistance
        Vec3 pos = this.position();
        AABB range = new AABB(pos.x - 60, pos.y - 20, pos.z - 60, pos.x + 60, pos.y + 20, pos.z + 60);
        List<LivingEntity> frozen = level.getEntitiesOfClass(LivingEntity.class, range,
                e -> e != this && e.isAlive() && !(e instanceof ArchonEntity));

        for (LivingEntity e : frozen) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 255, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 255, false, false));
            if (e instanceof net.minecraft.world.entity.Mob m) m.setNoAi(true);
        }

        // Time-stop shader for all nearby players
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceTo(this) < 80) {
                ModNetworking.sendEffect(sp, "time_stop", 1.0f, 310f);
                ModNetworking.sendEffect(sp, "shake", 0.8f, 20f);
            }
        }
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0f, 0.3f);

        // Resume after 15s — unfreeze + massive detonation
        DelayedEffectScheduler.schedule(300, () -> {
            timeSilenceActive = false;
            for (LivingEntity e : frozen) {
                if (!e.isAlive()) continue;
                if (e instanceof net.minecraft.world.entity.Mob m) m.setNoAi(false);
                e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                e.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                // Detonation burst on each entity
                e.hurt(e.damageSources().magic(), 25.0f);
                level.sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.1f), 1.5f),
                        e.getX(), e.getY() + 1, e.getZ(), 15, 0.5, 0.5, 0.5, 0.08);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 2, pos.z, 8, 0, 0, 0, 0);
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0f, 0.4f);
            for (ServerPlayer sp : level.players()) {
                if (sp.distanceTo(this) < 80) {
                    ModNetworking.sendEffect(sp, "shake", 2.0f, 20f);
                    ModNetworking.sendEffect(sp, "fov_punch", 35f, 15f);
                }
            }
        });
    }

    // ── Damage handling: lightning shield ───────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Phase 1 lightning shield: reflect non-magic damage
        if (currentPhase == Phase.ONE && shieldHits > 0) {
            boolean isMagic = source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR);
            if (!isMagic && source.getDirectEntity() instanceof LivingEntity attacker) {
                attacker.hurt(this.damageSources().indirectMagic(this, this), amount * 0.6f);
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                // Sparks from shield
                ServerLevel level = (ServerLevel) this.level();
                level.sendParticles(ModParticles.THUNDER_SPARK.get(),
                        this.getX(), this.getY() + 1.5, this.getZ(), 15, 0.5, 0.5, 0.5, 0.12);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.8f, 1.2f);
                // Shield hit counter — eventually breaks
                shieldHits--;
                return false; // nullify damage while shield holds
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_AMBIENT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
}
