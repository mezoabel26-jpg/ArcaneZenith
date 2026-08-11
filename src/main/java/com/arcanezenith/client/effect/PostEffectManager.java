package com.arcanezenith.client.effect;

import com.arcanezenith.ArcaneZenith;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Manages all Arcane Zenith post-processing effects.
 *
 * 10 effects total:
 *   Base 5:  HEAT_HAZE, GRAVITY_LENS, TIME_STOP, LIGHTNING_FLASH, HOLY_BLOOM
 *   New 5:   DARK_FANTASY (game shader), VOID_RIFT, BLOOD_CURSE, STELLAR_FIRE,
 *            ARCANE_OVERDRIVE
 *
 * DARK_FANTASY runs as a persistent "game shader" — always active at base
 * intensity 0.35, ramping up during combat. The others are spell-triggered.
 */
public final class PostEffectManager {

    private static final Logger LOG = LoggerFactory.getLogger(ArcaneZenith.MOD_ID + "/PostFX");

    public enum Effect {
        // ── Spell-triggered ──────────────────────────────────────────────────
        HEAT_HAZE        ("arcanezenith:shaders/post/heat_haze.json"),
        GRAVITY_LENS     ("arcanezenith:shaders/post/gravity_lens.json"),
        TIME_STOP        ("arcanezenith:shaders/post/time_stop.json"),
        LIGHTNING_FLASH  ("arcanezenith:shaders/post/lightning_flash.json"),
        HOLY_BLOOM       ("arcanezenith:shaders/post/holy_bloom.json"),
        VOID_RIFT        ("arcanezenith:shaders/post/void_rift.json"),
        BLOOD_CURSE      ("arcanezenith:shaders/post/blood_curse.json"),
        STELLAR_FIRE     ("arcanezenith:shaders/post/stellar_fire.json"),
        ARCANE_OVERDRIVE ("arcanezenith:shaders/post/arcane_overdrive.json"),
        // ── Persistent game shader ───────────────────────────────────────────
        DARK_FANTASY     ("arcanezenith:shaders/post/dark_fantasy.json");

        public final ResourceLocation path;
        Effect(String p) { this.path = ResourceLocation.parse(p); }
    }

    private static Effect    activeEffect   = null;
    private static PostChain activeChain    = null;
    private static int       ticksRemaining = 0;
    private static int       totalTicks     = 0;
    private static float     currentTime    = 0f;

    // Dark Fantasy persistent game shader
    private static PostChain darkFantasyChain  = null;
    private static float     darkFantasyIntensity = 0.35f; // always-on base

    // Extra data for specific effects
    public static float gravityLensCenterX = 0.5f;
    public static float gravityLensCenterY = 0.5f;
    public static float bloodCurseGreenPulse = 0.0f;

    private PostEffectManager() {}

    /** Initialise the persistent Dark Fantasy game shader. Call once on world load. */
    public static void initDarkFantasy() {
        if (darkFantasyChain != null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            darkFantasyChain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    Effect.DARK_FANTASY.path);
            darkFantasyChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (IOException e) {
            LOG.warn("Failed to init Dark Fantasy shader: {}", e.getMessage());
            darkFantasyChain = null;
        }
    }

    /** Set Dark Fantasy combat intensity (0=exploration, 1=full combat). */
    public static void setCombatIntensity(float combat) {
        if (!com.arcanezenith.config.ArcaneZenithConfig.shaderEnabled) return;
        darkFantasyIntensity = com.arcanezenith.config.ArcaneZenithConfig.shaderIntensity
                + combat * (1.0f - com.arcanezenith.config.ArcaneZenithConfig.shaderIntensity);
    }

