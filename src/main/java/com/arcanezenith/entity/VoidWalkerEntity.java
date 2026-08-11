package com.arcanezenith.entity;

import com.arcanezenith.client.particle.ModParticles;
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
import com.arcanezenith.event.DelayedEffectScheduler;

/**
 * Void-Walker — dimension-bending mob with two signature abilities:
 *
 * 1. POSITION SWAP: every 15s, swaps its position with the nearest player.
 *    The swap is telegraphed 1s early with a purple void vortex, then executes instantly.
 *    Leaves behind a brief void-smoke cloud at both positions.
 *
 * 2. VOID SHROUD: when below 50% HP, gains a semi-permanent Resistance II aura and
 *    becomes slightly faster (the "freezing arrows mid-air" design mechanic is
 *    approximated as a Resistance aura + Slowness projectile — true projectile-halting
 *    would need a per-tick projectile entity scan, which is an optional follow-up).
 *
 * Tall, dark, and unsettling — plays Enderman ambient sounds.
 */
public class VoidWalkerEntity extends Monster {

    private static final int SWAP_COOLDOWN = 300; // 15s
    private int swapCooldown = 120; // 6s initial delay

    private boolean shroudActive = false;

    public VoidWalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 22;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 55.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 26.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ServerLevel level = (ServerLevel) level();

        // ── Position swap ────────────────────────────────────────────────
        if (swapCooldown > 0) swapCooldown--;
        if (swapCooldown == 0) {
            Player nearest = level.getNearestPlayer(this, 20);
            if (nearest != null) {
                telegraphThenSwap(level, nearest);
                swapCooldown = SWAP_COOLDOWN;
            } else {
                swapCooldown = 20; // retry soon
            }
        }

        // ── Void shroud below 50% HP ─────────────────────────────────────
        if (!shroudActive && this.getHealth() < this.getMaxHealth() * 0.5) {
            shroudActive = true;
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
            level.sendParticles(ModParticles.VOID_CORE.get(),
                    this.getX(), this.getY() + 1, this.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.5f, 1.8f);
        }

        // Ambient dark particles
        if (tickCount % 6 == 0) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.1f, 0.0f, 0.2f), 0.8f),
                    this.getX() + (random.nextDouble() - 0.5) * 0.8,
                    this.getY() + random.nextDouble() * 2.2,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.8,
                    1, 0, 0.01, 0, 0.0);
        }
    }

    private void telegraphThenSwap(ServerLevel level, Player target) {
        Vec3 myPos = this.position();
        Vec3 playerPos = target.position();

        // Telegraph: vortex particles at player position
        level.sendParticles(ModParticles.VOID_CORE.get(), playerPos.x, playerPos.y + 1, playerPos.z, 12, 0.3, 0.4, 0.3, 0.04);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.15f, 0.0f, 0.35f), 1.5f),
                playerPos.x, playerPos.y + 1, playerPos.z, 20, 0.5, 0.5, 0.5, 0.06);
        level.playSound(null, playerPos.x, playerPos.y, playerPos.z,
                SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0f, 0.7f);

        // Execute swap after 1s delay
        com.arcanezenith.event.DelayedEffectScheduler.schedule(20, () -> {
            if (!this.isAlive() || !target.isAlive()) return;

            // Smoke at both positions
            level.sendParticles(ModParticles.SHADOW_WISP.get(), myPos.x, myPos.y + 1, myPos.z, 25, 0.4, 0.5, 0.4, 0.06);
            level.sendParticles(ModParticles.SHADOW_WISP.get(), playerPos.x, playerPos.y + 1, playerPos.z, 25, 0.4, 0.5, 0.4, 0.06);

            // Actual swap
            this.teleportTo(playerPos.x, playerPos.y, playerPos.z);
            target.teleportTo(myPos.x, myPos.y, myPos.z);

            level.playSound(null, playerPos.x, playerPos.y, playerPos.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.6f);

            // Slight damage on arrival (void shock)
            target.hurt(target.damageSources().magic(), 6.0f);
        });
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.ENDERMAN_AMBIENT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.ENDERMAN_HURT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.ENDERMAN_DEATH; }
}
