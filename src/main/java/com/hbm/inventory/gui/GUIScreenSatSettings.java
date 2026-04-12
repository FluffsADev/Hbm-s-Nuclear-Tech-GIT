package com.hbm.inventory.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.trait.CBT_Impact;
import com.hbm.dim.trait.CBT_Lights;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTItemControlPacket;
import com.hbm.render.shader.Shader;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteFoeq;
import com.hbm.saveddata.satellites.SatelliteLaser;
import com.hbm.saveddata.satellites.SatelliteLunarMiner;
import com.hbm.saveddata.satellites.SatelliteMapper;
import com.hbm.saveddata.satellites.SatelliteMiner;
import com.hbm.saveddata.satellites.SatelliteRadar;
import com.hbm.saveddata.satellites.SatelliteResonator;
import com.hbm.saveddata.satellites.SatelliteScanner;
import com.hbm.util.AstronomyUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;

public class GUIScreenSatSettings extends GuiScreen {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/satellites/gui_sat_settings.png");
	private static final ResourceLocation starmapTexture = new ResourceLocation(RefStrings.MODID + ":textures/gui/starmap3.png");
	private static final ResourceLocation ringTexture = new ResourceLocation(RefStrings.MODID + ":textures/misc/space/rings.png");
	private static final ResourceLocation impactTexture = new ResourceLocation(RefStrings.MODID + ":textures/misc/space/impact.png");
	private static final ResourceLocation defaultMask = new ResourceLocation(RefStrings.MODID, "textures/misc/space/default_mask.png");
	private static final ResourceLocation satelliteTextureDefault = new ResourceLocation(RefStrings.MODID, "textures/items/sat_base.png");
	private static final ResourceLocation satelliteTextureFoeq = new ResourceLocation(RefStrings.MODID, "textures/items/sat_foeq.png");
	private static final ResourceLocation satelliteTextureLaser = new ResourceLocation(RefStrings.MODID, "textures/items/sat_laser.png");
	private static final ResourceLocation satelliteTextureMapper = new ResourceLocation(RefStrings.MODID, "textures/items/sat_mapper.png");
	private static final ResourceLocation satelliteTextureMiner = new ResourceLocation(RefStrings.MODID, "textures/items/sat_miner.png");
	private static final ResourceLocation satelliteTextureRadar = new ResourceLocation(RefStrings.MODID, "textures/items/sat_radar.png");
	private static final ResourceLocation satelliteTextureResonator = new ResourceLocation(RefStrings.MODID, "textures/items/sat_resonator.png");
	private static final ResourceLocation satelliteTextureScanner = new ResourceLocation(RefStrings.MODID, "textures/items/sat_scanner.png");
	private static final Map<Class<?>, ResourceLocation> satelliteTextureByClass = new HashMap<Class<?>, ResourceLocation>();
	private static final ResourceLocation[] citylights = new ResourceLocation[]{
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_0.png"),
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_1.png"),
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_2.png"),
		new ResourceLocation(RefStrings.MODID, "textures/misc/space/citylights_3.png")
	};
	private static final Shader planetShader = new Shader(new ResourceLocation(RefStrings.MODID, "shaders/crescent.frag"));

	static {
		satelliteTextureByClass.put(SatelliteMapper.class, satelliteTextureMapper);
		satelliteTextureByClass.put(SatelliteScanner.class, satelliteTextureScanner);
		satelliteTextureByClass.put(SatelliteRadar.class, satelliteTextureRadar);
		satelliteTextureByClass.put(SatelliteLaser.class, satelliteTextureLaser);
		satelliteTextureByClass.put(SatelliteResonator.class, satelliteTextureResonator);
		satelliteTextureByClass.put(SatelliteFoeq.class, satelliteTextureFoeq);
		satelliteTextureByClass.put(SatelliteMiner.class, satelliteTextureMiner);
	}

	private final EntityPlayer player;
	private final Map<ResourceLocation, Boolean> textureAlphaCache = new HashMap<ResourceLocation, Boolean>();
	private int guiLeft;
	private int guiTop;
	private int draggedSlider = -1;

	public GUIScreenSatSettings(EntityPlayer player) {
		this.player = player;
	}

	@Override
	public void initGui() {
		super.initGui();
		guiLeft = (width - 134) / 2;
		guiTop = (height - 221) / 2;
	}

