package me.wiefferink.areashop.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.AreaShopPlugin;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.events.ask.BuyingRegionEvent;
import me.wiefferink.areashop.events.ask.RentingRegionEvent;
import me.wiefferink.areashop.events.notify.BoughtRegionEvent;
import me.wiefferink.areashop.events.notify.RentedRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.FileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.bukkitdo.Do;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class AddCommand extends CommandAreaShop {

	@Inject
	private MessageBridge messageBridge;
	@Inject
	private AreaShopPlugin plugin;
	@Inject
	private WorldEditInterface worldEditInterface;
	@Inject
	private WorldGuardInterface worldGuardInterface;
	@Inject
	private RegionFactory regionFactory;
	
	@Override
	public String getCommandStart() {
		return "areashop add";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.createrent")
				|| target.hasPermission("areashop.createrent.member")
				|| target.hasPermission("areashop.createrent.owner")

				|| target.hasPermission("areashop.createbuy")
				|| target.hasPermission("areashop.createbuy.member")
				|| target.hasPermission("areashop.createbuy.owner")) {
			return "help-add";
		}
		return null;
	}

	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if(!sender.hasPermission("areashop.createrent")
				&& !sender.hasPermission("areashop.createrent.member")
				&& !sender.hasPermission("areashop.createrent.owner")

				&& !sender.hasPermission("areashop.createbuy")
				&& !sender.hasPermission("areashop.createbuy.member")
				&& !sender.hasPermission("areashop.createbuy.owner")) {
			messageBridge.message(sender, "add-noPermission");
			return;
		}

		if(args.length < 2 || args[1] == null || (!"rent".equalsIgnoreCase(args[1]) && !"buy".equalsIgnoreCase(args[1]))) {
			messageBridge.message(sender, "add-help");
			return;
		}
		Map<String, ProtectedRegion> regions = new HashMap<>();
		World world;
		Player player = null;
		if(sender instanceof Player) {
			player = (Player)sender;
		}
		if(args.length == 2) {
			if(player == null) {
				messageBridge.message(sender, "cmd-weOnlyByPlayer");
				return;
			}
			WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
			if(selection == null) {
				messageBridge.message(player, "cmd-noSelection");
				return;
			}
			world = selection.getWorld();
			regions = Utils.getWorldEditRegionsInSelection(selection).stream().collect(Collectors.toMap(ProtectedRegion::getId, region -> region));
			if(regions.isEmpty()) {
				messageBridge.message(player, "cmd-noWERegionsFound");
				return;
			}
		} else {
			if(player != null) {
				if(args.length == 4) {
					world = Bukkit.getWorld(args[3]);
					if(world == null) {
						messageBridge.message(sender, "add-incorrectWorld", args[3]);
						return;
					}
				} else {
					world = ((Player)sender).getWorld();
				}
			} else {
				if(args.length < 4) {
					messageBridge.message(sender, "add-specifyWorld");
					return;
				} else {
					world = Bukkit.getWorld(args[3]);
					if(world == null) {
						messageBridge.message(sender, "add-incorrectWorld", args[3]);
						return;
					}
				}
			}
			ProtectedRegion region = plugin.getRegionManager(world).getRegion(args[2]);
			if(region == null) {
				messageBridge.message(sender, "cmd-noRegion", args[2]);
				return;
			}
			regions.put(args[2], region);
		}
		final boolean isRent = "rent".equalsIgnoreCase(args[1]);
		final Player finalPlayer = player;
		AreaShopPlugin.debug("Starting add task with " + regions.size() + " regions");

		TreeSet<GeneralRegion> regionsSuccess = new TreeSet<>();
		TreeSet<GeneralRegion> regionsAlready = new TreeSet<>();
		TreeSet<GeneralRegion> regionsAlreadyOtherWorld = new TreeSet<>();
		TreeSet<GeneralRegion> regionsRentCancelled = new TreeSet<>(); // Denied by an event listener
		TreeSet<GeneralRegion> regionsBuyCancelled = new TreeSet<>(); // Denied by an event listener
		TreeSet<String> namesBlacklisted = new TreeSet<>();
		TreeSet<String> namesNoPermission = new TreeSet<>();
		TreeSet<String> namesAddCancelled = new TreeSet<>(); // Denied by an event listener
		Do.forAll(
			plugin.getConfig().getInt("adding.regionsPerTick"),
			regions.entrySet(),
			regionEntry -> {
				String regionName = regionEntry.getKey();
				ProtectedRegion region = regionEntry.getValue();
				// Determine if the player is an owner or member of the region
				boolean isMember = finalPlayer != null && worldGuardInterface.containsMember(region, finalPlayer.getUniqueId());
				boolean isOwner = finalPlayer != null && worldGuardInterface.containsOwner(region, finalPlayer.getUniqueId());
				String type;
				if(isRent) {
					type = "rent";
				} else {
					type = "buy";
				}
				FileManager.AddResult result = plugin.getFileManager().checkRegionAdd(sender, region, world, isRent ? GeneralRegion.RegionType.RENT : GeneralRegion.RegionType.BUY);
				if(result == FileManager.AddResult.ALREADYADDED) {
					regionsAlready.add(plugin.getFileManager().getRegion(regionName));
				} else if(result == FileManager.AddResult.ALREADYADDEDOTHERWORLD) {
					regionsAlreadyOtherWorld.add(plugin.getFileManager().getRegion(regionName));
				} else if(result == FileManager.AddResult.BLACKLISTED) {
					namesBlacklisted.add(regionName);
				} else if(result == FileManager.AddResult.NOPERMISSION) {
					namesNoPermission.add(regionName);
				} else {
					// Check if the player should be landlord
					boolean landlord = (!sender.hasPermission("areashop.create" + type)
							&& ((sender.hasPermission("areashop.create" + type + ".owner") && isOwner)
							|| (sender.hasPermission("areashop.create" + type + ".member") && isMember)));
					List<UUID> existing = new ArrayList<>();
					existing.addAll(worldGuardInterface.getOwners(region).asUniqueIdList());
					existing.addAll(worldGuardInterface.getMembers(region).asUniqueIdList());

					AreaShopPlugin.debug("regionAddLandlordStatus:", regionName,
							"landlord:", landlord,
							"existing:", existing,
							"isMember:", isMember,
							"isOwner:", isOwner,
							"createPermission:", sender.hasPermission("areashop.create" + type),
							"ownerPermission:", sender.hasPermission("areashop.create" + type + ".owner"),
							"memberPermission:", sender.hasPermission("areashop.create" + type + ".member"));

					if(isRent) {
						RentRegion rent = regionFactory.createRentRegion(regionName, world);
						// Set landlord
						if(landlord) {
							rent.setLandlord(finalPlayer.getUniqueId(), finalPlayer.getName());
						}

						AddingRegionEvent event = plugin.getFileManager().addRegion(rent);
						if (event.isCancelled()) {
							namesAddCancelled.add(rent.getName());
							return;
						}
						rent.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
						rent.update();

						// Add existing owners/members if any
						if(!landlord && !existing.isEmpty()) {
							UUID rentBy = existing.remove(0);
							OfflinePlayer rentByPlayer = Bukkit.getOfflinePlayer(rentBy);

							RentingRegionEvent rentingRegionEvent = new RentingRegionEvent(rent, rentByPlayer, false);
							Bukkit.getPluginManager().callEvent(rentingRegionEvent);
							if(rentingRegionEvent.isCancelled()) {
								regionsRentCancelled.add(rent);
							} else {
								// Add values to the rent and send it to FileManager
								rent.setRentedUntil(Calendar.getInstance().getTimeInMillis() + rent.getDuration());
								rent.setRenter(rentBy);
								rent.updateLastActiveTime();

								// Fire schematic event and updated times extended
								rent.handleSchematicEvent(GeneralRegion.RegionEvent.RENTED);

								// Add others as friends
								for(UUID friend : existing) {
									rent.getFriendsFeature().addFriend(friend, null);
								}

								rent.notifyAndUpdate(new RentedRegionEvent(rent, false));
							}
						}

						regionsSuccess.add(rent);
					} else {
						BuyRegion buy = regionFactory.createBuyRegion(regionName, world);
						// Set landlord
						if(landlord) {
							buy.setLandlord(finalPlayer.getUniqueId(), finalPlayer.getName());
						}

						AddingRegionEvent event = plugin.getFileManager().addRegion(buy);
						if (event.isCancelled()) {
							namesAddCancelled.add(buy.getName());
							return;
						}

						buy.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
						buy.update();

						// Add existing owners/members if any
						if(!landlord && !existing.isEmpty()) {
							UUID buyBy = existing.remove(0);
							OfflinePlayer buyByPlayer = Bukkit.getOfflinePlayer(buyBy);

							BuyingRegionEvent buyingRegionEvent = new BuyingRegionEvent(buy, buyByPlayer);
							Bukkit.getPluginManager().callEvent(buyingRegionEvent);
							if(buyingRegionEvent.isCancelled()) {
								regionsBuyCancelled.add(buy);
							} else {
								// Set the owner
								buy.setBuyer(buyBy);
								buy.updateLastActiveTime();

								// Update everything
								buy.handleSchematicEvent(GeneralRegion.RegionEvent.BOUGHT);

								// Add others as friends
								for (UUID friend : existing) {
									buy.getFriendsFeature().addFriend(friend, null);
								}

								buy.notifyAndUpdate(new BoughtRegionEvent(buy));
							}
						}

						regionsSuccess.add(buy);
					}
				}
			},
			() -> {
				if(!regionsSuccess.isEmpty()) {
					messageBridge.message(sender, "add-success", args[1], Utils.combinedMessage(regionsSuccess, "region"));
				}
				if(!regionsAlready.isEmpty()) {
					messageBridge.message(sender, "add-failed", Utils.combinedMessage(regionsAlready, "region"));
				}
				if(!regionsAlreadyOtherWorld.isEmpty()) {
					messageBridge.message(sender, "add-failedOtherWorld", Utils.combinedMessage(regionsAlreadyOtherWorld, "region"));
				}
				if(!regionsRentCancelled.isEmpty()) {
					messageBridge.message(sender, "add-rentCancelled", Utils.combinedMessage(regionsRentCancelled, "region"));
				}
				if(!regionsBuyCancelled.isEmpty()) {
					messageBridge.message(sender, "add-buyCancelled", Utils.combinedMessage(regionsBuyCancelled, "region"));
				}
				if(!namesBlacklisted.isEmpty()) {
					messageBridge.message(sender, "add-blacklisted", Utils.createCommaSeparatedList(namesBlacklisted));
				}
				if(!namesNoPermission.isEmpty()) {
					messageBridge.message(sender, "add-noPermissionRegions", Utils.createCommaSeparatedList(namesNoPermission));
					messageBridge.message(sender, "add-noPermissionOwnerMember");
				}
				if(!namesAddCancelled.isEmpty()) {
					messageBridge.message(sender, "add-rentCancelled", Utils.createCommaSeparatedList(namesAddCancelled));
				}
			}
		);
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			if(sender.hasPermission("areashop.createrent")) {
				result.add("rent");
			}
			if(sender.hasPermission("areashop.createbuy")) {
				result.add("buy");
			}
		} else if(toComplete == 3) {
			if(sender instanceof Player) {
				Player player = (Player)sender;
				if(sender.hasPermission("areashop.createrent") || sender.hasPermission("areashop.createbuy")) {
					for(ProtectedRegion region : plugin.getRegionManager(player.getWorld()).getRegions().values()) {
						result.add(region.getId());
					}
				}
			}
		}
		return result;
	}

}










