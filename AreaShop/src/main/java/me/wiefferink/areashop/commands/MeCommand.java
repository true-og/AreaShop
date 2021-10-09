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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class MeCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private FileManager fileManager;

	@Override
	public String getCommandStart() {
		return "areashop me";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.me")) {
			return "help-me";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.me")) {
			messageBridge.message(sender, "me-noPermission");
			return;
		}
		OfflinePlayer player = null;
		if(!(sender instanceof Player)) {
			if(args.length <= 1) {
				messageBridge.message(sender, "me-notAPlayer");
				return;
			}
		} else {
			player = (OfflinePlayer)sender;
		}
		if(args.length > 1) {
			player = Bukkit.getOfflinePlayer(args[1]);
			if(!player.hasPlayedBefore()) {
				messageBridge.message(sender, "me-noPlayer", args[1]);
				return;
			}
		}
		if(!player.hasPlayedBefore()) {
			return;
		}

		// Get the regions owned by the player
		Set<RentRegion> rentRegions = new HashSet<>();
		for(RentRegion region : fileManager.getRentsRef()) {
			if(region.isOwner(player)) {
				rentRegions.add(region);
			}
		}
		Set<BuyRegion> buyRegions = new HashSet<>();
		for(BuyRegion region : fileManager.getBuysRef()) {
			if(region.isOwner(player)) {
				buyRegions.add(region);
			}
		}
		// Get the regions the player is added as friend
		Set<GeneralRegion> friendRegions = new HashSet<>();
		for(GeneralRegion region : fileManager.getRegionsRef()) {
			if(region.getFriendsFeature().getFriends().contains(player.getUniqueId())) {
				friendRegions.add(region);
			}
		}

		// Send messages
		boolean foundSome = !rentRegions.isEmpty() || !buyRegions.isEmpty() || !friendRegions.isEmpty();
		if(foundSome) {
			messageBridge.message(sender, "me-header", player.getName());
		}
		if(!rentRegions.isEmpty()) {
			for(RentRegion region : rentRegions) {
				messageBridge.messageNoPrefix(sender, "me-rentLine", region);
			}
		}
		if(!buyRegions.isEmpty()) {
			for(BuyRegion region : buyRegions) {
				messageBridge.messageNoPrefix(sender, "me-buyLine", region);
			}
		}
		if(!friendRegions.isEmpty()) {
			for(GeneralRegion region : friendRegions) {
				messageBridge.messageNoPrefix(sender, "me-friendLine", region);
			}
		}

		if(!foundSome) {
			messageBridge.message(sender, "me-nothing", player.getName());
		} else {
			messageBridge.messageNoPrefix(sender, "me-clickHint");
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			for(Player player : Utils.getOnlinePlayers()) {
				result.add(player.getName());
			}
		}
		return result;
	}
}


























