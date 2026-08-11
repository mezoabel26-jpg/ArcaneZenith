// Arcane Zenith — Void Rift Shader
// Reality tears open: screen splits into overlapping copies with displacement,
// dark purple chromatic fringing, reality-crack scanlines.
#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;   // 0=off, 1=full tear
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float hash(float n) { return fract(sin(n) * 43758.5453); }

void main() {
    float I = clamp(Intensity, 0.0, 1.0);

    // Reality fracture: displaced sample 1 and 2
    float tear    = I * 0.04;
    float scanline= mod(texCoord.y * 480.0 + Time * 8.0, 6.0);
    float crack   = step(5.8, scanline) * 0.5;

    // Horizontal tear displacement
    float offset1 = sin(texCoord.y * 12.0 + Time * 4.0) * tear;
    float offset2 = cos(texCoord.y *  8.0 - Time * 3.0) * tear * 0.7;

    vec4 c1 = texture(DiffuseSampler, vec2(texCoord.x + offset1, texCoord.y));
    vec4 c2 = texture(DiffuseSampler, vec2(texCoord.x - offset2, texCoord.y));
    vec4 base = texture(DiffuseSampler, texCoord);

    // Blend tears
    vec4 col = mix(base, mix(c1, c2, 0.5), I * 0.7);

    // Purple fringing on edges of tear
    col.r = mix(col.r, c1.r * 0.5, I * 0.4);
    col.b = mix(col.b, c2.b * 1.4, I * 0.5);

    // Dark crack scanlines
    col.rgb *= 1.0 - crack * I;

    // Void darkening at center
    vec2 d = texCoord - 0.5;
    float voidPull = smoothstep(0.3, 0.0, length(d)) * I * 0.5;
    col.rgb *= 1.0 - voidPull;
    col.rgb += vec3(0.05, 0.0, 0.12) * voidPull; // purple glow

    fragColor = col;
}
