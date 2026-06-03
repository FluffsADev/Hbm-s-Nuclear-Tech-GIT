package com.hbm.items.tool;

import com.hbm.items.IItemControlReceiver;
import com.hbm.main.MainRegistry;
import com.hbm.main.NTMSounds;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.util.CompatExternal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.inventory.Container;

import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;


public class ItemPadlockRadio extends ItemLock implements IGUIProvider, IItemControlReceiver {

	public static final String KEY_POLLING = "p";
	public static final String KEY_CUSTOMMAP = "m";
	public static final String KEY_CHANNEL = "c";
	public static final String KEY_MAP_PREFIX = "m";

	public ItemPadlockRadio(double mod) {
		super(mod);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (player.isSneaking()) {
			if (world.isRemote) {
				openGUI(player);  // Call helper instead of direct Minecraft call
			}
			return stack;
		}
		return super.onItemRightClick(stack, world, player);
	}

	@SideOnly(Side.CLIENT)
	private void openGUI(EntityPlayer player) {
		ItemPadlockRadioClient.openPadlockGUI(player);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float f0, float f1, float f2) {

		if (player.isSneaking() && !world.isRemote) {
			TileEntity te = CompatExternal.getCoreFromPos(world, x, y, z);

			if (!(te instanceof TileEntityLockableBase)) {
				TileEntity raw = world.getTileEntity(x, y, z);
				if (raw instanceof TileEntityLockableBase) te = raw;
			}
			if (!(te instanceof TileEntityLockableBase)) {
				outer:
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							TileEntity nte = world.getTileEntity(x + dx, y + dy, z + dz);
							if (nte instanceof TileEntityLockableBase) {
								te = nte;
								break outer;
							}
						}
					}
				}
			}

			if (te instanceof TileEntityLockableBase) {
				TileEntityLockableBase tile = (TileEntityLockableBase) te;

				if (!tile.isLocked() && !tile.isRadioMode()) {
					if (this.getPins(stack) != 0) {
						tile.setPins(this.getPins(stack));
						tile.lock();
						tile.setMod(this.lockMod);

						String appliedChannel = null;
						if (stack.hasTagCompound()) {
							NBTTagCompound tag = stack.stackTagCompound;
							if (tag.hasKey(KEY_CHANNEL) && !tag.getString(KEY_CHANNEL).isEmpty()) {
								appliedChannel = tag.getString(KEY_CHANNEL);
							} else {
								for (int i = 0; i < 16; i++) {
									String k = KEY_MAP_PREFIX + i;
									if (tag.hasKey(k)) {
										String val = tag.getString(k);
										if (val != null && !val.isEmpty()) {
											appliedChannel = val;
											break;
										}
									}
								}
							}
						} else if (stack.hasDisplayName()) {
							appliedChannel = stack.getDisplayName();
						}

						if (appliedChannel != null) {
							tile.setRadioChannel(appliedChannel);
						}
						tile.setRadioMode(true);

						tile.markDirty();
						world.markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);

						world.playSoundAtEntity(player, NTMSounds.PADLOCK, 1.0F, 1.0F);
						stack.stackSize--;

						return true;
					}
				} else if (tile.isRadioMode()) {
					return true;
				}
			}
		}

		boolean placed = super.onItemUse(stack, player, world, x, y, z, side, f0, f1, f2);

		if (placed && !world.isRemote) {
			TileEntity te2 = CompatExternal.getCoreFromPos(world, x, y, z);
			if (te2 != null && te2 instanceof TileEntityLockableBase) {
				TileEntityLockableBase tile = (TileEntityLockableBase) te2;

				String appliedChannel = null;
				if (stack.hasTagCompound()) {
					NBTTagCompound tag = stack.stackTagCompound;
					if (tag.hasKey(KEY_CHANNEL) && !tag.getString(KEY_CHANNEL).isEmpty()) {
						appliedChannel = tag.getString(KEY_CHANNEL);
					} else {
						for (int i = 0; i < 16; i++) {
							String k = KEY_MAP_PREFIX + i;
							if (tag.hasKey(k)) {
								String val = tag.getString(k);
								if (val != null && !val.isEmpty()) {
									appliedChannel = val;
									break;
								}
							}
						}
					}
				} else if (stack.hasDisplayName()) {
					appliedChannel = stack.getDisplayName();
				}

				if (appliedChannel != null) tile.setRadioChannel(appliedChannel);
				tile.setRadioMode(true);

				tile.markDirty();
				world.markBlockForUpdate(tile.xCoord, tile.yCoord, tile.zCoord);

				world.playSoundAtEntity(player, NTMSounds.PADLOCK, 1.0F, 1.0F);
			}
			return true;
		}

		if (!placed && player.isSneaking()) {
			if (world.isRemote) {
				openGUI(player);
			}
			return true;
		}

		return placed;
	}

	@Override
	public void receiveControl(ItemStack stack, NBTTagCompound data) {
		if (stack == null || data == null) return;
		if (!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();

		if (data.hasKey(KEY_POLLING)) stack.stackTagCompound.setBoolean(KEY_POLLING, data.getBoolean(KEY_POLLING));
		if (data.hasKey(KEY_CUSTOMMAP)) stack.stackTagCompound.setBoolean(KEY_CUSTOMMAP, data.getBoolean(KEY_CUSTOMMAP));
		if (data.hasKey(KEY_CHANNEL)) stack.stackTagCompound.setString(KEY_CHANNEL, data.getString(KEY_CHANNEL));

		for (int i = 0; i < 16; i++) {
			String k = KEY_MAP_PREFIX + i;
			if (data.hasKey(k)) stack.stackTagCompound.setString(k, data.getString(k));
		}
	}

	@Override
	public void addInformation(ItemStack stack, net.minecraft.entity.player.EntityPlayer player, java.util.List list, boolean bool) {
		super.addInformation(stack, player, list, bool);

		String firstChan = null;
		if (stack.hasTagCompound()) {
			NBTTagCompound tag = stack.stackTagCompound;
			if (tag.hasKey(KEY_CHANNEL) && !tag.getString(KEY_CHANNEL).isEmpty()) {
				firstChan = tag.getString(KEY_CHANNEL);
			} else {
				for (int i = 0; i < 16; i++) {
					String k = KEY_MAP_PREFIX + i;
					if (tag.hasKey(k)) {
						String v = tag.getString(k);
						if (v != null && !v.isEmpty()) {
							firstChan = v;
							break;
						}
					}
				}
			}
		}

		if (firstChan != null) {
			list.add("Channel: " + firstChan);
		} else if (stack.hasDisplayName()) {
			list.add("Channel: " + stack.getDisplayName());
		} else {
			list.add("No channel set (open GUI to set)");
		}

		list.add(EnumChatFormatting.YELLOW + "Shift right-click to set frequency");
		list.add(EnumChatFormatting.YELLOW+ "Placeable on Mechanical Doors");
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new com.hbm.inventory.gui.GUIScreenPadlockReceiver(player);
	}
}
