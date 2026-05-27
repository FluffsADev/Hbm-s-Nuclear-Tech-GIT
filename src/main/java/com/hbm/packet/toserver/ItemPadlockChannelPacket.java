package com.hbm.packet.toserver;

import com.hbm.items.tool.ItemPadlockRadio;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemPadlockChannelPacket implements IMessage {

	private String channel;

	public ItemPadlockChannelPacket() { }

	public ItemPadlockChannelPacket(String chan) {
		this.channel = chan == null ? "" : chan;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int len = buf.readInt();
		if(len > 0) {
			byte[] b = new byte[len];
			buf.readBytes(b);
			this.channel = new String(b);
		} else {
			this.channel = "";
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		if(this.channel == null) {
			buf.writeInt(0);
			return;
		}
		byte[] b = this.channel.getBytes();
		buf.writeInt(b.length);
		buf.writeBytes(b);
	}

	public static class Handler implements IMessageHandler<ItemPadlockChannelPacket, IMessage> {
		@Override
		public IMessage onMessage(ItemPadlockChannelPacket msg, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			if(player == null) return null;

			ItemStack held = player.getHeldItem();
			if(held != null && held.getItem() instanceof ItemPadlockRadio) {
				if(!held.hasTagCompound()) held.stackTagCompound = new NBTTagCompound();
				held.stackTagCompound.setString("rchan", msg.channel == null ? "" : msg.channel);
			}
			return null;
		}
	}
}
