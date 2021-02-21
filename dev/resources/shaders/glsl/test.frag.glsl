#version 460 core

layout (location = 0) out vec4 FragColor;

layout (std140, binding = 0) uniform Inputs
{
  vec4 color;
} inputs;

void main()
{
  FragColor = vec4(1.0, 1.0, inputs.color.g, 1.0);
}
