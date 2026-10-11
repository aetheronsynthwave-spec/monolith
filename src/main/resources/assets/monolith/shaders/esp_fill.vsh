#version 330

// Monolith ESP fill - vertex stage. Plain coloured geometry (the UV attribute exists only because the
// pipeline reuses the same vertex format as the glow particles, so it is read but unused).

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color + vec4(UV0 * 0.0, 0.0, 0.0);
}
