package me.wiefferink.areashop.commands;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ResellCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;
	
	@Override
	public String getCommandStart() {
		return "areashop resell";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.resellall") || target.hasPermission("areashop.resell")) {
			return "help-resell";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.resell") && !sender.hasPermission("areashop.resellall")) {
			messageBridge.message(sender, "resell-noPermissionOther");
			return;
		}

		if(args.length <= 1) {
			messageBridge.message(sender, "resell-help");
			return;
		}
		double price;
		try {
			price = Double.parseDouble(args[1]);
		} catch(NumberFormatException e) {
			messageBridge.message(sender, "resell-wrongPrice", args[1]);
			return;
		}

		if(price < 0) {
			messageBridge.message(sender, "resell-wrongPrice", args[1]);
			return;
		}

		BuyRegion buy;
		if(args.length <= 2) {
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
			buy = fileManager.getBuy(args[2]);
			if(buy == null) {
				messageBridge.message(sender, "resell-notRegistered", args[2]);
				return;
			}
		}
		if(buy == null) {
			messageBridge.message(sender, "cmd-noRegionsAtLocation");
			return;
		}
		if(!buy.isSold()) {
			messageBridge.message(sender, "resell-notBought", buy);
			return;
		}
		if(sender.hasPermission("areashop.resellall")) {
			buy.enableReselling(price);
			buy.update();
			messageBridge.message(sender, "resell-success", buy);
		} else if(sender.hasPermission("areashop.resell") && sender instanceof Player) {
			if(!buy.isOwner((Player)sender)) {
				messageBridge.message(sender, "resell-noPermissionOther", buy);
				return;
			}

			if(buy.getBooleanSetting("buy.resellDisabled")) {
				messageBridge.message(sender, "resell-disabled", buy);
				return;
			}

			buy.enableReselling(price);
			buy.update();
			messageBridge.message(sender, "resell-success", buy);
		} else {
			messageBridge.message(sender, "resell-noPermission", buy);
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 3) {
			for(BuyRegion region : fileManager.getBuysRef()) {
				if(region.isSold() && !region.isInResellingMode()) {
					result.add(region.getName());
				}
			}
		}
		return result;
	}
}
















