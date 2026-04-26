#version 120

uniform float phase;
uniform float offset;
uniform float atmosphereDensity;
uniform float atmosphereTime;
uniform float patternOffset;
uniform int atmosphereStyle;

uniform sampler2D bodyTex;
uniform sampler2D lights;
uniform sampler2D cityMask;
uniform int blackouts;
uniform int useBodyAlphaMask;

#define PI 3.1415926538
const float PIXEL_GRID = 16.0;

vec2 quantize(vec2 inp, vec2 period) {
	return floor(inp / period) * period;
}

float hash(float x) { return fract(cos(x * 124.123) * 412.0); }

float hash2(vec2 p) {
	return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	vec2 u = f * f * (3.0 - 2.0 * f);

	return mix(
		mix(hash2(i), hash2(i + vec2(1.0, 0.0)), u.x),
		mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), u.x),
		u.y
	);
}

float fbm(vec2 p) {
	float value = 0.0;
	float amplitude = 0.5;

	for (int i = 0; i < 4; i++) {
		value += noise(p) * amplitude;
		p = p * 2.02 + vec2(17.13, -11.7);
		amplitude *= 0.5;
	}

	return value;
}

vec3 sampleCityLight(vec2 sampleUV) {
	vec2 wrappedUV = fract(sampleUV);
	vec4 city = texture2D(cityMask, wrappedUV);
	vec3 lightColor = texture2D(lights, wrappedUV).rgb * city.rgb;
	return lightColor * city.a;
}

