// ╔══════════════════════════════════════════════════════════════════════╗
// ║  ARCANE ZENITH — Dark Fantasy Game Shader  (Config-driven)          ║
// ║  Fully beállítható a játékon belüli Config Screen-ből               ║
// ║                                                                      ║
// ║  Uniforms (mind beállítható Config Screen-ből):                     ║
// ║    Intensity        — 0.0=off, 1.0=full                            ║
// ║    Contrast         — 1.0=neutral, 1.5=punchy                      ║
// ║    Saturation       — 1.0=full color, 0.0=grayscale                ║
// ║    VignetteStrength — 0.0=none, 2.0=heavy                          ║
// ║    GrainStrength    — 0.0=none, 0.06=filmic                        ║
// ║    ChromaticAb      — 0.0=off, 1.0=on                              ║
// ║    ColorStyle       — 0=dark, 1=cold, 2=warm, 3=neutral            ║
// ║    Time             — game time for grain animation                 ║
// ╚══════════════════════════════════════════════════════════════════════╝
#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;
uniform float Contrast;
uniform float Saturation;
uniform float VignetteStrength;
uniform float GrainStrength;
uniform float ChromaticAb;
uniform float ColorStyle;   // 0=dark, 1=cold, 2=warm, 3=neutral

in  vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

float hash(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

void main() {
    float I = clamp(Intensity, 0.0, 1.0);

    // ── Chromatic aberration (config-alapú) ────────────────────────────────
    float ab = 0.0018 * I * ChromaticAb;
    vec3 col;
    col.r = texture(DiffuseSampler, texCoord + vec2( ab, 0.0)).r;
    col.g = texture(DiffuseSampler, texCoord              ).g;
    col.b = texture(DiffuseSampler, texCoord + vec2(-ab, 0.0)).b;
    float alpha = texture(DiffuseSampler, texCoord).a;

    // ── Contrast ───────────────────────────────────────────────────────────
    float c = mix(1.0, Contrast, I);
    col = ((col - 0.5) * c) + 0.5;

    // ── Saturation ─────────────────────────────────────────────────────────
    float lum = luminance(col);
    float s   = mix(1.0, Saturation, I);
    col       = mix(vec3(lum), col, s);

    // ── Color Grading (4 stílus) ───────────────────────────────────────────
    float grade = I * 0.85;
    float shadowMask = smoothstep(0.4, 0.0, lum);
    float midMask    = smoothstep(0.0, 0.5, lum) * smoothstep(1.0, 0.5, lum);
    float hiMask     = smoothstep(0.7, 1.0, lum);

    if (ColorStyle < 0.5) {
        // DARK — teal shadows, amber mids (default)
        col = mix(col, col * vec3(0.55, 0.70, 0.95), shadowMask * grade * 0.6);
        col += vec3(0.04, 0.02, -0.02) * midMask * grade;
        col = mix(col, vec3(lum), hiMask * grade * 0.3);
    } else if (ColorStyle < 1.5) {
        // COLD — very blue/grey, icey
        col = mix(col, col * vec3(0.45, 0.60, 1.0),  shadowMask * grade * 0.8);
        col += vec3(-0.02, -0.01, 0.05) * midMask * grade;
        col = mix(col, vec3(lum * 0.9, lum * 0.95, lum * 1.1), hiMask * grade * 0.5);
    } else if (ColorStyle < 2.5) {
        // WARM — amber/sepia, golden
        col = mix(col, col * vec3(0.85, 0.65, 0.40), shadowMask * grade * 0.7);
        col += vec3(0.06, 0.04, -0.03) * midMask * grade;
        col = mix(col, vec3(lum * 1.1, lum, lum * 0.8), hiMask * grade * 0.3);
    }
    // NEUTRAL (3.0): csak contrast + vignette, szín-eltolás nélkül

    // ── Vignette ───────────────────────────────────────────────────────────
    vec2  vigCoord = texCoord - 0.5;
    vigCoord.x    *= 1.55;
    float vd       = dot(vigCoord, vigCoord);
    float vig      = 1.0 - smoothstep(0.18, 0.72, vd);
    col           *= mix(1.0, vig, VignetteStrength * I);

    // ── Film grain (config-alapú erősség) ──────────────────────────────────
    if (GrainStrength > 0.001) {
        float grainFrame = floor(Time * 24.0);
        float grain = hash(texCoord * 512.0 + grainFrame) * 2.0 - 1.0;
        float gs    = GrainStrength * I * (1.0 - lum * 0.55);
        col += grain * gs;
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), alpha);
}
