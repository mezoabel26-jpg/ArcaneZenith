package com.arcanezenith.client;

import com.arcanezenith.config.ArcaneZenithConfig;
import com.arcanezenith.client.effect.PostEffectManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Arcane Zenith beállítások képernyő.
 * Elérhető: Minecraft főmenü → Mods → Arcane Zenith → Config
 *
 * Tartalmaz:
 *  - Dark Fantasy Shader be/ki + intenzitás csúszka
 *  - Vizuális előnézet (a shader valós időben változik)
 *  - Preset gombok: Off / Subtle / Standard / Cinematic / MAX
 *  - Spell FX opciók
 */
public class ArcaneZenithConfigScreen extends Screen {

    private final Screen parent;

    // Shader sliders (y pozíció alapú egyszerű slider emuláció)
    private float previewIntensity;
    private float previewContrast;
    private float previewSaturation;
    private float previewVignette;
    private boolean previewEnabled;
    private boolean previewGrain;
    private boolean previewChromaticAb;
    private boolean previewSpellShaders;
    private boolean previewScreenShake;
    private String  previewColorStyle;

    // Slider state
    private int draggedSlider = -1; // 0=intensity,1=contrast,2=saturation,3=vignette
    private static final int[] SLIDER_Y = {130, 155, 180, 205};
    private static final String[] SLIDER_NAMES = {"Alap intenzitás", "Kontraszt", "Telítettség", "Vignette"};
    private static final float[] SLIDER_MIN = {0.0f, 1.0f, 0.0f, 0.0f};
    private static final float[] SLIDER_MAX = {1.0f, 2.0f, 1.5f, 2.0f};

    public ArcaneZenithConfigScreen(Screen parent) {
        super(Component.literal("Arcane Zenith — Beállítások"));
        this.parent = parent;
        loadFromConfig();
    }

