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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DelfriendCommand extends CommandAreaShop {

    private final IFileManager fileManager;
    private final OfflinePlayerHelper offlinePlayerHelper;
    private final BukkitSchedulerExecutor executor;

    @Inject
    public DelfriendCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper,
            @Nonnull BukkitSchedulerExecutor executor
    ) {
        super(messageBridge);
        this.fileManager = fileManager;
        this.offlinePlayerHelper = offlinePlayerHelper;
        this.executor = executor;
    }

    /**
     * Check if a person can remove friends.
     *
     * @param person The person to check
     * @param region The region to check for
     * @return true if the person can remove friends, otherwise false
     */
    public static boolean canUse(CommandSender person, GeneralRegion region) {
        if (person.hasPermission("areashop.delfriendall")) {
            return true;
        }
        if (person instanceof Player player) {
            return region.isOwner(player) && player.hasPermission("areashop.delfriend");
        }
        return false;
    }

    @Override
    public String getCommandStart() {
        return "areashop delfriend";
    }

    @Override
    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.delfriendall") || target.hasPermission("areashop.delfriend")) {
            return "help-delFriend";
        }
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("areashop.delfriend") && !sender.hasPermission("areashop.delfriendall")) {
            messageBridge.message(sender, "delfriend-noPermission");
            return;
        }
        if (args.length < 2) {
            messageBridge.message(sender, "delfriend-help");
            return;
        }
        GeneralRegion region;
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                messageBridge.message(sender, "cmd-automaticRegionOnlyByPlayer");
                return;
            }
            // get the region by location
            List<GeneralRegion> regions = Utils.getImportantRegions(player.getLocation());
            if (regions.isEmpty()) {
                messageBridge.message(sender, "cmd-noRegionsAtLocation");
                return;
            } else if (regions.size() > 1) {
                messageBridge.message(sender, "cmd-moreRegionsAtLocation");
                return;
            } else {
                region = regions.get(0);
            }
        } else {
            region = fileManager.getRegion(args[2]);
            if (region == null) {
                messageBridge.message(sender, "cmd-notRegistered", args[2]);
                return;
            }
        }
        if (sender.hasPermission("areashop.delfriendall")) {
            if ((region instanceof RentRegion rentRegion && !rentRegion.isRented())
                    || (region instanceof BuyRegion buyRegion && !buyRegion.isSold())) {
                messageBridge.message(sender, "delfriend-noOwner", region);
                return;
            }
            this.offlinePlayerHelper.lookupOfflinePlayerAsync(args[1]).thenAcceptAsync(friend -> {
                if (!region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
                    messageBridge.message(sender, "delfriend-notAdded", friend.getName(), region);
                    return;
                }
                if (region.getFriendsFeature().deleteFriend(friend.getUniqueId(), sender)) {
                    region.update();
                    messageBridge.message(sender, "delfriend-successOther", friend.getName(), region);
                }
            }, this.executor);
            return;
        }
        if (!sender.hasPermission("areashop.delfriend") || !(sender instanceof Player player)) {
            messageBridge.message(sender, "delfriend-noPermission", region);
            return;
        }
        if (!region.isOwner(player)) {
            messageBridge.message(sender, "delfriend-noPermissionOther", region);
            return;
        }
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(args[1]).thenAcceptAsync(friend -> {
            if (!region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
                messageBridge.message(sender, "delfriend-notAdded", friend.getName(), region);
            } else if (region.getFriendsFeature().deleteFriend(friend.getUniqueId(), sender)) {
                region.update();
                messageBridge.message(sender, "delfriend-success", friend.getName(), region);
            }
        }, this.executor);
    }

    @Override
    public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
        ArrayList<String> result = new ArrayList<>();
        if (toComplete == 2) {
            for (Player player : Utils.getOnlinePlayers()) {
                result.add(player.getName());
            }
        } else if (toComplete == 3) {
            result.addAll(fileManager.getRegionNames());
        }
        return result;
    }
}








