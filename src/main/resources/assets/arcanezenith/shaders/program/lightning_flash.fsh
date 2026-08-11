// Arcane Zenith — Lightning Flash fragment shader
// Fired every time a chain-lightning bolt strikes.
// FlashStrength: 0=no flash, 1=full white-out (decays instantly)
// EdgeDark: darkens screen edges slightly to simulate lightning illuminating only the center

#version 150

uniform sampler2D DiffuseSampler;
uniform float FlashStrength;
uniform float EdgeDark;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Vignette: darken edges before the flash to amplify contrast
    vec2  uv      = texCoord - 0.5;
    float vignette = 1.0 - dot(uv, uv) * EdgeDark * 4.0;
    color.rgb *= clamp(vignette, 0.0, 1.0);

    // White flash: additive blend toward (1,1,1)
    // Strong cyan tint on the flash (lightning color)
    vec3 flashColor = vec3(0.9, 0.95, 1.0);
    color.rgb = mix(color.rgb, flashColor, FlashStrength);

    // Slight blue-white oversaturation at peak
    float lum = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    color.rgb += vec3(0.0, 0.05, 0.15) * FlashStrength * (1.0 - lum);

    fragColor = color;
}
