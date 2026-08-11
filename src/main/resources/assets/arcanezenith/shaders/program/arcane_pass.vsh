// Arcane Zenith - shared fullscreen quad vertex shader
// Used by gravity_lens, time_stop, heat_haze

#version 150

in vec4 Position;
in vec2 UV0;

out vec2 texCoord;

uniform vec2 OutSize;

void main() {
    gl_Position = Position;
    texCoord    = UV0;
}