	@Override
	public void updateScreen() {
		if(getHeldSatellite() == null) player.closeScreen();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		GL11.glColor4f(1F, 1F, 1F, 1F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		func_146110_a(guiLeft, guiTop, 0, 0, 134, 221, 152, 221);

		ItemStack held = getHeldSatellite();
		if(held == null) return;

		drawOrbitPreview(held, partialTicks);

		int r = toColorChannel(Satellite.getColorR(held));
		int g = toColorChannel(Satellite.getColorG(held));
		int b = toColorChannel(Satellite.getColorB(held));

		drawLeftAligned(10, 130, 140, "Owner: " + Satellite.getOwner(held), 0x00FF00);
		drawLeftAligned(10, 145, 155, "Altitude: " + formatValue(Satellite.getAltitude(held)) + "km", 0x00FF00);
		drawLeftAligned(10, 160, 170, "Inclination: " + formatValue(Satellite.getInclination(held)) + "\u00B0", 0x00FF00);
		drawRect(guiLeft + 81, guiTop + 176, guiLeft + 110, guiTop + 199, 0xFF000000 | (r << 16) | (g << 8) | b);
		drawRightAligned(110, 205, 214, formatValue(Satellite.getBlinkPeriod(held)) + "s", 0xFFFFFF, 2F / 3F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		drawSlider(r, 180, 17);
		drawSlider(g, 187, 24);
		drawSlider(b, 194, 31);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	protected void keyTyped(char c, int key) {
		if(key == 1 || key == mc.gameSettings.keyBindInventory.getKeyCode()) {
			mc.thePlayer.closeScreen();
			return;
		}

		super.keyTyped(c, key);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);

		if(button != 0) return;

		draggedSlider = getSliderAt(mouseX, mouseY);
		if(draggedSlider >= 0) {
			updateSlider(draggedSlider, mouseX);
		}
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

		if(clickedMouseButton == 0 && draggedSlider >= 0) {
			updateSlider(draggedSlider, mouseX);
		}
	}

	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
		super.mouseMovedOrUp(mouseX, mouseY, button);

		if(button == 0) {
			draggedSlider = -1;
		}
	}

	private void drawLeftAligned(int x, int y1, int y2, String text, int color) {
		drawScaledString(text, guiLeft + x, getCenteredY(y1, y2, 2F / 3F), color, 2F / 3F);
	}

	private void drawRightAligned(int x, int y1, int y2, String text, int color, float scale) {
		drawScaledString(text, guiLeft + x - Math.round(fontRendererObj.getStringWidth(text) * scale), getCenteredY(y1, y2, scale), color, scale);
	}

	private int getCenteredY(int y1, int y2, float scale) {
		return guiTop + y1 + ((y2 - y1 + 1) - Math.round(fontRendererObj.FONT_HEIGHT * scale)) / 2;
	}

	private void drawScaledString(String text, int x, int y, int color, float scale) {
		GL11.glPushMatrix();
		GL11.glScalef(scale, scale, 1F);
		fontRendererObj.drawString(text, Math.round(x / scale), Math.round(y / scale), color);
		GL11.glPopMatrix();
	}

	private void drawSlider(int value, int y, int v) {
		func_146110_a(guiLeft + 12 + Math.round(value * 62 / 255F), guiTop + y - 2, 136, v, 2, 6, 152, 221);
	}

	private int getSliderAt(int mouseX, int mouseY) {
		int x = mouseX - guiLeft;
		int y = mouseY - guiTop;

		if(x < 12 || x >= 12 + 64) return -1;
		if(y >= 180 - 2 && y < 180 - 2 + 6) return 0;
		if(y >= 187 - 2 && y < 187 - 2 + 6) return 1;
		if(y >= 194 - 2 && y < 194 - 2 + 6) return 2;
		return -1;
	}

	private void updateSlider(int slider, int mouseX) {
		ItemStack held = getHeldSatellite();
		if(held == null) return;

		int value = Math.round(MathHelper.clamp_int(mouseX - guiLeft - 12, 0, 62) * 255F / 62);
		int r = toColorChannel(Satellite.getColorR(held));
		int g = toColorChannel(Satellite.getColorG(held));
		int b = toColorChannel(Satellite.getColorB(held));
		int oldR = r;
		int oldG = g;
		int oldB = b;

		if(slider == 0) r = value;
		if(slider == 1) g = value;
		if(slider == 2) b = value;

		if(r == oldR && g == oldG && b == oldB) return;

		Satellite.setColor(held, r / 255F, g / 255F, b / 255F);

		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("satColorR", r);
		data.setInteger("satColorG", g);
		data.setInteger("satColorB", b);
		PacketDispatcher.wrapper.sendToServer(new NBTItemControlPacket(data));
	}

	private ItemStack getHeldSatellite() {
		ItemStack held = player.getHeldItem();
		return held != null && Satellite.isSatelliteItem(held.getItem()) ? held : null;
	}

	private void drawOrbitPreview(ItemStack held, float partialTicks) {
		CelestialBody body = getPreviewBody(held);
		float bodySizeAt1x = getBodySizePxAt1x(body);
		float baseOrbitRadiusMapPx = bodySizeAt1x * 1.5F;
		Map<Integer, Satellite> satellites = SatelliteSavedData.getClientSats();
		String owner = Satellite.getOwner(held);
		float maxAltitude = Satellite.getAltitude(held);

		for(Satellite satellite : satellites.values()) {
			if(owner.equals(satellite.owner)) {
				maxAltitude = Math.max(maxAltitude, satellite.altitude);
			}
		}

		float renderZoom = getPreviewZoom(bodySizeAt1x, baseOrbitRadiusMapPx, maxAltitude) * 2.0F;
		float centerX = guiLeft + 9 + 116 * 0.5F;
		float centerY = guiTop + 8 + 116 * 0.5F;
		float bodySize = MathHelper.clamp_float(bodySizeAt1x * renderZoom, 8F, 96F);
		float iconSize = MathHelper.clamp_float(bodySize * 0.75F * 0.25F, 0.4F, 9.0F);
		float angle = getArtificialSatelliteAngle();

		float heldAltitude = Satellite.getAltitude(held);
		float heldInclination = Satellite.getInclination(held);
		float heldR = Satellite.getColorR(held);
		float heldG = Satellite.getColorG(held);
		float heldB = Satellite.getColorB(held);
		ResourceLocation heldTexture = getSatelliteTextureByType(Satellite.itemToClass.get(held.getItem()));

		double dayTicks = mc.theWorld.getTotalWorldTime() + partialTicks;
		double worldTicks = dayTicks * AstronomyUtil.TIME_MULTIPLIER;
		BodyPosition bodyPosition = new BodyPosition();
		BodyPosition parentPosition = new BodyPosition();
		resolveBodyPositions(body, worldTicks, bodyPosition, parentPosition);

		pushScissor(9, 8, 116, 116);
		drawStarmapBackground();

		drawOwnedSatellites(satellites, owner, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, angle, iconSize, false);
		drawSatelliteOrbitHalf(centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldAltitude, heldInclination, heldR, heldG, heldB, false, 0.45F);
		drawSatelliteIcon(heldTexture, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldAltitude, heldInclination, angle, false, iconSize * 1.2F);
		drawBodyPreview(body, centerX, centerY, bodySize, dayTicks, bodyPosition, parentPosition);

		drawOwnedSatellites(satellites, owner, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, angle, iconSize, true);
		drawSatelliteOrbitHalf(centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldAltitude, heldInclination, heldR, heldG, heldB, true, 0.45F);
		drawSatelliteIcon(heldTexture, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldAltitude, heldInclination, angle, true, iconSize * 1.2F);

		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GL11.glColor4f(1F, 1F, 1F, 1F);
	}

	private void drawStarmapBackground() {
		float bgSrcW = 116;
		float bgSrcH = 116;
		float bgU = MathHelper.clamp_float(1024F * 0.5F - bgSrcW * 0.5F, 0F, 1024F - bgSrcW);
		float bgV = MathHelper.clamp_float(1024F * 0.5F - bgSrcH * 0.5F, 0F, 1024F - bgSrcH);
		float bgScaleU = 1F;
		float bgScaleV = 1F;
		float bgUTex = bgU * bgScaleU;
		float bgVTex = bgV * bgScaleV;
		float bgSrcWTex = bgSrcW * bgScaleU;
		float bgSrcHTex = bgSrcH * bgScaleV;

		mc.getTextureManager().bindTexture(starmapTexture);
		drawPartialTex(
			guiLeft + 9,
			guiTop + 8,
			116,
			116,
			bgUTex / 1024F,
			bgVTex / 1024F,
			(bgUTex + bgSrcWTex) / 1024F,
			(bgVTex + bgSrcHTex) / 1024F
		);
	}

	private void drawOwnedSatellites(Map<Integer, Satellite> satellites, String owner, float centerX, float centerY, float baseOrbitRadiusMapPx, float zoom, float angle, float iconSize, boolean frontHalf) {
		for(Satellite satellite : satellites.values()) {
			if(!owner.equals(satellite.owner)) continue;

			drawSatelliteOrbitHalf(centerX, centerY, baseOrbitRadiusMapPx, zoom, satellite.altitude, satellite.inclination, satellite.colorR, satellite.colorG, satellite.colorB, frontHalf, 0.25F);
		}

		for(Satellite satellite : satellites.values()) {
			if(!owner.equals(satellite.owner)) continue;

			drawSatelliteIcon(getSatelliteTextureByType(satellite.getClass()), centerX, centerY, baseOrbitRadiusMapPx, zoom, satellite.altitude, satellite.inclination, angle, frontHalf, iconSize);
		}
	}

	private void drawSatelliteOrbitHalf(float centerX, float centerY, float baseRadiusMapPx, float zoom, float altitude, float inclination, float r, float g, float b, boolean frontHalf, float alpha) {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glLineWidth(1F);

		Tessellator tess = Tessellator.instance;
		float lineR = MathHelper.clamp_float(r, 0F, 1F);
		float lineG = MathHelper.clamp_float(g, 0F, 1F);
		float lineB = MathHelper.clamp_float(b, 0F, 1F);

		boolean hasPrev = false;
		float prevX = 0F;
		float prevY = 0F;
		float prevDepth = 0F;
		boolean prevFront = false;
		boolean drawing = false;

		for(int i = 0; i <= 64; i++) {
			float orbitAngle = (float) (2D * Math.PI * ((double) i / 64D));
			SatelliteOrbitPoint orbitPoint = getArtificialSatelliteOrbitPoint(altitude, inclination, orbitAngle, baseRadiusMapPx);
			float currX = mapToScreenX(centerX, orbitPoint.offsetU, orbitPoint.offsetV, zoom);
			float currY = mapToScreenY(centerY, orbitPoint.offsetU, orbitPoint.offsetV, zoom);
			float currDepth = orbitPoint.depth;
			boolean currFront = currDepth <= 0F;

			if(!hasPrev) {
				prevX = currX;
				prevY = currY;
				prevDepth = currDepth;
				prevFront = currFront;
				hasPrev = true;
				continue;
			}

			boolean prevSelected = prevFront == frontHalf;
			boolean currSelected = currFront == frontHalf;

			if(prevSelected && currSelected) {
				if(!drawing) {
					tess.startDrawing(GL11.GL_LINE_STRIP);
					tess.setColorRGBA_F(lineR, lineG, lineB, alpha);
					tess.addVertex(prevX, prevY, this.zLevel);
					drawing = true;
				}
				tess.addVertex(currX, currY, this.zLevel);
			} else if(prevSelected != currSelected) {
				float depthDelta = currDepth - prevDepth;
				float t = depthDelta == 0F ? 0.5F : (-prevDepth) / depthDelta;
				t = MathHelper.clamp_float(t, 0F, 1F);
				float crossX = prevX + (currX - prevX) * t;
				float crossY = prevY + (currY - prevY) * t;

				if(prevSelected) {
					if(!drawing) {
						tess.startDrawing(GL11.GL_LINE_STRIP);
						tess.setColorRGBA_F(lineR, lineG, lineB, alpha);
						tess.addVertex(prevX, prevY, this.zLevel);
						drawing = true;
					}
					tess.addVertex(crossX, crossY, this.zLevel);
					tess.draw();
					drawing = false;
				} else {
					tess.startDrawing(GL11.GL_LINE_STRIP);
					tess.setColorRGBA_F(lineR, lineG, lineB, alpha);
					tess.addVertex(crossX, crossY, this.zLevel);
					tess.addVertex(currX, currY, this.zLevel);
					drawing = true;
				}
			}

			prevX = currX;
			prevY = currY;
			prevDepth = currDepth;
			prevFront = currFront;
		}

		if(drawing) tess.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1F, 1F, 1F, 1F);
	}

