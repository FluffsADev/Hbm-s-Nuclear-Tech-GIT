#version 120

uniform float offset;

uniform sampler2D bodyTex;
uniform int useBodyAlphaMask;
uniform float atmosphereColorR;
uniform float atmosphereColorG;
uniform float atmosphereColorB;
uniform float atmosphereAlpha;

vec2 quantize(vec2 inp, vec2 period) {
	return floor(inp / period) * period;
}

void main() {
	vec2 movingUV = gl_TexCoord[0].xy + vec2(offset, 0);

	float alphaMask = 1.0;
	if (useBodyAlphaMask != 0) {
		alphaMask = texture2D(bodyTex, movingUV).a;
		if (alphaMask <= 0.001) {
			gl_FragColor = vec4(0.0);
			return;
		}
	}

	vec2 fragCoord = quantize(movingUV, vec2(0.0625, 0.0625)) - vec2(offset, 0);
	vec2 suv = (2.0 * fragCoord - 1.0);
	float bodyDist = clamp(dot(suv, suv), 0.0, 1.0);
	float bodyMask = 1.0 - smoothstep(0.92, 1.0, bodyDist);
	float rimMask = smoothstep(0.2, 0.95, bodyDist);
	float overlayMask = bodyMask * mix(0.72, 1.0, rimMask);

	gl_FragColor = vec4(atmosphereColorR, atmosphereColorG, atmosphereColorB, atmosphereAlpha * overlayMask * alphaMask);
}
