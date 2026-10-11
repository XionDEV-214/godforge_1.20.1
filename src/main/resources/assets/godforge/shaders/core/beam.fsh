#version 150

uniform sampler2D Sampler0;
uniform vec3 BeamColor;
uniform vec3 CameraPos;
uniform float Intensity;

in vec2 texCoord0;
in vec2 localPos;
in float segTransparency;

out vec4 fragColor;

float luminanceOf(vec3 c) {
    return 0.2126 * c.r + 0.7152 * c.g + 0.0722 * c.b;
}

void main() {
    float effect = dot(normalize(CameraPos.xy), normalize(localPos));
    float transparency = max((effect - 0.5) * 2.0, 0.0);
    transparency = pow(transparency, Intensity);

    vec4 texColor = texture(Sampler0, texCoord0);
    float luminance = 1.0 - luminanceOf(texColor.rgb);
    luminance = mix(luminance, 1.0, 1.0 - pow(transparency, 6.0));
    float alpha = clamp(transparency * segTransparency * luminance, 0.0, 1.0);
    fragColor = vec4(BeamColor, alpha);
}
