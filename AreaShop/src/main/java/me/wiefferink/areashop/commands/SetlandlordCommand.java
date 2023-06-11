package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SetlandlordCommand extends CommandAreaShop {

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
        return "areashop setlandlord";
    }

    @Override
    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.setlandlord")) {
            return "help-setlandlord";
        }
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("areashop.setlandlord")) {
            messageBridge.message(sender, "setlandlord-noPermission");
            return;
        }
        if (args.length < 2) {
            messageBridge.message(sender, "setlandlord-help");
            return;
        }
        GeneralRegion region;
        if (args.length < 3) {
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
        }
        if (region == null) {
            messageBridge.message(sender, "cmd-notRegistered", args[2]);
            return;
        }
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(args[1]).thenAcceptAsync(offlinePlayer -> {
            String playerName = offlinePlayer.getName();
            if (playerName == null || playerName.isEmpty()) {
                playerName = args[1];
            }
            region.setLandlord(offlinePlayer.getUniqueId(), playerName);
            region.update();
            messageBridge.message(sender, "setlandlord-success", playerName, region);
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
