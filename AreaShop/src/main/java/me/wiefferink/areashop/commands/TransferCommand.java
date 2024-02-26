package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Singleton
public class TransferCommand extends CommandAreaShop {

    private final IFileManager fileManager;
    private final Server server;

    @Inject
    public TransferCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull Server server
    ) {
        super(messageBridge);
        this.fileManager = fileManager;
        this.server = server;
    }

    @Override
    public String getCommandStart() {
        return "areashop transfer";
    }

    @Override
    public String getHelp(CommandSender target) {
        if (!target.hasPermission("areashop.transfer")) {
            return null;
        }
        return "help-transfer";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("areashop.transfer")) {
            this.messageBridge.message(sender, "transfer-noPermission");
            return;
        }
        if (sender instanceof Player player) {
            handlePlayer(player, args);
        } else {
            this.messageBridge.message(sender, "cmd-onlyByPlayer");
        }
    }

    private void handlePlayer(Player player, String[] args) {
        if (args.length < 2) {
            this.messageBridge.message(player, "transfer-help");
            return;
        }
        String targetPlayerName = args[1];
        if (targetPlayerName.equals(player.getName())) {
            this.messageBridge.message(player, "transfer-transferSelf");
            return;
        }
        GeneralRegion region;
        if (args.length > 2) {
            String targetRegionName = args[2];
            region = this.fileManager.getRegion(targetRegionName);
            if (region == null) {
                messageBridge.message(player, "cmd-noRegion");
                return;
            }
        } else {
            List<GeneralRegion> regions = Utils.getImportantRegions(player.getLocation());
            if (regions.isEmpty()) {
                messageBridge.message(player, "cmd-noRegionsAtLocation");
                return;
            } else if (regions.size() > 1) {
                messageBridge.message(player, "cmd-moreRegionsAtLocation");
                return;
            }
            region = regions.get(0);
        }
        if (!region.isTransferEnabled()) {
            this.messageBridge.message(player, "transfer-disabled");
            return;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer targetPlayer = this.server.getOfflinePlayer(targetPlayerName);
        if (!targetPlayer.hasPlayedBefore()) {
            // Unknown player
            this.messageBridge.message(player, "transfer-noPlayer", targetPlayerName);
            return;
        }
        if (region.isLandlord(player.getUniqueId())) {
            // Transfer ownership if same as landlord
            region.getFriendsFeature().deleteFriend(region.getOwner(), null);
            region.setOwner(targetPlayer.getUniqueId());
            region.setLandlord(targetPlayer.getUniqueId(), targetPlayerName);
            this.messageBridge.message(player, "transfer-transferred-owner", targetPlayerName, region);
            this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-owner", targetPlayerName, region);
            region.update();
            region.saveRequired();
            return;
        }
        if (!region.isOwner(player.getUniqueId())) {
            // Cannot transfer tenant if we aren't the current tenant
            this.messageBridge.message(player, "transfer-notCurrentTenant");
            return;
        }
        region.getFriendsFeature().deleteFriend(region.getOwner(), null);
        // Swap the owner/occupant (renter or buyer)
        region.setOwner(targetPlayer.getUniqueId());

        this.messageBridge.message(player, "transfer-transferred-tenant", targetPlayerName, region);
        if (targetPlayer.isOnline()) {
            this.messageBridge.message(targetPlayer.getPlayer(), "transfer-transferred-tenant", targetPlayerName, region);
        }
        region.update();
        region.saveRequired();
    }

    @Override
    public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
        if (toComplete == 2) {
            Collection<? extends Player> players = this.server.getOnlinePlayers();
            List<String> ret = new ArrayList<>(players.stream()
                    .map(Player::getName)
                    .toList());
            if (sender instanceof Player player) {
                ret.remove(player.getName());
            }
            return ret;
        } else if (toComplete == 3) {
            if (!(sender instanceof Player player)) {
                return this.fileManager.getRegionNames();
            }
            UUID uuid = player.getUniqueId();
            return new ArrayList<>(this.fileManager.getRegions()
                    .stream()
                    .filter(region -> region.isOwner(uuid) || region.isLandlord(uuid))
                    .map(GeneralRegion::getName)
                    .toList());
        }
        return Collections.emptyList();
    }
}
