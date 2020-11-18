#version 460 core

layout (location = 0) out vec4 FragColor;

layout (std140, binding = 0) uniform OurUniforms
{
  vec4 ourColor;
  vec4 test;
  vec3 x;
} ourUniforms;

void main()
{
  FragColor = ourUniforms.ourColor + vec4(ourUniforms.x, 1.0);
}