	private void drawSatelliteIcon(ResourceLocation texture, float centerX, float centerY, float baseRadiusMapPx, float zoom, float altitude, float inclination, float angle, boolean frontHalf, float size) {
		SatelliteOrbitPoint orbitPoint = getArtificialSatelliteOrbitPoint(altitude, inclination, angle, baseRadiusMapPx);
		float screenX = mapToScreenX(centerX, orbitPoint.offsetU, orbitPoint.offsetV, zoom);
		float screenY = mapToScreenY(centerY, orbitPoint.offsetU, orbitPoint.offsetV, zoom);
		if((orbitPoint.depth <= 0F) != frontHalf) return;

		float half = size * 0.5F;
		float minX = guiLeft + 9;
		float minY = guiTop + 8;
		float maxX = minX + 116;
		float maxY = minY + 116;

		if(screenX + half < minX || screenX - half > maxX || screenY + half < minY || screenY - half > maxY) return;

		GL11.glColor4f(1F, 1F, 1F, 1F);
		mc.getTextureManager().bindTexture(texture != null ? texture : satelliteTextureDefault);
		drawPartialTex(screenX - half, screenY - half, size, size, 0F, 0F, 1F, 1F);
	}

	private void drawBodyPreview(CelestialBody body, float centerX, float centerY, float size, double dayTicks, BodyPosition bodyPosition, BodyPosition parentPosition) {
		float half = size * 0.5F;
		float ringHalfWidth = 0F;
		float ringHalfHeight = 0F;
		float minX = centerX - half;
		float maxX = centerX + half;
		float minY = centerY - half;
		float maxY = centerY + half;

		if(body.hasRings) {
			ringHalfWidth = size * 0.5F * Math.max(1F, body.ringSize);
			float ringTiltSin = Math.abs(MathHelper.sin((float) Math.toRadians(body.ringTilt)));
			ringTiltSin = Math.max(0.08F, ringTiltSin);
			ringHalfHeight = Math.max(0.5F, ringHalfWidth * ringTiltSin);

			minX = Math.min(minX, centerX - ringHalfWidth);
			maxX = Math.max(maxX, centerX + ringHalfWidth);
			minY = Math.min(minY, centerY - ringHalfHeight);
			maxY = Math.max(maxY, centerY + ringHalfHeight);
		}

		if(maxX < guiLeft + 9 || minX > guiLeft + 9 + 116 || maxY < guiTop + 8 || minY > guiTop + 8 + 116) {
			return;
		}

		if(body.hasRings) {
			drawBodyRingHalf(body, centerX, centerY, ringHalfWidth, ringHalfHeight, false);
		}

		if(body.texture != null) {
			mc.getTextureManager().bindTexture(body.texture);
			if(body.parent == null) {
				drawTexturedQuad(centerX, centerY, size, 0F);
			} else {
				float phase = getBodyRotationPhase(body, dayTicks);
				float bodyRotationAngle = phase * 360F;
				boolean rotateBody = hasTransparentPixels(body.texture);
				float textureUOffset = 0F;

				if(rotateBody) {
					drawTexturedQuadRotating(centerX, centerY, size, bodyRotationAngle);
				} else {
					drawTexturedQuad(centerX, centerY, size, phase);
					textureUOffset = phase;
				}

				drawBodyCrescentOverlay(body, centerX, centerY, size, bodyPosition, parentPosition, rotateBody, bodyRotationAngle, dayTicks, textureUOffset);
			}
		} else {
			int color = 0xFF666666;
			if(body.color != null && body.color.length >= 3) {
				color = 0xFF000000 | (toColorChannel(body.color[0]) << 16) | (toColorChannel(body.color[1]) << 8) | toColorChannel(body.color[2]);
			}
			drawRect((int) (centerX - half), (int) (centerY - half), (int) (centerX + half), (int) (centerY + half), color);
		}

		if(body.hasRings) {
			drawBodyRingHalf(body, centerX, centerY, ringHalfWidth, ringHalfHeight, true);
		}
	}

