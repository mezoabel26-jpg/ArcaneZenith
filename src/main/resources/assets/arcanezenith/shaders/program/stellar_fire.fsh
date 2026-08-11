// Arcane Zenith — Stellar Fire Shader
// Screen erupts in heat: orange-red desaturation of cool colors,
// rim lighting from below (as if standing next to lava), fire shimmer.
#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) { return dot(c, vec3(0.2126,0.7152,0.0722)); }

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 74.9);
    return fract(p.x * p.y);
}

void main() {
    float I = clamp(Intensity, 0.0, 1.0);

    // Heat shimmer — vertical UV wobble
    float shimmerY = texCoord.y * 24.0 - Time * 6.0;
    float shimmer  = sin(shimmerY + sin(texCoord.x * 8.0 + Time)) * 0.003 * I;
    vec4 col = texture(DiffuseSampler, texCoord + vec2(shimmer, 0.0));

    float lum = luminance(col.rgb);

    // Cool colors (blue/green) get crushed and warmed
    float coolness = clamp(col.b - col.r, 0.0, 1.0);
    col.r = mix(col.r, col.r + coolness * 0.4, I * 0.8);
    col.b = mix(col.b, col.b * 0.2,             I * 0.8);
    col.g = mix(col.g, col.g * 0.6,             I * 0.5);

    // Bottom-of-screen lava light (orange glow from below)
    float bottomGlow = smoothstep(0.5, 0.0, texCoord.y) * I;
    col.rgb += vec3(0.25, 0.10, 0.0) * bottomGlow;

    // Noise grain for heat distortion look
    float grain = (hash(texCoord * 200.0 + Time) - 0.5) * 0.04 * I;
    col.rgb += grain;

    // Contrast boost (fire pops out)
    col.rgb = mix(col.rgb, smoothstep(0.0, 1.0, col.rgb), I * 0.3);

    fragColor = vec4(clamp(col.rgb, 0.0, 1.0), col.a);
}
