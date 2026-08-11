package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.capability.ModAttachments;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector3f;

/**
 * Angel's Help — full cleanse, invulnerability, and dramatic heavenly restoration.
 * Wing geometry built from 48 HOLY_STAR particles per tick forming actual wing curves,
 * ground light-rays ascend beneath the caster, golden bloom filter activates.
 */
public class AngelsHelpSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "angels_help");

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 50.0f; }
    @Override public int cooldownTicks()      { return 600; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();

        // ── Cleanse every negative effect ───────────────────────────────
        caster.getActiveEffects().stream()
                .filter(e -> !e.getEffect().value().isBeneficial())
                .map(e -> e.getEffect())
                .toList()
                .forEach(caster::removeEffect);

        // ── Invulnerability + full restoration ──────────────────────────
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 65, 4, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION,      65, 4, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,       200, 3, false, false));
        caster.setHealth(caster.getMaxHealth());

        var mana = caster.getData(ModAttachments.MANA);
        mana.restore(mana.getMaxMana());
        ModNetworking.syncMana(caster);

        // ── Cinematic blast ──────────────────────────────────────────────
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.6f, 0.9f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.8f);

        // Instant golden screen bloom + FOV punch upward
        ModNetworking.sendEffect(caster, "fov_punch",  -20f, 12f);
        ModNetworking.sendEffect(caster, "shake",        0.4f,  8f);
        ModNetworking.sendEffect(caster, "holy_bloom",   1.0f, 65f);

        // Cast-moment burst: HOLY_STAR explosion from feet upward
        for (int i = 0; i < 60; i++) {
            double ang = Math.random() * Math.PI * 2;
            double r   = Math.random() * 2.5;
            level.sendParticles(ModParticles.HOLY_STAR.get(),
                    caster.getX() + Math.cos(ang)*r,
                    caster.getY() + Math.random()*2.5,
                    caster.getZ() + Math.sin(ang)*r,
                    1, (Math.random()-0.5)*0.1, 0.08, (Math.random()-0.5)*0.1, 0.0);
        }
        level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                caster.getX(), caster.getY(), caster.getZ(),
                20, 0.2, 0.5, 0.2, 0.06);
        level.sendParticles(new DustParticleOptions(new Vector3f(1.0f,0.95f,0.6f),3.5f),
                caster.getX(), caster.getY()+1, caster.getZ(),
                30, 1.5, 1.5, 1.5, 0.04);

        // ── 3-second wing + healing aura loop (60 ticks, every 3 ticks) ─
        for (int tick = 3; tick <= 60; tick += 3) {
            int t = tick;
            DelayedEffectScheduler.schedule(t, () -> {
                double cx = caster.getX();
                double cy = caster.getY();
                double cz = caster.getZ();
                double ht = caster.getBbHeight();
                double wingBase = cy + ht * 0.75;
                double fade = 1.0 - t / 65.0;

                // ── Left wing: 24-point bezier-like arc ─────────────────
                for (int f = 0; f < 24; f++) {
                    double progress = f / 23.0;
                    // Sweep from shoulder to wingtip (arc outward + upward then curving down)
                    double outward  = progress * 3.5;
                    double vertical = Math.sin(progress * Math.PI) * 2.2;
                    double twist    = Math.cos(progress * Math.PI * 0.5) * 0.4;
                    level.sendParticles(ModParticles.HOLY_STAR.get(),
                            cx - outward, wingBase + vertical, cz + twist,
                            1, 0,0,0, 0.0);
                    // Feather detail: small gold dust offset
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f,0.9f,0.5f), 0.8f),
                            cx - outward + (Math.random()-0.5)*0.3,
                            wingBase + vertical + (Math.random()-0.5)*0.2,
                            cz + twist + (Math.random()-0.5)*0.2,
                            1, 0,0,0, 0.0);
                }

                // ── Right wing: mirror ────────────────────────────────────
                for (int f = 0; f < 24; f++) {
                    double progress = f / 23.0;
                    double outward  = progress * 3.5;
                    double vertical = Math.sin(progress * Math.PI) * 2.2;
                    double twist    = Math.cos(progress * Math.PI * 0.5) * 0.4;
                    level.sendParticles(ModParticles.HOLY_STAR.get(),
                            cx + outward, wingBase + vertical, cz + twist,
                            1, 0,0,0, 0.0);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f,0.9f,0.5f), 0.8f),
                            cx + outward + (Math.random()-0.5)*0.3,
                            wingBase + vertical + (Math.random()-0.5)*0.2,
                            cz + twist + (Math.random()-0.5)*0.2,
                            1, 0,0,0, 0.0);
                }

                // ── Ground light rays rising ─────────────────────────────
                for (int r = 0; r < 8; r++) {
                    double ang = r * Math.PI * 2 / 8.0 + t * 0.06;
                    level.sendParticles(ParticleTypes.END_ROD,
                            cx + Math.cos(ang)*1.5, cy, cz + Math.sin(ang)*1.5,
                            1, 0, 0.12, 0, 0.0);
                }

                // ── Floating flower petals around caster ─────────────────
                level.sendParticles(ModParticles.HOLY_STAR.get(),
                        cx, cy+1, cz,
                        3, 1.8, 0.3, 1.8, 0.04);

                // ── Heaven beam column above caster ─────────────────────
                level.sendParticles(ModParticles.HEAVEN_BEAM.get(),
                        cx, cy+ht+1, cz,
                        4, 0.3, 0.8, 0.3, 0.06);
            });
        }

        // ── Final fade-out bell chime ────────────────────────────────────
        DelayedEffectScheduler.schedule(62, () ->
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.5f));
    }
}
