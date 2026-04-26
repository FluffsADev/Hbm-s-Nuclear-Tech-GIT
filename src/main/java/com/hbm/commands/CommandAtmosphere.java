package com.hbm.commands;

import java.util.Collections;
import java.util.List;

import com.hbm.dim.CelestialBody;
import com.hbm.handler.CelestialNukeShockHandler;
import com.hbm.items.ItemVOTVdrive.Target;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class CommandAtmosphere extends CommandBase {

	private static final int DEFAULT_TEST_NUKE_RADIUS = 140;

	@Override
	public String getCommandName() {
		return "ntmatmo";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/ntmatmo testNuke [blastRadius]";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if(!(sender instanceof EntityPlayer)) {
			throw new PlayerNotFoundException();
		}

		if(args.length < 1) {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}

		if("testNuke".equalsIgnoreCase(args[0])) {
			runTestNuke(sender, args);
			return;
		}

		throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
	}

	private void runTestNuke(ICommandSender sender, String[] args) {
		World world = sender.getEntityWorld();
		if(world == null) {
			throw new WrongUsageException(getCommandUsage(sender), new Object[0]);
		}

		ChunkCoordinates pos = sender.getPlayerCoordinates();
		Target target = CelestialBody.getTarget(world, pos.posX, pos.posZ);
		CelestialBody body = target != null ? target.body : null;
		if(body == null) {
			throw new WrongUsageException("No celestial body is available for atmospheric testing.", new Object[0]);
		}

		int blastRadius = args.length >= 2 ? parseIntBounded(sender, args[1], 1, 5000) : DEFAULT_TEST_NUKE_RADIUS;
		CelestialNukeShockHandler.trigger(world, pos.posX, pos.posZ, blastRadius);
		sender.addChatMessage(new ChatComponentText("Triggered atmospheric nuke test on " + body.name + " with blast radius " + blastRadius + "."));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		if(args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "testNuke");
		}

		if(args.length == 2 && "testNuke".equalsIgnoreCase(args[0])) {
			return getListOfStringsMatchingLastWord(args, Integer.toString(DEFAULT_TEST_NUKE_RADIUS), "70", "100", "140", "280");
		}

		return Collections.emptyList();
	}
}
