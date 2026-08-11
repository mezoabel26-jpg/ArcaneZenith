package com.arcanezenith.spell;

import com.arcanezenith.ArcaneZenith;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * ELDRITCH TEMPEST — Dr. Strange golden energy whips.
 * 8 arany energiaostor csap ki 360°-ban egyszerre, minden ostor
 * saját célpontot keres 14 blokkon belül. Összesített max sebzés: 360 dmg.
 * VFX: GOLDEN_LIGHT spirál trail minden ostornál, kamera 15° forgás,
 * minden csapásnál egyedi hang + spark burst.
 */
public class EldritchTempestSpell implements Spell {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArcaneZenith.MOD_ID, "eldritch_tempest");

    private static final int   WHIP_COUNT = 8;
    private static final float WHIP_DMG   = 45.0f;
    private static final double RANGE     = 14.0;

    @Override public ResourceLocation id()    { return ID; }
    @Override public float manaCost()         { return 55.0f; }
    @Override public int cooldownTicks()      { return 180; }

    @Override
    public void cast(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 pos = caster.position().add(0, 1.0, 0);

        // Cast burst — arany gyűrű + hang
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48.0;
            level.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.1f), 1.8f),
                    pos.x + Math.cos(a)*2.5, pos.y, pos.z + Math.sin(a)*2.5,
                    1, 0, 0.04, 0, 0.0);
        }
        level.sendParticles(ModParticles.ELDRITCH_WHIP.get(), pos.x, pos.y, pos.z, 12, 0.1,0.1,0.1, 0.02);
        level.sendParticles(ModParticles.RUNE_RING.get(), pos.x, pos.y, pos.z, 6, 0.1,0.1,0.1, 0.0);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0f, 0.6f);
        ModNetworking.sendEffect(caster, "fov_punch", 20f, 15f);
        ModNetworking.sendEffect(caster, "arcane_overdrive", 0.9f, 20f);
        ModNetworking.sendEffect(caster, "shake",     0.6f, 10f);

        // 8 ostor, egyenként eltolva 2 tickkel
        for (int w = 0; w < WHIP_COUNT; w++) {
            final int ww = w;
            DelayedEffectScheduler.schedule(w * 2, () -> {
                double baseAngle = ww * Math.PI * 2.0 / WHIP_COUNT;

                // Ostor trail — arany spirál a caster-től kifelé
                for (int step = 0; step <= 14; step++) {
                    double t  = step / 14.0;
                    double r  = t * RANGE;
                    double wa = baseAngle + t * Math.PI * 0.8; // csavar
                    Vec3 p = pos.add(Math.cos(wa)*r, Math.sin(t*Math.PI)*1.5, Math.sin(wa)*r);
                    level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                            p.x, p.y, p.z, 2, 0.08,0.08,0.08, 0.02);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.82f, 0.2f), 1.2f),
                            p.x, p.y, p.z, 1, 0,0,0, 0.0);
                }

                // Hitscan — minden entitás az ostor ívén
                AABB box = new AABB(pos.x-RANGE, pos.y-3, pos.z-RANGE,
                                    pos.x+RANGE, pos.y+5, pos.z+RANGE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                        en -> en != caster && en.isAlive())) {
                    // Csak az ostor szektorában lévők
                    Vec3 dir = e.position().subtract(pos).normalize();
                    double dot = dir.x * Math.cos(baseAngle) + dir.z * Math.sin(baseAngle);
                    if (dot < 0.35 || pos.distanceTo(e.position()) > RANGE) continue;

                    e.hurt(caster.damageSources().indirectMagic(caster, caster), WHIP_DMG);

                    // Csapás effekt
                    Vec3 ep = e.position().add(0, e.getBbHeight()/2.0, 0);
                    level.sendParticles(ModParticles.GOLDEN_LIGHT.get(),
                            ep.x, ep.y, ep.z, 25, 0.6,0.6,0.6, 0.14);
                    level.sendParticles(ParticleTypes.CRIT,
                            ep.x, ep.y, ep.z, 15, 0.4,0.4,0.4, 0.08);
                    level.sendParticles(new DustParticleOptions(
                            new Vector3f(1.0f, 0.9f, 0.3f), 2.5f),
                            ep.x, ep.y, ep.z, 10, 0.3,0.3,0.3, 0.04);
                    level.playSound(null, ep.x, ep.y, ep.z,
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                            1.2f, 0.7f + ww * 0.05f);
                }
                // Utolsó ostor után nagy zárás
                if (ww == WHIP_COUNT - 1) {
                    ModNetworking.sendEffect(caster, "shake",     1.0f, 12f);
                    ModNetworking.sendEffect(caster, "fov_punch", 15f,  8f);
                }
            });
        }
    }
}
