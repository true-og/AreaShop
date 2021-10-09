package me.wiefferink.areashop.commands;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.FileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class BuyCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private Plugin plugin;
	@Inject
	private FileManager fileManager;

	@Override
	public String getCommandStart() {
		return "areashop buy";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.buy")) {
			return "help-buy";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.buy")) {
			messageBridge.message(sender, "buy-noPermission");
			return;
		}
		if(!(sender instanceof Player)) {
			messageBridge.message(sender, "cmd-onlyByPlayer");
			return;
		}
		Player player = (Player)sender;
		if(args.length > 1 && args[1] != null) {
			BuyRegion region = fileManager.getBuy(args[1]);
			if(region == null) {
				messageBridge.message(player, "buy-notBuyable", args[1]);
			} else {
				region.buy(player);
			}
		} else {
			// get the region by location
			List<BuyRegion> regions = Utils.getImportantBuyRegions(((Player)sender).getLocation());
			if(regions.isEmpty()) {
				messageBridge.message(sender, "cmd-noRegionsAtLocation");
			} else if(regions.size() > 1) {
				messageBridge.message(sender, "cmd-moreRegionsAtLocation");
			} else {
				regions.get(0).buy(player);
			}
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			for(BuyRegion region : fileManager.getBuys()) {
				if(!region.isSold()) {
					result.add(region.getName());
				}
			}
		}
		return result;
	}
}
