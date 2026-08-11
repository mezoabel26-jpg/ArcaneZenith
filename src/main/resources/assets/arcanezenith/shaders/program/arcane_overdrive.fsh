// Arcane Zenith — Arcane Overdrive Shader
// Screen overloads with magical energy: desaturates to white,
// electric blue fringing, scanline flicker, radial energy burst.
#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) { return dot(c, vec3(0.2126,0.7152,0.0722)); }
float hash(float n) { return fract(sin(n) * 43758.5453); }

void main() {
    float I = clamp(Intensity, 0.0, 1.0);

    // Electric scanlines
    float scan = 1.0 - step(0.5, fract(texCoord.y * 240.0 + Time * 12.0)) * 0.08 * I;

    // Radial energy burst from center
    vec2  dc   = texCoord - 0.5;
    float dist = length(dc);
    float burst = smoothstep(0.5, 0.0, dist) * I * 0.3;

    // Blue-white chromatic split
    float ab = I * 0.008;
    vec3 col;
    col.r = texture(DiffuseSampler, texCoord + vec2( ab,  ab)).r;
    col.g = texture(DiffuseSampler, texCoord              ).g;
    col.b = texture(DiffuseSampler, texCoord + vec2(-ab, -ab)).b;

    col *= scan;
    float lum = luminance(col);

    // Wash to bright white at high intensity
    col = mix(col, vec3(lum * 1.4), I * 0.55);

    // Electric blue tint
    col.b = min(1.0, col.b + I * 0.15);
    col.r = mix(col.r, col.r * 0.7, I * 0.4);

    // Burst glow
    col += vec3(0.15, 0.25, 0.5) * burst;

    // Flicker
    float flicker = 1.0 - hash(Time * 60.0) * 0.06 * I;
    col *= flicker;

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