    /** Apply config values directly (called from Config Screen live preview). */
    public static void applyConfigValues(float intensity, float contrast, float saturation,
                                          float vignette, float grain, String colorStyle,
                                          boolean chromaticAb) {
        darkFantasyIntensity = intensity;
        // Store for applyIfActive
        configContrast   = contrast;
        configSaturation = saturation;
        configVignette   = vignette;
        configGrain      = grain;
        configColorStyle = colorStyleToFloat(colorStyle);
        configChromaticAb= chromaticAb ? 1.0f : 0.0f;
        if (darkFantasyChain == null) initDarkFantasy();
    }

    /** Disable the Dark Fantasy shader (config screen OFF button). */
    public static void disableDarkFantasy() {
        darkFantasyIntensity = 0f;
    }

    private static float colorStyleToFloat(String style) {
        return switch (style) { case "cold" -> 1.0f; case "warm" -> 2.0f; case "neutral" -> 3.0f; default -> 0.0f; };
    }

    public static float timeSlopAberration = 0f; // time_resume aberration

    // Config-driven values (updated from Config Screen)
    private static float configContrast    = 1.25f;
    private static float configSaturation  = 0.78f;
    private static float configVignette    = 1.1f;
    private static float configGrain       = 0.028f;
    private static float configColorStyle  = 0.0f;
    private static float configChromaticAb = 1.0f;

