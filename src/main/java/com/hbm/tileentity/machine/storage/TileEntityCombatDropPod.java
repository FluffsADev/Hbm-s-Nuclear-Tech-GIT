package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerSoyuzCapsule;
import com.hbm.inventory.gui.GUISoyuzCapsule;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityInventoryBase;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.tileentity.TileEntityProxyCombo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TileEntityCombatDropPod extends TileEntity implements IBufPacketReceiver{

	public NBTTagCompound entityType;
	public int amount;
	public int color;
	public int delay = 40;

	public TileEntityCombatDropPod() {

	}

	public void setPayload(NBTTagCompound entityType, int amount, int color) {
		this.entityType = entityType;
		this.amount = amount;
		this.color = color;
	}

	public int getColor() {
		return this.color;
	}
	
	public void setColor(int color) {
		this.color = color;

	}

	@Override
	public void updateEntity() {	
		
		if(worldObj.isRemote)
			return;
		
		if (delay > 0) {
			delay--;
			return;
		}

		if (entityType != null && amount > 0) {

			for (int i = 0; i < amount; i++) {

				Entity entity = EntityList.createEntityFromNBT(entityType, worldObj);

				if (entity != null) {
					entity.setPosition(xCoord + 0.5, yCoord + 1, zCoord + 0.5);

					worldObj.spawnEntityInWorld(entity);
				}
			}

			amount = 0;
		}
	}

	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + 2, yCoord + 3,
				zCoord + 2);
	}
	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(color);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.color = buf.readInt();
	}

	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
	    super.writeToNBT(nbt);

	    if(entityType != null)
	        nbt.setTag("EntityType", entityType);

	    nbt.setInteger("amount", amount);
	    nbt.setInteger("color", color);
	    nbt.setInteger("delay", delay);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
	    super.readFromNBT(nbt);

	    entityType = nbt.getCompoundTag("EntityType");
	    amount = nbt.getInteger("amount");
	    color = nbt.getInteger("color");
	    delay = nbt.getInteger("delay");
	}
	

}
