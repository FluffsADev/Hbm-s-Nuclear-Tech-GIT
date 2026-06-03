package com.hbm.items.tool;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import com.hbm.blocks.ModBlocks;
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
import com.hbm.world.gen.terrain.MapGenBubble;
import com.hbm.main.StructureManager;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.NBTStructure;

import net.minecraft.block.Block;
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
		"Spire",               // 25
		"Vertibird",           // 26
		"Crashed Vertibird",   // 27
		"Beached Patrol",      // 28
		"Aircraft Carrier",    // 29
		"Oil Rig",             // 30
		"Lighthouse",          // 31
		"Dish",                // 32
		"Forest Chem",         // 33
		"Laboratory",          // 34
		"Forest Post",         // 35
		"Radio House",         // 36
		"Factory",             // 37
		"Crane",               // 38
		"Broadcasting Tower",  // 39
		"Crashed Plane 1",     // 40
		"Crashed Plane 2",     // 41
		"Desert Shack 1",      // 42
		"Desert Shack 2",      // 43
		"Desert Shack 3",      // 44
		"NTM Ruins A",         // 45
		"NTM Ruins B",         // 46
		"NTM Ruins C",         // 47
		"NTM Ruins D",         // 48
		"NTM Ruins E",         // 49
		"NTM Ruins F",         // 50
		"NTM Ruins G",         // 51
		"NTM Ruins H",         // 52
		"NTM Ruins I",         // 53
		"NTM Ruins J",         // 54
		"Oil Bubble",          // 55
	};

	private static final int STRUCTURE_COUNT = STRUCTURE_NAMES.length;
	private static final String NBT_KEY = "StructureIndex";

	private static final double REACH = 50.0;

	// ── Cached JigsawPiece instances ──────────────────────────────────────────
	private static JigsawPiece[] jigsawPieces = new JigsawPiece[55];

	static {
		// Initialize cached jigsaw pieces (25-54)
		jigsawPieces[25] = new JigsawPiece("wand_spire", StructureManager.spire, -1);
		jigsawPieces[26] = new JigsawPiece("wand_vertibird", StructureManager.vertibird, -3);
		jigsawPieces[27] = new JigsawPiece("wand_crashed_vertibird", StructureManager.crashed_vertibird, -10);
		jigsawPieces[28] = new JigsawPiece("wand_beached_patrol", StructureManager.beached_patrol, -5);
		jigsawPieces[29] = new JigsawPiece("wand_aircraft_carrier", StructureManager.aircraft_carrier, -6);
		jigsawPieces[30] = new JigsawPiece("wand_oil_rig", StructureManager.oil_rig, -20);
		jigsawPieces[31] = new JigsawPiece("wand_lighthouse", StructureManager.lighthouse, -40);
		jigsawPieces[32] = new JigsawPiece("wand_dish", StructureManager.dish, -10);

		JigsawPiece forestChemPiece = new JigsawPiece("wand_forest_chem", StructureManager.forest_chem, -9);
		jigsawPieces[33] = forestChemPiece;

		jigsawPieces[34] = new JigsawPiece("wand_laboratory", StructureManager.laboratory, -10);
		jigsawPieces[35] = new JigsawPiece("wand_forest_post", StructureManager.forest_post, -10);
		jigsawPieces[36] = new JigsawPiece("wand_radio_house", StructureManager.radio_house, -6);
		jigsawPieces[37] = new JigsawPiece("wand_factory", StructureManager.factory, -10);
		jigsawPieces[38] = new JigsawPiece("wand_crane", StructureManager.crane, -9);
		jigsawPieces[39] = new JigsawPiece("wand_broadcasting_tower", StructureManager.broadcasting_tower, -9);
		jigsawPieces[40] = new JigsawPiece("wand_crashed_plane_1", StructureManager.plane1, -5);
		jigsawPieces[41] = new JigsawPiece("wand_crashed_plane_2", StructureManager.plane2, -8);
		jigsawPieces[42] = new JigsawPiece("wand_desert_shack_1", StructureManager.desert_shack_1, -7);
		jigsawPieces[43] = new JigsawPiece("wand_desert_shack_2", StructureManager.desert_shack_2, -7);
		jigsawPieces[44] = new JigsawPiece("wand_desert_shack_3", StructureManager.desert_shack_3, -5);

		JigsawPiece ntmRuinsAPiece = new JigsawPiece("wand_ntm_ruins_a", StructureManager.ntmruinsA, -1);
		ntmRuinsAPiece.conformToTerrain = true;
		jigsawPieces[45] = ntmRuinsAPiece;

		JigsawPiece ntmRuinsBPiece = new JigsawPiece("wand_ntm_ruins_b", StructureManager.ntmruinsB, -1);
		ntmRuinsBPiece.conformToTerrain = true;
		jigsawPieces[46] = ntmRuinsBPiece;

		JigsawPiece ntmRuinsCPiece = new JigsawPiece("wand_ntm_ruins_c", StructureManager.ntmruinsC, -1);
		ntmRuinsCPiece.conformToTerrain = true;
		jigsawPieces[47] = ntmRuinsCPiece;

		JigsawPiece ntmRuinsDPiece = new JigsawPiece("wand_ntm_ruins_d", StructureManager.ntmruinsD, -1);
		ntmRuinsDPiece.conformToTerrain = true;
		jigsawPieces[48] = ntmRuinsDPiece;

		JigsawPiece ntmRuinsEPiece = new JigsawPiece("wand_ntm_ruins_e", StructureManager.ntmruinsE, -1);
		ntmRuinsEPiece.conformToTerrain = true;
		jigsawPieces[49] = ntmRuinsEPiece;

		JigsawPiece ntmRuinsFPiece = new JigsawPiece("wand_ntm_ruins_f", StructureManager.ntmruinsF, -1);
		ntmRuinsFPiece.conformToTerrain = true;
		jigsawPieces[50] = ntmRuinsFPiece;

		JigsawPiece ntmRuinsGPiece = new JigsawPiece("wand_ntm_ruins_g", StructureManager.ntmruinsG, -1);
		ntmRuinsGPiece.conformToTerrain = true;
		jigsawPieces[51] = ntmRuinsGPiece;

		JigsawPiece ntmRuinsHPiece = new JigsawPiece("wand_ntm_ruins_h", StructureManager.ntmruinsH, -1);
		ntmRuinsHPiece.conformToTerrain = true;
		jigsawPieces[52] = ntmRuinsHPiece;

		JigsawPiece ntmRuinsIPiece = new JigsawPiece("wand_ntm_ruins_i", StructureManager.ntmruinsI, -1);
		ntmRuinsIPiece.conformToTerrain = true;
		jigsawPieces[53] = ntmRuinsIPiece;

		JigsawPiece ntmRuinsJPiece = new JigsawPiece("wand_ntm_ruins_j", StructureManager.ntmruinsJ, -1);
		ntmRuinsJPiece.conformToTerrain = true;
		jigsawPieces[54] = ntmRuinsJPiece;
	}

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

	private static boolean spawnNBTStructure(World world, Random rand, int x, int y, int z, String name, JigsawPiece piece) {
		if (piece == null || piece.structure == null) {
			System.err.println("[StructureGenWand] Missing NBT structure piece for " + name);
			return false;
		}

		piece.structure.build(world, piece, x, y, z, rand.nextInt(4), name);
		return true;
	}

	private static boolean spawnOilBubble(World world, Random rand, int x, int y, int z) {
		try {
			// Import needed
			net.minecraft.block.Block oilBlock = ModBlocks.ore_oil; // Replace with actual oil block
			net.minecraft.block.Block airBlock = net.minecraft.init.Blocks.air;
			net.minecraft.block.Block stoneBlock = net.minecraft.init.Blocks.stone;

			// Spawn depth: 30-56 blocks underground
			int depth = 30 + rand.nextInt(26);
			int oilCenterY = y - depth;

			// Target: ~200 oil blocks in a roughly spherical cluster
			int blocksPlaced = 0;
			int maxBlocks = 200;
			int radius = 8; // Initial search radius

			// Create a natural-looking oil pocket using random walk
			int currentX = x;
			int currentY = oilCenterY;
			int currentZ = z;

			while (blocksPlaced < maxBlocks) {
				// Random walk to create irregular shape
				currentX += rand.nextInt(3) - 1; // -1, 0, or 1
				currentY += rand.nextInt(3) - 1;
				currentZ += rand.nextInt(3) - 1;

				// Clamp to reasonable bounds
				currentY = Math.max(5, Math.min(currentY, 100));

				// Check if we're in stone
				net.minecraft.block.Block blockAtPos = world.getBlock(currentX, currentY, currentZ);

				if (blockAtPos == stoneBlock) {
					world.setBlock(currentX, currentY, currentZ, oilBlock, 0, 2);
					blocksPlaced++;

					// Also fill neighboring stones occasionally for more natural look
					if (rand.nextInt(3) == 0) {
						for (int i = 0; i < 3; i++) {
							int nx = currentX + rand.nextInt(3) - 1;
							int ny = currentY + rand.nextInt(3) - 1;
							int nz = currentZ + rand.nextInt(3) - 1;

							net.minecraft.block.Block neighborBlock = world.getBlock(nx, ny, nz);
							if (neighborBlock == stoneBlock && blocksPlaced < maxBlocks) {
								world.setBlock(nx, ny, nz, oilBlock, 0, 2);
								blocksPlaced++;
							}
						}
					}
				}

				// Prevent infinite loops - if we've wandered too far, reset closer to origin
				int distX = Math.abs(currentX - x);
				int distZ = Math.abs(currentZ - z);
				if (distX > 20 || distZ > 20) {
					currentX = x + rand.nextInt(5) - 2;
					currentZ = z + rand.nextInt(5) - 2;
				}
			}

			return true;
		} catch (Exception e) {
			System.err.println("[StructureGenWand] Error spawning oil bubble: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	// ── Structure spawn dispatcher ────────────────────────────────────────────
	private static boolean spawnStructure(World world, Random rand, int x, int y, int z, int index) {
		try {
			switch (index) {

				// ── 0-24: Legacy and component structures (unchanged) ────────

				case 0: new AncientTomb().build(world, rand, x, y, z); return true;

				case 1: {
					Antenna antenna = new Antenna() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return antenna.generate_r0(world, rand, x, y, z);
				}

				case 2: {
					ArcticVault vault = new ArcticVault();
					Method buildMethod = ArcticVault.class.getDeclaredMethod("build", World.class, int.class, int.class, int.class);
					buildMethod.setAccessible(true);
					buildMethod.invoke(vault, world, x, y, z);
					return true;
				}

				case 3: {
					Barrel barrel = new Barrel() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return barrel.generate_r0(world, rand, x, y, z);
				}

				case 4: {
					DesertAtom001 da1 = new DesertAtom001() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return da1.generate_r0(world, rand, x, y, z);
				}

				case 5: {
					DesertAtom002 da2 = new DesertAtom002() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return da2.generate_r00(world, rand, x, y, z);
				}

				case 6: {
					DesertAtom003 da3 = new DesertAtom003() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return da3.generate_r00(world, rand, x, y, z);
				}

				case 7: {
					LibraryDungeon lib = new LibraryDungeon() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return lib.generate_r0(world, rand, x, y, z);
				}

				case 8: {
					Ruin001 ruin1 = new Ruin001() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return ruin1.generate_r0(world, rand, x, y - 8, z);
				}

				case 9: {
					Silo silo = new Silo() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return silo.generate_r0(world, rand, x, y, z);
				}

				case 10: {
					Spaceship ship = new Spaceship() {
						@Override
						public boolean LocationIsValidSpawn(World w, int bx, int by, int bz) { return true; }
					};
					return ship.generate_r0(world, rand, x, y, z);
				}

				case 11: return new Spaceship2().generate_r00(world, rand, x, y, z);

				case 12: {
					BunkerComponents.BunkerStart start = new BunkerComponents.BunkerStart(world, rand, x >> 4, z >> 4);
					start.generateStructure(world, rand, fullWorldBox());
					return true;
				}

				case 13: {
					SiloComponent sc = new SiloComponent(rand, x, z);
					sc.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 14: {
					CivilianFeatures.NTMHouse1 house1 = new CivilianFeatures.NTMHouse1(rand, x, z);
					house1.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 15: {
					CivilianFeatures.NTMHouse2 house2 = new CivilianFeatures.NTMHouse2(rand, x, z);
					house2.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 16: {
					CivilianFeatures.NTMLab1 lab1 = new CivilianFeatures.NTMLab1(rand, x, z);
					lab1.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 17: {
					CivilianFeatures.NTMLab2 lab2 = new CivilianFeatures.NTMLab2(rand, x, z);
					lab2.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 18: {
					CivilianFeatures.RuralHouse1 rural = new CivilianFeatures.RuralHouse1(rand, x, z);
					rural.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 19: {
					RuinFeatures.NTMRuin1 ruin1 = new RuinFeatures.NTMRuin1(rand, x, z);
					ruin1.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 20: {
					RuinFeatures.NTMRuin2 ruin2 = new RuinFeatures.NTMRuin2(rand, x, z);
					ruin2.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 21: {
					RuinFeatures.NTMRuin3 ruin3 = new RuinFeatures.NTMRuin3(rand, x, z);
					ruin3.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 22: {
					RuinFeatures.NTMRuin4 ruin4 = new RuinFeatures.NTMRuin4(rand, x, z);
					ruin4.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 23: {
					OfficeFeatures.LargeOffice office = new OfficeFeatures.LargeOffice(rand, x, z);
					office.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				case 24: {
					OfficeFeatures.LargeOfficeCorner corner = new OfficeFeatures.LargeOfficeCorner(rand, x, z);
					corner.addComponentParts(world, rand, fullWorldBox());
					return true;
				}

				// ── 25-54: NBT/Jigsaw structures ────────────────────────────

				case 25: return spawnNBTStructure(world, rand, x, y, z, "spire", jigsawPieces[25]);
				case 26: return spawnNBTStructure(world, rand, x, y, z, "vertibird", jigsawPieces[26]);
				case 27: return spawnNBTStructure(world, rand, x, y - 5, z, "crashed_vertibird", jigsawPieces[27]);
				case 28: return spawnNBTStructure(world, rand, x, y - 2, z, "beached_patrol", jigsawPieces[28]);
				case 29: return spawnNBTStructure(world, rand, x, y - 10, z, "aircraft_carrier", jigsawPieces[29]);
				case 30: return spawnNBTStructure(world, rand, x, y - 50, z, "oil_rig", jigsawPieces[30]);
				case 31: return spawnNBTStructure(world, rand, x, y, z, "lighthouse", jigsawPieces[31]);
				case 32: return spawnNBTStructure(world, rand, x, y, z, "dish", jigsawPieces[32]);
				case 33: return spawnNBTStructure(world, rand, x, y - 8, z, "forest_chem", jigsawPieces[33]);
				case 34: return spawnNBTStructure(world, rand, x, y - 9, z, "laboratory", jigsawPieces[34]);
				case 35: return spawnNBTStructure(world, rand, x, y - 9, z, "forest_post", jigsawPieces[35]);
				case 36: return spawnNBTStructure(world, rand, x, y - 6, z, "radio_house", jigsawPieces[36]);
				case 37: return spawnNBTStructure(world, rand, x, y - 10, z, "factory", jigsawPieces[37]);
				case 38: return spawnNBTStructure(world, rand, x, y - 5, z, "crane", jigsawPieces[38]);
				case 39: return spawnNBTStructure(world, rand, x, y - 9, z, "broadcasting_tower", jigsawPieces[39]);
				case 40: return spawnNBTStructure(world, rand, x, y - 4, z, "crashed_plane_1", jigsawPieces[40]);
				case 41: return spawnNBTStructure(world, rand, x, y - 4, z, "crashed_plane_2", jigsawPieces[41]);
				case 42: return spawnNBTStructure(world, rand, x, y - 8, z, "desert_shack_1", jigsawPieces[42]);
				case 43: return spawnNBTStructure(world, rand, x, y - 7, z, "desert_shack_2", jigsawPieces[43]);
				case 44: return spawnNBTStructure(world, rand, x, y - 6, z, "desert_shack_3", jigsawPieces[44]);
				case 45: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsA", jigsawPieces[45]);
				case 46: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsB", jigsawPieces[46]);
				case 47: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsC", jigsawPieces[47]);
				case 48: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsD", jigsawPieces[48]);
				case 49: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsE", jigsawPieces[49]);
				case 50: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsF", jigsawPieces[50]);
				case 51: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsG", jigsawPieces[51]);
				case 52: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsH", jigsawPieces[52]);
				case 53: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsI", jigsawPieces[53]);
				case 54: return spawnNBTStructure(world, rand, x, y, z, "NTMRuinsJ", jigsawPieces[54]);

				// ── 55  Oil Bubble ──────────────────────────────────────────
				case 55: return spawnOilBubble(world, rand, x, y, z);

				default: return false;
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
