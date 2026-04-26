#version 120

uniform float offset;
uniform sampler2D bodyTex;
uniform int useBodyAlphaMask;
uniform int nukeShockCount;
uniform float nukeShockTime[4];
uniform float nukeShockCenterX[4];
uniform float nukeShockCenterY[4];
uniform float nukeShockStrength[4];

const float FLASH_GRID = 6.0;
const int MAX_NUKE_SHOCKS = 4;

float stableHash(vec2 p) {
	return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float getNukeFlash(vec2 localUV, float time, vec2 center, float strength) {
	if (time < 0.0 || strength <= 0.001) {
		return 0.0;
	}

	vec2 flashCellCoord = floor(localUV * FLASH_GRID);
	vec2 flashCellUV = (flashCellCoord + 0.5) / FLASH_GRID;
	float distanceFromCenter = length(flashCellUV - center);
	float stableSeed = stableHash(center + vec2(strength * 0.31, strength * 0.73));
	float flashRadius = mix(0.08, 0.22, strength) * mix(0.9, 1.45, stableSeed);
	float flashEdge = mix(0.05, 0.095, strength);
	float flashFade = 1.0 - smoothstep(0.0, mix(10.0, 18.0, strength), time);
	float flashCore = 1.0 - smoothstep(flashRadius, flashRadius + flashEdge, distanceFromCenter);

	return flashCore * flashFade;
}

void main() {
	vec2 localUV = gl_TexCoord[0].xy;
	vec2 wrappedUV = fract(localUV + vec2(offset, 0.0));
	float alphaMask = 1.0;

	if (useBodyAlphaMask != 0) {
		alphaMask = texture2D(bodyTex, wrappedUV).a;
		if (alphaMask <= 0.001) {
			gl_FragColor = vec4(0.0);
			return;
		}
	} else {
		vec2 diskUV = localUV * 2.0 - 1.0;
		float diskMask = 1.0 - smoothstep(0.96, 1.02, length(diskUV));
		if (diskMask <= 0.001) {
			gl_FragColor = vec4(0.0);
			return;
		}
		alphaMask = diskMask;
	}

	float flashAlpha = 0.0;
	for (int i = 0; i < MAX_NUKE_SHOCKS; i++) {
		if (i < nukeShockCount) {
			float shockStrength = clamp(nukeShockStrength[i], 0.0, 1.0);
			float flash = getNukeFlash(localUV, nukeShockTime[i], vec2(nukeShockCenterX[i], nukeShockCenterY[i]), shockStrength);
			flashAlpha = max(flashAlpha, flash);
		}
	}

	gl_FragColor = vec4(vec3(1.0), clamp(flashAlpha * alphaMask, 0.0, 1.0));
}
