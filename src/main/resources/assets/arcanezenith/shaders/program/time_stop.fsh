// Arcane Zenith — Time Stop (UPGRADED)
// Deeper desaturation, cold blue-teal shift, subtle frozen-world scanlines,
// chromatic aberration pulse on unfreeze.
#version 150

uniform sampler2D DiffuseSampler;
uniform float Desaturation;
uniform float Aberration;

in  vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 c) { return dot(c, vec3(0.2126,0.7152,0.0722)); }

void main() {
    float ab = Aberration * 0.010;
    vec4 rS  = texture(DiffuseSampler, texCoord + vec2( ab*1.5, 0.0));
    vec4 gS  = texture(DiffuseSampler, texCoord);
    vec4 bS  = texture(DiffuseSampler, texCoord + vec2(-ab,     0.0));
    vec4 col = vec4(rS.r, gS.g, bS.b, gS.a);

    float lum = luminance(col.rgb);

    // Magic color detection — more generous
    float isCyan    = smoothstep(0.2, 0.55, col.b*0.7 + col.g*0.5 - col.r*1.2);
    float isCrimson = smoothstep(0.2, 0.55, col.r - col.g*0.8 - col.b*0.8);
    float isGold    = smoothstep(0.3, 0.65, col.r*0.7 + col.g*0.5 - col.b*1.2);
    float keepColor = max(max(isCyan, isCrimson), isGold);

    // Frozen gray — colder and darker than normal
    vec3 frozenGray = vec3(lum * 0.45, lum * 0.50, lum * 0.70);

    // Frozen scanlines (subtle, like ice crystal refraction)
    float scanline = 1.0 - step(0.9, fract(texCoord.y * 180.0)) * 0.07 * Desaturation;

    vec3 result = mix(col.rgb, frozenGray, Desaturation * (1.0 - keepColor));
    result *= scanline;

    // Overall blue-teal push during time stop
    result = mix(result, result * vec3(0.7, 0.85, 1.1), Desaturation * 0.35);

    fragColor = vec4(result, col.a);
}