    public static void activate(Effect effect, int durationTicks) {
        if (effect == Effect.DARK_FANTASY) { darkFantasyIntensity = 1.0f; return; }
        if (effect == activeEffect && ticksRemaining > 0) {
            ticksRemaining = Math.max(ticksRemaining, durationTicks);
            return;
        }
        teardown();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            activeChain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    effect.path);
            activeChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            activeEffect   = effect;
            ticksRemaining = durationTicks;
            totalTicks     = durationTicks;
            currentTime    = 0f;
        } catch (IOException e) {
            LOG.warn("Failed to load {}: {}", effect.path, e.getMessage());
            activeChain = null;
        }
    }

    public static void deactivate() { teardown(); }

    public static boolean tick() {
        if (activeChain != null && activeEffect != null) {
            if (ticksRemaining <= 0) { teardown(); }
            else { ticksRemaining--; currentTime += 0.05f; }
        }
        return activeChain != null;
    }

    public static void applyIfActive(float partialTick) {
        float t = currentTime + partialTick * 0.05f;
        float frac = totalTicks > 0 ? 1.0f - (float) ticksRemaining / totalTicks : 0f;

        // ── Apply persistent Dark Fantasy first ───────────────────────────
        if (darkFantasyChain != null && darkFantasyIntensity > 0.001f) {
            // Read from config cache
            com.arcanezenith.config.ArcaneZenithConfig cfg = null; // static access only
            float contrast    = com.arcanezenith.config.ArcaneZenithConfig.shaderContrast;
            float saturation  = com.arcanezenith.config.ArcaneZenithConfig.shaderSaturation;
            float vignette    = com.arcanezenith.config.ArcaneZenithConfig.shaderVignette;
            float grain       = com.arcanezenith.config.ArcaneZenithConfig.shaderFilmGrain;
            float chromatic   = com.arcanezenith.config.ArcaneZenithConfig.shaderChromaticAb ? 1.0f : 0.0f;
            float colorStyle  = colorStyleToFloat(com.arcanezenith.config.ArcaneZenithConfig.shaderColorStyle);
            // Live preview overrides
            if (configContrast != 1.25f || configSaturation != 0.78f) {
                contrast = configContrast; saturation = configSaturation;
                vignette = configVignette; grain = configGrain;
                colorStyle = configColorStyle; chromatic = configChromaticAb;
            }
            try {
                setUniformOnChain(darkFantasyChain, "Intensity",        darkFantasyIntensity);
                setUniformOnChain(darkFantasyChain, "Time",             t);
                setUniformOnChain(darkFantasyChain, "Contrast",         contrast);
                setUniformOnChain(darkFantasyChain, "Saturation",       saturation);
                setUniformOnChain(darkFantasyChain, "VignetteStrength", vignette);
                setUniformOnChain(darkFantasyChain, "GrainStrength",    grain);
                setUniformOnChain(darkFantasyChain, "ChromaticAb",      chromatic);
                setUniformOnChain(darkFantasyChain, "ColorStyle",       colorStyle);
                darkFantasyChain.process(partialTick);
            } catch (Exception e) {
                LOG.debug("Dark Fantasy process error: {}", e.getMessage());
            }
        }

        // ── Apply spell-triggered effect on top ───────────────────────────
        if (activeChain == null || activeEffect == null) return;
        try {
            switch (activeEffect) {
                case HEAT_HAZE -> {
                    float intensity = (float)(0.6 + 0.4 * Math.sin(frac * Math.PI));
                    setU("Intensity", intensity); setU("Time", t);
                }
                case GRAVITY_LENS -> {
                    float strength = (float)(0.08 + 0.22 * Math.sin(frac * Math.PI));
                    setU2("Center", gravityLensCenterX, gravityLensCenterY);
                    setU("Strength", strength);
                    setU("EventRadius", 0.05f + strength * 0.08f);
                    setU("Time", t);
                }
                case TIME_STOP -> {
                    setU("Desaturation", Math.min(1.0f, frac * 10f));
                    setU("Aberration",   0f);
                }
                case LIGHTNING_FLASH -> {
                    float flash = (float) Math.pow(Math.max(0, 1.0 - frac * 8.0), 0.35);
                    setU("FlashStrength", flash);
                    setU("EdgeDark",      (float)(0.2 * Math.max(0, 1.0 - frac * 3.0)));
                }
                case HOLY_BLOOM -> {
                    float bloom = (float)(Math.sin(frac * Math.PI) * 0.95);
                    setU("BloomStrength", bloom);
                    setU("GoldTint",      (float)(0.3 + 0.6 * Math.sin(frac * Math.PI)));
                    setU("Time", t);
                }
                case VOID_RIFT -> {
                    float intensity = (float)(Math.sin(frac * Math.PI) * 0.9);
                    setU("Intensity", intensity); setU("Time", t);
                }
                case BLOOD_CURSE -> {
                    float intensity = (float)(Math.sin(frac * Math.PI) * 0.85);
                    setU("Intensity", intensity);
                    setU("Time", t);
                    setU("GreenPulse", bloodCurseGreenPulse);
                }
                case STELLAR_FIRE -> {
                    float intensity = (float)(0.4 + 0.6 * Math.sin(frac * Math.PI));
                    setU("Intensity", intensity); setU("Time", t);
                }
                case ARCANE_OVERDRIVE -> {
                    float intensity = (float)(Math.sin(frac * Math.PI) * 0.95);
                    setU("Intensity", intensity); setU("Time", t);
                }
                default -> {}
            }
            activeChain.process(partialTick);
        } catch (Exception e) {
            LOG.debug("PostChain error: {}", e.getMessage());
            teardown();
        }
    }

    public static boolean isActive()  { return activeChain != null && ticksRemaining > 0; }
    public static Effect  getActive() { return activeEffect; }

    private static void setU(String n, float v) {
        if (activeChain == null) return;
        try {
            var u = activeChain.getUniform(n);
            if (u != null) u.set(v);
        } catch (Exception ignored) {}
    }
    private static void setU2(String n, float x, float y) {
        if (activeChain == null) return;
        try {
            var u = activeChain.getUniform(n);
            if (u != null) u.set(x, y);
        } catch (Exception ignored) {}
    }
    private static void setUniformOnChain(PostChain chain, String n, float v) {
        try {
            var u = chain.getUniform(n);
            if (u != null) u.set(v);
        } catch (Exception ignored) {}
    }
    private static void teardown() {
        if (activeChain!=null) { try{activeChain.close();}catch(Exception ignored){} activeChain=null; }
        activeEffect=null; ticksRemaining=0; totalTicks=0;
    }
}
