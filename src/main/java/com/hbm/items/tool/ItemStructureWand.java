package com.hbm.items.tool;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import com.hbm.world.dungeon.AncientTomb;
import com.hbm.world.dungeon.Antenna;
import com.hbm.world.dungeon.ArcticVault;
import com.hbm.world.dungeon.Barrel;
import com.hbm.world.dungeon.DesertAtom001;
import com.hbm.world.dungeon.DesertAtom002;
import com.hbm.world.dungeon.DesertAtom003;
import com.hbm.world.dungeon.LibraryDungeon;
import com.hbm.world.dungeon.Ruin001;
import com.hbm.world.dungeon.Silo;
import com.hbm.world.dungeon.Spaceship;
import com.hbm.world.dungeon.Spaceship2;
import com.hbm.world.gen.component.BunkerComponents;
import com.hbm.world.gen.component.CivilianFeatures;
import com.hbm.world.gen.component.OfficeFeatures;
import com.hbm.world.gen.component.RuinFeatures;
import com.hbm.world.gen.component.SiloComponent;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class ItemStructureWand extends Item {

	// ── Structure registry ────────────────────────────────────────────────────

	private static final String[] STRUCTURE_NAMES = {
		"Ancient Tomb",        //  0
		"Antenna",             //  1
		"Arctic Vault",        //  2
		"Barrel Facility",     //  3
		"Desert Atom 001",     //  4
		"Desert Atom 002",     //  5
		"Desert Atom 003",     //  6
		"Library Dungeon",     //  7
		"Ruin 001",            //  8
		"Silo (Legacy)",       //  9
		"Spaceship",           // 10
		"Spaceship 2",         // 11
		"Bunker (Procedural)", // 12
		"Silo (Component)",    // 13
		"House (Sandstone 1)", // 14
		"House (Sandstone 2)", // 15
		"Lab 1",               // 16
		"Lab 2",               // 17
		"Rural House",         // 18
		"Concrete Ruin 1",     // 19
		"Concrete Ruin 2",     // 20
		"Concrete Ruin 3",     // 21
		"Concrete Ruin 4",     // 22
		"Large Office",        // 23
		"Large Office Corner", // 24
	};

	private static final int STRUCTURE_COUNT = STRUCTURE_NAMES.length;
	private static final String NBT_KEY = "StructureIndex";

	private static final double REACH = 50.0;

	// ── Constructor ───────────────────────────────────────────────────────────

	public ItemStructureWand() {
		super();
		setMaxStackSize(1);
		setMaxDamage(0);
	}

	// ── NBT helpers (also used by StructureWandScrollHandler) ─────────────────

	public static int getSelectedStructure(ItemStack stack) {
		return getIndex(stack);
	}

	private static int getIndex(ItemStack stack) {
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_KEY)) {
			int idx = stack.getTagCompound().getInteger(NBT_KEY);
			return Math.max(0, Math.min(idx, STRUCTURE_COUNT - 1));
		}
		return 0;
	}

	private static void setIndex(ItemStack stack, int index) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger(NBT_KEY, ((index % STRUCTURE_COUNT) + STRUCTURE_COUNT) % STRUCTURE_COUNT);
	}

	public static void shiftSelection(ItemStack stack, int delta) {
		setIndex(stack, getIndex(stack) + delta);
	}

	// ── Tooltip ───────────────────────────────────────────────────────────────

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
		int idx = getIndex(stack);
		list.add("\u00a77Selected: \u00a7a" + STRUCTURE_NAMES[idx]);
		list.add("\u00a77[" + (idx + 1) + " / " + STRUCTURE_COUNT + "]");
		list.add("\u00a78Right-click any block to spawn");
		list.add("\u00a78Shift+right-click to cycle forward");
	}

	// ── Item use ──────────────────────────────────────────────────────────────

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {

		// Shift+right-click: cycle to next structure (no block target required)
		if (player.isSneaking()) {
			if (!world.isRemote) {
				int next = (getIndex(stack) + 1) % STRUCTURE_COUNT;
				setIndex(stack, next);
				player.addChatMessage(new ChatComponentText(
					"\u00a7aStructure Gen Wand \u00a77\u00bb Selected: \u00a7a"
						+ STRUCTURE_NAMES[next]
						+ " \u00a77[" + (next + 1) + "/" + STRUCTURE_COUNT + "]"));
			}
			return stack;
		}

		MovingObjectPosition mop = longRangeRayTrace(world, player);

		if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) {
			if (!world.isRemote) {
				player.addChatMessage(new ChatComponentText(
					"\u00a7cNo block in range (max " + (int) REACH + " blocks). Aim at a block."));
			}
			return stack;
		}

		if (!world.isRemote) {
			// Spawn at the surface of the hit block (one block above it)
			int x = mop.blockX;
			int y = mop.blockY + 1;
			int z = mop.blockZ;

			int idx = getIndex(stack);
			boolean success = spawnStructure(world, new Random(), x, y, z, idx);

			if (success) {
				player.addChatMessage(new ChatComponentText(
					"\u00a7aSpawned \u00a7f" + STRUCTURE_NAMES[idx]
						+ "\u00a7a at " + x + ", " + y + ", " + z));
			} else {
				player.addChatMessage(new ChatComponentText(
					"\u00a7cFailed to spawn \u00a7f" + STRUCTURE_NAMES[idx]
						+ "\u00a7c — check console for details."));
			}
		}

		return stack;
	}

	// ── Long-range raycast ────────────────────────────────────────────────────
	private static MovingObjectPosition longRangeRayTrace(World world, EntityPlayer player) {
		Vec3 eye = Vec3.createVectorHelper(
			player.posX,
			player.posY + player.getEyeHeight(),
			player.posZ);

		Vec3 look = player.getLookVec();

		Vec3 end = Vec3.createVectorHelper(
			eye.xCoord + look.xCoord * REACH,
			eye.yCoord + look.yCoord * REACH,
			eye.zCoord + look.zCoord * REACH);

		return world.rayTraceBlocks(eye, end, false);
	}

	// ── Shared bounding box ───────────────────────────────────────────────────
	private static StructureBoundingBox fullWorldBox() {
		return new StructureBoundingBox(
			Integer.MIN_VALUE / 2, 0, Integer.MIN_VALUE / 2,
			Integer.MAX_VALUE / 2, 255, Integer.MAX_VALUE / 2);
	}

	// ── Structure spawn dispatcher ────────────────────────────────────────────
	private static boolean spawnStructure(World world, Random rand, int x, int y, int z, int index) {
		try {
			switch (index) {

				// ── 0  Ancient Tomb ─────────────────────────────────────────
				case 0: {
					new AncientTomb().build(world, rand, x, y, z);
					return true;
				}

				// ── 1  Antenna ──────────────────────────────────────────────
				// LocationIsValidSpawn checks a single offset corner; bypass it.
				case 1: {
					Antenna antenna = new Antenna() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return antenna.generate_r0(world, rand, x, y, z);
				}

				// ── 2  Arctic Vault ─────────────────────────────────────────
				// build() is private and trySpawn() checks biome temperature.
				// We bypass both via reflection so it always spawns.
				case 2: {
					ArcticVault vault = new ArcticVault();
					Method buildMethod = ArcticVault.class.getDeclaredMethod(
						"build", World.class, int.class, int.class, int.class);
					buildMethod.setAccessible(true);
					buildMethod.invoke(vault, world, x, y, z);
					return true;
				}

				// ── 3  Barrel Facility ──────────────────────────────────────
				// Checks all four corners; bypass LocationIsValidSpawn.
				case 3: {
					Barrel barrel = new Barrel() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return barrel.generate_r0(world, rand, x, y, z);
				}

				// ── 4  Desert Atom 001 ──────────────────────────────────────
				// Checks a point 20 blocks away; bypass.
				case 4: {
					DesertAtom001 da1 = new DesertAtom001() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return da1.generate_r0(world, rand, x, y, z);
				}

				// ── 5  Desert Atom 002 ──────────────────────────────────────
				case 5: {
					DesertAtom002 da2 = new DesertAtom002() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return da2.generate_r00(world, rand, x, y, z);
				}

				// ── 6  Desert Atom 003 ──────────────────────────────────────
				case 6: {
					DesertAtom003 da3 = new DesertAtom003() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return da3.generate_r00(world, rand, x, y, z);
				}

				// ── 7  Library Dungeon ──────────────────────────────────────
				// Checks four corners; bypass.
				case 7: {
					LibraryDungeon lib = new LibraryDungeon() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return lib.generate_r0(world, rand, x, y, z);
				}

				// ── 8  Ruin 001 ─────────────────────────────────────────────
				case 8: {
					Ruin001 ruin1 = new Ruin001() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return ruin1.generate_r0(world, rand, x, y - 8, z);
				}

				// ── 9  Silo (Legacy) ───────────────────────────────────────
				// Checks a corner 10 blocks away; bypass.
				case 9: {
					Silo silo = new Silo() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return silo.generate_r0(world, rand, x, y, z);
				}

				// ── 10  Spaceship ──────────────────────────────────────────
				// Checks four corners (up to x+12, z+23); bypass.
				case 10: {
					Spaceship ship = new Spaceship() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) {
							return true;
						}
					};
					return ship.generate_r0(world, rand, x, y, z);
				}

				// ── 11  Spaceship 2 ────────────────────────────────────────
				// generate_r00 has no location check.
				case 11: {
					return new Spaceship2().generate_r00(world, rand, x, y, z);
				}

				// ── 12  Bunker (Procedural) ────────────────────────────────
				case 12: {
					BunkerComponents.BunkerStart start =
						new BunkerComponents.BunkerStart(world, rand, x >> 4, z >> 4);
					start.generateStructure(world, rand, fullWorldBox());
					return true;
				}

				// ── 13  Silo (Component / new procedural) ──────────────────
				case 13: {
					SiloComponent sc = new SiloComponent(rand, x, z);
					sc.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 14  House (Sandstone 1) ────────────────────────────────
				case 14: {
					CivilianFeatures.NTMHouse1 house1 = new CivilianFeatures.NTMHouse1(rand, x, z);
					house1.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 15  House (Sandstone 2) ────────────────────────────────
				case 15: {
					CivilianFeatures.NTMHouse2 house2 = new CivilianFeatures.NTMHouse2(rand, x, z);
					house2.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 16  Lab 1 ──────────────────────────────────────────────
				case 16: {
					CivilianFeatures.NTMLab1 lab1 = new CivilianFeatures.NTMLab1(rand, x, z);
					lab1.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 17  Lab 2 ──────────────────────────────────────────────
				// Offsets boundingBox down by 7 internally to place the
				// underground section correctly.
				case 17: {
					CivilianFeatures.NTMLab2 lab2 = new CivilianFeatures.NTMLab2(rand, x, z);
					lab2.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 18  Rural House ────────────────────────────────────────
				case 18: {
					CivilianFeatures.RuralHouse1 rural = new CivilianFeatures.RuralHouse1(rand, x, z);
					rural.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 19  Concrete Ruin 1 ────────────────────────────────────
				case 19: {
					RuinFeatures.NTMRuin1 ruin1 = new RuinFeatures.NTMRuin1(rand, x, z);
					ruin1.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 20  Concrete Ruin 2 ────────────────────────────────────
				case 20: {
					RuinFeatures.NTMRuin2 ruin2 = new RuinFeatures.NTMRuin2(rand, x, z);
					ruin2.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 21  Concrete Ruin 3 ────────────────────────────────────
				case 21: {
					RuinFeatures.NTMRuin3 ruin3 = new RuinFeatures.NTMRuin3(rand, x, z);
					ruin3.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 22  Concrete Ruin 4 ────────────────────────────────────
				case 22: {
					RuinFeatures.NTMRuin4 ruin4 = new RuinFeatures.NTMRuin4(rand, x, z);
					ruin4.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 23  Large Office ───────────────────────────────────────
				// Offsets boundingBox down by 1 internally.
				case 23: {
					OfficeFeatures.LargeOffice office = new OfficeFeatures.LargeOffice(rand, x, z);
					office.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 24  Large Office Corner ────────────────────────────────
				case 24: {
					OfficeFeatures.LargeOfficeCorner corner = new OfficeFeatures.LargeOfficeCorner(rand, x, z);
					corner.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				default:
					return false;
			}
		} catch (Exception e) {
			System.err.println("[StructureGenWand] Exception spawning structure #" + index
				+ " (" + (index < STRUCTURE_NAMES.length ? STRUCTURE_NAMES[index] : "?") + "): " + e);
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void registerIcons(IIconRegister reg) {
		itemIcon = reg.registerIcon("hbm:structureWand");
	}
}
