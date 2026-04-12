package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.lib.RefStrings;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTItemControlPacket;
import com.hbm.saveddata.satellites.Satellite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIScreenSatSettings extends GuiScreen {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/satellites/gui_sat_settings.png");
	private static final int texW = 152;
	private static final int texH = 221;
	private static final int xSize = 134;
	private static final int ySize = 221;
	private static final float textScale = 2F / 3F;

	private static final int sliderX = 12;
	private static final int sliderW = 64;
	private static final int sliderLineH = 2;
	private static final int sliderKnobW = 2;
	private static final int sliderKnobH = 6;
	private static final int sliderU = 136;
	private static final int sliderVRed = 17;
	private static final int sliderVGreen = 24;
	private static final int sliderVBlue = 31;
	private static final int sliderYRed = 180;
	private static final int sliderYGreen = 187;
	private static final int sliderYBlue = 194;

	private final EntityPlayer player;
	private int guiLeft;
	private int guiTop;
	private int draggedSlider = -1;

	public GUIScreenSatSettings(EntityPlayer player) {
		this.player = player;
	}

	@Override
	public void initGui() {
		super.initGui();
		guiLeft = (width - xSize) / 2;
		guiTop = (height - ySize) / 2;
	}

	@Override
	public void updateScreen() {
		ItemStack held = player.getHeldItem();
		if(held == null || !Satellite.isSatelliteItem(held.getItem())) player.closeScreen();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		GL11.glColor4f(1F, 1F, 1F, 1F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		func_146110_a(guiLeft, guiTop, 0, 0, xSize, ySize, texW, texH);

		ItemStack held = player.getHeldItem();
		if(held == null || !Satellite.isSatelliteItem(held.getItem())) return;

		int r = toColorChannel(Satellite.getColorR(held));
		int g = toColorChannel(Satellite.getColorG(held));
		int b = toColorChannel(Satellite.getColorB(held));

		drawLeftAligned(10, 130, 140, "Owner: " + Satellite.getOwner(held), 0x00FF00);
		drawLeftAligned(10, 145, 155, "Altitude: " + formatValue(Satellite.getAltitude(held)) + "km", 0x00FF00);
		drawLeftAligned(10, 160, 170, "Inclination: " + formatValue(Satellite.getInclination(held)) + "\u00B0", 0x00FF00);
		drawRect(guiLeft + 81, guiTop + 176, guiLeft + 111, guiTop + 200, 0xFF000000 | (r << 16) | (g << 8) | b);
		drawRightAligned(110, 205, 214, formatValue(Satellite.getBlinkPeriod(held)) + "s", 0xFFFFFF, textScale);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		drawSlider(r, sliderYRed, sliderVRed);
		drawSlider(g, sliderYGreen, sliderVGreen);
		drawSlider(b, sliderYBlue, sliderVBlue);
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
		drawScaledString(text, guiLeft + x, getCenteredY(y1, y2, textScale), color, textScale);
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
		drawTexturedModalRect(guiLeft + sliderX + Math.round(value * (sliderW - sliderKnobW) / 255F), guiTop + y - (sliderKnobH - sliderLineH) / 2, sliderU, v, sliderKnobW, sliderKnobH);
	}

	private int getSliderAt(int mouseX, int mouseY) {
		int x = mouseX - guiLeft;
		int y = mouseY - guiTop;

		if(x < sliderX || x >= sliderX + sliderW) return -1;
		if(y >= sliderYRed - (sliderKnobH - sliderLineH) / 2 && y < sliderYRed - (sliderKnobH - sliderLineH) / 2 + sliderKnobH) return 0;
		if(y >= sliderYGreen - (sliderKnobH - sliderLineH) / 2 && y < sliderYGreen - (sliderKnobH - sliderLineH) / 2 + sliderKnobH) return 1;
		if(y >= sliderYBlue - (sliderKnobH - sliderLineH) / 2 && y < sliderYBlue - (sliderKnobH - sliderLineH) / 2 + sliderKnobH) return 2;
		return -1;
	}

	private void updateSlider(int slider, int mouseX) {
		ItemStack held = player.getHeldItem();
		if(held == null || !Satellite.isSatelliteItem(held.getItem())) return;

		int value = Math.round(Math.max(0, Math.min(sliderW - 1, mouseX - guiLeft - sliderX)) * 255F / (sliderW - 1));
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
