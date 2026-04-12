package com.hbm.saveddata.satellites;

import com.hbm.dim.SolarSystem;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.util.AstronomyUtil;
import com.hbm.util.BufferUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL11;

public abstract class Satellite {

	public static final List<Class<? extends Satellite>> satellites = new ArrayList<>();
	public static final HashMap<Item, Class<? extends Satellite>> itemToClass = new HashMap<>();
	private static final HashMap<Class<? extends Satellite>, float[]> satelliteColors = new HashMap<>();
	public static final float DEFAULT_INCLINATION = 0F;
	public static final float MAX_INCLINATION = 360.0F;
	public static final float DEFAULT_ALTITUDE_KM = AstronomyUtil.DEFAULT_ALTITUDE_KM;
	public static final float MIN_ALTITUDE_KM = 80.0F;
	public static final float MAX_ALTITUDE_KM = 125.0F;
	public static final float DEFAULT_BLINK_PERIOD = 0.0F;
	public static final float MIN_BLINK_PERIOD = 0.3F;
	public static final String DEFAULT_OWNER = "None";
	private static final float ORBIT_SPEED_MIN = 0.9F;
	private static final float ORBIT_SPEED_RANGE = 0.2F;

	private static final ResourceLocation satelliteTexture = new ResourceLocation(RefStrings.MODID, "textures/misc/space/satellite.png");

	public enum InterfaceActions {
		HAS_MAP,		//lets the interface display loaded chunks
		CAN_CLICK,		//enables onClick events
		SHOW_COORDS,	//enables coordinates as a mouse tooltip
		HAS_RADAR,		//lets the interface display loaded entities
		HAS_ORES		//like HAS_MAP but only shows ores
	}

	public enum CoordActions {
		HAS_Y		//enables the Y-coord field which is disabled by default
	}

	public enum Interfaces {
		NONE,		//does not interact with any sat interface (i.e. asteroid miners)
		SAT_PANEL,	//allows to interact with the sat interface panel (for graphical applications)
		SAT_COORD	//allows to interact with the sat coord remote (for teleportation or other coord related actions)
	}

	public List<InterfaceActions> ifaceAcs = new ArrayList<>();
	public List<CoordActions> coordAcs = new ArrayList<>();
	public Interfaces satIface = Interfaces.NONE;
	public float inclination = DEFAULT_INCLINATION;
	public float altitude = DEFAULT_ALTITUDE_KM;
	public float blinkPeriod = DEFAULT_BLINK_PERIOD;
	public String owner = DEFAULT_OWNER;
	public float colorR;
	public float colorG;
	public float colorB;

	public static void register() {
		registerSatellite(SatelliteMapper.class, ModItems.sat_mapper, 0.538F, 1.0F, 0.523F);
		registerSatellite(SatelliteScanner.class, ModItems.sat_scanner, 0.544F, 0.680F, 1.0F);
		registerSatellite(SatelliteRadar.class, ModItems.sat_radar, 0.134F, 1.0F, 0.134F);
		registerSatellite(SatelliteLaser.class, ModItems.sat_laser, 0.221F, 0.663F, 1.0F);
		registerSatellite(SatelliteResonator.class, ModItems.sat_resonator, 1.0F, 0.646F, 0.181F);
		registerSatellite(SatelliteFoeq.class, ModItems.sat_foeq, 0.0F, 0.0F, 0.0F);
		registerSatellite(SatelliteMiner.class, ModItems.sat_miner, 0.0F, 0.0F, 0.0F);
		registerSatellite(SatelliteLunarMiner.class, ModItems.sat_lunar_miner, 0.0F, 0.0F, 0.0F);
		registerSatellite(SatelliteDysonRelay.class, ModItems.sat_dyson_relay, 1.0F, 0.9F, 0.8F);
		registerSatellite(SatelliteHorizons.class, ModItems.sat_gerald, 0.0F, 0.0F, 0.0F);
		registerSatellite(SatelliteRailgun.class, ModItems.sat_war, 0.0F, 0.0F, 0.0F);
	}

	/**
	 * Register satellite.
	 * @param sat - Satellite class
	 * @param item - Satellite item (which will be placed in a rocket)
	 */
	public static void registerSatellite(Class<? extends Satellite> sat, Item item, float r, float g, float b) {
		if(!itemToClass.containsKey(item) && !itemToClass.containsValue(sat)) {
			satellites.add(sat);
			itemToClass.put(item, sat);
			satelliteColors.put(sat, new float[] { r, g, b });
		}
	}

