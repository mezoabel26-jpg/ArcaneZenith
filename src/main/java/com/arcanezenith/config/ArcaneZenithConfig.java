package com.arcanezenith.config;

import com.arcanezenith.ArcaneZenith;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Arcane Zenith Client Configuration.
 *
 * Elérhető itt: .minecraft/config/arcanezenith-client.toml
 * A játékon belül: Mods → Arcane Zenith → Config
 *
 * ═══════════════════════════════════════════════════════════
 *  DARK FANTASY SHADER BEÁLLÍTÁSOK
 * ═══════════════════════════════════════════════════════════
 *
 *  [dark_fantasy_shader]
 *    enabled         = true     # be/ki kapcsoló
 *    base_intensity  = 0.35     # alap intenzitás (0.0–1.0)
 *    combat_boost    = true     # combat alatt 1.0-ra ugrik
 *    contrast        = 1.25     # 1.0=semleges, 1.5=nagyon kontrasztos
 *    saturation      = 0.78     # 1.0=teljes szín, 0.0=szürke
 *    vignette        = 1.1      # 0=nincs, 2.0=nagyon erős
 *    film_grain      = 0.028    # 0=nincs, 0.06=nagyon erős
 *    color_style     = "dark"   # "dark" / "cold" / "warm" / "neutral"
 *    chromatic_aber  = true     # kromatikus aberráció be/ki
 *
 *  [spell_effects]
 *    screen_shake        = true
 *    fov_punch           = true
 *    spell_shaders       = true  # spell-specifikus shaderek (gravity lens stb.)
 *    particle_intensity  = 1.0   # particle sűrűség szorzó
 */
public final class ArcaneZenithConfig {

    // ── Dark Fantasy Shader ───────────────────────────────────────────────────
    public static ModConfigSpec.BooleanValue  SHADER_ENABLED;
    public static ModConfigSpec.DoubleValue   SHADER_BASE_INTENSITY;
    public static ModConfigSpec.BooleanValue  SHADER_COMBAT_BOOST;
    public static ModConfigSpec.DoubleValue   SHADER_CONTRAST;
    public static ModConfigSpec.DoubleValue   SHADER_SATURATION;
    public static ModConfigSpec.DoubleValue   SHADER_VIGNETTE;
    public static ModConfigSpec.DoubleValue   SHADER_FILM_GRAIN;
    public static ModConfigSpec.ConfigValue<String> SHADER_COLOR_STYLE;
    public static ModConfigSpec.BooleanValue  SHADER_CHROMATIC_AB;

    // ── Spell Effects ─────────────────────────────────────────────────────────
    public static ModConfigSpec.BooleanValue  FX_SCREEN_SHAKE;
    public static ModConfigSpec.BooleanValue  FX_FOV_PUNCH;
    public static ModConfigSpec.BooleanValue  FX_SPELL_SHADERS;
    public static ModConfigSpec.DoubleValue   FX_PARTICLE_INTENSITY;

    // ── Cached values (read every frame, avoid spec lookups) ──────────────────
    public static boolean shaderEnabled      = true;
    public static float   shaderIntensity    = 0.35f;
    public static boolean shaderCombatBoost  = true;
    public static float   shaderContrast     = 1.25f;
    public static float   shaderSaturation   = 0.78f;
    public static float   shaderVignette     = 1.1f;
    public static float   shaderFilmGrain    = 0.028f;
    public static String  shaderColorStyle   = "dark";
    public static boolean shaderChromaticAb  = true;
    public static boolean fxScreenShake      = true;
    public static boolean fxFovPunch         = true;
    public static boolean fxSpellShaders     = true;
    public static float   fxParticleIntensity= 1.0f;

    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // ── DARK FANTASY SHADER ───────────────────────────────────────────────
        builder.comment(
            "════════════════════════════════════════════════════",
            "  ARCANE ZENITH — Dark Fantasy Shader Beállítások",
            "  Ez egy egyedi post-processing shader amely",
            "  sötétíti az égboltot, kontrasztot és filmes",
            "  hangulatot ad a varázslatoknak.",
            "════════════════════════════════════════════════════"
        ).push("dark_fantasy_shader");

        SHADER_ENABLED = builder
            .comment("A Dark Fantasy shader be/ki kapcsolója.",
                     "true  = mindig aktív",
                     "false = kikapcsolt (vanilla megjelenés)")
            .define("enabled", true);

