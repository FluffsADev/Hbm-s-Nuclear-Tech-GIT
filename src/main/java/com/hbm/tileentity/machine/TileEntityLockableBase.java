package com.hbm.tileentity.machine;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKey;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.ArmorUtil;
import com.hbm.util.BufferUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.IRORValueProvider;
import api.hbm.redstoneoverradio.RORFunctionException;

import com.hbm.tileentity.network.RTTYSystem;

public abstract class TileEntityLockableBase extends TileEntityLoadedBase implements IRORInteractive, IRORValueProvider {

	protected int lock;
	private boolean isLocked = false;
	protected double lockMod = 0.1D;

	// RoR fields
	protected String radioChannel = "";
	protected boolean radioMode = false;
	protected boolean radioLocked = false;

	// ROR listening bookkeeping to avoid reprocessing the same message
	protected long rorLastUpdate = -1L;

	/** Whether a counterfeit lock can be made out of it*/
	public boolean cheesable = true;
	public boolean isLocked() {
		return isLocked;
	}

	public void lock() {
		if (lock == 0) {
			MainRegistry.logger.error("A block has been set to locked state before setting pins, this should not happen and may cause errors! " + this.toString());
		}
		isLocked = true;
		markDirty();
	}

	public void unlock() {
		isLocked = false;
		markDirty();
	}

	public void setPins(int pins) { lock = pins; markDirty(); }
	public int getPins() { return lock; }
	public void setMod(double mod) { lockMod = mod; markDirty(); }
	public double getMod() { return lockMod; }

	public void setRadioChannel(String c) {
		this.radioChannel = c == null ? "" : c;
		this.markDirty();
	}
	public String getRadioChannel() { return this.radioChannel; }

	public void setRadioMode(boolean m) {
		this.radioMode = m;
		this.markDirty();
	}
	public boolean isRadioMode() { return this.radioMode; }

	public void setRadioLocked(boolean locked) {
		boolean changed = this.radioLocked != locked;
		this.radioLocked = locked;

		if (locked) {
			this.lock();
		} else {
			this.unlock();
		}

		this.markDirty();
		if (changed && this.worldObj != null) {
			this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			this.worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
		}
	}
	public boolean isRadioLocked() { return this.radioLocked; }

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (this.worldObj == null || this.worldObj.isRemote) return;

		if (!this.radioMode || this.radioChannel == null || this.radioChannel.isEmpty()) return;

		long now = this.worldObj.getTotalWorldTime();
		boolean shouldLog = ((now + xCoord + yCoord + zCoord) % 60L) == 0L;

		try {
			RTTYSystem.RTTYChannel chan = RTTYSystem.listen(worldObj, this.radioChannel);
			if (chan == null) {
				if (shouldLog) MainRegistry.logger.info("Padlock TE " + xCoord + "," + yCoord + "," + zCoord + " listen returned null for channel '" + this.radioChannel + "'");
				return;
			}

			long ts = chan.timeStamp;
			if (ts == -1L) {
				if (shouldLog) MainRegistry.logger.info("Padlock TE " + xCoord + "," + yCoord + "," + zCoord + " channel '" + this.radioChannel + "' has no messages (ts=-1)");
				return;
			}

			if (ts > this.rorLastUpdate) {
				this.rorLastUpdate = ts;
				String rec = "" + chan.signal;
				if (shouldLog) MainRegistry.logger.info("Padlock TE " + xCoord + "," + yCoord + "," + zCoord + " received RTTY '" + rec + "' on channel '" + this.radioChannel + "' (ts=" + ts + ")");
				try {
					this.runRORFunction(IRORInteractive.PREFIX_FUNCTION + IRORInteractive.getCommand(rec),
						IRORInteractive.getParams(rec));
				} catch (RORFunctionException e) {
					if (shouldLog) MainRegistry.logger.warn("runRORFunction failed on padlock TE: " + e.getMessage());
				}
			} else {
				if (shouldLog) MainRegistry.logger.info("Padlock TE " + xCoord + "," + yCoord + "," + zCoord + " no new RTTY (ts=" + ts + " <= last=" + this.rorLastUpdate + ")");
			}
		} catch (Throwable t) {
			if (shouldLog) MainRegistry.logger.warn("Error listening RTTY on padlock TE: " + t.getMessage());
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		lock = nbt.getInteger("lock");
		cheesable = nbt.getBoolean("cheesable");
		isLocked = nbt.getBoolean("isLocked");
		lockMod = nbt.getDouble("lockMod");

		radioChannel = nbt.getString("rchan");
		radioMode = nbt.getBoolean("rmode");
		radioLocked = nbt.getBoolean("rlocked");

		if (nbt.hasKey("rorLastUpdate")) {
			rorLastUpdate = nbt.getLong("rorLastUpdate");
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setInteger("lock", lock);
		nbt.setBoolean("cheesable", cheesable);
		nbt.setBoolean("isLocked", isLocked);
		nbt.setDouble("lockMod", lockMod);

		if (radioChannel != null) nbt.setString("rchan", radioChannel);
		nbt.setBoolean("rmode", radioMode);
		nbt.setBoolean("rlocked", radioLocked);

		nbt.setLong("rorLastUpdate", rorLastUpdate);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);

		buf.writeInt(lock);
		buf.writeBoolean(cheesable);
		buf.writeBoolean(isLocked);
		buf.writeDouble(lockMod);

		BufferUtil.writeString(buf, radioChannel);
		buf.writeBoolean(radioMode);
		buf.writeBoolean(radioLocked);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);