void main() {
	vec2 movingUV = gl_TexCoord[0].xy + vec2(offset, 0.0);
	vec2 wrappedUV = fract(movingUV);
	vec2 patternUV = gl_TexCoord[0].xy + vec2(patternOffset, 0.0);

	float alphaMask = 1.0;
	if (useBodyAlphaMask != 0) {
		alphaMask = texture2D(bodyTex, wrappedUV).a;
		if (alphaMask <= 0.001) {
			gl_FragColor = vec4(0.0);
			return;
		}
	}

	if (atmosphereStyle != 1 && atmosphereStyle != 2) {
		gl_FragColor = vec4(0.0);
		return;
	}

	vec2 fragCoord = quantize(movingUV, vec2(0.0625, 0.0625)) - vec2(offset, 0.0);
	vec2 uv = (2.25 * fragCoord - 1.1);
	vec2 suv = (2.0 * fragCoord - 1.0);
	vec3 light = vec3(sin(phase * PI), 0.0, cos(phase * PI));
	vec3 n = vec3(uv, sqrt(1.0 - clamp(dot(uv, uv), 0.0, 1.0)));
	float brightness = dot(n, light);

	brightness = max(brightness, (abs(phase) - 0.7) * clamp(dot(suv, suv), 0.0, 1.0));

	if (abs(phase) < 0.5) {
		if (phase < 0.0) {
			brightness = phase * 4.0 + 2.0 - uv.x;
		} else {
			brightness = -phase * 4.0 + 2.0 + uv.x;
		}
	}

	brightness = max(brightness, 0.05);

	float nightFactor = clamp(0.8 - brightness, 0.0, 1.0);
	if (nightFactor <= 0.001) {
		gl_FragColor = vec4(0.0);
		return;
	}

	float cloudMotionScale = mix(1.0, 1.5, step(0.999, atmosphereDensity));
	float motionTime = atmosphereTime * cloudMotionScale;
	vec2 texelCoord = floor(patternUV * PIXEL_GRID);
	vec2 texelFlow = vec2(motionTime * 0.18, -motionTime * 0.11);
	vec2 glowCellUV = fract((texelCoord + 0.5) / PIXEL_GRID + vec2(offset - patternOffset, 0.0));
	vec2 glowStep = vec2(1.0 / (PIXEL_GRID * 3.0), 1.0 / (PIXEL_GRID * 3.0));
	float emissiveMask = 0.0;

	if (atmosphereStyle == 2) {
		vec2 uvp = (texelCoord + 0.5) / PIXEL_GRID;
		vec2 flowDrift = texelFlow / PIXEL_GRID;
		float hazeField = fbm(uvp * 2.4 + flowDrift * 0.55);
		float hazeSheet = fbm(uvp * 4.0 + vec2(-motionTime * 0.012, motionTime * 0.01));
		float hazeMix = smoothstep(0.28, 0.88, mix(hazeField, hazeSheet, 0.4));
		emissiveMask = hazeMix * (0.38 + atmosphereDensity * 0.34);
	} else {
		vec2 cloudBase = (texelCoord + texelFlow) / vec2(7.5, 6.0);
		float largeSwirl = fbm(cloudBase * 0.75);
		float shear = fbm((texelCoord.yx + vec2(-motionTime * 0.12, motionTime * 0.09)) / vec2(8.5, 10.0));
		vec2 cloudUv = cloudBase + vec2(largeSwirl * 0.48, shear * 0.18);
		float cloudCover = clamp(0.22 + atmosphereDensity * 0.95, 0.0, 1.0);
		float cloudField = fbm(cloudUv);
		float wisps = fbm(cloudUv * 1.3 + vec2(-motionTime * 0.01, motionTime * 0.007) + vec2(cloudField, largeSwirl));
		float cloudPattern = mix(cloudField, wisps, 0.32);
		float cloudMask = smoothstep(mix(0.54, 0.4, cloudCover), mix(0.76, 0.67, cloudCover), cloudPattern);
		float cloudCoverage = smoothstep(mix(0.46, 0.31, cloudCover), mix(0.68, 0.59, cloudCover), cloudPattern + 0.06);
		float jet = 0.5 + 0.5 * sin((texelCoord.y + largeSwirl * 1.35) * 1.05 + motionTime * 0.45);
		float jetMask = smoothstep(mix(0.8, 0.65, cloudCover), mix(0.96, 0.92, cloudCover), jet)
			* smoothstep(mix(0.5, 0.38, cloudCover), mix(0.84, 0.76, cloudCover), wisps);
		float cloudPresence = max(max(cloudMask, cloudCoverage * (0.58 + cloudCover * 0.18)), jetMask * (0.46 + cloudCover * 0.18));
		float cloudCore = smoothstep(0.2, 0.82, cloudPresence);
		emissiveMask = cloudPresence * (0.34 + atmosphereDensity * 0.28) + cloudCore * (0.12 + atmosphereDensity * 0.14);
	}

	vec3 blurredLights = sampleCityLight(glowCellUV) * 0.40;
	blurredLights += sampleCityLight(glowCellUV + vec2(glowStep.x, 0.0)) * 0.17;
	blurredLights += sampleCityLight(glowCellUV - vec2(glowStep.x, 0.0)) * 0.17;
	blurredLights += sampleCityLight(glowCellUV + vec2(0.0, glowStep.y)) * 0.17;
	blurredLights += sampleCityLight(glowCellUV - vec2(0.0, glowStep.y)) * 0.17;

	float glowLuma = dot(blurredLights, vec3(0.299, 0.587, 0.114));
	float glowPresence = smoothstep(0.02, 0.24, glowLuma);
	float denseAtmosphereMute = smoothstep(0.72, 1.0, atmosphereDensity);
	vec3 warmGlow = vec3(glowLuma * 1.32, glowLuma * 1.04, glowLuma * 0.48);
	vec3 mutedGlow = vec3(glowLuma * 1.14, glowLuma * 0.98, glowLuma * 0.58);
	vec3 glowTint = mix(vec3(0.75, 0.68, 0.4), vec3(0.69, 0.66, 0.5), denseAtmosphereMute);
	vec3 glowCore = mix(warmGlow, mutedGlow, denseAtmosphereMute);
	vec3 glowColor = mix(blurredLights * glowTint, glowCore, mix(0.82, 0.76, denseAtmosphereMute));
	float glowAlpha = clamp(
		nightFactor
		* emissiveMask
		* glowPresence
		* (0.8 + atmosphereDensity * 0.45),
		0.0,
		1.0
	);

	gl_FragColor = vec4(glowColor, glowAlpha * alphaMask);

	for (int i = 0; i < blackouts; i++) {
		float bx = hash(i * 100.0 + 1.0);
		float by = hash(i * 100.0 + 2.0);

		if (gl_TexCoord[0].x > bx - 0.15 && gl_TexCoord[0].x < bx + 0.15 && gl_TexCoord[0].y > by - 0.15 && gl_TexCoord[0].y < by + 0.15) {
			gl_FragColor = vec4(0.0);
		}
	}
}
