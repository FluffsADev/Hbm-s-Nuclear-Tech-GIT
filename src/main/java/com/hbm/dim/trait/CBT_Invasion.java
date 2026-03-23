package com.hbm.dim.trait;

import com.hbm.entity.mob.siege.EntitySiegeCraft;
import com.hbm.entity.mob.siege.EntitySiegeUFO;

import io.netty.buffer.ByteBuf;

import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.entity.missile.EntityCombatDropPod;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.siege.EntitySiegeCraft;
import com.hbm.main.MainRegistry;
import com.hbm.util.ContaminationUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class CBT_Invasion extends CelestialBodyTrait{

	//while i could polymorphize to the heavens, this event is more-or-less "scripted" in the sense that you would be fighting the ufo types we HAVE sequentially to the boss
	//oh dont worry you still get to have fun killing! :)
	
	public int wave;
	public int kills;
	public double waveTime;
	public boolean isInvading;
	public int lastSpawns; //prevent over-lagging the server
	public int spawndelay;
	
	public int podBurstCounter = 0;
	public int podCooldown = 0;
	public boolean bossSpawned = false;
	
	public boolean warningPlayed;
	
	public CBT_Invasion() {
		
	}
	


	public CBT_Invasion(int wave, double waveTime, boolean isInvading) {
		this.wave = wave;
		this.waveTime = waveTime;
		this.isInvading = isInvading;
	}
	
	public void Prepare() {

		if (!isInvading && waveTime >= 0) {
			waveTime--;
			warningPlayed = true;
			if (waveTime <= 5) {
				warningPlayed = false;

			}
			if (waveTime <= 0) {

				isInvading = true;
			}

		}
		if(isInvading) {
			return;
		}

	}
	
	public void Invade(int killReq, double wavetimerbase) {
		if(!isInvading) return;
		waveTime--;
		if(waveTime <= 0 || kills % 10 == 0) {
			wave++;
			waveTime = wavetimerbase;
		}
		
	}
	
	@Override
	public void update(boolean isRemote, CelestialBody body) {
		if (!isRemote) {
			Prepare();
			World world = DimensionManager.getWorld(body.dimensionId);
			if (isInvading) {
	
				if (world == null || world.playerEntities.isEmpty())
					return;

				LogicTick(world);
				HandleBurstSpawning(world);
				SpawnAttempt(world);
			} else {
				if (!isInvading && !warningPlayed) {
					warningPlayed = true;
					System.out.println(isInvading);
					MainRegistry.proxy.me().playSound("hbm:alarm.ping", 10F, 1F);
					MainRegistry.proxy.me().addChatComponentMessage(
							new ChatComponentText("Incoming Invasion!").setChatStyle(
									new ChatStyle().setColor(EnumChatFormatting.RED)));
				}

			}
		}

	}
	public void SpawnCattle(World world) {
		EntityPlayer player = (EntityPlayer) world.playerEntities.get(world.rand.nextInt(world.playerEntities.size()));
		if (!(player instanceof EntityPlayerMP)) return;

		EntityPlayerMP playerMP = (EntityPlayerMP) player;
		Random rand = world.rand;

		EntityCombatDropPod pod = new EntityCombatDropPod(world);
		pod.posX = playerMP.posX + (rand.nextGaussian() * 15);
		pod.posY = 250;
		pod.posZ = playerMP.posZ + (rand.nextGaussian() * 15);
		pod.motionY = -1.5;
		
		    EntityGlyphid glyph;

		    if (wave == 1) {
			    glyph = new EntityGlyphid(world);

		    } else if (wave == 2) {
		        int roll = rand.nextInt(3);
		        switch (roll) {
		            case 0: glyph = new EntityGlyphid(world); break;
		            case 1: glyph = new EntityGlyphidBrawler(world); break;
		            default: glyph = new EntityGlyphidDigger(world); break;
		        }

		    } else if (wave >= 3) {
		        int roll = rand.nextInt(5);
		        switch (roll) {
		            case 0: glyph = new EntityGlyphid(world); break;
		            case 1: glyph = new EntityGlyphidBrawler(world); break;
		            case 2: glyph = new EntityGlyphidDigger(world); break;
		            case 3: glyph = new EntityGlyphidBlaster(world); break;
		            default: glyph = new EntityGlyphidBehemoth(world); break;
		        }

		    } else {
			    glyph = new EntityGlyphid(world);
		    }

		    NBTTagCompound nbt = new NBTTagCompound();
		    nbt.setString("id", EntityList.getEntityString(glyph));
		    glyph.writeToNBT(nbt);

		    pod.setPayload(nbt, 2, 2); 

		    world.spawnEntityInWorld(pod);

	}
	
	
	private void HandleBurstSpawning(World world) {
		if (wave > 3) return;
		if (podCooldown > 0) { podCooldown--; return; }
		Random rand = world.rand;

		if (world.getTotalWorldTime() % 10 + world.rand.nextInt(3) == 0) {
			SpawnCattle(world);
			podBurstCounter++;
			if (podBurstCounter >= 3+(wave-1)) {
				podBurstCounter = 0;
				podCooldown = 500;
			}
		}
	}
	private void LogicTick(World world) {
		if(!isInvading) return;
		switch (wave) {
		case 0: advanceWave(world); 
		break;
		case 1: if (kills >= 1) advanceWave(world); 
		break;
		case 2: if (kills >= 22) advanceWave(world); 
		break;
		case 3: if (kills >= 152) advanceWave(world); 
		break;
		case 4:
			if (!bossSpawned) {
				spawnBoss(world);
				bossSpawned = true;
			}
			break;
		}
	}

	private void advanceWave(World world) {
		wave++;
		kills = 0;
		broadcast(world, "Wave " + (wave == 4 ? "FINAL" : wave) + " is starting!", EnumChatFormatting.GOLD);
	}
	
	public void SpawnAttempt(World world) {
		
		if (wave > 3) return;
		    int timer = 200; 
		    if (wave == 2) timer = 100; 
		    if (wave == 3) timer = 80; 

		    if (world.getTotalWorldTime() % timer == 0) {
		        EntityPlayer player = (EntityPlayer) world.playerEntities.get(world.rand.nextInt(world.playerEntities.size()));
		        
		        double spawnX = player.posX + world.rand.nextGaussian() * 30;
		        double spawnZ = player.posZ + world.rand.nextGaussian() * 30;
		        double spawnY = player.posY + 30 + world.rand.nextInt(20);

		        
		        float bigUfoChance = 0.0F;

		        if (wave == 2) {
		            bigUfoChance = 0.2F; 
		        } else if (wave == 3) {
		            bigUfoChance = 0.5F; 
		        }

		        if (world.rand.nextFloat() < bigUfoChance) {
		            EntitySiegeCraft bigUfo = new EntitySiegeCraft(world);
		            bigUfo.setLocationAndAngles(spawnX, spawnY, spawnZ, world.rand.nextFloat() * 360.0F, 0.0F);
		            world.spawnEntityInWorld(bigUfo);
		        } else {
		            EntitySiegeUFO smallUfo = new EntitySiegeUFO(world);
		            smallUfo.setLocationAndAngles(spawnX, spawnY, spawnZ, world.rand.nextFloat() * 360.0F, 0.0F);
		            world.spawnEntityInWorld(smallUfo);
		        }
		        
		        lastSpawns++;
		    }
		}
	private void spawnBoss(World world) {
		EntityPlayer player = (EntityPlayer) world.playerEntities.get(0);
		
	}
	
	private void broadcast(World world, String text, EnumChatFormatting color) {
		for (Object p : world.playerEntities) {
			if (p instanceof EntityPlayer) {
				((EntityPlayer) p).addChatComponentMessage(new ChatComponentText(text)
						.setChatStyle(new ChatStyle().setColor(color).setBold(true)));
			}
		}
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("wave", wave);
		nbt.setInteger("kills", kills);
		nbt.setDouble("waveTime", waveTime);
		nbt.setBoolean("isInvading", isInvading);
		nbt.setBoolean("warningPlayed", warningPlayed);
		nbt.setInteger("podBurst", podBurstCounter);
		nbt.setInteger("podCooldown", podCooldown);
		nbt.setBoolean("bossSpawned", bossSpawned);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		wave = nbt.getInteger("wave");
		kills = nbt.getInteger("kills");
		waveTime = nbt.getDouble("waveTime");
		isInvading = nbt.getBoolean("isInvading");
		warningPlayed = nbt.getBoolean("warningPlayed");
		podBurstCounter = nbt.getInteger("podBurst");
		podCooldown = nbt.getInteger("podCooldown");
		bossSpawned = nbt.getBoolean("bossSpawned");
	}

	@Override
	public void writeToBytes(ByteBuf buf) {
		buf.writeInt(wave);
		buf.writeInt(kills);
		buf.writeDouble(waveTime);
		buf.writeBoolean(isInvading);
		buf.writeBoolean(warningPlayed);
		buf.writeInt(podBurstCounter);
		buf.writeInt(podCooldown);
		buf.writeBoolean(bossSpawned);
	}

	@Override
	public void readFromBytes(ByteBuf buf) {
		wave = buf.readInt();
		kills = buf.readInt();
		waveTime = buf.readDouble();
		isInvading = buf.readBoolean();
		warningPlayed = buf.readBoolean();
		podBurstCounter = buf.readInt();
		podCooldown = buf.readInt();
		bossSpawned = buf.readBoolean();
	}
}
