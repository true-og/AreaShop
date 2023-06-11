package me.wiefferink.areashop.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class StackCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;
	@Inject
	private WorldEditInterface worldEditInterface;
	@Inject
	private WorldGuardInterface worldGuardInterface;
	@Inject
	private RegionFactory regionFactory;
	@Inject
	private Plugin plugin;
	
	@Override
	public String getCommandStart() {
		return "areashop stack";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.stack")) {
			return "help-stack";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		// Check permission
		if(!sender.hasPermission("areashop.stack")) {
			messageBridge.message(sender, "stack-noPermission");
			return;
		}
		// Only from ingame
		if(!(sender instanceof final Player player)) {
			messageBridge.message(sender, "cmd-onlyByPlayer");
			return;
		}
		// Specify enough arguments
		if(args.length < 5) {
			messageBridge.message(sender, "stack-help");
			return;
		}
		// Check amount
		int tempAmount = -1;
		try {
			tempAmount = Integer.parseInt(args[1]);
		} catch(NumberFormatException e) {
			// Incorrect number
		}
		if(tempAmount <= 0) {
			messageBridge.message(player, "stack-wrongAmount", args[1]);
			return;
		}
		// Check gap
		int gap;
		try {
			gap = Integer.parseInt(args[2]);
		} catch(NumberFormatException e) {
			messageBridge.message(player, "stack-wrongGap", args[2]);
			return;
		}
		// Check region type
		if(!"rent".equalsIgnoreCase(args[4]) && !"buy".equalsIgnoreCase(args[4])) {
			messageBridge.message(sender, "stack-help");
			return;
		}
		// Get WorldEdit selection
		final WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
		if(selection == null) {
			messageBridge.message(player, "stack-noSelection");
			return;
		}
		// Get or create group
		RegionGroup group = null;
		if(args.length > 5) {
			group = fileManager.getGroup(args[5]);
			if(group == null) {
				group = regionFactory.createRegionGroup(args[5]);
				fileManager.addGroup(group);
			}
		}
		// Get facing of the player (must be clearly one of the four directions to make sure it is no mistake)
		BlockFace facing = Utils.yawToFacing(player.getLocation().getYaw());
		if(player.getLocation().getPitch() > 45) {
			facing = BlockFace.DOWN;
		} else if(player.getLocation().getPitch() < -45) {
			facing = BlockFace.UP;
		}
		if(!(facing == BlockFace.NORTH || facing == BlockFace.EAST || facing == BlockFace.SOUTH || facing == BlockFace.WEST || facing == BlockFace.UP || facing == BlockFace.DOWN)) {
			messageBridge.message(player, "stack-unclearDirection", facing.toString().toLowerCase().replace('_', '-'));
			return;
		}
		Vector shift = new Vector(0, 0, 0);
		if(facing == BlockFace.SOUTH) {
			shift = shift.setZ(-selection.getLength() - gap);
		} else if(facing == BlockFace.WEST) {
			shift = shift.setX(selection.getWidth() + gap);
		} else if(facing == BlockFace.NORTH) {
			shift = shift.setZ(selection.getLength() + gap);
		} else if(facing == BlockFace.EAST) {
			shift = shift.setX(-selection.getWidth() - gap);
		} else if(facing == BlockFace.DOWN) {
			shift = shift.setY(-selection.getHeight() - gap);
		} else if(facing == BlockFace.UP) {
			shift = shift.setY(selection.getHeight() + gap);
		}
		AreaShop.debug("  calculated shift vector: " + shift + ", with facing=" + facing);
		// Create regions and add them to AreaShop
		final String nameTemplate = args[3];


		final int regionsPerTick = plugin.getConfig().getInt("adding.regionsPerTick");
		final boolean rentRegions = "rent".equalsIgnoreCase(args[4]);
		final int amount = tempAmount;
		final RegionGroup finalGroup = group;
		final Vector finalShift = shift;
		String type;
		if(rentRegions) {
			type = "rent";
		} else {
			type = "buy";
		}
		Message groupsMessage = Message.empty();
		if(group != null) {
			groupsMessage = Message.fromKey("stack-addToGroup").replacements(group.getName());
		}
		messageBridge.message(player, "stack-accepted", amount, type, gap, nameTemplate, groupsMessage);
		messageBridge.message(player, "stack-addStart", amount, regionsPerTick * 20);

		Location minimumLocation = selection.getMinimumLocation();
		Vector minimumVector = new Vector(minimumLocation.getX(), minimumLocation.getY(), minimumLocation.getZ());
		Location maximumLocation = selection.getMaximumLocation();
		Vector maximumVector = new Vector(maximumLocation.getX(), maximumLocation.getY(), maximumLocation.getZ());
		new BukkitRunnable() {
			private int current = -1;
			private final RegionManager manager = AreaShop.getInstance().getRegionManager(selection.getWorld());
			private int counter = 1;
			private int tooLow = 0;
			private int tooHigh = 0;

			@Override
			public void run() {
				for(int i = 0; i < regionsPerTick; i++) {
					current++;
					if(current < amount) {
						// Create the region name
						String regionName = countToName(nameTemplate, counter);
						while(manager.getRegion(regionName) != null || AreaShop.getInstance().getFileManager().getRegion(regionName) != null) {
							counter++;
							regionName = countToName(nameTemplate, counter);
						}

						// Add the region to WorldGuard (at startposition shifted by the number of this region times the blocks it should shift)
						Vector minimum = minimumVector.clone().add(finalShift.clone().multiply(current));
						Vector maximum = maximumVector.clone().add(finalShift.clone().multiply(current));

						// Check for out of bounds
						if(minimum.getBlockY() < 0) {
							tooLow++;
							continue;
						} else if(maximum.getBlockY() > 256) {
							tooHigh++;
							continue;
						}
						ProtectedCuboidRegion region = worldGuardInterface.createCuboidRegion(regionName, minimum,maximum);
						manager.addRegion(region);

						// Add the region to AreaShop
						GeneralRegion newRegion;
						if(rentRegions) {
							newRegion = regionFactory.createRentRegion(regionName, selection.getWorld());

						} else {
							newRegion = regionFactory.createBuyRegion(regionName, selection.getWorld());
						}

						if(finalGroup != null) {
							finalGroup.addMember(newRegion);
						}
						AddingRegionEvent event = fileManager.addRegion(newRegion);
						if (event.isCancelled()) {
							messageBridge.message(player, "general-cancelled", event.getReason());
							continue;
						}
						newRegion.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
						newRegion.update();
					}
				}
				if(current >= amount) {
					if(player.isOnline()) {
						int added = amount - tooLow - tooHigh;
						Message wrong = Message.empty();
						if(tooHigh > 0) {
							wrong.append(Message.fromKey("stack-tooHigh").replacements(tooHigh));
						}
						if(tooLow > 0) {
							wrong.append(Message.fromKey("stack-tooLow").replacements(tooLow));
						}
						messageBridge.message(player, "stack-addComplete", added, wrong);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 1, 1);
	}

	/**
	 * Build a name from a count, with the right length.
	 * @param template Template to put the name in (# to put the count there, otherwise count is appended)
	 * @param count Number to use
	 * @return name with prepended 0's
	 */
	private String countToName(String template, int count) {
		StringBuilder counterName =  new StringBuilder().append(count);
		int minimumLength = plugin.getConfig().getInt("stackRegionNumberLength");
		while(counterName.length() < minimumLength) {
			counterName.insert(0, "0");
		}

		if(template.contains("#")) {
			return template.replace("#", counterName);
		} else {
			return template + counterName;
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 5) {
			result.add("rent");
			result.add("buy");
		} else if(toComplete == 6) {
			result.addAll(fileManager.getGroupNames());
		}
		return result;
	}

}










