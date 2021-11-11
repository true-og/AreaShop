package me.wiefferink.areashop.commands;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class SchematiceventCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;
	
	@Override
	public String getCommandStart() {
		return "areashop schemevent";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.schematicevents")) {
			return "help-schemevent";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.schematicevents")) {
			messageBridge.message(sender, "schemevent-noPermission");
			return;
		}

		if(args.length < 3 || args[1] == null || args[2] == null) {
			messageBridge.message(sender, "schemevent-help");
			return;
		}
		GeneralRegion region = fileManager.getRegion(args[1]);
		if(region == null) {
			messageBridge.message(sender, "cmd-notRegistered", args[1]);
			return;
		}
		if(region.getRegion() == null) {
			messageBridge.message(sender, "general-noRegion", region);
			return;
		}
		GeneralRegion.RegionEvent event = null;
		boolean exception = false;
		try {
			event = GeneralRegion.RegionEvent.valueOf(args[2].toUpperCase());
		} catch(IllegalArgumentException e) {
			exception = true;
		}
		// Check for a totally wrong event or a non matching event
		if(exception) {
			ArrayList<String> values = new ArrayList<>();
			for(GeneralRegion.RegionEvent value : GeneralRegion.RegionEvent.values()) {
				values.add(value.getValue().toLowerCase());
			}
			messageBridge.message(sender, "schemevent-wrongEvent", args[2], Utils.createCommaSeparatedList(values), region);
			return;
		}
		region.handleSchematicEvent(event);
		region.update();
		messageBridge.message(sender, "schemevent-success", args[2], region);
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result.addAll(fileManager.getRegionNames());
		} else if(toComplete == 3) {
			GeneralRegion region = fileManager.getRegion(start[2]);
			if(region != null) {
				if(region instanceof RentRegion) {
					result.addAll(Arrays.asList("created", "deleted", "rented", "unrented"));
				} else if(region instanceof BuyRegion) {
					result.addAll(Arrays.asList("created", "deleted", "bought", "sold"));
				}
			}
		}
		return result;
	}
}