        SHADER_BASE_INTENSITY = builder
            .comment("Alap intenzitás amikor nem harcolsz (0.0–1.0).",
                     "0.0 = nincs hatás | 0.35 = enyhe | 1.0 = teljes")
            .defineInRange("base_intensity", 0.35, 0.0, 1.0);

        SHADER_COMBAT_BOOST = builder
            .comment("Ha true, combat alatt az intenzitás 1.0-ra ugrik.")
            .define("combat_boost", true);

        SHADER_CONTRAST = builder
            .comment("Kontraszt erőssége (1.0 = semleges, 1.5 = nagyon kontrasztos).")
            .defineInRange("contrast", 1.25, 1.0, 2.0);

        SHADER_SATURATION = builder
            .comment("Szín-telítettség (1.0 = teljes szín, 0.0 = szürke).",
                     "0.78 ajánlott — dark fantasy hangulat megőrzi a spell-színeket.")
            .defineInRange("saturation", 0.78, 0.0, 1.5);

        SHADER_VIGNETTE = builder
            .comment("Vignette erőssége (0.0 = nincs, 2.0 = erős fekete keret).")
            .defineInRange("vignette", 1.1, 0.0, 2.0);

        SHADER_FILM_GRAIN = builder
            .comment("Film grain intenzitása (0.0 = tiszta, 0.06 = filmes).",
                     "Alacsony értéknél alig látható, de mélységet ad.")
            .defineInRange("film_grain", 0.028, 0.0, 0.1);

        SHADER_COLOR_STYLE = builder
            .comment("Szín-paletta stílus:",
                     "  dark    = hideg teal árnyékok, amber fények (ajánlott)",
                     "  cold    = nagyon kék/szürke, sötét télies hangulat",
                     "  warm    = meleg amber, sepia-szerű",
                     "  neutral = csak kontraszt + vignette, nincs szín-eltolás")
            .define("color_style", "dark");

        SHADER_CHROMATIC_AB = builder
            .comment("Kromatikus aberráció (RGB eltolás a szélek felé).",
                     "Cinematic hatás, kis teljesítmény-hatással.")
            .define("chromatic_aberration", true);

        builder.pop();

        // ── SPELL EFFECTS ─────────────────────────────────────────────────────
        builder.comment(
            "════════════════════════════════════════════════════",
            "  Varázslat Vizuális Hatások",
            "════════════════════════════════════════════════════"
        ).push("spell_effects");

        FX_SCREEN_SHAKE = builder
            .comment("Kamera-rázás varázslatok becsapódásakor.")
            .define("screen_shake", true);

        FX_FOV_PUNCH = builder
            .comment("FOV-punch effekt (zoom-in/out varázslás közben).")
            .define("fov_punch", true);

        FX_SPELL_SHADERS = builder
            .comment("Spell-specifikus shaderek (gravity lens, time stop, stb.)",
                     "false = csak a Dark Fantasy game shader fut.")
            .define("spell_shaders", true);

        FX_PARTICLE_INTENSITY = builder
            .comment("Particle sűrűség szorzó (0.5 = fele, 1.0 = normal, 2.0 = dupla).",
                     "Csökkentsd ha gyengébb géped van.")
            .defineInRange("particle_intensity", 1.0, 0.1, 2.0);

        builder.pop();

        SPEC = builder.build();
    }

    /** NeoForge config regisztrálás. Hívd a mod konstruktorban. */
    public static void register() {
        // Config registered via ModContainer in ArcaneZenith constructor
    }

    /** Config értékek betöltése a cache-be. Automatikusan hívódik config load/reload-kor. */
    public static void bake() {
        shaderEnabled      = SHADER_ENABLED.get();
        shaderIntensity    = SHADER_BASE_INTENSITY.get().floatValue();
        shaderCombatBoost  = SHADER_COMBAT_BOOST.get();
        shaderContrast     = SHADER_CONTRAST.get().floatValue();
        shaderSaturation   = SHADER_SATURATION.get().floatValue();
        shaderVignette     = SHADER_VIGNETTE.get().floatValue();
        shaderFilmGrain    = SHADER_FILM_GRAIN.get().floatValue();
        shaderColorStyle   = SHADER_COLOR_STYLE.get();
        shaderChromaticAb  = SHADER_CHROMATIC_AB.get();
        fxScreenShake      = FX_SCREEN_SHAKE.get();
        fxFovPunch         = FX_FOV_PUNCH.get();
        fxSpellShaders     = FX_SPELL_SHADERS.get();
        fxParticleIntensity= FX_PARTICLE_INTENSITY.get().floatValue();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }
}
