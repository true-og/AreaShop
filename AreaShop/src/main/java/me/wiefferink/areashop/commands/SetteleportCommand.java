package me.wiefferink.areashop.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
public class SetteleportCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private IFileManager fileManager;
	
	@Override
	public String getCommandStart() {
		return "areashop settp";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.setteleportall") || target.hasPermission("areashop.setteleport")) {
			return "help-setteleport";
		}
		return null;
	}

	/**
	 * Check if a person can set the teleport location of the region.
	 * @param person The person to check
	 * @param region The region to check for
	 * @return true if the person can set the teleport location, otherwise false
	 */
	public static boolean canUse(CommandSender person, GeneralRegion region) {
		if(!(person instanceof Player player)) {
			return false;
		}
		return player.hasPermission("areashop.setteleportall")
				|| region.isOwner(player) && player.hasPermission("areashop.setteleport");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.setteleport") && !sender.hasPermission("areashop.setteleportall")) {
			messageBridge.message(sender, "setteleport-noPermission");
			return;
		}
		if(!(sender instanceof Player player)) {
			messageBridge.message(sender, "onlyByPlayer");
			return;
		}
		GeneralRegion region;
		if(args.length < 2) {
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
			region = fileManager.getRegion(args[1]);
		}

		boolean owner;

		if(region == null) {
			messageBridge.message(player, "setteleport-noRentOrBuy", args[1]);
			return;
		}
		if(region instanceof RentRegion) {
			owner = player.getUniqueId().equals(((RentRegion)region).getRenter());
		} else {
			owner = player.getUniqueId().equals(((BuyRegion)region).getBuyer());
		}
		if(!player.hasPermission("areashop.setteleport")) {
			messageBridge.message(player, "setteleport-noPermission", region);
			return;
		} else if(!owner && !player.hasPermission("areashop.setteleportall")) {
			messageBridge.message(player, "setteleport-noPermissionOther", region);
			return;
		}

		ProtectedRegion wgRegion = region.getRegion();
		if(args.length > 2 && args[2] != null && (args[2].equalsIgnoreCase("reset") || args[2].equalsIgnoreCase("yes") || args[2].equalsIgnoreCase("true"))) {
			region.getTeleportFeature().setTeleport(null);
			region.update();
			messageBridge.message(player, "setteleport-reset", region);
			return;
		}
		if(!player.hasPermission("areashop.setteleportoutsideregion") && (wgRegion == null || !wgRegion.contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()))) {
			messageBridge.message(player, "setteleport-notInside", region);
			return;
		}
		region.getTeleportFeature().setTeleport(player.getLocation());
		region.update();
		messageBridge.message(player, "setteleport-success", region);
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result.addAll(fileManager.getRegionNames());
		} else if(toComplete == 3) {
			result.add("reset");
		}
		return result;
	}

}
