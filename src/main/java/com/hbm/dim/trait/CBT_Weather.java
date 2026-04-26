package com.hbm.dim.trait;

import java.util.Random;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystemWorldSavedData;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CBT_Weather extends CelestialBodyTrait {

	private static final Random WEATHER_RANDOM = new Random();
	private static final int SAVE_INTERVAL = 200;

	public boolean raining;
	public boolean thundering;
	public int rainTime;
	public int thunderTime;
	public float prevRainStrength;
	public float rainStrength;
	public float prevThunderStrength;
	public float thunderStrength;

	private long lastUpdateTick = Long.MIN_VALUE;

	public static boolean supportsWeather(CelestialBody body) {
		if(body == null || body.gas != null) {
			return false;
		}

		CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
		CBT_Water water = body.getTrait(CBT_Water.class);
		return atmosphere != null && atmosphere.getPressure() > 0.5D && water != null && water.fluid != null;
	}

	public static CBT_Weather ensureTrait(CelestialBody body) {
		if(body == null) {
			return null;
		}

		CBT_Weather weather = body.getTrait(CBT_Weather.class);
		if(weather == null && supportsWeather(body) && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
			body.modifyTraits(new CBT_Weather());
			weather = body.getTrait(CBT_Weather.class);
		}

		return weather;
	}

	public static void updateGlobalWeather() {
		MinecraftServer server = MinecraftServer.getServer();
		if(server == null) {
			return;
		}

		long tick = server.getTickCounter();
		boolean dirty = false;

		for(CelestialBody body : CelestialBody.getAllBodies()) {
			CBT_Weather weather = ensureTrait(body);
			if(weather == null) {
				continue;
			}

			WorldServer world = DimensionManager.getWorld(body.dimensionId);
			Random random = world != null ? world.rand : WEATHER_RANDOM;
			if(weather.updateForTick(tick, random, body)) {
				dirty = true;
			}
		}

		if(dirty) {
			SolarSystemWorldSavedData.get().markDirty();
		}
	}

	private static int getStormDuration(Random rand) {
		return rand.nextInt(12000) + 3600;
	}

	private static int getRainDuration(Random rand) {
		return rand.nextInt(12000) + 12000;
	}

	private static int getClearDuration(Random rand) {
		return rand.nextInt(168000) + 12000;
	}

	public void forceClear(Random rand, int duration) {
		raining = false;
		thundering = false;
		rainTime = Math.max(1, duration);
		thunderTime = getClearDuration(rand);
		prevRainStrength = 0.0F;
		rainStrength = 0.0F;
		prevThunderStrength = 0.0F;
		thunderStrength = 0.0F;
	}

	public void forceRain(Random rand, int duration) {
		raining = true;
		thundering = false;
		rainTime = Math.max(1, duration);
		thunderTime = getClearDuration(rand);
	}

	public void forceThunder(int duration) {
		raining = true;
		thundering = true;
		rainTime = Math.max(1, duration);
		thunderTime = Math.max(1, duration);
	}

	public boolean updateForTick(long tick, Random rand, CelestialBody body) {
		if(lastUpdateTick == tick) {
			return false;
		}

		lastUpdateTick = tick;

		if(!supportsWeather(body)) {
			boolean hadWeather = rainTime != 0
				|| thunderTime != 0
				|| raining
				|| thundering
				|| prevRainStrength > 0.0F
				|| rainStrength > 0.0F
				|| prevThunderStrength > 0.0F
				|| thunderStrength > 0.0F;

			rainTime = 0;
			thunderTime = 0;
			raining = false;
			thundering = false;
			prevRainStrength = 0.0F;
			rainStrength = 0.0F;
			prevThunderStrength = 0.0F;
			thunderStrength = 0.0F;
			return hadWeather;
		}

		boolean stateChanged = false;

		if(thunderTime <= 0) {
			thunderTime = thundering ? getStormDuration(rand) : getClearDuration(rand);
			stateChanged = true;
		} else {
			thunderTime--;
			if(thunderTime <= 0) {
				thundering = !thundering;
				thunderTime = thundering ? getStormDuration(rand) : getClearDuration(rand);
				stateChanged = true;
			}
		}

		prevThunderStrength = thunderStrength;
		if(thundering) {
			thunderStrength = MathHelper.clamp_float(thunderStrength + 0.01F, 0.0F, 1.0F);
		} else {
			thunderStrength = MathHelper.clamp_float(thunderStrength - 0.01F, 0.0F, 1.0F);
		}

		if(rainTime <= 0) {
			rainTime = raining ? getRainDuration(rand) : getClearDuration(rand);
			stateChanged = true;
		} else {
			rainTime--;
			if(rainTime <= 0) {
				raining = !raining;
				if(!raining) {
					thundering = false;
					thunderTime = getClearDuration(rand);
				}
				rainTime = raining ? getRainDuration(rand) : getClearDuration(rand);
				stateChanged = true;
			}
		}

		prevRainStrength = rainStrength;
		if(raining) {
			rainStrength = MathHelper.clamp_float(rainStrength + 0.01F, 0.0F, 1.0F);
		} else {
			rainStrength = MathHelper.clamp_float(rainStrength - 0.01F, 0.0F, 1.0F);
		}

		return stateChanged || Math.floorMod(body.dimensionId + (int) tick, SAVE_INTERVAL) == 0;
	}

	public float getRainStrength(float partialTicks) {
		return MathHelper.clamp_float(prevRainStrength + (rainStrength - prevRainStrength) * partialTicks, 0.0F, 1.0F);
	}

	public float getThunderStrength(float partialTicks) {
		return MathHelper.clamp_float(prevThunderStrength + (thunderStrength - prevThunderStrength) * partialTicks, 0.0F, 1.0F);
	}

	public float getWeightedThunderStrength(float partialTicks) {
		return getRainStrength(partialTicks) * getThunderStrength(partialTicks);
	}

	@Override
	public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
		nbt.setBoolean("raining", raining);
		nbt.setBoolean("thundering", thundering);
		nbt.setInteger("rainTime", rainTime);
		nbt.setInteger("thunderTime", thunderTime);
		nbt.setFloat("prevRainStrength", prevRainStrength);
		nbt.setFloat("rainStrength", rainStrength);
		nbt.setFloat("prevThunderStrength", prevThunderStrength);
		nbt.setFloat("thunderStrength", thunderStrength);
	}

	@Override
	public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
		raining = nbt.getBoolean("raining");
		thundering = nbt.getBoolean("thundering");
		rainTime = nbt.getInteger("rainTime");
		thunderTime = nbt.getInteger("thunderTime");
		prevRainStrength = nbt.getFloat("prevRainStrength");
		rainStrength = nbt.getFloat("rainStrength");
		prevThunderStrength = nbt.getFloat("prevThunderStrength");
		thunderStrength = nbt.getFloat("thunderStrength");
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeBoolean(raining);
		buf.writeBoolean(thundering);
		buf.writeInt(rainTime);
		buf.writeInt(thunderTime);
		buf.writeFloat(prevRainStrength);
		buf.writeFloat(rainStrength);
		buf.writeFloat(prevThunderStrength);
		buf.writeFloat(thunderStrength);
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		raining = buf.readBoolean();
		thundering = buf.readBoolean();
		rainTime = buf.readInt();
		thunderTime = buf.readInt();
		prevRainStrength = buf.readFloat();
		rainStrength = buf.readFloat();
		prevThunderStrength = buf.readFloat();
		thunderStrength = buf.readFloat();
	}
}
