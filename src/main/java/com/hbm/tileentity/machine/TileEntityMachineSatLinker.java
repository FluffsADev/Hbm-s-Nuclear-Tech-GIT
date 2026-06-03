package com.hbm.tileentity.machine;

import com.hbm.handler.CompatHandler;
import com.hbm.inventory.container.ContainerMachineSatLinker;
import com.hbm.inventory.gui.GUIMachineSatLinker;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteLaser;
import com.hbm.tileentity.IGUIProvider;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@Optional.InterfaceList({
	@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")
})
public class TileEntityMachineSatLinker extends TileEntity implements ISidedInventory, IGUIProvider, SimpleComponent, CompatHandler.OCComponent {

	private ItemStack[] slots;

	private static final int[] slots_top = new int[] {0};
	private static final int[] slots_bottom = new int[] {1};
	private static final int[] slots_side = new int[] {2};

	private String customName;

	public TileEntityMachineSatLinker() {
		slots = new ItemStack[3];
	}

	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		if(slots[i] != null) {
			ItemStack itemStack = slots[i];
			slots[i] = null;
			return itemStack;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemStack) {
		slots[i] = itemStack;
		if(itemStack != null && itemStack.stackSize > getInventoryStackLimit()) {
			itemStack.stackSize = getInventoryStackLimit();
		}
		markDirty();
	}

	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.satLinker";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}

	public void setCustomName(String name) {
		this.customName = name;
		markDirty();
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		if(worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		} else {
			return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64;
		}
	}

	@Override
	public void openInventory() { }

