// Arcane Zenith — Blood Curse Shader
// Screen bleeds crimson at the edges, pulsing heartbeat-like,
// green death pulse for Avada Curse, red crush for Crimson Bands.
#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;
uniform float GreenPulse;  // 1.0 = Avada green flash, 0 = crimson mode

in  vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) { return dot(c, vec3(0.2126,0.7152,0.0722)); }

void main() {
    vec4 col = texture(DiffuseSampler, texCoord);
    float I  = clamp(Intensity, 0.0, 1.0);
    float lum = luminance(col.rgb);

    // Edge bleed — crimson/green ooze from corners
    vec2  d     = abs(texCoord - 0.5) * 2.0;
    float edge  = pow(max(d.x, d.y), 3.0);
    float pulse = 0.5 + 0.5 * sin(Time * 6.0); // heartbeat ~60bpm

    vec3 bleedColor = mix(
        vec3(0.7, 0.02, 0.02),  // crimson
        vec3(0.05, 0.9, 0.05),  // Avada green
        GreenPulse
    );

    col.rgb = mix(col.rgb, bleedColor, edge * I * (0.6 + pulse * 0.4));

    // Global tint: everything gets slightly blood-red / sickly green
    col.rgb += bleedColor * I * 0.08;

    // High contrast — darken darks further
    col.rgb = mix(col.rgb, col.rgb * col.rgb, I * 0.4);

    fragColor = col;
}
