package com.hbm.tileentity.network;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.hbm.util.BufferUtil;
import com.hbm.util.Compat;
import com.hbm.util.CompatExternal;
import com.hbm.main.MainRegistry;

import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.RORFunctionException;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityRadioTorchController extends TileEntityLoadedBase implements IControlReceiver {

	public String channel = "";
	public String prev;
	public boolean polling = true;

	@Override
	public void updateEntity() {

		if(!worldObj.isRemote) {

			if(channel != null && !channel.isEmpty()) {
				ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata()).getOpposite();

				int tx = xCoord + dir.offsetX;
				int ty = yCoord + dir.offsetY;
				int tz = zCoord + dir.offsetZ;


				TileEntity tile = CompatExternal.getCoreFromPos(worldObj, tx, ty, tz);

				if (!(tile instanceof IRORInteractive)) {
					tile = Compat.getTileStandard(worldObj, tx, ty, tz);
				}


				if (!(tile instanceof IRORInteractive)) {
					outer:
					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							for (int dz = -1; dz <= 1; dz++) {
								TileEntity candidate = CompatExternal.getCoreFromPos(worldObj, tx + dx, ty + dy, tz + dz);
								if (candidate instanceof IRORInteractive) {
									tile = candidate;
									break outer;
								}
							}
						}
					}
				}

				if(tile instanceof IRORInteractive) {
					IRORInteractive ror = (IRORInteractive) tile;

					RTTYChannel chan = RTTYSystem.listen(worldObj, channel);
					if(chan != null) {
						String rec = "" + chan.signal;
						MainRegistry.logger.info("RTTY controller at " + xCoord + "," + yCoord + "," + zCoord + " heard on channel '" + channel + "': '" + rec + "' (ts=" + chan.timeStamp + ")");

						if("selfdestruct".equals(rec)) {
							worldObj.func_147480_a(xCoord, yCoord, zCoord, false);
							ExplosionVNT vnt = new ExplosionVNT(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, 5, null);
							vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
							vnt.setPlayerProcessor(new PlayerProcessorStandard());
							vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
							vnt.explode();
							return;
						}
						if((this.polling && chan.timeStamp >= worldObj.getTotalWorldTime() - 1) || !rec.equals(prev)) {
							try {
								if(rec != null && !rec.isEmpty()) {
									MainRegistry.logger.info("Controller invoking runRORFunction on adjacent TE with command: '" + IRORInteractive.PREFIX_FUNCTION + IRORInteractive.getCommand(rec) + "' params=" + java.util.Arrays.toString(IRORInteractive.getParams(rec)));
									ror.runRORFunction(IRORInteractive.PREFIX_FUNCTION + IRORInteractive.getCommand(rec), IRORInteractive.getParams(rec));
								}
							} catch(RORFunctionException ex) {
								MainRegistry.logger.warn("RORFunctionException in controller invocation: " + ex.getMessage());
							}
							prev = rec;
						}
					}
				}
			}

			networkPackNT(50);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(this.polling);
		BufferUtil.writeString(buf, channel);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.polling = buf.readBoolean();
		channel = BufferUtil.readString(buf);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.polling = nbt.getBoolean("p");
		channel = nbt.getString("c");
		this.prev = nbt.getString("prev");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("p", polling);
		nbt.setString("c", channel);
		if(prev != null) nbt.setString("prev", prev);
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("p")) this.polling = data.getBoolean("p");
		if(data.hasKey("c")) channel = data.getString("c");
		this.markDirty();
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return player.getDistance(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 16D;
	}
}