    private void loadFromConfig() {
        previewEnabled       = ArcaneZenithConfig.shaderEnabled;
        previewIntensity     = ArcaneZenithConfig.shaderIntensity;
        previewContrast      = ArcaneZenithConfig.shaderContrast;
        previewSaturation    = ArcaneZenithConfig.shaderSaturation;
        previewVignette      = ArcaneZenithConfig.shaderVignette;
        previewGrain         = ArcaneZenithConfig.shaderFilmGrain > 0.005f;
        previewChromaticAb   = ArcaneZenithConfig.shaderChromaticAb;
        previewSpellShaders  = ArcaneZenithConfig.fxSpellShaders;
        previewScreenShake   = ArcaneZenithConfig.fxScreenShake;
        previewColorStyle    = ArcaneZenithConfig.shaderColorStyle;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // ── PRESET GOMBOK ─────────────────────────────────────────────────────
        // Ki
        this.addRenderableWidget(Button.builder(
                Component.literal("Ki"), b -> applyPreset("off"))
                .bounds(cx - 210, 72, 60, 20).build());
        // Halvány
        this.addRenderableWidget(Button.builder(
                Component.literal("Halvány"), b -> applyPreset("subtle"))
                .bounds(cx - 145, 72, 70, 20).build());
        // Normál
        this.addRenderableWidget(Button.builder(
                Component.literal("Normál ✓"), b -> applyPreset("standard"))
                .bounds(cx - 70, 72, 70, 20).build());
        // Cinematic
        this.addRenderableWidget(Button.builder(
                Component.literal("Cinematic"), b -> applyPreset("cinematic"))
                .bounds(cx + 5, 72, 80, 20).build());
        // MAX
        this.addRenderableWidget(Button.builder(
                Component.literal("MAX"), b -> applyPreset("max"))
                .bounds(cx + 90, 72, 50, 20).build());

        // ── SZÍN STÍLUS ───────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("Sötét (" + getStyleEmoji("dark") + ")"),
                b -> { previewColorStyle = "dark"; applyLivePreview(); })
                .bounds(cx - 210, 240, 95, 18).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Hideg (" + getStyleEmoji("cold") + ")"),
                b -> { previewColorStyle = "cold"; applyLivePreview(); })
                .bounds(cx - 110, 240, 95, 18).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Meleg (" + getStyleEmoji("warm") + ")"),
                b -> { previewColorStyle = "warm"; applyLivePreview(); })
                .bounds(cx - 10, 240, 95, 18).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Semleges (" + getStyleEmoji("neutral") + ")"),
                b -> { previewColorStyle = "neutral"; applyLivePreview(); })
                .bounds(cx + 90, 240, 105, 18).build());

        // ── CHECKBOX-OK ────────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal((previewEnabled ? "✔ " : "✘ ") + "Dark Fantasy Shader"),
                b -> { previewEnabled = !previewEnabled;
                       b.setMessage(Component.literal((previewEnabled?"✔ ":"✘ ")+"Dark Fantasy Shader"));
                       applyLivePreview(); })
                .bounds(cx - 210, 100, 180, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal((previewGrain ? "✔ " : "✘ ") + "Film Grain"),
                b -> { previewGrain = !previewGrain;
                       b.setMessage(Component.literal((previewGrain?"✔ ":"✘ ")+"Film Grain"));
                       applyLivePreview(); })
                .bounds(cx + 5, 100, 110, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal((previewChromaticAb ? "✔ " : "✘ ") + "Kromatikus AB"),
                b -> { previewChromaticAb = !previewChromaticAb;
                       b.setMessage(Component.literal((previewChromaticAb?"✔ ":"✘ ")+"Kromatikus AB"));
                       applyLivePreview(); })
                .bounds(cx + 120, 100, 130, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal((previewSpellShaders ? "✔ " : "✘ ") + "Spell Shaderek"),
                b -> { previewSpellShaders = !previewSpellShaders;
                       b.setMessage(Component.literal((previewSpellShaders?"✔ ":"✘ ")+"Spell Shaderek"));
                       applyLivePreview(); })
                .bounds(cx - 210, 265, 150, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal((previewScreenShake ? "✔ " : "✘ ") + "Kamera Rázás"),
                b -> { previewScreenShake = !previewScreenShake;
                       b.setMessage(Component.literal((previewScreenShake?"✔ ":"✘ ")+"Kamera Rázás"));
                       applyLivePreview(); })
                .bounds(cx + 5, 265, 150, 18).build());

        // ── MENTÉS / MÉGSE ────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("Mentés"), b -> saveAndClose())
                .bounds(cx - 80, this.height - 28, 75, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Mégse"), b -> this.minecraft.setScreen(parent))
                .bounds(cx + 5, this.height - 28, 75, 20).build());
    }

    private String getStyleEmoji(String style) {
        return switch (style) {
            case "dark"    -> previewColorStyle.equals("dark")    ? "●" : "○";
            case "cold"    -> previewColorStyle.equals("cold")    ? "●" : "○";
            case "warm"    -> previewColorStyle.equals("warm")    ? "●" : "○";
            case "neutral" -> previewColorStyle.equals("neutral") ? "●" : "○";
            default -> "○";
        };
    }

    private void applyPreset(String preset) {
        switch (preset) {
            case "off"      -> { previewEnabled=false; previewIntensity=0f; previewContrast=1f;
                                  previewSaturation=1f; previewVignette=0f; previewGrain=false; }
            case "subtle"   -> { previewEnabled=true; previewIntensity=0.15f; previewContrast=1.1f;
                                  previewSaturation=0.9f; previewVignette=0.5f; previewGrain=false; }
            case "standard" -> { previewEnabled=true; previewIntensity=0.35f; previewContrast=1.25f;
                                  previewSaturation=0.78f; previewVignette=1.1f; previewGrain=true; }
            case "cinematic"-> { previewEnabled=true; previewIntensity=0.6f; previewContrast=1.4f;
                                  previewSaturation=0.65f; previewVignette=1.5f; previewGrain=true;
                                  previewChromaticAb=true; }
            case "max"      -> { previewEnabled=true; previewIntensity=1.0f; previewContrast=1.7f;
                                  previewSaturation=0.5f; previewVignette=2.0f; previewGrain=true;
                                  previewChromaticAb=true; previewColorStyle="dark"; }
        }
        applyLivePreview();
    }

    private void applyLivePreview() {
        // Valós idejű shader frissítés — látod a hatást azonnal
        if (previewEnabled) {
            PostEffectManager.applyConfigValues(
                previewIntensity, previewContrast, previewSaturation,
                previewVignette, previewGrain ? ArcaneZenithConfig.shaderFilmGrain : 0f,
                previewColorStyle, previewChromaticAb
            );
        } else {
            PostEffectManager.disableDarkFantasy();
        }
        ArcaneZenithConfig.fxSpellShaders  = previewSpellShaders;
        ArcaneZenithConfig.fxScreenShake   = previewScreenShake;
    }

    private float[] getSliderValues() {
        return new float[]{previewIntensity, previewContrast, previewSaturation, previewVignette};
    }

    private void setSliderValue(int idx, float v) {
        float[] vals = getSliderValues();
        vals[idx] = v;
        previewIntensity  = vals[0];
        previewContrast   = vals[1];
        previewSaturation = vals[2];
        previewVignette   = vals[3];
        applyLivePreview();
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        this.renderBackground(gfx, mx, my, pt);

        // Cím
        gfx.drawCenteredString(this.font,
                "✦ Arcane Zenith — Beállítások ✦",
                this.width/2, 12, 0xFFE6D0FF);

        gfx.drawString(this.font, "Presetek:", this.width/2 - 210, 60, 0xFFCCBBEE);
        gfx.drawString(this.font, "Dark Fantasy Shader:", this.width/2 - 210, 122, 0xFFCCBBEE);
        gfx.drawString(this.font, "Szín stílus:", this.width/2 - 210, 230, 0xFFCCBBEE);
        gfx.drawString(this.font, "Egyéb effektek:", this.width/2 - 210, 256, 0xFFCCBBEE);

        // Slider-ek rajzolása
        float[] vals = getSliderValues();
        int sliderX  = this.width/2 - 210;
        int sliderW  = 280;

        for (int i = 0; i < 4; i++) {
            int sy = SLIDER_Y[i];
            float t = (vals[i] - SLIDER_MIN[i]) / (SLIDER_MAX[i] - SLIDER_MIN[i]);
            int fillW = (int)(t * sliderW);

            // Track
            gfx.fill(sliderX, sy+5, sliderX+sliderW, sy+10, 0xFF333344);
            // Fill
            gfx.fill(sliderX, sy+5, sliderX+fillW, sy+10, 0xFF8855CC);
            // Handle
            gfx.fill(sliderX+fillW-2, sy+2, sliderX+fillW+2, sy+13, 0xFFCC99FF);

            // Label + value
            String label = SLIDER_NAMES[i] + ": " + String.format("%.2f", vals[i]);
            gfx.drawString(this.font, label, sliderX + sliderW + 8, sy + 2, 0xFFDDCCFF);

            // Hover highlight
            if (mx >= sliderX && mx <= sliderX+sliderW && my >= sy && my <= sy+16) {
                gfx.fill(sliderX, sy+5, sliderX+sliderW, sy+10, 0x22FFFFFF);
            }
        }

        // Aktuális preset jelzése
        String styleInfo = "Aktív stílus: " + previewColorStyle + 
            (previewEnabled ? " | Shader: BE" : " | Shader: KI");
        gfx.drawString(this.font, styleInfo, this.width/2 - 210, this.height - 45, 0xFF998AAA);

        super.render(gfx, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Slider kattintás/húzás indítás
        int sliderX = this.width/2 - 210;
        int sliderW = 280;
        for (int i = 0; i < 4; i++) {
            int sy = SLIDER_Y[i];
            if (mx >= sliderX && mx <= sliderX+sliderW && my >= sy && my <= sy+16) {
                draggedSlider = i;
                float t = (float)(mx - sliderX) / sliderW;
                float v = SLIDER_MIN[i] + t * (SLIDER_MAX[i] - SLIDER_MIN[i]);
                setSliderValue(i, Math.max(SLIDER_MIN[i], Math.min(SLIDER_MAX[i], v)));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggedSlider >= 0) {
            int sliderX = this.width/2 - 210;
            int sliderW = 280;
            float t = (float)(mx - sliderX) / sliderW;
            float v = SLIDER_MIN[draggedSlider] + t * (SLIDER_MAX[draggedSlider] - SLIDER_MIN[draggedSlider]);
            setSliderValue(draggedSlider, Math.max(SLIDER_MIN[draggedSlider], Math.min(SLIDER_MAX[draggedSlider], v)));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggedSlider = -1;
        return super.mouseReleased(mx, my, button);
    }

    private void saveAndClose() {
        // Írjuk vissza a config spec-be
        ArcaneZenithConfig.SHADER_ENABLED.set(previewEnabled);
        ArcaneZenithConfig.SHADER_BASE_INTENSITY.set((double) previewIntensity);
        ArcaneZenithConfig.SHADER_CONTRAST.set((double) previewContrast);
        ArcaneZenithConfig.SHADER_SATURATION.set((double) previewSaturation);
        ArcaneZenithConfig.SHADER_VIGNETTE.set((double) previewVignette);
        ArcaneZenithConfig.SHADER_FILM_GRAIN.set(previewGrain ? (double) ArcaneZenithConfig.shaderFilmGrain : 0.0);
        ArcaneZenithConfig.SHADER_CHROMATIC_AB.set(previewChromaticAb);
        ArcaneZenithConfig.SHADER_COLOR_STYLE.set(previewColorStyle);
        ArcaneZenithConfig.FX_SPELL_SHADERS.set(previewSpellShaders);
        ArcaneZenithConfig.FX_SCREEN_SHAKE.set(previewScreenShake);
        // Bake cache
        ArcaneZenithConfig.bake();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
