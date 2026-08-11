package com.arcanezenith.event;

import com.arcanezenith.client.particle.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.joml.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import com.arcanezenith.event.DelayedEffectScheduler;

/**
 * EnemyReactionHandler — drámai ellenség-reakció animációk nagy sebzésnél.
 *
 * Három reakció szint:
 *  - 20–49 dmg  → "stagger": kis visszalökés + spark burst
 *  - 50–99 dmg  → "heavy hit": erős visszalökés + forgás + particle robbanás
 *  - 100+ dmg   → "devastate": feldobás + forgás a levegőben + becsapódás VFX
 *
 * Csak mágikus sebzésnél aktiválódik (magic damage source vagy indirectMagic).
 * Közeli ellenségek esetén a játékos is kap shake-et (a ModNetworking-en keresztül
 * nem tudjuk kliensoldalon, ezért server-side shake-et küldünk).
 */
public class EnemyReactionHandler {

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        float damage = event.getNewDamage();

        // Csak server-oldalon, csak moboknál, csak mágikus forrásból
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob)) return;
        if (damage < 20f) return;

        DamageSource source = event.getSource();
        boolean isMagic = source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                || source.getMsgId().contains("magic")
                || source.getMsgId().contains("arcane")
                || source.getMsgId().contains("indirect");
        if (!isMagic) return;

        ServerLevel level = (ServerLevel) entity.level();
        Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2.0, 0);

        if (damage >= 100f) {
            devastate(level, entity, pos, damage);
        } else if (damage >= 50f) {
            heavyHit(level, entity, pos, damage);
        } else {
            stagger(level, entity, pos, damage);
        }
    }

    // ── STAGGER (20–49 dmg) ─────────────────────────────────────────────────
    private void stagger(ServerLevel level, LivingEntity entity, Vec3 pos, float dmg) {
        // Kis visszalökés a sebzés irányával ellentétesen
        Vec3 kb = entity.getDeltaMovement();
        entity.setDeltaMovement(kb.x * 1.5, 0.3, kb.z * 1.5);

        // Kis spark burst
        level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(new DustParticleOptions(
                new Vector3f(0.8f, 0.3f, 1.0f), 1.2f),
                pos.x, pos.y, pos.z, 8, 0.25, 0.25, 0.25, 0.04);
    }

    // ── HEAVY HIT (50–99 dmg) ───────────────────────────────────────────────
    private void heavyHit(ServerLevel level, LivingEntity entity, Vec3 pos, float dmg) {
        // Erős visszalökés + Y-impulzus
        Vec3 kb = entity.getDeltaMovement();
        double kbMult = 0.5 + dmg / 100.0;
        entity.setDeltaMovement(
                kb.x * 2.5 * kbMult,
                0.6 + dmg / 200.0,
                kb.z * 2.5 * kbMult);
        entity.hurtMarked = true;

        // Közepes particle robbanás
        level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                pos.x, pos.y, pos.z, 30, 0.6, 0.6, 0.6, 0.14);
        level.sendParticles(new DustParticleOptions(
                new Vector3f(0.7f, 0.2f, 1.0f), 2.0f),
                pos.x, pos.y, pos.z, 20, 0.5, 0.5, 0.5, 0.06);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                pos.x, pos.y, pos.z, 15, 0.4, 0.4, 0.4, 0.08);

        // Hang
        level.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // ── DEVASTATE (100+ dmg) ─────────────────────────────────────────────────
    private void devastate(ServerLevel level, LivingEntity entity, Vec3 pos, float dmg) {
        // Feldobás + erős oldalra lökés
        Vec3 kb = entity.getDeltaMovement();
        double strength = Math.min(3.5, 1.0 + dmg / 120.0);
        entity.setDeltaMovement(
                kb.x * 3.0 * strength,
                1.8 + dmg / 180.0,
                kb.z * 3.0 * strength);
        entity.hurtMarked = true;

        // Forgás effekt — ismételt velocity nullázás + újra-alkalmazás simul
        // (valódi forgáshoz custom entity renderer kellene, ezt közelítjük)
        for (int t = 0; t < 3; t++) {
            final int ft = t;
            DelayedEffectScheduler.schedule(t * 4, () -> {
                if (!entity.isAlive()) return;
                // Folyamatosan csúszó velocity — "forgás" hatást kelt
                Vec3 v = entity.getDeltaMovement();
                entity.setDeltaMovement(v.x * 0.95, v.y, v.z * 0.95);
            });
        }

        // Nagy particle robbanás — azonnali
        level.sendParticles(ModParticles.ARCANE_SPARK.get(),
                pos.x, pos.y, pos.z, 60, 1.0, 1.0, 1.0, 0.18);
        level.sendParticles(new DustParticleOptions(
                new Vector3f(0.6f, 0.15f, 1.0f), 2.8f),
                pos.x, pos.y, pos.z, 40, 0.8, 0.8, 0.8, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                pos.x, pos.y, pos.z, 4, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                pos.x, pos.y, pos.z, 20, 0.6, 0.6, 0.6, 0.12);

        // Hangok: THUD + üveg
        level.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.5f);

        // Becsapódás VFX 0.8 másodperccel később (mikor földet ér)
        DelayedEffectScheduler.schedule(16, () -> {
            if (!entity.isAlive()) return;
            Vec3 land = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            level.sendParticles(ModParticles.SINGULARITY_NOVA.get(),
                    land.x, land.y, land.z, 4, 0.2, 0.1, 0.2, 0.03);
            level.sendParticles(new DustParticleOptions(
                    new Vector3f(0.5f, 0.1f, 0.9f), 2.0f),
                    land.x, land.y, land.z, 20, 0.5, 0.2, 0.5, 0.05);
            level.playSound(null, land.x, land.y, land.z,
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
        });
    }
}
