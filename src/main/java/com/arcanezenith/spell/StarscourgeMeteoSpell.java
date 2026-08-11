package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.combat.ModDamageTypes;
import com.arcanezenith.combat.TerrainDestruction;
import com.arcanezenith.event.DelayedEffectScheduler;
import com.arcanezenith.network.ModNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.arcanezenith.spell.SpellEnvironmentFX;

/**
 * STARSCOURGE METEOR — Elden Ring Radahn ihlette kozmikus meteor-csapás.
 *
 * A caster lángoló vörös kozmikus energiává alakul, kilövi magát az égbe (Y+60),
 * majd 2 másodperccel később vakító vörös meteorként csapódik vissza — 20 blokk
 * sugarú pusztítás, 280 true dmg, masszív kráter, shockwave gyűrű.
 *
 * VFX: felfelé tartó LAVA_GEYSER + PLASMA_SPIRAL csóva, orbita közben vörös
 * kozmikus aura, becsapódáskor teljes képernyő-villanás + 2.0 shake.
 */
public class StarscourgeMeteoSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "starscourge_meteor");

    private static final float IMPACT_DMG = 280.0f;
    private static final double IMPACT_RADIUS = 20.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 90.0f; }
    @Override public int cooldownTicks()      { return 700; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 launchPos   = caster.position();
        Vec3 impactPos   = launchPos; // csapódik vissza ahol volt

        // ── FELSZÁLLÁS — caster eltűnik vörös lánggal ────────────────
        level.sendParticles(ModParticles.METEOR_TRAIL.get(),
                launchPos.x, launchPos.y+1, launchPos.z, 25, 0.3,0.8,0.3, 0.10);
        level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                launchPos.x, launchPos.y, launchPos.z, 60, 0.4,0.6,0.4, 0.18);
        level.sendParticles(new DustParticleOptions(
                new Vector3f(1.0f, 0.15f, 0.0f), 3.0f),
                launchPos.x, launchPos.y+1, launchPos.z, 40, 0.5,0.8,0.5, 0.10);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                launchPos.x, launchPos.y+1, launchPos.z, 2, 0,0,0, 0);
        level.playSound(null, launchPos.x, launchPos.y, launchPos.z,
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.5f, 0.4f);
        ModNetworking.sendEffect(caster, "heat_haze", 1.0f, 50f);
        ModNetworking.sendEffect(caster, "stellar_fire", 1.0f, 50f);
        ModNetworking.sendEffect(caster, "fov_punch", -30f, 20f);

        // Caster láthatatlanná válik (teleport az égbe)
        caster.teleportTo(launchPos.x, launchPos.y + 60, launchPos.z);
        caster.setInvisible(true);
        // LEBEGÉS a kilövés előtt — az energia összegyűlik
        SpellEnvironmentFX.chargeFloatAnimation(level, caster, 15, ModParticles.METEOR_TRAIL.get());
        // Fény-kibocsátás a meteornál
        SpellEnvironmentFX.spawnLight(level, launchPos.add(0, 30, 0), 45);

        // ── METEORCSÓVA — leszálló tűzpálya 40 ticken ────────────────
        for (int tick = 0; tick < 40; tick++) {
            final int t = tick;
            DelayedEffectScheduler.schedule(tick, () -> {
                double progress = t / 40.0;
                // Y interpoláció: Y+60 → Y+0
                double curY = launchPos.y + 60 - progress * 60;
                double spread = (1.0 - progress) * 2.0;

                // Vörös kozmikus csóva
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.2f + (float)progress*0.3f, 0.0f), 2.5f),
                        launchPos.x, curY, launchPos.z,
                        8, spread*0.3, 0.2, spread*0.3, 0.04);
                level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                        launchPos.x, curY, launchPos.z,
                        4, spread*0.2, 0.1, spread*0.2, 0.06);
                // Utolsó 10 tickben fehér izzás
                if (t > 30) {
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.9f, 0.8f), 3.5f),
                            launchPos.x, curY, launchPos.z,
                            12, 0.3,0.1,0.3, 0.02);
                }
                // Növekvő hang
                if (t % 8 == 0)
                    level.playSound(null, launchPos.x, curY, launchPos.z,
                            SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                            0.5f + (float)progress*1.5f, 0.4f + (float)progress*0.6f);
            });
        }

        // ── BECSAPÓDÁS 2 másodperccel később ─────────────────────────
        DelayedEffectScheduler.schedule(42, () -> {
            // Caster visszatér
            caster.setInvisible(false);
            caster.teleportTo(impactPos.x, impactPos.y, impactPos.z);

            // ── HATÁS ─────────────────────────────────────────────────
            // 1. Vakító vörös-fehér becsapódás villanás
            for (int i = 0; i < 100; i++) {
                double ang = Math.random() * Math.PI * 2;
                double r   = Math.random() * IMPACT_RADIUS;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.3f + (float)Math.random()*0.4f, 0.0f), 3.0f),
                        impactPos.x + Math.cos(ang)*r, impactPos.y+0.5,
                        impactPos.z + Math.sin(ang)*r,
                        1, 0, 0.3, 0, 0.05);
            }
            // 2. Shockwave gyűrű — 80 pontos kör
            for (int i = 0; i < 80; i++) {
                double ang = i * Math.PI * 2 / 80.0;
                level.sendParticles(new DustParticleOptions(
                        new Vector3f(1.0f, 0.5f, 0.1f), 2.5f),
                        impactPos.x + Math.cos(ang)*IMPACT_RADIUS*0.5, impactPos.y+0.3,
                        impactPos.z + Math.sin(ang)*IMPACT_RADIUS*0.5,
                        1, Math.cos(ang)*0.15, 0.08, Math.sin(ang)*0.15, 0.02);
                level.sendParticles(ParticleTypes.EXPLOSION,
                        impactPos.x + Math.cos(ang)*IMPACT_RADIUS, impactPos.y+0.5,
                        impactPos.z + Math.sin(ang)*IMPACT_RADIUS,
                        1, 0, 0.1, 0, 0.0);
            }
            // 3. Debris — kődarabok repülnek
            level.sendParticles(ModParticles.LAVA_GEYSER.get(),
                    impactPos.x, impactPos.y+2, impactPos.z, 80, 3.0,1.0,3.0, 0.25);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    impactPos.x, impactPos.y+1, impactPos.z, 10, 0,0,0, 0);

            // 4. Hangok
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.2f);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0f, 0.3f);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5f, 0.5f);

            // 5. Kamera-effektek
            ModNetworking.sendEffect(caster, "shake",      2.0f, 28f);
            ModNetworking.sendEffect(caster, "fov_punch",  35f,  20f);
            ModNetworking.sendEffect(caster, "heat_haze",  0.8f, 40f);

            // 6. TRUE DMG 20 blokk sugarú körben
            AABB blast = new AABB(
                    impactPos.x-IMPACT_RADIUS, impactPos.y-3, impactPos.z-IMPACT_RADIUS,
                    impactPos.x+IMPACT_RADIUS, impactPos.y+15, impactPos.z+IMPACT_RADIUS);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, blast,
                    en -> en != caster && en.isAlive())) {
                e.hurt(ModDamageTypes.arcaneTrue(level, caster), IMPACT_DMG);
                // Felfelé lökés
                Vec3 knock = e.position().subtract(impactPos);
                double dist = Math.max(0.1, knock.length());
                e.setDeltaMovement(knock.x/dist*2.0, 3.5, knock.z/dist*2.0);
            }

            // 7. Kráter
            TerrainDestruction.carveCrater(level, impactPos.x, impactPos.y, impactPos.z, 8.0);
            TerrainDestruction.playCrumbleSound(level, impactPos.x, impactPos.y, impactPos.z);

            // 8. Utózúgás
            DelayedEffectScheduler.schedule(10, () ->
                level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                        SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 2.0f, 0.3f));
        });
    }
}
