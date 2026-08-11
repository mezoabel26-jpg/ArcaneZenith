// Arcane Zenith - Heat Haze post-processing vertex shader
// Standard pass-through; displacement happens in the fragment shader.

#version 150

in vec4 Position;
in vec2 UV0;

out vec2 texCoord;
out vec2 oneTexel;

uniform vec2 OutSize;

void main() {
    gl_Position = Position;
    texCoord = UV0;
    oneTexel = 1.0 / OutSize;
}
