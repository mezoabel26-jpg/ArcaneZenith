package com.arcanezenith.entity;

import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.client.particle.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import com.arcanezenith.network.ModNetworking;

/**
 * Mana-Leech Drake — aerial mana predator with two mechanics:
 *
 * 1. MANA DRAIN AURA: every 2s, drains 12 mana from every player within 6 blocks.
 *    The drained mana is stored internally as a "mana charge" (max 80).
 *
 * 2. PLASMA BREATH: when mana charge >= 40, fires a sweeping plasma ray at its target
 *    that deals 6 damage per tick over 1.5s (30 ticks) and lights the target on fire.
 *    Uses the stored mana charge (costs 40). Visual: hot-pink particle stream.
 *
 * Flies using the Phantom-style flying movement (FlyingMob base is private in 1.21,
 * so we extend Monster and manually set y-velocity). In practice this means the Drake
 * "floats" near the ceiling/open air rather than being a true graceful flier.
 */
public class ManaLeechDrakeEntity extends Monster {

    private static final float MAX_MANA_CHARGE = 80f;
    private static final int DRAIN_INTERVAL = 40; // 2s
    private static final int BREATH_COOLDOWN = 120; // 6s after breath ends

    private float manaCharge = 0f;
    private int drainTimer = 0;
    private int breathTimer = 0;
    private int breathCooldown = 0;
    private boolean breathing = false;

    public ManaLeechDrakeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 38.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ServerLevel level = (ServerLevel) level();

        // Flying hover — push up if below y+1 of target
        LivingEntity target = this.getTarget();
        if (target != null && this.getY() < target.getY() + 1.5) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.06, 0));
        }

        // ── Mana drain aura ──────────────────────────────────────────────
        drainTimer++;
        if (drainTimer >= DRAIN_INTERVAL) {
            drainTimer = 0;
            drainManaFromNearby(level);
        }

        // ── Plasma breath ─────────────────────────────────────────────────
        if (breathCooldown > 0) breathCooldown--;

        if (!breathing && manaCharge >= 40 && target != null && breathCooldown == 0) {
            breathing = true;
            breathTimer = 30; // 1.5s
            manaCharge -= 40;
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2f, 0.5f);
        }

        if (breathing) {
            breathTimer--;
            if (target != null) fireBreathTick(level, target);
            if (breathTimer <= 0) {
                breathing = false;
                breathCooldown = BREATH_COOLDOWN;
            }
        }

        // Ambient leech glow particles
        if (tickCount % 8 == 0) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.8f, 0.1f, 0.6f), 0.7f),
                    this.getX() + (random.nextDouble() - 0.5),
                    this.getY() + 0.3,
                    this.getZ() + (random.nextDouble() - 0.5),
                    1, 0, 0.01, 0, 0.0);
        }
    }

    private void drainManaFromNearby(ServerLevel level) {
        AABB drainBox = this.getBoundingBox().inflate(6);
        for (Player player : level.getEntitiesOfClass(Player.class, drainBox, Player::isAlive)) {
            if (player instanceof ServerPlayer sp) {
                var mana = sp.getData(ModAttachments.MANA);
                float drained = Math.min(mana.getMana(), 12.0f);
                if (drained > 0) {
                    mana.spend(drained);
                    manaCharge = Math.min(MAX_MANA_CHARGE, manaCharge + drained);
                    com.arcanezenith.network.ModNetworking.syncMana(sp);
                    // Drain visual: pink particles flowing from player to drake
                    level.sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.2f, 0.7f), 0.8f),
                            player.getX(), player.getY() + 1, player.getZ(),
                            8, 0.3, 0.3, 0.3, 0.06);
                }
            }
        }
    }

    private void fireBreathTick(ServerLevel level, LivingEntity target) {
        Vec3 start = this.getEyePosition();
        Vec3 dir = target.getEyePosition().subtract(start).normalize();
        double range = 12.0;

        // Particle stream
        for (int i = 1; i <= 12; i++) {
            Vec3 p = start.add(dir.scale(i));
            level.sendParticles(ModParticles.PLASMA_SPIRAL.get(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.03);
            level.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.6f), 1.0f),
                    p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.0);
        }

        // Damage target if in range
        if (this.distanceTo(target) <= range) {
            target.hurt(this.damageSources().indirectMagic(this, this), 6.0f);
            target.setRemainingFireTicks(40);
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.PHANTOM_AMBIENT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.PHANTOM_HURT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.PHANTOM_DEATH; }
}
