package me.wiefferink.areashop.commands;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SetpriceCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;
	
	@Override
	public String getCommandStart() {
		return "areashop setprice";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.setprice")) {
			return "help-setprice";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.setprice") && (!sender.hasPermission("areashop.setprice.landlord") && sender instanceof Player)) {
			messageBridge.message(sender, "setprice-noPermission");
			return;
		}
		if(args.length < 2 || args[1] == null) {
			messageBridge.message(sender, "setprice-help");
			return;
		}
		GeneralRegion region;
		if(args.length < 3) {
			if(sender instanceof Player) {
				// get the region by location
				List<GeneralRegion> regions = Utils.getImportantRegions(((Player)sender).getLocation());
				if(regions.isEmpty()) {
					messageBridge.message(sender, "cmd-noRegionsAtLocation");
					return;
				} else if(regions.size() > 1) {
					messageBridge.message(sender, "cmd-moreRegionsAtLocation");
					return;
				} else {
					region = regions.get(0);
				}
			} else {
				messageBridge.message(sender, "cmd-automaticRegionOnlyByPlayer");
				return;
			}
		} else {
			region = fileManager.getRegion(args[2]);
		}
		if(region == null) {
			messageBridge.message(sender, "setprice-notRegistered", args[2]);
			return;
		}
		if(!sender.hasPermission("areashop.setprice") && !(sender instanceof Player && region.isLandlord(((Player)sender).getUniqueId()))) {
			messageBridge.message(sender, "setprice-noLandlord", region);
			return;
		}
		if("default".equalsIgnoreCase(args[1]) || "reset".equalsIgnoreCase(args[1])) {
			if(region instanceof RentRegion) {
				((RentRegion)region).setPrice(null);
			} else if(region instanceof BuyRegion) {
				((BuyRegion)region).setPrice(null);
			}
			region.update();
			messageBridge.message(sender, "setprice-successRemoved", region);
			return;
		}
		double price;
		try {
			price = Double.parseDouble(args[1]);
		} catch(NumberFormatException e) {
			messageBridge.message(sender, "setprice-wrongPrice", args[1], region);
			return;
		}
		if(region instanceof RentRegion) {
			((RentRegion)region).setPrice(price);
			messageBridge.message(sender, "setprice-successRent", region);
		} else if(region instanceof BuyRegion) {
			((BuyRegion)region).setPrice(price);
			messageBridge.message(sender, "setprice-successBuy", region);
		}
		region.update();
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 3) {
			result = fileManager.getRegionNames();
		}
		return result;
	}

}
