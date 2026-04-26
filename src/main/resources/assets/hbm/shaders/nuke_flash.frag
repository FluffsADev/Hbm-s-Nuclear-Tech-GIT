#version 120

uniform sampler2D effectTex;
uniform float effectCenterX;
uniform float effectCenterY;
uniform float effectRadius;
uniform float effectAlpha;

void main() {
	vec2 localUV = gl_TexCoord[0].xy;
	vec2 diskUV = localUV * 2.0 - 1.0;
	float diskMask = 1.0 - smoothstep(0.96, 1.02, length(diskUV));
	if (diskMask <= 0.001 || effectRadius <= 0.0001 || effectAlpha <= 0.001) {
		gl_FragColor = vec4(0.0);
		return;
	}

	vec2 centeredUV = (localUV - vec2(effectCenterX, effectCenterY)) / effectRadius;
	vec2 effectUV = centeredUV * 0.5 + 0.5;
	if (effectUV.x < 0.0 || effectUV.x > 1.0 || effectUV.y < 0.0 || effectUV.y > 1.0) {
		gl_FragColor = vec4(0.0);
		return;
	}

	vec4 effectSample = texture2D(effectTex, effectUV);
	gl_FragColor = vec4(effectSample.rgb, effectSample.a * effectAlpha * diskMask);
}
