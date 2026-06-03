package com.hbm.items.tool;

import com.hbm.inventory.gui.GUIScreenPadlockReceiver;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class ItemPadlockRadioClient {

	public static void openPadlockGUI(EntityPlayer player) {
		Minecraft.getMinecraft().displayGuiScreen(new GUIScreenPadlockReceiver(player));
	}
}