	public static boolean isSatelliteItem(Item item) {
		return itemToClass.containsKey(item);
	}

	public static void ensureItemData(ItemStack stack) {
		getItemData(stack);
	}

	private static NBTTagCompound getItemData(ItemStack stack) {
		NBTTagCompound nbt = stack.stackTagCompound;
		if(nbt == null) {
			nbt = new NBTTagCompound();
			float[] color = getRegisteredColor(stack.getItem());
			nbt.setFloat("satInclination", DEFAULT_INCLINATION);
			nbt.setFloat("satAltitude", DEFAULT_ALTITUDE_KM);
			nbt.setFloat("satBlink", DEFAULT_BLINK_PERIOD);
			nbt.setString("satOwner", DEFAULT_OWNER);
			nbt.setFloat("satColorR", color[0]);
			nbt.setFloat("satColorG", color[1]);
			nbt.setFloat("satColorB", color[2]);
			stack.stackTagCompound = nbt;
		}

		return nbt;
	}

	public static float getInclination(ItemStack stack) {
		return sanitizeInclination(getItemData(stack).getFloat("satInclination"));
	}

	public static float getAltitude(ItemStack stack) {
		return sanitizeAltitude(getItemData(stack).getFloat("satAltitude"));
	}

	public static String getOwner(ItemStack stack) {
		return getItemData(stack).getString("satOwner");
	}

	public static float getColorR(ItemStack stack) {
		return getItemData(stack).getFloat("satColorR");
	}

	public static float getColorG(ItemStack stack) {
		return getItemData(stack).getFloat("satColorG");
	}

	public static float getColorB(ItemStack stack) {
		return getItemData(stack).getFloat("satColorB");
	}

	public static float getBlinkPeriod(ItemStack stack) {
		return sanitizeBlinkPeriod(getItemData(stack).getFloat("satBlink"));
	}

	public static void setInclination(ItemStack stack, float inclination) {
		getItemData(stack).setFloat("satInclination", sanitizeInclination(inclination));
	}

	public static void setAltitude(ItemStack stack, float altitude) {
		getItemData(stack).setFloat("satAltitude", sanitizeAltitude(altitude));
	}

	public static void setOwner(ItemStack stack, String owner) {
		getItemData(stack).setString("satOwner", owner);
	}

	public static void setColor(ItemStack stack, float r, float g, float b) {
		NBTTagCompound nbt = getItemData(stack);
		nbt.setFloat("satColorR", r);
		nbt.setFloat("satColorG", g);
		nbt.setFloat("satColorB", b);
	}

	public static void setBlinkPeriod(ItemStack stack, float blinkPeriod) {
		getItemData(stack).setFloat("satBlink", sanitizeBlinkPeriod(blinkPeriod));
	}

	public static void copyItemData(ItemStack from, ItemStack to) {
		if(to == null) return;
		setInclination(to, getInclination(from));
		setAltitude(to, getAltitude(from));
		setOwner(to, getOwner(from));
		setColor(to, getColorR(from), getColorG(from), getColorB(from));
		setBlinkPeriod(to, getBlinkPeriod(from));
	}

	public static void orbit(World world, int id, int freq, double x, double y, double z, ItemStack stack) {
		if(world.isRemote) {
			return;
		}

		Satellite sat = create(id);

		if(sat != null) {
			if(sat instanceof SatelliteFoeq) world = DimensionManager.getWorld(SolarSystem.Body.DUNA.getDimensionId()); // dunaian probe goes to duna
			if(sat instanceof SatelliteLunarMiner) world = DimensionManager.getWorld(SolarSystem.Body.MUN.getDimensionId()); // lunar miner goes to mun
			if(sat instanceof SatelliteMiner) world = DimensionManager.getWorld(SolarSystem.Body.DRES.getDimensionId()); // asteroid miner goes to dres, logical explanation is to mine the dres' asteroid ring
			
			sat.inclination = getInclination(stack);
			sat.altitude = getAltitude(stack);
			sat.blinkPeriod = getBlinkPeriod(stack);
			sat.owner = getOwner(stack);
			sat.colorR = getColorR(stack);
			sat.colorG = getColorG(stack);
			sat.colorB = getColorB(stack);

			SatelliteSavedData data = SatelliteSavedData.getData(world, (int)x, (int)z);
			data.sats.put(freq, sat);
			sat.onOrbit(world, x, y, z);
			data.markDirty();
		}
	}

