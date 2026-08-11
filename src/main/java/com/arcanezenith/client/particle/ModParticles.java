package com.arcanezenith.client.particle;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ArcaneZenith.MOD_ID);

    // ── Original base particles (Tier 0-4 spells) ───────────────────────────
    public static final Supplier<SimpleParticleType> ARCANE_SPARK =
            PARTICLES.register("arcane_spark", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> RUNE_RING =
            PARTICLES.register("rune_ring", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> VOID_CORE =
            PARTICLES.register("void_core", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> GOLDEN_LIGHT =
            PARTICLES.register("golden_light", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> PLASMA_BEAM =
            PARTICLES.register("plasma_beam", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> THUNDER_SPARK =
            PARTICLES.register("thunder_spark", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> HOLY_STAR =
            PARTICLES.register("holy_star", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> SHADOW_WISP =
            PARTICLES.register("shadow_wisp", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> FROST_SHARD =
            PARTICLES.register("frost_shard", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> LAVA_GEYSER =
            PARTICLES.register("lava_geyser", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> PLASMA_SPIRAL =
            PARTICLES.register("plasma_spiral", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> GRAVITY_DUST =
            PARTICLES.register("gravity_dust", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> HEAVEN_BEAM =
            PARTICLES.register("heaven_beam", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> SINGULARITY_NOVA =
            PARTICLES.register("singularity_nova", () -> new SimpleParticleType(false));

    // ── Tier 5 Legendary spell particles ────────────────────────────────────
    /** EldritchTempest — arany energiaostor csík */
    public static final Supplier<SimpleParticleType> ELDRITCH_WHIP =
            PARTICLES.register("eldritch_whip", () -> new SimpleParticleType(false));
    /** AvadaCurse — zöld halálfény villanás */
    public static final Supplier<SimpleParticleType> DEATH_FLASH =
            PARTICLES.register("death_flash", () -> new SimpleParticleType(false));
    /** StarscourgeMeteo — vörös meteor csóva */
    public static final Supplier<SimpleParticleType> METEOR_TRAIL =
            PARTICLES.register("meteor_trail", () -> new SimpleParticleType(false));
    /** ExcaliburBeam — tiszta arany lézersugár */
    public static final Supplier<SimpleParticleType> EXCALIBUR_BEAM =
            PARTICLES.register("excalibur_beam", () -> new SimpleParticleType(false));
    /** VortexEssence — sötétlila gravitációs gyűrű */
    public static final Supplier<SimpleParticleType> VORTEX_RING =
            PARTICLES.register("vortex_ring", () -> new SimpleParticleType(false));
    /** BladesOfChaos — tűzpenge szikra */
    public static final Supplier<SimpleParticleType> CHAOS_BLADE =
            PARTICLES.register("chaos_blade", () -> new SimpleParticleType(false));
    /** LightningStorm — lila-kék viharvillám */
    public static final Supplier<SimpleParticleType> STORM_BOLT =
            PARTICLES.register("storm_bolt", () -> new SimpleParticleType(false));
    /** Chidori — kék elektromos kéz szikra */
    public static final Supplier<SimpleParticleType> CHIDORI_SPARK =
            PARTICLES.register("chidori_spark", () -> new SimpleParticleType(false));
    /** GlintstonePhalanx — neon cián mágikus tőr */
    public static final Supplier<SimpleParticleType> GLINT_DAGGER =
            PARTICLES.register("glint_dagger", () -> new SimpleParticleType(false));
    /** CrimsonBands — skarlát kötelék láncszem */
    public static final Supplier<SimpleParticleType> CRIMSON_CHAIN =
            PARTICLES.register("crimson_chain", () -> new SimpleParticleType(false));
}
