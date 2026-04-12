package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.lib.RefStrings;
import com.hbm.saveddata.satellites.Satellite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GUIScreenSatSettings extends GuiScreen {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/satellites/gui_sat_settings.png");
	private static final int xSize = 133;
	private static final int ySize = 192;

	private final EntityPlayer player;
	private int guiLeft;
	private int guiTop;

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
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
