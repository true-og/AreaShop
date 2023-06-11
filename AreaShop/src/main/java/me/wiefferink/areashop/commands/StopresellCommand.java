package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class StopresellCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;
	
	@Override
	public String getCommandStart() {
		return "areashop stopresell";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.stopresellall") || target.hasPermission("areashop.stopresell")) {
			return "help-stopResell";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.stopresell") && !sender.hasPermission("areashop.stopresellall")) {
			messageBridge.message(sender, "stopresell-noPermissionOther");
			return;
		}

		BuyRegion buy;
		if(args.length <= 1) {
			if(sender instanceof Player) {
				// get the region by location
				List<BuyRegion> regions = Utils.getImportantBuyRegions(((Player)sender).getLocation());
				if(regions.isEmpty()) {
					messageBridge.message(sender, "cmd-noRegionsAtLocation");
					return;
				} else if(regions.size() > 1) {
					messageBridge.message(sender, "cmd-moreRegionsAtLocation");
					return;
				} else {
					buy = regions.get(0);
				}
			} else {
				messageBridge.message(sender, "cmd-automaticRegionOnlyByPlayer");
				return;
			}
		} else {
			buy = fileManager.getBuy(args[1]);
			if(buy == null) {
				messageBridge.message(sender, "stopresell-notRegistered", args[1]);
				return;
			}
		}
		if(buy == null) {
			messageBridge.message(sender, "cmd-noRegionsAtLocation");
			return;
		}
		if(!buy.isInResellingMode()) {
			messageBridge.message(sender, "stopresell-notResell", buy);
			return;
		}
		if(sender.hasPermission("areashop.stopresellall")) {
			buy.disableReselling();
			buy.update();
			messageBridge.message(sender, "stopresell-success", buy);
		} else if(sender.hasPermission("areashop.stopresell") && sender instanceof Player) {
			if(buy.isOwner((Player)sender)) {
				buy.disableReselling();
				buy.update();
				messageBridge.message(sender, "stopresell-success", buy);
			} else {
				messageBridge.message(sender, "stopresell-noPermissionOther", buy);
			}
		} else {
			messageBridge.message(sender, "stopresell-noPermission", buy);
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			for(BuyRegion region : fileManager.getBuysRef()) {
				if(region.isSold() && region.isInResellingMode()) {
					result.add(region.getName());
				}
			}
		}
		return result;
	}
}
















