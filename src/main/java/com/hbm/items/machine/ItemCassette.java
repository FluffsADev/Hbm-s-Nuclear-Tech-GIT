package com.hbm.items.machine;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

public class ItemCassette extends Item {

	IIcon overlayIcon;
	IIcon passIcon;
	IIcon soundIcon;

	public enum TrackType {

		NULL(				" ", 						null,															SoundType.SOUND,	0,			0),
		HATCH(				"Hatch Siren", 				new ResourceLocation("hbm:alarm.hatch"),				SoundType.LOOP,		3358839,	250),
		AUTOPILOT(			"Autopilot Disconnected", 	new ResourceLocation("hbm:alarm.autopilot"),			SoundType.LOOP,		11908533,	50),
		AMS_SIREN(			"AMS Siren", 				new ResourceLocation("hbm:alarm.amsSiren"),				SoundType.LOOP,		15055698,	50),
		BLAST_DOOR(			"Blast Door Alarm", 		new ResourceLocation("hbm:alarm.blastDoorAlarm"),		SoundType.LOOP,		11665408,	50),
		APC_LOOP(			"APC Siren", 				new ResourceLocation("hbm:alarm.apcLoop"),				SoundType.LOOP,		3565216,	50),
		APC_PASS_2(			"APC Siren Pass V2", 		new ResourceLocation("hbm:alarm.apcPass2"),			SoundType.PASS,		3565216,	50),
		KLAXON(				"Klaxon", 					new ResourceLocation("hbm:alarm.klaxon"),				SoundType.LOOP,		8421504,	50),
		KLAXON_PASS(		"Klaxon Pass", 				new ResourceLocation("hbm:alarm.klaxonPass"),			SoundType.PASS,		8421504,	50),
		KLAXON_A(			"Vault Door Alarm",			new ResourceLocation("hbm:alarm.foKlaxonA"),			SoundType.LOOP,		0x8c810b,	50),
		KLAXON_B(			"Security Alert", 			new ResourceLocation("hbm:alarm.foKlaxonB"),			SoundType.LOOP,		0x76818e,	50),
		SIREN(				"Standard Siren", 			new ResourceLocation("hbm:alarm.regularSiren"),		SoundType.LOOP,		6684672,	100),
		SIREN_PASS(			"Standard Siren Pass", 		new ResourceLocation("hbm:alarm.regularsirenPass"),	SoundType.PASS,		6684672,	100),
		CLASSIC(			"Classic Siren", 			new ResourceLocation("hbm:alarm.classic"),				SoundType.LOOP,		0xc0cfe8,	100),
		BANK_ALARM(			"Bank Alarm", 				new ResourceLocation("hbm:alarm.bankAlarm"),			SoundType.LOOP,		3572962,	100),
		BANK_ALARM_PASS(	"Bank Alarm Pass", 			new ResourceLocation("hbm:alarm.bankPass"),				SoundType.PASS,		3572962,	100),
		BEEP_SIREN(			"Beep Siren", 				new ResourceLocation("hbm:alarm.beepSiren"),			SoundType.LOOP,		13882323,	100),
		CONTAINER_ALARM(	"Container Alarm", 			new ResourceLocation("hbm:alarm.containerAlarm"),		SoundType.LOOP,		14727839,	100),
		SWEEP_SIREN(		"Sweep Siren", 				new ResourceLocation("hbm:alarm.sweepSiren"),			SoundType.LOOP,		15592026,	500),
		STRIDER_SIREN(		"Missile Silo Siren", 		new ResourceLocation("hbm:alarm.striderSiren"),		SoundType.LOOP,		11250586,	500),
		AIR_RAID(			"Air Raid Siren", 			new ResourceLocation("hbm:alarm.airRaid"),				SoundType.LOOP,		0xDF3795,	500),
		NOSTROMO_SIREN(		"Nostromo Self Destruct",	new ResourceLocation("hbm:alarm.nostromoSiren"),		SoundType.LOOP,		0x5dd800,	100),
		EAS_ALARM(			"EAS Alarm Screech",		new ResourceLocation("hbm:alarm.easAlarm"),				SoundType.LOOP,		0xb3a8c1,	50),
		INTRUDE_ALERT_PASS(	"Intruder Alert Pass",		new ResourceLocation("hbm:alarm.intrudealarmpass"),	SoundType.PASS,		26624,		50),
		INTRUDE_ALERT_ALARM("Intruder Alert Alarm",		new ResourceLocation("hbm:alarm.intrudealarmLoop"),	SoundType.LOOP,		26624,		50),
		APC_PASS(			"APC Pass", 				new ResourceLocation("hbm:alarm.apcPass"),				SoundType.PASS,		3422163,	50),
		MANHACK(			"Manhack Alarm", 			new ResourceLocation("hbm:alarm.manhackLoop"),			SoundType.LOOP,		16711769,	50),
		MANHACK_PASS(		"Manhack Alarm Pass", 		new ResourceLocation("hbm:alarm.manhackPass"),			SoundType.PASS,		65280  ,	50),
		INDUST_ALARM(		"Industrial Alarm", 		new ResourceLocation("hbm:alarm.industalarmLoop"),		SoundType.LOOP,		255 ,	100),
		INDUST_ALARM_PASS(	"Industrial Alarm Pass", 	new ResourceLocation("hbm:alarm.industalarmPass"),		SoundType.PASS,		16776960 ,	100),
		RAZORTRAIN(			"Razortrain Horn", 			new ResourceLocation("hbm:alarm.razortrainHorn"),		SoundType.SOUND,	7819501,	250),
		DETECT_ALARM(		"Detection Alarm 1", 		new ResourceLocation("hbm:alarm.detectPass"),			SoundType.LOOP,		16711935 ,	50),
		DETECT_ALARM_PASS(	"Detection Alarm 1 Pass", 	new ResourceLocation("hbm:alarm.detectPass"),			SoundType.PASS,		65535 ,	50),
		DETECT_ALARM_2(		"Detection Alarm 2", 		new ResourceLocation("hbm:alarm.detectPass2"),			SoundType.LOOP,		16777215 ,	50),
		DETECT_ALARM_2_PASS("Detection Alarm 2 Pass", 	new ResourceLocation("hbm:alarm.detectPass2"),			SoundType.PASS,		8355711 ,	50),
		SECURITY_ALERT(		"Security Alarm", 			new ResourceLocation("hbm:alarm.securityalertLoop"),	SoundType.LOOP,		16744448 ,	50),
		SECURITY_ALERT_PASS("Security Alarm Pass", 		new ResourceLocation("hbm:alarm.securityalertPass"),	SoundType.PASS,		13421772 ,	50),
		BREACH_ALARM(		"Breach Alarm", 			new ResourceLocation("hbm:alarm.breachLoop"),			SoundType.LOOP,		1644825 ,	50),
		BREACH_ALARM_PASS(	"Breach Alarm Pass", 		new ResourceLocation("hbm:alarm.breachPass"),			SoundType.PASS,		16764108,	50),
		EMERGENCY_ALARM(	"Emergency Alarm", 			new ResourceLocation("hbm:alarm.emergencyLoop"),		SoundType.LOOP,		7451646 ,	50),
		EMERGENCY_ALARM_PASS("Emergency Alarm Pass", 	new ResourceLocation("hbm:alarm.emergencyPass"),		SoundType.PASS,		10079487 ,	50),
		LOCKDOWN_ALARM(		"Lockdown Alarm", 			new ResourceLocation("hbm:alarm.lockdownLoop"),		SoundType.LOOP,		14745599 ,	50),
		LOCKDOWN_ALARM_PASS("Lockdown Alarm Pass", 		new ResourceLocation("hbm:alarm.lockdownPass"),		SoundType.PASS,		3394611 ,	50);

