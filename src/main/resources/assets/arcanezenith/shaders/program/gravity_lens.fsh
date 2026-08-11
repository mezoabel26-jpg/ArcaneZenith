// Arcane Zenith — Gravitational Lens (UPGRADED)
// Much stronger black hole effect: deeper event horizon, wider accretion glow,
// particle lensing around the ring, radial darkening outside.
#version 150

uniform sampler2D DiffuseSampler;
uniform vec2  Center;
uniform float Strength;
uniform float EventRadius;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2  delta = texCoord - Center;
    float dist  = length(delta);

    // Event horizon — pure void black with subtle blue rim
    if (dist < EventRadius * 0.85) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    if (dist < EventRadius) {
        float t = (dist - EventRadius*0.85) / (EventRadius*0.15);
        fragColor = vec4(vec3(0.0,0.0,0.12)*t, 1.0);
        return;
    }

    // Inverse-square displacement — stronger than before
    float falloff   = (Strength * 1.4) / (dist * dist * 2.0 + 0.0008);
    vec2  displaced = texCoord - normalize(delta) * falloff;
    displaced       = clamp(displaced, 0.001, 0.999);
    vec4 color = texture(DiffuseSampler, displaced);

    // Rotating accretion disk — 2 rings
    float ringA = smoothstep(EventRadius+0.002, EventRadius, dist)
                * smoothstep(EventRadius, EventRadius+0.022, dist);
    float ringB = smoothstep(EventRadius+0.025, EventRadius+0.018, dist)
                * smoothstep(EventRadius+0.040, EventRadius+0.025, dist);
    // Rotate color based on angle + time
    float ang = atan(delta.y, delta.x) + Time * 0.8;
    float diskPulse = 0.65 + 0.35 * sin(ang * 3.0);

    color.rgb += vec3(0.55, 0.12, 1.0)  * ringA * 3.0 * diskPulse;
    color.rgb += vec3(0.20, 0.04, 0.45) * ringB * 1.8;

    // Radial darkening outside the black hole (cosmic shadow)
    float outerDark = smoothstep(0.4, 0.0, dist - EventRadius);
    color.rgb *= 1.0 - outerDark * 0.35;

    // Subtle blue-shift near event horizon (gravitational blueshift)
    float blueshift = smoothstep(0.1, EventRadius*2.0, dist);
    color.b += (1.0 - blueshift) * 0.15;

    fragColor = color;
}
