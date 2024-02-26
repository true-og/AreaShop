package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.events.ask.DeletingRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Singleton
public class DelCommand extends CommandAreaShop {

	private final WorldEditInterface worldEditInterface;
	private final IFileManager fileManager;

	@Inject
	public DelCommand(
			@Nonnull MessageBridge messageBridge,
			@Nonnull WorldEditInterface worldEditInterface,
			@Nonnull IFileManager fileManager
	) {
		super(messageBridge);
		this.worldEditInterface = worldEditInterface;
		this.fileManager = fileManager;
	}

	@Override
	public String getCommandStart() {
		return "areashop del";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.destroyrent") || target.hasPermission("areashop.destroybuy") || target.hasPermission("areashop.destroyrent.landlord") || target.hasPermission("areashop.destroybuy.landlord")) {
			return "help-del";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.destroybuy")
				&& !sender.hasPermission("areashop.destroybuy.landlord")

				&& !sender.hasPermission("areashop.destroyrent")
				&& !sender.hasPermission("areashop.destroyrent.landlord")) {
			messageBridge.message(sender, "del-noPermission");
			return;
		}
		if(args.length < 2) {
			// Only players can have a selection
			if(!(sender instanceof Player player)) {
				messageBridge.message(sender, "cmd-weOnlyByPlayer");
				return;
			}
			WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
			if(selection == null) {
				messageBridge.message(player, "cmd-noSelection");
				return;
			}
			List<GeneralRegion> regions = Utils.getRegionsInSelection(selection);
			if(regions == null || regions.isEmpty()) {
				messageBridge.message(player, "cmd-noRegionsFound");
				return;
			}
			// Start removing the regions that he has permission for
			ArrayList<String> namesSuccess = new ArrayList<>();
			TreeSet<GeneralRegion> regionsFailed = new TreeSet<>();
			TreeSet<GeneralRegion> regionsCancelled = new TreeSet<>();
			for(GeneralRegion region : regions) {
				boolean isLandlord = region.isLandlord(((Player)sender).getUniqueId());
				if(region instanceof RentRegion) {
					if(!sender.hasPermission("areashop.destroyrent") && !(isLandlord && sender.hasPermission("areashop.destroyrent.landlord"))) {
						regionsFailed.add(region);
						continue;
					}
				} else if(region instanceof BuyRegion) {
					if(!sender.hasPermission("areashop.destroybuy") && !(isLandlord && sender.hasPermission("areashop.destroybuy.landlord"))) {
						regionsFailed.add(region);
						continue;
					}
				}

				DeletingRegionEvent event = fileManager.deleteRegion(region, true);
				if (event.isCancelled()) {
					regionsCancelled.add(region);
				} else {
					namesSuccess.add(region.getName());
				}
			}

			// Send messages
			if(!namesSuccess.isEmpty()) {
				messageBridge.message(sender, "del-success", Utils.createCommaSeparatedList(namesSuccess));
			}
			if(!regionsFailed.isEmpty()) {
				messageBridge.message(sender, "del-failed", Utils.combinedMessage(regionsFailed, "region"));
			}
			if(!regionsCancelled.isEmpty()) {
				messageBridge.message(sender, "del-cancelled", Utils.combinedMessage(regionsCancelled, "region"));
			}
		} else {
			GeneralRegion region = fileManager.getRegion(args[1]);
			if(region == null) {
				messageBridge.message(sender, "cmd-notRegistered", args[1]);
				return;
			}
			boolean isLandlord = sender instanceof Player && region.isLandlord(((Player)sender).getUniqueId());
			if(region instanceof RentRegion) {
				// Remove the rent if the player has permission
				if(sender.hasPermission("areashop.destroyrent") || (isLandlord && sender.hasPermission("areashop.destroyrent.landlord"))) {
					DeletingRegionEvent event = fileManager.deleteRegion(region, true);
					if (event.isCancelled()) {
						messageBridge.message(sender, "general-cancelled", event.getReason());
					} else {
						messageBridge.message(sender, "destroy-successRent", region);
					}
				} else {
					messageBridge.message(sender, "destroy-noPermissionRent", region);
				}
			} else if(region instanceof BuyRegion) {
				// Remove the buy if the player has permission
				if(sender.hasPermission("areashop.destroybuy") || (isLandlord && sender.hasPermission("areashop.destroybuy.landlord"))) {
					DeletingRegionEvent event = fileManager.deleteRegion(region, true);
					if (event.isCancelled()) {
						messageBridge.message(sender, "general-cancelled", event.getReason());
					} else {
						messageBridge.message(sender, "destroy-successBuy", region);
					}
				} else {
					messageBridge.message(sender, "destroy-noPermissionBuy", region);
				}
			}
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result = fileManager.getRegionNames();
		}
		return result;
	}

}