	@Override
	public void closeInventory() { }

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		return stack != null && stack.getItem() instanceof ISatChip;
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if(slots[i] != null) {
			if(slots[i].stackSize <= j) {
				ItemStack itemStack = slots[i];
				slots[i] = null;
				markDirty();
				return itemStack;
			}
			ItemStack itemStack1 = slots[i].splitStack(j);
			if(slots[i].stackSize == 0) {
				slots[i] = null;
			}
			markDirty();
			return itemStack1;
		} else {
			return null;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("items", 10);

		slots = new ItemStack[getSizeInventory()];

		for(int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt1 = list.getCompoundTagAt(i);
			byte b0 = nbt1.getByte("slot");
			if(b0 >= 0 && b0 < slots.length) {
				slots[b0] = ItemStack.loadItemStackFromNBT(nbt1);
			}
		}

		customName = nbt.getString("name");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		NBTTagList list = new NBTTagList();

		for(int i = 0; i < slots.length; i++) {
			if(slots[i] != null) {
				NBTTagCompound nbt1 = new NBTTagCompound();
				nbt1.setByte("slot", (byte)i);
				slots[i].writeToNBT(nbt1);
				list.appendTag(nbt1);
			}
		}
		nbt.setTag("items", list);

		if(customName != null) {
			nbt.setString("name", customName);
		}
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return side == 0 ? slots_bottom : (side == 1 ? slots_top : slots_side);
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemStack, int side) {
		return this.isItemValidForSlot(i, itemStack);
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int side) {
		return true;
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(slots[0] != null && slots[1] != null && slots[0].getItem() instanceof ISatChip && slots[1].getItem() instanceof ISatChip) {
				int srcFreq = ISatChip.getFreqS(slots[0]);
				if(ISatChip.getFreqS(slots[1]) != srcFreq) {
					ISatChip.setFreqS(slots[1], srcFreq);
					markDirty();
				}

				if(Satellite.isSatelliteItem(slots[0].getItem()) && Satellite.isSatelliteItem(slots[1].getItem())) {
					Satellite.copyItemData(slots[0], slots[1]);
					markDirty();
				}
			}

			if(slots[2] != null && slots[2].getItem() instanceof ISatChip) {
				SatelliteSavedData satelliteData = SatelliteSavedData.getData(worldObj, xCoord, zCoord);
				if(ISatChip.getFreqS(slots[2]) <= 0) {
					int newId = worldObj.rand.nextInt(100000);
					if(!satelliteData.isFreqTaken(newId)) {
						ISatChip.setFreqS(slots[2], newId);
						markDirty();
					}
				}
			}
		}
	}

	private ItemStack getControlChip() {
		if(slots[0] != null && slots[0].getItem() instanceof ISatChip) {
			return slots[0];
		}
		return null;
	}

	private int getControlFrequency() {
		ItemStack chip = getControlChip();
		return chip != null ? ISatChip.getFreqS(chip) : -1;
	}

	private SatelliteSavedData getLinkedSatelliteData() {
		int freq = getControlFrequency();
		if(freq < 0 || worldObj == null) {
			return null;
		}
		return SatelliteSavedData.getDataFromFreq(worldObj, xCoord, zCoord, freq);
	}

	private Satellite getLinkedSatellite() {
		int freq = getControlFrequency();
		if(freq < 0) {
			return null;
		}

		SatelliteSavedData data = getLinkedSatelliteData();
		return data != null ? data.getSatFromFreq(freq) : null;
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		return "sat_linker";
	}

	@Callback(direct = true, limit = 4, doc = "getFrequency() -- Returns the frequency of the control chip in slot 1.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getFrequency(Context context, Arguments args) {
		return new Object[] { getControlFrequency() };
	}

	@Callback(direct = true, limit = 4, doc = "hasSatellite() -- Returns whether the control chip is linked to a satellite.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] hasSatellite(Context context, Arguments args) {
		return new Object[] { getLinkedSatellite() != null };
	}

	@Callback(direct = true, limit = 4, doc = "getSatelliteType() -- Returns the linked satellite class name, or nil if none is linked.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getSatelliteType(Context context, Arguments args) {
		Satellite sat = getLinkedSatellite();
		return new Object[] { sat != null ? sat.getClass().getSimpleName() : null };
	}

	@Callback(direct = true, limit = 4, doc = "getSatelliteInfo() -- Returns basic information about the linked satellite.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getSatelliteInfo(Context context, Arguments args) {
		Satellite sat = getLinkedSatellite();

		if(sat == null) {
			return new Object[] { false, "no linked satellite" };
		}

		return new Object[] {
			true,
			sat.getClass().getSimpleName(),
			sat.owner,
			sat.altitude,
			sat.inclination,
			sat.phaseOffset,
			sat.isBlinking,
			sat.blinkPeriod
		};
	}

	@Callback(direct = true, limit = 4, doc = "getTargetableType() -- Returns the target control mode supported by the linked satellite.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getTargetableType(Context context, Arguments args) {
		Satellite sat = getLinkedSatellite();

		if(sat == null) {
			return new Object[] { null };
		}

		if(sat.satIface == Satellite.Interfaces.SAT_PANEL) {
			return new Object[] { "panel" };
		}

		if(sat.satIface == Satellite.Interfaces.SAT_COORD) {
			return new Object[] { "coord" };
		}

		return new Object[] { "none" };
	}

	@Callback(direct = true, limit = 4, doc = "isLaserReady() -- Returns whether the linked laser satellite is ready to fire.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] isLaserReady(Context context, Arguments args) {
		Satellite sat = getLinkedSatellite();

		if(sat == null) {
			return new Object[] { false, "no linked satellite" };
		}

		if(!(sat instanceof SatelliteLaser)) {
			return new Object[] { false, "linked satellite is not a laser" };
		}

		SatelliteLaser laser = (SatelliteLaser) sat;
		return new Object[] { true, laser.isReady() };
	}

	@Callback(direct = true, limit = 4, doc = "getLaserCooldown() -- Returns the remaining laser cooldown in milliseconds.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] getLaserCooldown(Context context, Arguments args) {
		Satellite sat = getLinkedSatellite();

		if(sat == null) {
			return new Object[] { false, "no linked satellite" };
		}

		if(!(sat instanceof SatelliteLaser)) {
			return new Object[] { false, "linked satellite is not a laser" };
		}

		SatelliteLaser laser = (SatelliteLaser) sat;
		return new Object[] { true, laser.getCooldownRemaining() };
	}

	@Callback(direct = true, limit = 1, doc = "fireLaser(x: number, z: number) -- Fires the linked laser satellite at the specified x/z coordinates.")
	@Optional.Method(modid = "OpenComputers")
	public Object[] fireLaser(Context context, Arguments args) {
		int x = args.checkInteger(0);
		int z = args.checkInteger(1);

		if(worldObj == null || worldObj.isRemote) {
			return new Object[] { false, "server side only" };
		}

		int freq = getControlFrequency();
		if(freq < 0) {
			return new Object[] { false, "no control chip in slot 1" };
		}

		SatelliteSavedData data = SatelliteSavedData.getDataFromFreq(worldObj, xCoord, zCoord, freq);
		if(data == null) {
			return new Object[] { false, "no satellite data found" };
		}

		Satellite sat = data.getSatFromFreq(freq);
		if(sat == null) {
			return new Object[] { false, "no linked satellite" };
		}

		if(!(sat instanceof SatelliteLaser)) {
			return new Object[] { false, "linked satellite is not a laser" };
		}

		SatelliteLaser laser = (SatelliteLaser) sat;

		if(!laser.isReady()) {
			return new Object[] { false, "laser is on cooldown", laser.getCooldownRemaining() };
		}

		boolean success = laser.trigger(worldObj, x, z);
		if(success) {
			data.markDirty();
		}

		return new Object[] { success };
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineSatLinker(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineSatLinker(player.inventory, this);
	}
}
