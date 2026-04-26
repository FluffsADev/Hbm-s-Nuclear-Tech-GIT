#version 120

uniform float offset;

uniform sampler2D bodyTex;
uniform int useBodyAlphaMask;
uniform float atmosphereColorR;
uniform float atmosphereColorG;
uniform float atmosphereColorB;
uniform float cloudColorR;
uniform float cloudColorG;
uniform float cloudColorB;
uniform float cloudTintStrength;
uniform float cloudStormDarkness;
uniform float cloudLightningStrength;
uniform float atmosphereAlpha;
uniform float atmosphereTime;
uniform float patternOffset;
uniform int atmosphereStyle;

const float PIXEL_GRID = 16.0;

float hash(vec2 p) {
	return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	vec2 u = f * f * (3.0 - 2.0 * f);

	return mix(
		mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
		mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
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

void main() {
	vec2 movingUV = gl_TexCoord[0].xy + vec2(offset, 0);
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

	float density = clamp(atmosphereAlpha, 0.0, 1.0);
	vec3 baseColor = vec3(atmosphereColorR, atmosphereColorG, atmosphereColorB);
	vec3 cloudTint = vec3(cloudColorR, cloudColorG, cloudColorB);
	float tintStrength = clamp(cloudTintStrength, 0.0, 1.0);
	float stormDarkness = clamp(cloudStormDarkness, 0.0, 1.0);
	float lightningStrength = clamp(cloudLightningStrength, 0.0, 1.0);
	vec2 texelCoord = floor(patternUV * PIXEL_GRID);
	vec2 uv = (texelCoord + 0.5) / PIXEL_GRID;
	vec2 texelFlow = vec2(atmosphereTime * 0.18, -atmosphereTime * 0.11);
	vec2 flowDrift = texelFlow / PIXEL_GRID;
	vec3 layeredColor = baseColor;
	float alphaBoost = 0.9;
	float overlayAlpha = atmosphereAlpha * alphaBoost;

	if (atmosphereStyle == 3) {
		float bandWarp = fbm(uv * vec2(2.5, 5.5) + vec2(atmosphereTime * 0.005, -atmosphereTime * 0.0025));
		float fineWarp = fbm(uv * vec2(5.0, 11.0) + vec2(-atmosphereTime * 0.007, atmosphereTime * 0.0035));
		float bands = 0.5 + 0.5 * sin((uv.y * PIXEL_GRID + bandWarp * 3.5) * 1.8 + atmosphereTime * 0.22);
		float thinBands = 0.5 + 0.5 * sin((uv.y * PIXEL_GRID + fineWarp * 5.0) * 4.6 - atmosphereTime * 0.11);
		float storm = smoothstep(0.64, 0.9, fbm(uv * vec2(6.0, 3.0) + vec2(atmosphereTime * 0.0035, -atmosphereTime * 0.006)));

		vec3 bandDark = baseColor * 0.74;
		vec3 bandLight = min(baseColor * 1.28 + vec3(0.08), vec3(1.0));
		layeredColor = mix(bandDark, bandLight, bands);
		layeredColor = mix(layeredColor, bandLight, thinBands * 0.25);
		layeredColor = mix(layeredColor, bandDark * 0.9, storm * 0.18);
		alphaBoost = 0.96 + storm * 0.04;
		overlayAlpha = atmosphereAlpha * alphaBoost;
	} else if (atmosphereStyle == 2) {
		float hazeField = fbm(uv * 2.4 + flowDrift * 0.55);
		float hazeSheet = fbm(uv * 4.0 + vec2(-atmosphereTime * 0.012, atmosphereTime * 0.01));
		float turbulence = noise(uv * 10.0 + vec2(atmosphereTime * 0.018, -atmosphereTime * 0.014));
		float hazeMix = smoothstep(0.28, 0.88, mix(hazeField, hazeSheet, 0.4));

		vec3 deepHaze = baseColor * mix(0.68, 0.55, density);
		vec3 brightHaze = min(baseColor * (1.08 + density * 0.18) + vec3(0.06), vec3(1.0));
		layeredColor = mix(deepHaze, brightHaze, hazeMix * 0.42 + turbulence * 0.16);
		alphaBoost = 0.94 + hazeMix * 0.06;
		overlayAlpha = atmosphereAlpha * alphaBoost;
	} else if (atmosphereStyle == 1) {
		vec2 cloudBase = (texelCoord + texelFlow) / vec2(7.5, 6.0);
		float largeSwirl = fbm(cloudBase * 0.75);
		float shear = fbm((texelCoord.yx + vec2(-atmosphereTime * 0.12, atmosphereTime * 0.09)) / vec2(8.5, 10.0));
		vec2 cloudUv = cloudBase + vec2(largeSwirl * 0.48, shear * 0.18);

		float cloudField = fbm(cloudUv);
		float wisps = fbm(cloudUv * 1.3 + vec2(-atmosphereTime * 0.01, atmosphereTime * 0.007) + vec2(cloudField, largeSwirl));
		float cloudPattern = mix(cloudField, wisps, 0.32);
		float cloudMask = smoothstep(0.4, 0.67, cloudPattern);
		float cloudCoverage = smoothstep(0.31, 0.59, cloudPattern + 0.06);
		float jet = 0.5 + 0.5 * sin((texelCoord.y + largeSwirl * 1.35) * 1.05 + atmosphereTime * 0.45);
		float jetMask = smoothstep(0.65, 0.92, jet) * smoothstep(0.38, 0.76, wisps);
		float turbulence = noise((texelCoord + vec2(atmosphereTime * 0.16, -atmosphereTime * 0.12)) / 2.75);
		float cloudPresence = max(max(cloudMask, cloudCoverage * 0.82), jetMask * 0.72);
		float stormMask = smoothstep(0.24, 0.78, cloudPresence);
		float stormShade = mix(1.0, 0.22, stormDarkness);

		vec3 shadowColor = baseColor * mix(0.72, 0.55, density);
		vec3 cloudColor = min(cloudTint * (1.18 + density * 0.18) + vec3(0.04 + density * 0.04), vec3(1.0));
		cloudColor *= mix(1.0, stormShade, 0.85);
		vec3 stormCloudColor = mix(cloudColor, vec3(0.22, 0.22, 0.24), 0.52 + stormDarkness * 0.28);
		vec3 airColor = mix(shadowColor, baseColor, 0.35 + turbulence * 0.3);
		layeredColor = mix(airColor, cloudColor, cloudMask * (0.96 + density * 0.3));
		layeredColor = mix(layeredColor, cloudColor, cloudCoverage * (0.48 + density * 0.2));
		layeredColor = mix(layeredColor, cloudColor, jetMask * (0.5 + density * 0.24));
		layeredColor = mix(layeredColor, stormCloudColor, stormMask * (0.84 + stormDarkness * 0.16));

		if (lightningStrength > 0.001) {
			float burstWindow = floor(atmosphereTime * 0.85 + patternOffset * 7.0);
			float burstSeed = hash(vec2(burstWindow, 23.17));
			float burstPhase = fract(atmosphereTime * 0.85 + burstSeed * 0.37);
			float burstGate = step(0.84, burstSeed) * smoothstep(0.28, 0.82, lightningStrength);
			float primaryFlash = smoothstep(0.0, 0.02, burstPhase) * (1.0 - smoothstep(0.02, 0.09, burstPhase));
			float secondaryFlash = smoothstep(0.11, 0.14, burstPhase) * (1.0 - smoothstep(0.14, 0.22, burstPhase));
			float flashPulse = burstGate * (primaryFlash + secondaryFlash * 0.65);
			float lightningPatch = hash(floor(texelCoord / 3.0) + vec2(burstWindow * 1.9, 41.3));
			float lightningMask = smoothstep(0.42, 0.82, cloudPresence) * smoothstep(0.7, 0.92, lightningPatch + cloudPattern * 0.4);
			float lightningMix = flashPulse * lightningMask * (0.72 + lightningStrength * 0.28);
			layeredColor = mix(layeredColor, vec3(1.0), lightningMix);
		}

		alphaBoost = 0.98 + cloudPresence * 0.64;
		overlayAlpha = max(atmosphereAlpha * alphaBoost, cloudPresence * (0.56 + density * 1.0 + stormDarkness * 0.22));
	} else {
		float shimmer = noise(uv * 8.0 + vec2(atmosphereTime * 0.015, -atmosphereTime * 0.011));
		vec3 tintLow = baseColor * 0.82;
		vec3 tintHigh = min(baseColor * 1.05 + vec3(0.02), vec3(1.0));
		layeredColor = mix(tintLow, tintHigh, shimmer * 0.35);
		alphaBoost = 0.88;
		overlayAlpha = atmosphereAlpha * alphaBoost;
	}

	float finalAlpha = clamp(overlayAlpha * alphaMask, 0.0, 1.0);

	gl_FragColor = vec4(layeredColor, finalAlpha);
}
