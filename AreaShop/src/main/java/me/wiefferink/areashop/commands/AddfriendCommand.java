package me.wiefferink.areashop.commands;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.FileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AddfriendCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private FileManager fileManager;
	@Inject
	private Plugin plugin;

	@Override
	public String getCommandStart() {
		return "areashop addfriend";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.addfriendall") || target.hasPermission("areashop.addfriend")) {
			return "help-addFriend";
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.addfriend") && !sender.hasPermission("areashop.addfriendall")) {
			messageBridge.message(sender, "addfriend-noPermission");
			return;
		}

		if(args.length < 2) {
			messageBridge.message(sender, "addfriend-help");
			return;
		}

		GeneralRegion region;
		if(args.length <= 2) {
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
			if(region == null) {
				messageBridge.message(sender, "cmd-notRegistered", args[2]);
				return;
			}
		}
		if(sender.hasPermission("areashop.addfriendall")) {
			if((region instanceof RentRegion && !((RentRegion)region).isRented())
					|| (region instanceof BuyRegion && !((BuyRegion)region).isSold())) {
				messageBridge.message(sender, "addfriend-noOwner", region);
				return;
			}
			OfflinePlayer friend = Bukkit.getOfflinePlayer(args[1]);
			if(friend.getLastPlayed() == 0 && !friend.isOnline() && !plugin.getConfig().getBoolean("addFriendNotExistingPlayers")) {
				messageBridge.message(sender, "addfriend-notVisited", args[1], region);
				return;
			}
			if(region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
				messageBridge.message(sender, "addfriend-alreadyAdded", friend.getName(), region);
				return;
			}
			if(region.isOwner(friend.getUniqueId())) {
				messageBridge.message(sender, "addfriend-self", friend.getName(), region);
				return;
			}
			if(region.getFriendsFeature().addFriend(friend.getUniqueId(), sender)) {
				region.update();
				messageBridge.message(sender, "addfriend-successOther", friend.getName(), region);
			}
		} else {
			if(sender.hasPermission("areashop.addfriend") && sender instanceof Player) {
				if(region.isOwner((Player)sender)) {
					OfflinePlayer friend = Bukkit.getOfflinePlayer(args[1]);
					if(friend.getLastPlayed() == 0 && !friend.isOnline() && !plugin.getConfig().getBoolean("addFriendNotExistingPlayers")) {
						messageBridge.message(sender, "addfriend-notVisited", args[1], region);
						return;
					}
					if(region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
						messageBridge.message(sender, "addfriend-alreadyAdded", friend.getName(), region);
						return;
					}
					if(region.isOwner(friend.getUniqueId())) {
						messageBridge.message(sender, "addfriend-self", friend.getName(), region);
						return;
					}

					if(region.getFriendsFeature().addFriend(friend.getUniqueId(), sender)) {
						region.update();
						messageBridge.message(sender, "addfriend-success", friend.getName(), region);
					}
				} else {
					messageBridge.message(sender, "addfriend-noPermissionOther", region);
				}
			} else {
				messageBridge.message(sender, "addfriend-noPermission", region);
			}
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			for(Player player : Utils.getOnlinePlayers()) {
				result.add(player.getName());
			}
		} else if(toComplete == 3) {
			result.addAll(fileManager.getRegionNames());
		}
		return result;
	}
}








