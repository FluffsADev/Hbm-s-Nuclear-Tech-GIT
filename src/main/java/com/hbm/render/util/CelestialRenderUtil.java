package com.hbm.render.util;

import java.util.HashMap;
import java.util.Map;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.dim.trait.CBT_Weather;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class CelestialRenderUtil {

	private static final float OPAQUE_ATMOSPHERE_PRESSURE = 5.0F;
	private static final float CLOUDY_ATMOSPHERE_PRESSURE = 0.5F;
	private static final float DENSE_HAZE_PRESSURE = 3.0F;
	private static final float CLOUD_STORM_SMOOTHING_RATE = 6.0F;

	private static final Map<Integer, CloudStormFadeState> CLOUD_STORM_FADE_STATES = new HashMap<Integer, CloudStormFadeState>();

	public static final int ATMOSPHERE_STYLE_CLEAR = 0;
	public static final int ATMOSPHERE_STYLE_CLOUDS = 1;
	public static final int ATMOSPHERE_STYLE_HAZE = 2;
	public static final int ATMOSPHERE_STYLE_GAS_BANDS = 3;

	private static final class CloudStormFadeState {

		private float darkness;
		private long lastSampleTime;

		private CloudStormFadeState(float darkness, long lastSampleTime) {
			this.darkness = darkness;
			this.lastSampleTime = lastSampleTime;
		}
	}

	private CelestialRenderUtil() { }

	private static float smoothCloudStormDarkness(CelestialBody body, float targetDarkness) {
		if(body == null) {
			return targetDarkness;
		}

		long now = Minecraft.getSystemTime();
		CloudStormFadeState state = CLOUD_STORM_FADE_STATES.get(body.dimensionId);
		if(state == null) {
			CLOUD_STORM_FADE_STATES.put(body.dimensionId, new CloudStormFadeState(targetDarkness, now));
			return targetDarkness;
		}

		float deltaSeconds = MathHelper.clamp_float((now - state.lastSampleTime) / 1000.0F, 0.0F, 0.5F);
		float blend = MathHelper.clamp_float(deltaSeconds * CLOUD_STORM_SMOOTHING_RATE, 0.0F, 1.0F);
		state.darkness += (targetDarkness - state.darkness) * blend;
		state.lastSampleTime = now;

		return state.darkness;
	}

	public static float getAtmosphereGlowAlpha(CelestialBody body) {
		if(body == null) {
			return 0.0F;
		}

		if(body.gas != null) {
			return 0.35F;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere != null) {
			float pressure = MathHelper.clamp_float((float) atmosphere.getPressure(), 0.0F, 3.0F);
			if(pressure <= 0.02F) {
				return 0.0F;
			}
			return MathHelper.clamp_float(0.08F + pressure * 0.16F, 0.08F, 0.5F);
		}

		return 0.0F;
	}

	public static float getAtmosphereSurfaceAlpha(CelestialBody body) {
		return getAtmosphereDensity(body);
	}

	public static float getAtmospherePressure(CelestialBody body) {
		if(body == null) {
			return 0.0F;
		}

		if(body.gas != null) {
			return OPAQUE_ATMOSPHERE_PRESSURE;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere == null) {
			return 0.0F;
		}

		return Math.max(0.0F, (float) atmosphere.getPressure());
	}

	public static float getAtmosphereDensity(CelestialBody body) {
		return MathHelper.clamp_float(getAtmospherePressure(body) / OPAQUE_ATMOSPHERE_PRESSURE, 0.0F, 1.0F);
	}

	public static int getAtmosphereStyle(CelestialBody body) {
		if(body == null) {
			return ATMOSPHERE_STYLE_CLEAR;
		}

		if(body.gas != null) {
			return ATMOSPHERE_STYLE_GAS_BANDS;
		}

		float pressure = getAtmospherePressure(body);
		if(pressure > DENSE_HAZE_PRESSURE) {
			return ATMOSPHERE_STYLE_HAZE;
		}
		if(pressure >= CLOUDY_ATMOSPHERE_PRESSURE) {
			return ATMOSPHERE_STYLE_CLOUDS;
		}

		return ATMOSPHERE_STYLE_CLEAR;
	}

	public static Vec3 getBodyAtmosphereColor(CelestialBody body) {
		if(body == null) {
			return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
		}

		if(body.gas != null) {
			return WorldProviderCelestial.getAtmosphereFluidColor(body.gas);
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere != null && !atmosphere.fluids.isEmpty()) {
			double totalPressure = 0.0D;
			double r = 0.0D;
			double g = 0.0D;
			double b = 0.0D;

			for(FluidEntry entry : atmosphere.fluids) {
				if(entry == null || entry.fluid == null || entry.pressure <= 0.0D) {
					continue;
				}

				Vec3 fluidColor = WorldProviderCelestial.getAtmosphereFluidColor(entry.fluid);
				r += fluidColor.xCoord * entry.pressure;
				g += fluidColor.yCoord * entry.pressure;
				b += fluidColor.zCoord * entry.pressure;
				totalPressure += entry.pressure;
			}

			if(totalPressure > 0.0D) {
				return Vec3.createVectorHelper(
					MathHelper.clamp_double(r / totalPressure, 0.0D, 1.0D),
					MathHelper.clamp_double(g / totalPressure, 0.0D, 1.0D),
					MathHelper.clamp_double(b / totalPressure, 0.0D, 1.0D)
				);
			}
		}

		return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
	}

	public static Vec3 getBodyCloudColor(CelestialBody body) {
		Vec3 baseClouds = Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);

		if(body == null) {
			return baseClouds;
		}

		if(body.gas != null) {
			Vec3 gasColor = getBodyAtmosphereColor(body);
			double gasPeak = Math.max(gasColor.xCoord, Math.max(gasColor.yCoord, gasColor.zCoord));
			if(gasPeak <= 0.0D) {
				return Vec3.createVectorHelper(1.0D, 1.0D, 1.0D);
			}

			return Vec3.createVectorHelper(
				MathHelper.clamp_double(gasColor.xCoord / gasPeak, 0.0D, 1.0D),
				MathHelper.clamp_double(gasColor.yCoord / gasPeak, 0.0D, 1.0D),
				MathHelper.clamp_double(gasColor.zCoord / gasPeak, 0.0D, 1.0D)
			);
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		return WorldProviderCelestial.getTintedCloudColor(atmosphere, baseClouds);
	}

	public static float getBodyCloudTintStrength(CelestialBody body) {
		if(body == null) {
			return 0.0F;
		}

		if(body.gas != null) {
			return 1.0F;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		return WorldProviderCelestial.getCloudTintStrength(atmosphere);
	}

	public static boolean bodyHasWeatherCycle(CelestialBody body) {
		return CBT_Weather.supportsWeather(body);
	}

	public static float getBodyCloudStormDarkness(CelestialBody body, float partialTicks) {
		CBT_Weather weather = body != null ? body.getTrait(CBT_Weather.class) : null;
		float targetDarkness = 0.0F;

		if(weather != null && bodyHasWeatherCycle(body)) {
			float rainStrength = weather.getRainStrength(partialTicks);
			float thunderStrength = weather.getWeightedThunderStrength(partialTicks);
			targetDarkness = MathHelper.clamp_float(rainStrength * 0.22F + thunderStrength * 0.28F, 0.0F, 0.5F);
		}

		return smoothCloudStormDarkness(body, targetDarkness);
	}

	public static float getBodyCloudLightningStrength(CelestialBody body, float partialTicks) {
		CBT_Weather weather = body != null ? body.getTrait(CBT_Weather.class) : null;
		if(weather == null || !bodyHasWeatherCycle(body)) {
			return 0.0F;
		}

		return MathHelper.clamp_float(weather.getThunderStrength(partialTicks), 0.0F, 1.0F);
	}

	public static void renderAtmosphereGlow2D(Tessellator tessellator, CelestialBody body, double centerX, double centerY, double size, float visibility) {
		float glowAlpha = getAtmosphereGlowAlpha(body) * visibility;
		if(glowAlpha <= 0.001F) {
			return;
		}

		Vec3 atmo = getBodyAtmosphereColor(body);
		float r = MathHelper.clamp_float((float) atmo.xCoord * 1.15F, 0.0F, 1.0F);
		float g = MathHelper.clamp_float((float) atmo.yCoord * 1.15F, 0.0F, 1.0F);
		float b = MathHelper.clamp_float((float) atmo.zCoord * 1.15F, 0.0F, 1.0F);

		double radius = size * 0.5D;
		float leadingGlow = glowAlpha;
		float trailingGlow = glowAlpha;
		double innerSize = radius * 0.98D;
		double middleSize = radius * 1.075D;
		double outerSize = radius * 1.15D * (1.0D + glowAlpha * 0.25D);

		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glShadeModel(GL11.GL_SMOOTH);

		tessellator.startDrawingQuads();

		// Top band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX - outerSize, centerY - outerSize, 0.0D);
		tessellator.addVertex(centerX + outerSize, centerY - outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);

		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow);
		tessellator.addVertex(centerX + innerSize, centerY - innerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow);
		tessellator.addVertex(centerX - innerSize, centerY - innerSize, 0.0D);

		// Left band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX + outerSize, centerY - outerSize, 0.0D);
		tessellator.addVertex(centerX + outerSize, centerY + outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);

		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow);
		tessellator.addVertex(centerX + innerSize, centerY + innerSize, 0.0D);
		tessellator.addVertex(centerX + innerSize, centerY - innerSize, 0.0D);

		// Bottom band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX + outerSize, centerY + outerSize, 0.0D);
		tessellator.addVertex(centerX - outerSize, centerY + outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);

		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow);
		tessellator.addVertex(centerX - innerSize, centerY + innerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, leadingGlow);
		tessellator.addVertex(centerX + innerSize, centerY + innerSize, 0.0D);

		// Right band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX - outerSize, centerY + outerSize, 0.0D);
		tessellator.addVertex(centerX - outerSize, centerY - outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);

		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, trailingGlow);
		tessellator.addVertex(centerX - innerSize, centerY - innerSize, 0.0D);
		tessellator.addVertex(centerX - innerSize, centerY + innerSize, 0.0D);

		tessellator.draw();

		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
	}
}