		lock = buf.readInt();
		cheesable = buf.readBoolean();
		isLocked = buf.readBoolean();
		lockMod = buf.readDouble();

		this.radioChannel = BufferUtil.readString(buf);
		this.radioMode = buf.readBoolean();
		this.radioLocked = buf.readBoolean();
	}

	public boolean canAccess(EntityPlayer player) {

		if (player == null) return false;

		ItemStack stack = player.getHeldItem();

		if (this.radioMode) {
			if (this.radioLocked) {
				if (stack != null && stack.getItem() == ModItems.key_red) {
					worldObj.playSoundAtEntity(player, "hbm:block.lockOpen", 1.0F, 1.0F);
					return true;
				}
				return false;
			} else {

				if (stack != null && stack.getItem() instanceof ItemKey && ItemKey.getPins(stack) == this.lock || stack != null && stack.getItem() == ModItems.key_red) {
					worldObj.playSoundAtEntity(player, "hbm:block.lockOpen", 1.0F, 1.0F);
					return true;
				}
				return false;
			}
		}

		if (!isLocked) {
			return true;
		}

		if (stack != null && stack.getItem() instanceof ItemKey && ItemKey.getPins(stack) == this.lock || stack != null && stack.getItem() == ModItems.key_red) {
			worldObj.playSoundAtEntity(player, "hbm:block.lockOpen", 1.0F, 1.0F);
			return true;
		}

		return false;
	}


	@Override
	public String[] getFunctionInfo() {
		return new String[] {
			IRORInteractive.PREFIX_FUNCTION + "lock",
			IRORInteractive.PREFIX_FUNCTION + "unlock",
			IRORInteractive.PREFIX_FUNCTION + "toggle",
			IRORInteractive.PREFIX_FUNCTION + "setlocked",
			IRORInteractive.PREFIX_FUNCTION + "setchannel",
			IRORInteractive.PREFIX_FUNCTION + "setradiomode",
			IRORValueProvider.PREFIX_VALUE + "isLocked",
			IRORValueProvider.PREFIX_VALUE + "channel",
			IRORValueProvider.PREFIX_VALUE + "radiomode",
			IRORValueProvider.PREFIX_VALUE + "radiolocked"
		};
	}

	@Override
	public String runRORFunction(String name, String[] params) {
		if (name == null) throw new RORFunctionException(IRORInteractive.EX_NULL);

		String cmd = name;
		if (cmd.startsWith(IRORInteractive.PREFIX_FUNCTION)) {
			cmd = cmd.substring(IRORInteractive.PREFIX_FUNCTION.length());
		}


		try {
			if ("lock".equalsIgnoreCase(cmd)) {
				setRadioLocked(true);
				if (this instanceof TileEntityBlastDoor) {
					TileEntityBlastDoor door = (TileEntityBlastDoor) this;
					if (door.canClose()) door.close();
				} else if (this instanceof TileEntityDoorGeneric) {
					TileEntityDoorGeneric door = (TileEntityDoorGeneric) this;
					door.close();
				}
				return "OK";
			}

			if ("unlock".equalsIgnoreCase(cmd)) {
				setRadioLocked(false);
				if (this instanceof TileEntityBlastDoor) {
					TileEntityBlastDoor door = (TileEntityBlastDoor) this;
					if (door.canOpen()) door.open();
				} else if (this instanceof TileEntityDoorGeneric) {
					TileEntityDoorGeneric door = (TileEntityDoorGeneric) this;
					door.open();
				}
				return "OK";
			}

			if ("toggle".equalsIgnoreCase(cmd)) {
				setRadioLocked(!this.radioLocked);
				return this.radioLocked ? "LOCKED" : "UNLOCKED";
			}

			if ("setlocked".equalsIgnoreCase(cmd)) {
				if (params == null || params.length == 0) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
				int val = IRORInteractive.parseInt(params[0], 0, 1);
				setRadioLocked(val == 1);
				if (this instanceof TileEntityBlastDoor) {
					TileEntityBlastDoor door = (TileEntityBlastDoor) this;
					if (val == 1 && door.canClose()) door.close();
					else if (val == 0 && door.canOpen()) door.open();
				} else if (this instanceof TileEntityDoorGeneric) {
					TileEntityDoorGeneric door = (TileEntityDoorGeneric) this;
					if (val == 1) door.close();
					else if (val == 0) door.open();
				}
				return "OK";
			}

			if ("setchannel".equalsIgnoreCase(cmd)) {
				if (params == null || params.length == 0) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
				setRadioChannel(params[0]);
				return "OK";
			}

			if ("setradiomode".equalsIgnoreCase(cmd)) {
				if (params == null || params.length == 0) throw new RORFunctionException(IRORInteractive.EX_FORMAT);
				int val = IRORInteractive.parseInt(params[0], 0, 1);
				setRadioMode(val == 1);
				return "OK";
			}

			// numeric shorthand: non-zero -> lock+close, zero -> unlock+open
			try {
				int n = Integer.parseInt(cmd);
				boolean shouldLock = n != 0;
				setRadioLocked(shouldLock);

				if (this instanceof TileEntityBlastDoor) {
					TileEntityBlastDoor door = (TileEntityBlastDoor) this;
					if (shouldLock) { if (door.canClose()) door.close(); }
					else { if (door.canOpen()) door.open(); }
				} else if (this instanceof TileEntityDoorGeneric) {
					TileEntityDoorGeneric door = (TileEntityDoorGeneric) this;
					if (shouldLock) { door.close(); }
					else { door.open(); }
				}

				return "OK";
			} catch (NumberFormatException nfe) {
				// not numeric — fall through
			}
		} catch (RORFunctionException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RORFunctionException("Exception: " + ex.getMessage());
		}

		throw new RORFunctionException("Exception: Unknown Command");
	}

	@Override
	public String provideRORValue(String name) {
		if (name == null) throw new RORFunctionException(IRORInteractive.EX_NULL);
		if (name.startsWith(IRORValueProvider.PREFIX_VALUE)) {
			String key = name.substring(IRORValueProvider.PREFIX_VALUE.length());
			if ("isLocked".equalsIgnoreCase(key)) {
				return (this.radioMode && this.radioLocked) ? "1" : (this.isLocked ? "1" : "0");
			}
			if ("channel".equalsIgnoreCase(key)) {
				return this.radioChannel == null ? "" : this.radioChannel;
			}
			if ("radiomode".equalsIgnoreCase(key)) {
				return this.radioMode ? "1" : "0";
			}
			if ("radiolocked".equalsIgnoreCase(key)) {
				return this.radioLocked ? "1" : "0";
			}
		}
		return null;
	}
}