		private String title;
		private ResourceLocation location;
		private SoundType type;
		private int color;
		private int volume;

		private TrackType(String name, ResourceLocation loc, SoundType sound, int msa, int intensity) {
			title = name;
			location = loc;
			type = sound;
			color = msa;
			volume = intensity;
		}

		public String getTrackTitle() { return title; }
		public ResourceLocation getSoundLocation() { return location; }
		public SoundType getType() { return type; }
		public int getColor() { return color; }
		public int getVolume() { return volume; }

		public static TrackType getEnum(int i) {
			if(i < TrackType.values().length)
				return TrackType.values()[i];
			else
				return TrackType.NULL;
		}
	};

	public enum SoundType {
		LOOP,
		PASS,
		SOUND;
	};

	public ItemCassette() {
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tabs, List list) {
		for(int i = 1; i < TrackType.values().length; ++i) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		if(!(stack.getItem() instanceof ItemCassette)) return;

		list.add("Siren sound cassette:");
		list.add("   Name: " + TrackType.getEnum(stack.getItemDamage()).getTrackTitle());
		list.add("   Type: " + TrackType.getEnum(stack.getItemDamage()).getType().name());
		list.add("   Volume: " + TrackType.getEnum(stack.getItemDamage()).getVolume());
	}

	public static TrackType getType(ItemStack stack) {
		if(stack != null && stack.getItem() instanceof ItemCassette)
			return TrackType.getEnum(stack.getItemDamage());
		else
			return TrackType.NULL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		this.overlayIcon = reg.registerIcon("hbm:cassette_overlay");

		this.passIcon = reg.registerIcon("hbm:cassette_pass");

		this.soundIcon = reg.registerIcon("hbm:cassette_sound");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamageForRenderPass(int meta, int pass) {
		if(pass == 1) return this.overlayIcon;
		if(TrackType.getEnum(meta).getType() == SoundType.PASS) return this.passIcon;
		if(TrackType.getEnum(meta).getType() == SoundType.SOUND) return this.soundIcon;
		return super.getIconFromDamageForRenderPass(meta, pass);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int pass) {
		if(pass == 0) return 16777215;
		int j = TrackType.getEnum(stack.getItemDamage()).getColor();
		if(j < 0) j = 16777215;
		return j;
	}
}
