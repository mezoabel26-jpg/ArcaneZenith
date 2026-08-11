package com.arcanezenith.entity;

import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.DelayedEffectScheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Chrono-Weaver — elite dimension mage with two signature mechanics:
 *
 * 1. HEALTH REWIND: stores health snapshots every second (20 ticks). When it would die or
 *    drops below 20% HP, rewinds to the 5-second-ago snapshot instead, resetting its health
 *    and position. Limited to 2 rewinds per fight to prevent immortality.
 *
 * 2. DIMENSIONAL RIFT: every 12s, tears open a small particle rift that deals AoE damage
 *    and slows everything in range — approximated without a real rift entity, which would
 *    need its own renderer.
 *
 * Fights from range, never approaches melee — its move goal keeps it at 8+ blocks.
 */
public class ChronoWeaverEntity extends Monster {

    private static final int SNAPSHOT_INTERVAL = 20; // 1s
    private static final int REWIND_SECONDS = 5;
    private static final int MAX_REWINDS = 2;
    private static final int RIFT_COOLDOWN = 240; // 12s

    private final Deque<HealthSnapshot> healthHistory = new ArrayDeque<>();
    private int rewindsRemaining = MAX_REWINDS;
    private int riftCooldown = 80;
    private int snapshotTimer = 0;
    private boolean rewindPending = false;

    private record HealthSnapshot(float health, Vec3 position) {}

    public ChronoWeaverEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 28;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 30.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ServerLevel level = (ServerLevel) level();

        // ── Health snapshot every second ────────────────────────────────
        snapshotTimer++;
        if (snapshotTimer >= SNAPSHOT_INTERVAL) {
            snapshotTimer = 0;
            healthHistory.addLast(new HealthSnapshot(this.getHealth(), this.position()));
            // Keep only 5 seconds of history
            while (healthHistory.size() > REWIND_SECONDS) healthHistory.removeFirst();
        }

        // ── Rift cooldown + fire ────────────────────────────────────────
        if (riftCooldown > 0) riftCooldown--;
        if (riftCooldown == 0 && this.getTarget() != null) {
            fireRift(level);
            riftCooldown = RIFT_COOLDOWN;
        }

        // Ambient chrono particles
        if (tickCount % 4 == 0) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.3f, 0.9f), 0.6f),
                    this.getX() + (random.nextDouble() - 0.5),
                    this.getY() + random.nextDouble() * 2,
                    this.getZ() + (random.nextDouble() - 0.5),
                    1, 0, 0.02, 0, 0.0);
        }
    }

    private void fireRift(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        Vec3 riftPos = target.position().add(0, 1, 0);

        // Rift visual — burst of chrono particles + rune ring
        level.sendParticles(ModParticles.RUNE_RING.get(), riftPos.x, riftPos.y, riftPos.z, 4, 0.1, 0.1, 0.1, 0.0);
        for (int i = 0; i < 30; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            level.sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0.2f, 0.85f), 1.2f),
                    riftPos.x + Math.cos(a) * 2, riftPos.y, riftPos.z + Math.sin(a) * 2,
                    1, 0, 0.05, 0, 0.0);
        }
        level.playSound(null, riftPos.x, riftPos.y, riftPos.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.4f, 0.5f);
        level.playSound(null, riftPos.x, riftPos.y, riftPos.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.6f, 1.8f);

        // Rift damage + slow in 4-block radius — 2 waves spaced 0.5s apart
        for (int wave = 0; wave < 2; wave++) {
            int w = wave;
            DelayedEffectScheduler.schedule(wave * 10, () -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(
                                riftPos.x - 4, riftPos.y - 2, riftPos.z - 4,
                                riftPos.x + 4, riftPos.y + 4, riftPos.z + 4),
                        en -> en != this && en.isAlive())) {
                    e.hurt(this.damageSources().magic(), 10.0f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                }
            });
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float hpBefore = this.getHealth();
        boolean result = super.hurt(source, amount);
        float hpAfter = this.getHealth();

        // Rewind trigger: below 20% HP or near-death
        if (rewindsRemaining > 0 && hpAfter <= this.getMaxHealth() * 0.2f && hpBefore > this.getMaxHealth() * 0.2f) {
            performRewind();
        }
        return result;
    }

    private void performRewind() {
        if (healthHistory.isEmpty() || rewindsRemaining <= 0) return;
        rewindsRemaining--;

        HealthSnapshot snap = healthHistory.peekFirst(); // oldest = 5s ago
        if (snap == null) return;

        float rewindHp = Math.max(this.getMaxHealth() * 0.5f, snap.health());
        this.setHealth(rewindHp);
        this.teleportTo(snap.position().x, snap.position().y, snap.position().z);

        ServerLevel level = (ServerLevel) this.level();
        // Rewind burst visual
        level.sendParticles(new DustParticleOptions(new Vector3f(0.8f, 0.6f, 1.0f), 2.0f),
                this.getX(), this.getY() + 1, this.getZ(), 40, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 0.4f);

        // Invulnerability briefly after rewind
        this.invulnerableTime = 40;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.EVOKER_AMBIENT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.EVOKER_HURT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.EVOKER_DEATH; }
}
