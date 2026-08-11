// Arcane Zenith — Heat Haze (UPGRADED)
// Stronger distortion, dual-frequency shimmer, orange heat tint,
// subtle particle-effect chromatic split.
#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) { return dot(c, vec3(0.2126,0.7152,0.0722)); }

void main() {
    float I = clamp(Intensity, 0.0, 1.0);

    // Dual-frequency heat ripple
    float freq1 = sin(texCoord.y * 18.0 + Time * 5.0
                    + sin(texCoord.x * 6.0 + Time * 2.5) * 1.5);
    float freq2 = sin(texCoord.y * 34.0 - Time * 8.0
                    + cos(texCoord.x * 9.0 - Time * 1.8) * 0.8);
    float haze  = (freq1 * 0.6 + freq2 * 0.4) * I * 0.018;

    // Slight vertical component too (hot air rises)
    float hazeY = sin(texCoord.x * 12.0 + Time * 4.0) * I * 0.006;

    // Chromatic split for heat shimmer
    float ab = I * 0.004;
    vec3 col;
    col.r = texture(DiffuseSampler, texCoord + vec2(haze + ab, hazeY)).r;
    col.g = texture(DiffuseSampler, texCoord + vec2(haze,      hazeY)).g;
    col.b = texture(DiffuseSampler, texCoord + vec2(haze - ab, hazeY)).b;

    float lum = luminance(col);

    // Orange heat tint — warm up the whole scene
    col.r = min(1.0, col.r + I * 0.06);
    col.g = min(1.0, col.g + I * 0.02);
    col.b = max(0.0, col.b - I * 0.04);

    // Slight contrast boost (fire scenes look punchier)
    col = mix(col, smoothstep(0.0, 1.0, col), I * 0.2);

    fragColor = vec4(col, 1.0);
}
