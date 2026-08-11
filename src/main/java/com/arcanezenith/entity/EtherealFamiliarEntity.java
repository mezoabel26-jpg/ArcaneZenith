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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;
import com.arcanezenith.network.ModNetworking;

/**
 * Ethereal Familiar — the player companion. Floats beside its owner, collecting nearby
 * item drops and periodically boosting their mana regeneration.
 *
 * Summoned by right-clicking the Arcane Wand (handled in WandItem). Persists until
 * killed or the owner logs out (despawns on server level unload — no persistent UUID
 * storage yet, that's a follow-up with NBT serialization if needed).
 *
 * Abilities:
 * 1. ITEM VACUUM: every 20 ticks, teleports any ItemEntity within 10 blocks to the owner.
 * 2. MANA BOOST: every 5s, grants a 5-mana bonus to the owner on top of normal regen.
 * 3. HOMING BOLT: every 8s, fires a short-range magic bolt at the nearest hostile within 12b.
 *
 * Extends TamableAnimal so it uses the owner UUID system natively. Doesn't breed/eat,
 * just a pure companion. Sitting/unsitting via sneak+right-click inherited from tamable.
 */
public class EtherealFamiliarEntity extends net.minecraft.world.entity.PathfinderMob {

    private static final int VACUUM_INTERVAL = 20;
    private static final int MANA_BOOST_INTERVAL = 100; // 5s
    private static final int BOLT_COOLDOWN = 160; // 8s

    private int vacuumTimer = 0;
    private int manaBoostTimer = 0;
    private int boltCooldown = 60;
    private float hoverOffset = 0f;

    public EtherealFamiliarEntity(EntityType<? extends net.minecraft.world.entity.PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.FOLLOW_RANGE, 14.0)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, false,
                e -> this.getOwner() != null && this.distanceTo(this.getOwner()) < 16));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        ServerLevel level = (ServerLevel) this.level();

        Player owner = getOwnerAsPlayer();

        // Hover bob
        hoverOffset += 0.08f;
        double bobY = Math.sin(hoverOffset) * 0.08;
        this.setDeltaMovement(this.getDeltaMovement().add(0, bobY * 0.1, 0));

        // Ambient cyan particles
        if (tickCount % 5 == 0) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.9f, 1.0f), 0.6f),
                    this.getX() + (random.nextDouble() - 0.5) * 0.5,
                    this.getY() + random.nextDouble() * 0.8,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.5,
                    1, 0, 0.01, 0, 0.0);
        }

        if (owner == null || !owner.isAlive()) return;

        // ── Item vacuum ──────────────────────────────────────────────────
        vacuumTimer++;
        if (vacuumTimer >= VACUUM_INTERVAL) {
            vacuumTimer = 0;
            collectNearbyItems(level, owner);
        }

        // ── Mana boost ───────────────────────────────────────────────────
        manaBoostTimer++;
        if (manaBoostTimer >= MANA_BOOST_INTERVAL) {
            manaBoostTimer = 0;
            if (owner instanceof ServerPlayer sp) {
                var mana = sp.getData(ModAttachments.MANA);
                mana.tickRegen(5.0f);
                com.arcanezenith.network.ModNetworking.syncMana(sp);
                level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                        this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.2, 0.2, 0.2, 0.04);
            }
        }

        // ── Homing bolt at nearest hostile ───────────────────────────────
        if (boltCooldown > 0) boltCooldown--;
        if (boltCooldown == 0) {
            LivingEntity target = level.getNearestEntity(Monster.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat(), this,
                    this.getX(), this.getY(), this.getZ(),
                    new AABB(this.getX() - 12, this.getY() - 6, this.getZ() - 12,
                             this.getX() + 12, this.getY() + 6, this.getZ() + 12));
            if (target != null && target.isAlive()) {
                fireHomingBolt(level, target);
                boltCooldown = BOLT_COOLDOWN;
            }
        }
    }

    private Player getOwnerAsPlayer() {
        net.minecraft.world.entity.Entity owner = this.getOwner();
        return owner instanceof Player p ? p : null;
    }

    private void collectNearbyItems(ServerLevel level, Player owner) {
        AABB vacuum = this.getBoundingBox().inflate(10);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, vacuum,
                ie -> ie.isAlive() && ie.getAge() > 40);
        for (ItemEntity ie : items) {
            ItemStack stack = ie.getItem();
            if (!owner.getInventory().add(stack)) continue;
            ie.discard();
            level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                    ie.getX(), ie.getY(), ie.getZ(), 6, 0.2, 0.2, 0.2, 0.06);
        }
    }

    private void fireHomingBolt(ServerLevel level, LivingEntity target) {
        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.lerp(end, i / (double) steps);
            level.sendParticles(ModParticles.ARCANE_SPARK.get(), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.03);
        }
        target.hurt(this.damageSources().magic(), 8.0f);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.4f);
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob other) {
        return null; // Familiars don't breed
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return SoundEvents.ALLAY_AMBIENT_WITH_ITEM; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource ds) { return SoundEvents.ALLAY_HURT; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.ALLAY_DEATH; }
    @Override
    public boolean canBeLeashed() { return true; }
}
