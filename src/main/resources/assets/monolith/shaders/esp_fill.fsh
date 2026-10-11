#version 330

// Monolith ESP fill - fragment stage. Flat translucent colour.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vertexColor;
out vec4 fragColor;

void main() {
    if (vertexColor.a <= 0.004) {
        discard;
    }
    fragColor = vertexColor * ColorModulator;
}
