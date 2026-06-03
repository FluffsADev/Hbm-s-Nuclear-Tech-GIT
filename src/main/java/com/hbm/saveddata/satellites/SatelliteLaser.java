package com.hbm.saveddata.satellites;

import com.hbm.entity.logic.EntityDeathBlast;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class SatelliteLaser extends Satellite {

	public long lastOp;

	public SatelliteLaser() {
		this.ifaceAcs.add(InterfaceActions.HAS_MAP);
		this.ifaceAcs.add(InterfaceActions.SHOW_COORDS);
		this.ifaceAcs.add(InterfaceActions.CAN_CLICK);
		this.satIface = Interfaces.SAT_PANEL;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("lastOp", lastOp);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		lastOp = nbt.getLong("lastOp");
	}

	public boolean isReady() {
		return lastOp + 10000L < System.currentTimeMillis();
	}

	public long getCooldownRemaining() {
		return Math.max(0L, (lastOp + 10000L) - System.currentTimeMillis());
	}

	public boolean trigger(World world, int x, int z) {

		if(!isReady()) {
			return false;
		}

		lastOp = System.currentTimeMillis();

		int y = world.getHeightValue(x, z);

		EntityDeathBlast blast = new EntityDeathBlast(world);
		blast.posX = x;
		blast.posY = y;
		blast.posZ = z;

		world.spawnEntityInWorld(blast);
		return true;
	}

	@Override
	public void onClick(World world, int x, int z) {
		trigger(world, x, z);
	}
}
