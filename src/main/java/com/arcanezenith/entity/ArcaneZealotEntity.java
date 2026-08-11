package com.arcanezenith.entity;

import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.DelayedEffectScheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.List;
import com.arcanezenith.network.ModNetworking;

/**
 * Arcane Zealot — rogue battle-mage with three signature abilities:
 *
 * 1. BLINK ESCAPE: if a hostile entity gets within 5 blocks, teleports 10–14 blocks away
 *    instantly (like an Enderman escape, but range-triggered and much more aggressive).
 *
 * 2. ELEMENTAL BARRIER: every 8s, erects a 4s barrier of swirling arcane particles around
 *    itself that reflects 40% of melee damage back at the attacker as magic damage.
 *
 * 3. CROWD CONTROL BOLT: every 5s, fires a slow-debuff bolt at its target that applies
 *    Slowness IV for 3s. Approximated as a AABB sphere cast rather than a true projectile
 *    entity (this keeps the code self-contained — a real projectile renderer is a separate task).
 */
public class ArcaneZealotEntity extends Monster {

    private static final int BLINK_RANGE = 5;
    private static final int BLINK_DISTANCE = 12;
    private static final int BARRIER_COOLDOWN = 160; // 8s
    private static final int CC_BOLT_COOLDOWN = 100; // 5s

    private int barrierCooldown = 0;
    private int ccBoltCooldown = 40; // stagger initial cast
    private boolean barrierActive = false;
    private int barrierTimer = 0;

    public ArcaneZealotEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        // Priority 1: flee melee range via blink — custom goal handles this in tick()
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) level();

        // ── Barrier cooldown ────────────────────────────────────────────
        if (barrierCooldown > 0) barrierCooldown--;
        if (barrierActive) {
            barrierTimer--;
            spawnBarrierParticles(serverLevel);
            if (barrierTimer <= 0) {
                barrierActive = false;
            }
        }

        // ── CC bolt cooldown ────────────────────────────────────────────
        if (ccBoltCooldown > 0) ccBoltCooldown--;

        LivingEntity target = this.getTarget();

        // ── Blink escape if target or any hostile is too close ───────────
        AABB meleeZone = this.getBoundingBox().inflate(BLINK_RANGE);
        boolean threatened = !level().getEntitiesOfClass(LivingEntity.class, meleeZone,
                e -> e != this && e.isAlive() && e instanceof Player).isEmpty();

        if (threatened && random.nextInt(10) == 0) {
            performBlink(serverLevel);
        }

        // ── Erect barrier if injured and off cooldown ────────────────────
        if (!barrierActive && barrierCooldown == 0 && this.getHealth() < this.getMaxHealth() * 0.8) {
            activateBarrier(serverLevel);
        }

        // ── CC bolt at target ────────────────────────────────────────────
        if (target != null && ccBoltCooldown == 0) {
            fireCCBolt(serverLevel, target);
            ccBoltCooldown = CC_BOLT_COOLDOWN;
        }
    }

    private void performBlink(ServerLevel level) {
        Vec3 pos = this.position();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = BLINK_DISTANCE - 2 + random.nextDouble() * 4;
        double nx = pos.x + Math.cos(angle) * dist;
        double nz = pos.z + Math.sin(angle) * dist;
        double ny = pos.y;

        // Smoke at origin
        level.sendParticles(ModParticles.SHADOW_WISP.get(), pos.x, pos.y + 1, pos.z, 20, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.4f);

        this.teleportTo(nx, ny, nz);

        // Arrival puff
        level.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.1f, 0.9f), 1.5f),
                nx, ny + 1, nz, 15, 0.3, 0.3, 0.3, 0.04);
        level.playSound(null, nx, ny, nz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.4f, 1.8f);
    }

    private void activateBarrier(ServerLevel level) {
        barrierActive = true;
        barrierTimer = 80; // 4s
        barrierCooldown = BARRIER_COOLDOWN;
        // Visual: flash of arcane rings
        level.sendParticles(ModParticles.RUNE_RING.get(), this.getX(), this.getY() + 1, this.getZ(), 5, 0.1, 0.1, 0.1, 0.0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.8f, 1.6f);
    }

    private void spawnBarrierParticles(ServerLevel level) {
        // 6 orbiting purple-blue dots
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0 + tickCount * 0.12;
            level.sendParticles(new DustParticleOptions(new Vector3f(0.4f, 0.2f, 0.9f), 0.8f),
                    this.getX() + Math.cos(a) * 1.2, this.getY() + 1.0, this.getZ() + Math.sin(a) * 1.2,
                    1, 0, 0, 0, 0.0);
        }
    }

    private void fireCCBolt(ServerLevel level, LivingEntity target) {
        // Approximate: instant hit on target if within 16 blocks (no flight time — placeholder for real projectile)
        double dist = this.distanceTo(target);
        if (dist > 16) return;

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true));
        target.hurt(this.damageSources().indirectMagic(this, this), 4.0f);

        // Bolt trail particles
        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = start.lerp(end, t);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0.2f, 1.0f), 0.7f),
                    p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9f, 0.8f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Barrier: reflect 40% of melee damage
        if (barrierActive && source.getDirectEntity() instanceof LivingEntity attacker) {
            float reflected = amount * 0.4f;
            attacker.hurt(this.damageSources().indirectMagic(this, this), reflected);
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.7f, 1.5f);
        }
        // Mana drain on hit from player
        if (source.getEntity() instanceof ServerPlayer player) {
            var mana = player.getData(ModAttachments.MANA);
            mana.spend(Math.min(mana.getMana(), 8.0f));
            com.arcanezenith.network.ModNetworking.syncMana(player);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.EVOKER_AMBIENT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.EVOKER_HURT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.EVOKER_DEATH; }
}
