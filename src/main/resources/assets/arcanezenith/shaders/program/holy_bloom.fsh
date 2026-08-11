// Arcane Zenith — Holy Bloom fragment shader
// Soft golden luminance bloom used by Angel's Help and Judgment of Heaven.
// BloomStrength: 0=off, 1=full angelic glow
// GoldTint: 0=white, 1=deep gold overlay
// Time: used for subtle shimmer/pulsing

#version 150

uniform sampler2D DiffuseSampler;
uniform float BloomStrength;
uniform float GoldTint;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Extract bright areas for bloom (threshold 0.6 luminance)
    float lum    = luminance(color.rgb);
    float bright = max(0.0, lum - 0.6) / 0.4; // 0..1 above threshold

    // Simple box blur approximation: sample 4 neighbors at bloom spread
    float spread = 0.004 * BloomStrength;
    vec3 bloomSample =
        texture(DiffuseSampler, texCoord + vec2( spread,  0.0   )).rgb * 0.25 +
        texture(DiffuseSampler, texCoord + vec2(-spread,  0.0   )).rgb * 0.25 +
        texture(DiffuseSampler, texCoord + vec2( 0.0,     spread)).rgb * 0.25 +
        texture(DiffuseSampler, texCoord + vec2( 0.0,    -spread)).rgb * 0.25;

    // Golden tint: warm up the bloom color
    vec3 goldColor = vec3(1.0, 0.88, 0.4);
    vec3 bloom     = mix(bloomSample, bloomSample * goldColor, GoldTint);

    // Subtle shimmer ripple
    float shimmer = 1.0 + 0.06 * sin(Time * 3.0 + lum * 8.0);

    // Additive bloom layer: only bright pixels get the halo
    color.rgb += bloom * BloomStrength * bright * 1.6 * shimmer;

    // Very slight overall warm lift during peak
    color.rgb += vec3(0.04, 0.03, 0.0) * GoldTint * BloomStrength;

    // Prevent overexposure
    color.rgb = min(color.rgb, vec3(1.0));

    fragColor = color;
}
