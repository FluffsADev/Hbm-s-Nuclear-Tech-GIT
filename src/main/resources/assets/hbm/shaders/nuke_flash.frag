#version 120

uniform int nukeShockCount;
uniform float nukeShockTime[4];
uniform float nukeShockCenterX[4];
uniform float nukeShockCenterY[4];
uniform float nukeShockStrength[4];

const float FLASH_GRID = 16.0;
const float RING_GRID = 64.0;
const int MAX_NUKE_SHOCKS = 4;

float stableHash(vec2 p) {
	return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float getFlashFade(float time, float strength) {
	return 1.0 - smoothstep(0.0, mix(4.0, 6.0, strength), time);
}

float getFlashMask(vec2 pixelUV, float time, vec2 center, float strength) {
	if (time < 0.0 || strength <= 0.001) {
		return 0.0;
	}

	float flashFade = getFlashFade(time, strength);
	float distanceFromCenter = length(pixelUV - center);
	float flashRadius = mix(0.012, 0.018, strength);
	float flashEdge = 0.02;
	return (1.0 - smoothstep(flashRadius, flashRadius + flashEdge, distanceFromCenter)) * flashFade;
}

float getAfterglowMask(vec2 pixelUV, float time, vec2 center, float strength) {
	if (time < 0.0 || strength <= 0.001) {
		return 0.0;
	}

	float warmRise = smoothstep(1.0, 4.0, time);
	float warmFade = 1.0 - smoothstep(8.0, mix(18.0, 30.0, strength), time);
	float distanceFromCenter = length(pixelUV - center);
	float glowRadius = mix(0.022, 0.038, strength);
	float glowEdge = 0.028;
	float glowMask = 1.0 - smoothstep(glowRadius, glowRadius + glowEdge, distanceFromCenter);
	return glowMask * warmRise * warmFade;
}

float getRingMask(vec2 pixelUV, float time, vec2 center, float strength) {
	if (time < 0.0 || strength <= 0.001) {
		return 0.0;
	}

	float stableSeed = stableHash(center + vec2(strength * 0.31, strength * 0.73));
	float coreRadius = mix(0.09, 0.16, strength) * mix(0.92, 1.28, stableSeed);
	float ringRadius = coreRadius * 0.92 + time * mix(0.00085, 0.00135, strength) * mix(0.92, 1.15, stableSeed);
	float ringProgress = clamp(time / mix(72.0, 120.0, strength), 0.0, 1.0);
	float ringWidth = mix(0.07, 0.032, ringProgress) * mix(0.95, 1.12, stableSeed);
	float ringReveal = smoothstep(1.5, 4.5, time);
	float ringFade = 1.0 - smoothstep(8.0, mix(68.0, 104.0, strength), time);
	float flashFade = getFlashFade(time, strength);
	float distanceFromCenter = length(pixelUV - center);
	float outerBand = smoothstep(max(ringRadius - ringWidth, 0.0), ringRadius, distanceFromCenter);
	float innerBand = 1.0 - smoothstep(ringRadius, ringRadius + ringWidth, distanceFromCenter);
	float ringMask = outerBand * innerBand;
	return ringMask * ringReveal * ringFade * (1.0 - flashFade * 0.9);
}

void main() {
	vec2 localUV = gl_TexCoord[0].xy;
	vec2 diskUV = localUV * 2.0 - 1.0;
	float diskMask = 1.0 - smoothstep(0.96, 1.02, length(diskUV));
	if (diskMask <= 0.001) {
		gl_FragColor = vec4(0.0);
		return;
	}

	vec2 flashPixelCoord = floor(localUV * FLASH_GRID);
	vec2 flashPixelUV = (flashPixelCoord + 0.5) / FLASH_GRID;
	vec2 ringPixelCoord = floor(localUV * RING_GRID);
	vec2 ringPixelUV = (ringPixelCoord + 0.5) / RING_GRID;
	float flashAlpha = 0.0;
	float afterglowAlpha = 0.0;
	float ringAlpha = 0.0;

	for (int i = 0; i < MAX_NUKE_SHOCKS; i++) {
		if (i < nukeShockCount) {
			float shockStrength = clamp(nukeShockStrength[i], 0.0, 1.0);
			vec2 shockCenter = vec2(nukeShockCenterX[i], nukeShockCenterY[i]);
			float shockTime = nukeShockTime[i];
			flashAlpha = max(flashAlpha, getFlashMask(flashPixelUV, shockTime, shockCenter, shockStrength));
			afterglowAlpha = max(afterglowAlpha, getAfterglowMask(flashPixelUV, shockTime, shockCenter, shockStrength));
			ringAlpha = max(ringAlpha, getRingMask(ringPixelUV, shockTime, shockCenter, shockStrength));
		}
	}

	float finalAlpha = clamp(max(max(flashAlpha, afterglowAlpha), ringAlpha * 0.95) * diskMask, 0.0, 1.0);
	vec3 ringColor = vec3(1.0, 0.98, 0.92);
	vec3 flashColor = vec3(1.0);
	vec3 afterglowWarm = vec3(1.0, 0.72, 0.38);
	vec3 afterglowAsh = vec3(0.62, 0.62, 0.6);
	float afterglowToAsh = clamp(afterglowAlpha > 0.0 ? smoothstep(0.18, 0.75, afterglowAlpha) : 0.0, 0.0, 1.0);
	vec3 afterglowColor = mix(afterglowAsh, afterglowWarm, afterglowToAsh);

	vec3 accumColor = ringColor * ringAlpha * 0.95;
	accumColor += afterglowColor * afterglowAlpha * 0.9;
	accumColor += flashColor * flashAlpha;
	vec3 finalColor = finalAlpha > 0.001 ? clamp(accumColor / max(finalAlpha, 0.001), 0.0, 1.0) : vec3(0.0);

	gl_FragColor = vec4(finalColor, finalAlpha);
}