	public static Satellite create(int id) {
		Satellite sat = null;

		try {
			Class<? extends Satellite> c = satellites.get(id);
			sat = c.newInstance();
			float[] color = getRegisteredColor(c);
			sat.colorR = color[0];
			sat.colorG = color[1];
			sat.colorB = color[2];
		} catch(Exception e) {
			e.printStackTrace();
		}

		return sat;
	}

	public static int getIDFromItem(Item item) {
		Class<? extends Satellite> sat = itemToClass.get(item);

		return satellites.indexOf(sat);
	}

	public int getID() {
		return satellites.indexOf(this.getClass());
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setFloat("satInclination", sanitizeInclination(inclination));
		nbt.setFloat("satAltitude", sanitizeAltitude(altitude));
		nbt.setFloat("satBlink", blinkPeriod);
		nbt.setString("satOwner", owner);
		nbt.setFloat("satColorR", colorR);
		nbt.setFloat("satColorG", colorG);
		nbt.setFloat("satColorB", colorB);
	}
	public void readFromNBT(NBTTagCompound nbt) {
		inclination = sanitizeInclination(nbt.getFloat("satInclination"));
		altitude = nbt.hasKey("satAltitude") ? sanitizeAltitude(nbt.getFloat("satAltitude")) : sanitizeAltitude(DEFAULT_ALTITUDE_KM);
		blinkPeriod = nbt.hasKey("satBlink") ? sanitizeBlinkPeriod(nbt.getFloat("satBlink")) : DEFAULT_BLINK_PERIOD;
		owner = nbt.hasKey("satOwner") ? nbt.getString("satOwner") : DEFAULT_OWNER;
		float[] registeredColor = getRegisteredColor(getClass());
		colorR = nbt.hasKey("satColorR") ? nbt.getFloat("satColorR") : registeredColor[0];
		colorG = nbt.hasKey("satColorG") ? nbt.getFloat("satColorG") : registeredColor[1];
		colorB = nbt.hasKey("satColorB") ? nbt.getFloat("satColorB") : registeredColor[2];
	}

	public void serialize(ByteBuf buf) {
		buf.writeFloat(sanitizeInclination(inclination));
		buf.writeFloat(sanitizeAltitude(altitude));
		BufferUtil.writeString(buf, owner);
		buf.writeFloat(colorR);
		buf.writeFloat(colorG);
		buf.writeFloat(colorB);
		buf.writeFloat(blinkPeriod);
	}
	public void deserialize(ByteBuf buf) {
		inclination = sanitizeInclination(buf.readFloat());
		altitude = sanitizeAltitude(buf.readFloat());
		owner = BufferUtil.readString(buf);
		colorR = buf.readFloat();
		colorG = buf.readFloat();
		colorB = buf.readFloat();
		blinkPeriod = sanitizeBlinkPeriod(buf.readFloat());
	}

	/**
	 * Called when the satellite reaches space, used to trigger achievements and other funny stuff.
	 * @param x posX of the rocket
	 * @param y ditto
	 * @param z ditto
	 */
	public void onOrbit(World world, double x, double y, double z) { }

	/**
	 * Called by the sat interface when clicking on the screen
	 * @param x the x-coordinate translated from the on-screen coords to actual world coordinates
	 * @param z ditto
	 */
	public void onClick(World world, int x, int z) { }

	/**
	 * Called by the coord sat interface
	 * @param x the specified x-coordinate
	 * @param y ditto
	 * @param z ditto
	 */
	public void onCoordAction(World world, EntityPlayer player, int x, int y, int z) { }


	public void render(float partialTicks, WorldClient world, Minecraft mc, float solarAngle, long id) {
		renderDefault(partialTicks, world, mc, solarAngle, id, colorR, colorG, colorB, inclination, altitude, blinkPeriod);
	}

