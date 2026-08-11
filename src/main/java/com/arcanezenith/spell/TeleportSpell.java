package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class TeleportSpell implements Spell {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "teleport");
    private static final float DECOY_DAMAGE = 30.0f;   // was 6 → 30

    @Override public ResourceLocation id() { return ID; }
    @Override public float manaCost() { return 15.0f; }
    @Override public int cooldownTicks() { return 100; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 origin = caster.position();
        Vec3 look   = caster.getLookAngle();

        // Find destination (40 blocks forward, stop at solid block)
        Vec3 dest = origin;
        for (int i = 1; i <= 40; i++) {
            Vec3 test = origin.add(look.scale(i));
            net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(test);
            if (!level.getBlockState(bp).isAir() || !level.getBlockState(bp.above()).isAir()) break;
            dest = test;
        }
        Vec3 finalDest = dest;

        // Origin dissolve — dense shadow wisps
        for (int i = 0; i < 40; i++) {
            double ox = origin.x + (Math.random()-0.5)*0.8;
            double oy = origin.y + Math.random()*2;
            double oz = origin.z + (Math.random()-0.5)*0.8;
            level.sendParticles(ModParticles.SHADOW_WISP.get(), ox,oy,oz, 1, 0.1,0.1,0.1, 0.03);
        }
        level.sendParticles(new DustParticleOptions(new Vector3f(0.3f,0.0f,0.6f),2.0f),
                origin.x, origin.y+1, origin.z, 30, 0.3,0.5,0.3, 0.05);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.6f);

        // FOV snap — sharp inward punch (100→70 feel)
        ModNetworking.sendEffect(caster, "fov_punch", -30f, 8f);

        // Teleport
        caster.teleportTo(finalDest.x, finalDest.y, finalDest.z);

        // Arrival — vacuum pop ring
        level.sendParticles(ModParticles.SHADOW_WISP.get(),
                finalDest.x, finalDest.y+1, finalDest.z, 25, 0.5,0.5,0.5, 0.08);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.6f,0.2f,1.0f),1.8f),
                finalDest.x, finalDest.y+1, finalDest.z, 20, 0.4,0.4,0.4, 0.04);
        level.playSound(null, finalDest.x, finalDest.y, finalDest.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.6f);
        ModNetworking.sendEffect(caster, "fov_punch", 12f, 6f);

        // Shadow decoy at origin — explodes after 2.5s drawing mob aggro
        Vec3 decoyPos = origin;
        DelayedEffectScheduler.schedule(50, () -> {
            // Decoy explosion
            level.sendParticles(ModParticles.SHADOW_WISP.get(),
                    decoyPos.x, decoyPos.y+1, decoyPos.z, 60, 1.0,1.0,1.0, 0.12);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.5f,0.0f,0.9f),3.0f),
                    decoyPos.x, decoyPos.y+1, decoyPos.z, 40, 0.8,0.8,0.8, 0.06);
            level.playSound(null, decoyPos.x, decoyPos.y, decoyPos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);

            AABB aoe = new AABB(decoyPos.x-4, decoyPos.y-2, decoyPos.z-4,
                                 decoyPos.x+4, decoyPos.y+4, decoyPos.z+4);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aoe,
                    en -> en != caster && en.isAlive())) {
                e.hurt(e.damageSources().magic(), DECOY_DAMAGE);
                Vec3 push = e.position().subtract(decoyPos).normalize().scale(1.2);
                e.setDeltaMovement(push.x, 0.6, push.z);
            }
        });
    }
}
