package com.hbm.render.util;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.WorldProviderCelestial;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class CelestialRenderUtil {

	private CelestialRenderUtil() { }

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
		return MathHelper.clamp_float(getAtmosphereGlowAlpha(body) * 0.35F, 0.0F, 0.16F);
	}

	public static float getAtmosphereDensity(CelestialBody body) {
		if(body == null) {
			return 0.0F;
		}

		if(body.gas != null) {
			return 1.0F;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		if(atmosphere == null) {
			return 0.0F;
		}

		return MathHelper.clamp_float((float) atmosphere.getPressure() / 2.5F, 0.0F, 1.0F);
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
		double innerSize = radius * 0.98D;
		double middleSize = radius * 1.075D;
		double outerSize = radius * 1.15D * (1.0D + glowAlpha * 0.25D);

		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);

		tessellator.startDrawingQuads();

		// Top band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX - outerSize, centerY - outerSize, 0.0D);
		tessellator.addVertex(centerX + outerSize, centerY - outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);

		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha);
		tessellator.addVertex(centerX + innerSize, centerY - innerSize, 0.0D);
		tessellator.addVertex(centerX - innerSize, centerY - innerSize, 0.0D);

		// Left band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX + outerSize, centerY - outerSize, 0.0D);
		tessellator.addVertex(centerX + outerSize, centerY + outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);

		tessellator.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha);
		tessellator.addVertex(centerX + innerSize, centerY + innerSize, 0.0D);
		tessellator.addVertex(centerX + innerSize, centerY - innerSize, 0.0D);

		// Bottom band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX + outerSize, centerY + outerSize, 0.0D);
		tessellator.addVertex(centerX - outerSize, centerY + outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);

		tessellator.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha);
		tessellator.addVertex(centerX - innerSize, centerY + innerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha);
		tessellator.addVertex(centerX + innerSize, centerY + innerSize, 0.0D);

		// Right band
		tessellator.setColorRGBA_F(r, g, b, 0.0F);
		tessellator.addVertex(centerX - outerSize, centerY + outerSize, 0.0D);
		tessellator.addVertex(centerX - outerSize, centerY - outerSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha / 2.0F);
		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);

		tessellator.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
		tessellator.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
		tessellator.setColorRGBA_F(r, g, b, glowAlpha);
		tessellator.addVertex(centerX - innerSize, centerY - innerSize, 0.0D);
		tessellator.addVertex(centerX - innerSize, centerY + innerSize, 0.0D);

		tessellator.draw();

		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
	}
}