	private void drawBodyRingHalf(CelestialBody body, float bodyScreenX, float bodyScreenY, float ringHalfWidth, float drawH, boolean frontHalf) {
		if(body == null || ringHalfWidth <= 0F || drawH <= 0F) return;

		float[] ringColor = body.ringColor != null && body.ringColor.length >= 3 ? body.ringColor : null;
		float r = ringColor != null ? ringColor[0] : 0.5F;
		float g = ringColor != null ? ringColor[1] : 0.5F;
		float b = ringColor != null ? ringColor[2] : 0.5F;
		float a = ringColor != null && ringColor.length >= 4 ? ringColor[3] : 1F;
		float drawX = bodyScreenX - ringHalfWidth;
		float drawY = frontHalf ? bodyScreenY : bodyScreenY - drawH;
		float drawW = ringHalfWidth * 2F;
		float v1 = frontHalf ? 0.5F : 0F;
		float v2 = frontHalf ? 1F : 0.5F;

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(r, g, b, a);
		mc.getTextureManager().bindTexture(ringTexture);
		drawPartialTex(drawX, drawY, drawW, drawH, 0F, v1, 1F, v2);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1F, 1F, 1F, 1F);
	}

	private void drawBodyCrescentOverlay(CelestialBody body, float bodyScreenX, float bodyScreenY, float drawSize, BodyPosition bodyPosition, BodyPosition parentPosition, boolean rotateBody, float bodyRotationAngle, double dayTicks, float textureUOffset) {
		float phase = calculateHorizontalCrescentPhase(body, bodyPosition.mapU, bodyPosition.mapV, parentPosition.mapU, parentPosition.mapV);
		CBT_Impact impact = body.getTrait(CBT_Impact.class);
		CBT_Lights light = body.getTrait(CBT_Lights.class);
		double impactTime = impact != null ? dayTicks - impact.time : 0.0D;
		int lightIntensity = light != null && impactTime < 40.0D ? MathHelper.clamp_int(light.getIntensity(), 0, citylights.length - 1) : 0;
		int activeBlackouts = Math.max(0, Math.min((int) (impactTime / 8.0D), 5));

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1F, 1F, 1F, 1F);

		planetShader.use();
		planetShader.setUniform1f("phase", phase);
		planetShader.setUniform1f("offset", textureUOffset);
		planetShader.setUniform1i("bodyTex", 0);
		planetShader.setUniform1i("lights", 1);
		planetShader.setUniform1i("cityMask", 2);
		planetShader.setUniform1i("blackouts", activeBlackouts);
		planetShader.setUniform1i("useBodyAlphaMask", 1);

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		mc.getTextureManager().bindTexture(body.texture);
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		mc.getTextureManager().bindTexture(citylights[lightIntensity]);
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		mc.getTextureManager().bindTexture(body.cityMask != null ? body.cityMask : defaultMask);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);

		if(rotateBody) {
			drawTexturedQuadRotating(bodyScreenX, bodyScreenY, drawSize, bodyRotationAngle);
		} else {
			drawTexturedQuad(bodyScreenX, bodyScreenY, drawSize, 0F);
		}

		planetShader.stop();

		if(impact != null) {
			float lavaAlpha = (float) Math.min(impactTime * 0.1D, 1.0D);
			if(lavaAlpha > 0F) {
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				GL11.glColor4f(1F, 1F, 1F, lavaAlpha);
				mc.getTextureManager().bindTexture(impactTexture);
				if(rotateBody) {
					drawTexturedQuadRotating(bodyScreenX, bodyScreenY, drawSize, bodyRotationAngle);
				} else {
					drawTexturedQuad(bodyScreenX, bodyScreenY, drawSize, textureUOffset);
				}
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
		}

		GL11.glColor4f(1F, 1F, 1F, 1F);
	}

	private boolean hasTransparentPixels(ResourceLocation texture) {
		Boolean cached = textureAlphaCache.get(texture);
		if(cached != null) return cached;

		boolean hasAlpha = false;
		InputStream stream = null;

		try {
			IResource resource = mc.getResourceManager().getResource(texture);
			stream = resource.getInputStream();
			BufferedImage image = ImageIO.read(stream);

			if(image.getColorModel().hasAlpha()) {
				for(int y = 0; y < image.getHeight() && !hasAlpha; y++) {
					for(int x = 0; x < image.getWidth(); x++) {
						if(((image.getRGB(x, y) >>> 24) & 255) < 255) {
							hasAlpha = true;
							break;
						}
					}
				}
			}
		} catch(IOException ignored) {
		} finally {
			if(stream != null) {
				try {
					stream.close();
				} catch(IOException ignored) {
				}
			}
		}

		textureAlphaCache.put(texture, hasAlpha);
		return hasAlpha;
	}

	private float calculateHorizontalCrescentPhase(CelestialBody body, float bodyMapU, float bodyMapV, float parentMapU, float parentMapV) {
		float sunMapU = 1024F * 0.5F;
		float sunMapV = 1024F * 0.5F;
		float bodyProjX = (bodyMapU - bodyMapV) * 0.70F;
		float bodyProjY = (bodyMapU + bodyMapV) * 0.35F;
		float sunProjX = (sunMapU - sunMapV) * 0.70F;
		float sunProjY = (sunMapU + sunMapV) * 0.35F;

		float dx = bodyProjX - sunProjX;
		float dy = bodyProjY - sunProjY;
		float lengthSq = dx * dx + dy * dy;
		if(lengthSq <= 0.0001F) return 0F;

		float length = MathHelper.sqrt_float(lengthSq);
		float verticalFactor = dy / length;
		float phaseMagnitude = MathHelper.clamp_float((verticalFactor + 1F) * 0.5F, 0F, 1F);
		phaseMagnitude = applyMoonEclipseDarkening(body, bodyMapU, bodyMapV, parentMapU, parentMapV, sunMapU, sunMapV, phaseMagnitude);

		float phaseSign = dx <= 0F ? 1F : -1F;
		return MathHelper.clamp_float(phaseMagnitude * phaseSign, -1F, 1F);
	}

	private float applyMoonEclipseDarkening(CelestialBody body, float moonMapU, float moonMapV, float parentMapU, float parentMapV, float sunMapU, float sunMapV, float phaseMagnitude) {
		if(!isMoon(body) || body.parent == null) return phaseMagnitude;

		float moonProjX = (moonMapU - moonMapV) * 0.70F;
		float moonProjY = (moonMapU + moonMapV) * 0.35F;
		float parentProjX = (parentMapU - parentMapV) * 0.70F;
		float parentProjY = (parentMapU + parentMapV) * 0.35F;
		float sunProjX = (sunMapU - sunMapV) * 0.70F;
		float sunProjY = (sunMapU + sunMapV) * 0.35F;

		float sunToParentX = parentProjX - sunProjX;
		float sunToParentY = parentProjY - sunProjY;
		float parentToMoonX = moonProjX - parentProjX;
		float parentToMoonY = moonProjY - parentProjY;

		float sunParentLenSq = sunToParentX * sunToParentX + sunToParentY * sunToParentY;
		if(sunParentLenSq <= 0.0001F) return phaseMagnitude;

		float sunParentLen = MathHelper.sqrt_float(sunParentLenSq);
		float alongShadowAxis = (sunToParentX * parentToMoonX + sunToParentY * parentToMoonY) / sunParentLen;
		float lineDistance = Math.abs(sunToParentX * parentToMoonY - sunToParentY * parentToMoonX) / sunParentLen;
		float eclipseRadius = Math.max(1.5F, getBodySizePxAt1x(body.parent) * 0.45F);
		float penumbraRadius = eclipseRadius * 2F;
		float behindFade = Math.max(0.15F, eclipseRadius * 0.12F);

		if(lineDistance >= penumbraRadius || alongShadowAxis <= -behindFade) return phaseMagnitude;

		float behindFactor = alongShadowAxis >= 0F ? 1F : Library.smoothstep(alongShadowAxis, -behindFade, 0F);
		if(lineDistance <= eclipseRadius) {
			return MathHelper.clamp_float(phaseMagnitude + (1F - phaseMagnitude) * behindFactor, 0F, 1F);
		}

		float penumbraFactor = 1F - Library.smoothstep(lineDistance, eclipseRadius, penumbraRadius);
		float eclipseFactor = behindFactor * penumbraFactor;
		return MathHelper.clamp_float(phaseMagnitude + (1F - phaseMagnitude) * eclipseFactor, 0F, 1F);
	}

	private void drawPartialTex(float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		tess.addVertexWithUV(x, y + h, this.zLevel, u1, v2);
		tess.addVertexWithUV(x + w, y + h, this.zLevel, u2, v2);
		tess.addVertexWithUV(x + w, y, this.zLevel, u2, v1);
		tess.addVertexWithUV(x, y, this.zLevel, u1, v1);
		tess.draw();
	}

	private void drawTexturedQuad(float x, float y, float size, float uOffset) {
		float half = size * 0.5F;
		float minU = uOffset;
		float maxU = 1F + uOffset;
		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		tess.addVertexWithUV(x - half, y + half, this.zLevel, minU, 1F);
		tess.addVertexWithUV(x + half, y + half, this.zLevel, maxU, 1F);
		tess.addVertexWithUV(x + half, y - half, this.zLevel, maxU, 0F);
		tess.addVertexWithUV(x - half, y - half, this.zLevel, minU, 0F);
		tess.draw();
	}

	private void drawTexturedQuadRotating(float x, float y, float size, float angle) {
		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0F);
		GL11.glRotatef(angle, 0F, 0F, 1F);
		drawTexturedQuad(0F, 0F, size, 0F);
		GL11.glPopMatrix();
	}

	private float getPreviewZoom(float bodySizeAt1x, float baseOrbitRadiusMapPx, float maxAltitude) {
		float altitude = Math.max(Satellite.DEFAULT_ALTITUDE_KM, Satellite.sanitizeAltitude(maxAltitude));
		float altitudeFactor = altitude / Satellite.DEFAULT_ALTITUDE_KM;
		float maxOrbitRadius = 116F * 0.46F;
		float zoomForOrbit = maxOrbitRadius / Math.max(0.0001F, baseOrbitRadiusMapPx * altitudeFactor * 0.70F);
		float zoomForBody = 16F / Math.max(0.0001F, bodySizeAt1x);
		return MathHelper.clamp_float(Math.min(zoomForOrbit, zoomForBody), 16F, 240F);
	}

	private float mapToScreenX(float centerX, float mapU, float mapV, float zoom) {
		return centerX + (mapU - mapV) * zoom * 0.70F;
	}

	private float mapToScreenY(float centerY, float mapU, float mapV, float zoom) {
		return centerY + (mapU + mapV) * zoom * 0.35F;
	}

	private SatelliteOrbitPoint getArtificialSatelliteOrbitPoint(float altitude, float inclination, float angle, float baseRadiusMapPx) {
		float satAltitude = Satellite.sanitizeAltitude(altitude);
		double satInclination = Math.toRadians(Satellite.sanitizeInclination(inclination));
		double radiusMapPx = baseRadiusMapPx * (satAltitude / Satellite.DEFAULT_ALTITUDE_KM);

		double x = radiusMapPx * MathHelper.cos(angle);
		double y = radiusMapPx * MathHelper.sin(angle);
		double z = Math.sin(satInclination) * y;
		y = Math.cos(satInclination) * y;

		y -= z * 0.35D;
		y *= 0.8D;

		return new SatelliteOrbitPoint((float) x, (float) y, (float) z);
	}

	private float getArtificialSatelliteAngle() {
		long cycle = 30000L;
		double progress = (double) (System.currentTimeMillis() % cycle) / (double) cycle;
		return (float) (-progress * 2D * Math.PI);
	}

	private CelestialBody getCurrentBody() {
		CelestialBody body = CelestialBody.getTarget(player.worldObj, (int) player.posX, (int) player.posZ).body;
		return body != null ? body : CelestialBody.getBody(player.worldObj);
	}

	private CelestialBody getPreviewBody(ItemStack held) {
		Class<? extends Satellite> satClass = Satellite.itemToClass.get(held.getItem());
		if(satClass != null) {
			if(SatelliteFoeq.class.isAssignableFrom(satClass)) return CelestialBody.getBody(SolarSystem.Body.DUNA.getDimensionId());
			if(SatelliteLunarMiner.class.isAssignableFrom(satClass)) return CelestialBody.getBody(SolarSystem.Body.MUN.getDimensionId());
			if(SatelliteMiner.class.isAssignableFrom(satClass)) return CelestialBody.getBody(SolarSystem.Body.DRES.getDimensionId());
		}
		return getCurrentBody();
	}

	private void resolveBodyPositions(CelestialBody body, double worldTicks, BodyPosition bodyPosition, BodyPosition parentPosition) {
		float centerU = 1024F * 0.5F;
		float centerV = 1024F * 0.5F;
		bodyPosition.mapU = centerU;
		bodyPosition.mapV = centerV;
		parentPosition.mapU = Float.NaN;
		parentPosition.mapV = Float.NaN;

		CelestialBody root = body.getStar();
		float systemOrbitScale = getOrbitScalePxPerKm(root);

		findBodyMapPosition(root, centerU, centerV, systemOrbitScale, worldTicks, body, bodyPosition);
		if(body.parent != null) {
			findBodyMapPosition(root, centerU, centerV, systemOrbitScale, worldTicks, body.parent, parentPosition);
		}
	}

	private boolean findBodyMapPosition(CelestialBody body, float bodyMapU, float bodyMapV, float systemOrbitScalePxPerKm, double worldTicks, CelestialBody targetBody, BodyPosition outPosition) {
		if(body == targetBody) {
			outPosition.mapU = bodyMapU;
			outPosition.mapV = bodyMapV;
			return true;
		}

		float childOrbitScale = getChildOrbitScalePxPerKm(body, systemOrbitScalePxPerKm);
		for(CelestialBody childBody : body.satellites) {
			double meanAnomaly = calculateMeanAnomaly(childBody, worldTicks);
			float[] orbitOffset = calculateOrbitOffsetPx(body, childBody, meanAnomaly, childOrbitScale);
			if(findBodyMapPosition(childBody, bodyMapU + orbitOffset[0], bodyMapV + orbitOffset[1], systemOrbitScalePxPerKm, worldTicks, targetBody, outPosition)) {
				return true;
			}
		}

		return false;
	}

	private float getOrbitScalePxPerKm(CelestialBody starBody) {
		float maxDistanceKm = getSystemMaxDistanceKm(starBody);
		float starRadiusPxAt1x = getBodySizePxAt1x(starBody) * 0.5F;
		float availableRadiusPx = Math.max(0F, (256F * 0.50F) - starRadiusPxAt1x);
		return availableRadiusPx / maxDistanceKm;
	}

	private float getSystemMaxDistanceKm(CelestialBody starBody) {
		float maxDistance = 0F;
		for(CelestialBody child : starBody.satellites) {
			float apoapsisKm = child.semiMajorAxisKm * (1F + child.eccentricity);
			maxDistance = Math.max(maxDistance, apoapsisKm);
		}

		return maxDistance;
	}

	private float getChildOrbitScalePxPerKm(CelestialBody parent, float systemOrbitScalePxPerKm) {
		if(parent.parent != null) {
			float moonOrbitScale = getMoonOrbitScalePxPerKm(parent, systemOrbitScalePxPerKm);
			if(moonOrbitScale > 0F) return moonOrbitScale;
		}
		return systemOrbitScalePxPerKm;
	}

	private float getMoonOrbitScalePxPerKm(CelestialBody parent, float systemOrbitScalePxPerKm) {
		float maxMoonDistanceKm = 0F;
		for(CelestialBody moon : parent.satellites) {
			maxMoonDistanceKm = Math.max(maxMoonDistanceKm, moon.semiMajorAxisKm * (1F + moon.eccentricity));
		}

		float parentRadiusPxAt1x = getBodySizePxAt1x(parent) * 0.5F;
		float moonOrbitRadiusPxAt1x = Math.max(10.0F, parentRadiusPxAt1x * 14F);
		float nearestSiblingGapKm = getNearestSiblingOrbitGapKm(parent);

		if(systemOrbitScalePxPerKm > 0F && nearestSiblingGapKm > 0F) {
			float siblingGapPxAt1x = nearestSiblingGapKm * systemOrbitScalePxPerKm;
			float maxAllowedMoonOrbitRadiusPxAt1x = siblingGapPxAt1x * 0.45F;
			if(maxAllowedMoonOrbitRadiusPxAt1x > 0F) {
				moonOrbitRadiusPxAt1x = Math.min(moonOrbitRadiusPxAt1x, maxAllowedMoonOrbitRadiusPxAt1x);
			}
		}

		return moonOrbitRadiusPxAt1x / maxMoonDistanceKm;
	}

	private float getNearestSiblingOrbitGapKm(CelestialBody body) {
		float nearestGapKm = Float.MAX_VALUE;
		for(CelestialBody sibling : body.parent.satellites) {
			if(sibling == body) continue;
			nearestGapKm = Math.min(nearestGapKm, Math.abs(sibling.semiMajorAxisKm - body.semiMajorAxisKm));
		}

		return nearestGapKm == Float.MAX_VALUE ? 0F : nearestGapKm;
	}

	private float[] calculateOrbitOffsetPx(CelestialBody parent, CelestialBody body, double meanAnomaly, float orbitScalePxPerKm) {
		double eccentricAnomaly = calculateEccentricAnomaly(meanAnomaly, body.eccentricity);
		double semiMinorAxisFactor = body.semiMinorAxisFactor > 0 ? body.semiMinorAxisFactor : Math.sqrt(1D - (body.eccentricity * body.eccentricity));
		double x = body.semiMajorAxisKm * (Math.cos(eccentricAnomaly) - body.eccentricity);
		double y = body.semiMajorAxisKm * semiMinorAxisFactor * Math.sin(eccentricAnomaly);
		double z;

		double px = x;
		x = Math.cos(body.argumentPeriapsis) * px - Math.sin(body.argumentPeriapsis) * y;
		y = Math.sin(body.argumentPeriapsis) * px + Math.cos(body.argumentPeriapsis) * y;

		z = Math.sin(body.inclination) * y;
		y = Math.cos(body.inclination) * y;

		px = x;
		x = Math.cos(body.ascendingNode) * px - Math.sin(body.ascendingNode) * y;
		y = Math.sin(body.ascendingNode) * px + Math.cos(body.ascendingNode) * y;

		y -= z * 0.35D;
		y *= 0.8D;

		float mapX = (float) (x * orbitScalePxPerKm);
		float mapY = (float) (y * orbitScalePxPerKm);

		float parentVisualRadiusPx = getBodySizePxAt1x(parent) * 0.5F;
		float distanceFromParentPx = MathHelper.sqrt_float(mapX * mapX + mapY * mapY);
		if(distanceFromParentPx > 0F) {
			float radialScale = (distanceFromParentPx + parentVisualRadiusPx) / distanceFromParentPx;
			mapX *= radialScale;
			mapY *= radialScale;
		} else {
			mapX = parentVisualRadiusPx;
			mapY = 0F;
		}

		return new float[]{mapX, mapY};
	}

	private double calculateMeanAnomaly(CelestialBody body, double worldTicks) {
		return 2D * Math.PI * (worldTicks / getOrbitalPeriodTicks(body));
	}

	private double getOrbitalPeriodTicks(CelestialBody body) {
		return body.getOrbitalPeriod() * (double) AstronomyUtil.TICKS_IN_DAY;
	}

	private double calculateEccentricAnomaly(double meanAnomaly, float eccentricity) {
		double eccentricAnomaly = meanAnomaly;
		for(int i = 0; i < 4; i++) {
			eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(eccentricAnomaly);
		}
		return eccentricAnomaly;
	}

	private float getBodyRotationPhase(CelestialBody body, double dayTicks) {
		double period = body.getRotationalPeriod();
		return (float) ((dayTicks % period) / period);
	}

	private float getBodySizePxAt1x(CelestialBody body) {
		if(body.parent == null) return 36F * 0.45F;
		if(isMoon(body)) return getMoonSizePxAt1x(body);

		float size = body.radiusKm * (36F / 261_600F) * 2.6F;
		return MathHelper.clamp_float(size, 0.8F, 2.0F) * 0.45F;
	}

	private float getMoonSizePxAt1x(CelestialBody moon) {
		float t = (Math.max(0F, moon.radiusKm) - 65F) / (500F - 65F);
		t = MathHelper.clamp_float(t, 0F, 1F);
		return (0.2F + (0.5F - 0.2F) * t) * 0.45F * 0.82F;
	}

	private ResourceLocation getSatelliteTextureByType(Class<?> type) {
		for(Class<?> current = type; current != null; current = current.getSuperclass()) {
			ResourceLocation texture = satelliteTextureByClass.get(current);
			if(texture != null) return texture;
			if(current == Satellite.class) break;
		}
		return satelliteTextureDefault;
	}

	private boolean isMoon(CelestialBody body) {
		return body.parent != null && body.parent.parent != null;
	}

	private void pushScissor(int x, int y, int w, int h) {
		ScaledResolution res = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
		int scale = res.getScaleFactor();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor((guiLeft + x) * scale, (this.height - (guiTop + y + h)) * scale, w * scale, h * scale);
	}

	private static class SatelliteOrbitPoint {
		private final float offsetU;
		private final float offsetV;
		private final float depth;

		private SatelliteOrbitPoint(float offsetU, float offsetV, float depth) {
			this.offsetU = offsetU;
			this.offsetV = offsetV;
			this.depth = depth;
		}
	}

	private static class BodyPosition {
		private float mapU;
		private float mapV;
	}

	private static int toColorChannel(float value) {
		int color = Math.round(value * 255.0F);
		if(color < 0) return 0;
		if(color > 255) return 255;
		return color;
	}

	private static String formatValue(float value) {
		return value == (int)value ? Integer.toString((int)value) : Float.toString(value);
	}
}
