package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class MeCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;

	@Inject
	private OfflinePlayerHelper offlinePlayerHelper;

	@Inject
	private BukkitSchedulerExecutor executor;

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
			// Lookup offline players async
			this.offlinePlayerHelper.lookupOfflinePlayerAsync(args[1]).thenAcceptAsync(offlinePlayer -> {
				if (!offlinePlayer.hasPlayedBefore()) {
					messageBridge.message(sender, "me-noPlayer", args[1]);
				} else {
					sendMessageAboutRegions(sender, offlinePlayer);
				}
			}, this.executor);
			return;
		}
		sendMessageAboutRegions(sender, player);
	}

	private void sendMessageAboutRegions(CommandSender sender, OfflinePlayer target) {
		// Get the regions owned by the player
		Set<RentRegion> rentRegions = new HashSet<>();
		for(RentRegion region : fileManager.getRentsRef()) {
			if(region.isOwner(target)) {
				rentRegions.add(region);
			}
		}
		Set<BuyRegion> buyRegions = new HashSet<>();
		for(BuyRegion region : fileManager.getBuysRef()) {
			if(region.isOwner(target)) {
				buyRegions.add(region);
			}
		}
		// Get the regions the player is added as friend
		Set<GeneralRegion> friendRegions = new HashSet<>();
		for(GeneralRegion region : fileManager.getRegionsRef()) {
			if(region.getFriendsFeature().getFriends().contains(target.getUniqueId())) {
				friendRegions.add(region);
			}
		}

		// Send messages
		boolean foundSome = !rentRegions.isEmpty() || !buyRegions.isEmpty() || !friendRegions.isEmpty();
		if(!foundSome) {
			messageBridge.message(sender, "me-nothing", target.getName());
			return;
		}
		messageBridge.message(sender, "me-header", target.getName());
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
		messageBridge.messageNoPrefix(sender, "me-clickHint");
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


