	public static void renderDefault(float partialTicks, WorldClient world, Minecraft mc, float solarAngle, long seed, float r, float g, float b, float inclination, float altitude, float blinkPeriod) {
		Tessellator tessellator = Tessellator.instance;

		double ticks = (double)System.currentTimeMillis() / 50.0D;
		float orbitAngle = applyFrequencyToOrbitAngle(seed, (ticks / 600.0D) * -360.0D, 360.0F);
		float renderAltitude = Math.max(1.0F, altitude);

		GL11.glPushMatrix();
		{

			GL11.glRotatef(solarAngle * -360.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(inclination, 0.0F, 0.0F, 1.0F);
			GL11.glRotated(orbitAngle, 1.0F, 0.0F, 0.0F);

			GL11.glColor4f(r, g, b, getBlinkAlpha(blinkPeriod));

			mc.renderEngine.bindTexture(satelliteTexture);

			float size = 0.5F;

			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-size, renderAltitude, -size, 0.0D, 0.0D);
			tessellator.addVertexWithUV(size, renderAltitude, -size, 0.0D, 1.0D);
			tessellator.addVertexWithUV(size, renderAltitude, size, 1.0D, 1.0D);
			tessellator.addVertexWithUV(-size, renderAltitude, size, 1.0D, 0.0D);
			tessellator.draw();

		}
		GL11.glPopMatrix();
	}

	public static float applyFrequencyToOrbitAngle(long frequency, double baseAngle, float fullRotation) {
		double speed = ORBIT_SPEED_MIN + getFrequencyFloat(frequency, 0x9E3779B97F4A7C15L) * ORBIT_SPEED_RANGE;
		double phase = getFrequencyFloat(frequency, 0xC2B2AE3D27D4EB4FL) * fullRotation;
		double angle = baseAngle * speed + phase;
		double wrapped = angle % fullRotation;
		if(wrapped < 0.0D) wrapped += fullRotation;
		return (float)wrapped;
	}

	private static float getFrequencyFloat(long frequency, long salt) {
		long x = frequency + salt;
		x ^= (x >>> 30);
		x *= 0xBF58476D1CE4E5B9L;
		x ^= (x >>> 27);
		x *= 0x94D049BB133111EBL;
		x ^= (x >>> 31);
		return (float)((x & 0xFFFFFFL) / (double)0x1000000L);
	}

	// killing myself
	public float getInterp() {
		return 0;
	}

	public static float sanitizeBlinkPeriod(float blinkPeriod) {
		if(Float.isNaN(blinkPeriod) || Float.isInfinite(blinkPeriod) || blinkPeriod <= 0.0F) {
			return DEFAULT_BLINK_PERIOD;
		}
		return Math.max(MIN_BLINK_PERIOD, blinkPeriod);
	}

	public static float sanitizeInclination(float inclination) {
		if(Float.isNaN(inclination) || Float.isInfinite(inclination)) {
			return DEFAULT_INCLINATION;
		}
		return MathHelper.clamp_float(inclination, DEFAULT_INCLINATION, MAX_INCLINATION);
	}

	public static float sanitizeAltitude(float altitude) {
		if(Float.isNaN(altitude) || Float.isInfinite(altitude)) {
			return MathHelper.clamp_float(DEFAULT_ALTITUDE_KM, MIN_ALTITUDE_KM, MAX_ALTITUDE_KM);
		}
		return MathHelper.clamp_float(altitude, MIN_ALTITUDE_KM, MAX_ALTITUDE_KM);
	}

	private static float getBlinkAlpha(float blinkPeriod) {
		float sanitizedBlink = sanitizeBlinkPeriod(blinkPeriod);
		if(sanitizedBlink <= 0.0F) {
			return 1.0F;
		}
		long cycleMillis = (long)(sanitizedBlink * 1000.0F);
		return 1.0F - (float)(System.currentTimeMillis() % cycleMillis) / cycleMillis;
	}

	private static float[] getRegisteredColor(Item item) {
		Class<? extends Satellite> satelliteClass = itemToClass.get(item);
		if(satelliteClass == null) {
			throw new IllegalStateException("No satellite class registered for item: " + item);
		}
		return getRegisteredColor(satelliteClass);
	}

	private static float[] getRegisteredColor(Class<? extends Satellite> satelliteClass) {
		float[] color = satelliteColors.get(satelliteClass);
		if(color == null) {
			throw new IllegalStateException("No color registered for satellite class: " + satelliteClass);
		}
		return color;
	}

}
