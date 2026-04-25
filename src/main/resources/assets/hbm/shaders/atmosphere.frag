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
		vec2 cloudBase = (texelCoord + texelFlow) / vec2(5.5, 4.5);
		float largeSwirl = fbm(cloudBase * 0.9);
		float shear = fbm((texelCoord.yx + vec2(-atmosphereTime * 0.12, atmosphereTime * 0.09)) / vec2(6.0, 8.0));
		vec2 cloudUv = cloudBase + vec2(largeSwirl * 0.55, shear * 0.25);

		float cloudField = fbm(cloudUv);
		float wisps = fbm(cloudUv * 1.6 + vec2(-atmosphereTime * 0.01, atmosphereTime * 0.007) + vec2(cloudField, largeSwirl));
		float cloudMask = smoothstep(0.44, 0.72, mix(cloudField, wisps, 0.32));
		float jet = 0.5 + 0.5 * sin((texelCoord.y + largeSwirl * 1.35) * 1.05 + atmosphereTime * 0.45);
		float jetMask = smoothstep(0.7, 0.96, jet) * smoothstep(0.42, 0.84, wisps);
		float turbulence = noise((texelCoord + vec2(atmosphereTime * 0.16, -atmosphereTime * 0.12)) / 2.75);
		float cloudPresence = max(cloudMask, jetMask * 0.75);

		vec3 shadowColor = baseColor * mix(0.72, 0.55, density);
		vec3 cloudColor = min(cloudTint * (1.18 + density * 0.18) + vec3(0.04 + density * 0.04), vec3(1.0));
		cloudColor *= mix(1.0, 0.82, tintStrength);
		cloudColor *= (1.0 - stormDarkness);
		vec3 airColor = mix(shadowColor, baseColor, 0.35 + turbulence * 0.3);
		layeredColor = mix(airColor, cloudColor, cloudMask * (0.92 + density * 0.28));
		layeredColor = mix(layeredColor, cloudColor, jetMask * (0.42 + density * 0.2));
		alphaBoost = 0.96 + cloudPresence * 0.58;
		overlayAlpha = max(atmosphereAlpha * alphaBoost, cloudPresence * (0.42 + density * 0.95));
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
