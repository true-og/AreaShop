package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.features.homeaccess.HomeAccessFeature;
import me.wiefferink.areashop.features.homeaccess.HomeAccessType;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Singleton
public final class TogglehomeControlCommand extends CommandAreaShop {

    private final IFileManager fileManager;

    @Inject
    public TogglehomeControlCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        super(messageBridge);
        this.fileManager = fileManager;
    }

    @Override
    public String getCommandStart() {
        return "areashop togglehome";
    }


    @Override
    public String getHelp(CommandSender target) {
        if (!target.hasPermission("sethomecontrol.control")) {
            return null;
        }
        return "help-togglehome";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sethomecontrol.control")) {
            this.messageBridge.message(sender, "togglehome-noPermission");
            return;
        }
        if (args.length < 1) {
            this.messageBridge.message(sender, "togglehome-help");
            return;
        }

        final String rawAccessType = args[0];
        final HomeAccessType accessType;
        try {
            accessType = HomeAccessType.valueOf(rawAccessType.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            this.messageBridge.message(sender, "togglehome-unknownAccessType");
            return;
        }
        final GeneralRegion region;
        if (args.length >= 2) {
            final String regionName = args[1];
            region = this.fileManager.getRegion(regionName);
            if (region == null) {
                this.messageBridge.message(sender, "cmd-noRegion", regionName);
                return;
            }
        } else if (sender instanceof Player player) {
            List<GeneralRegion> regions = Utils.getImportantRegions(player.getLocation());
            if (regions.isEmpty()) {
                this.messageBridge.message(sender, "cmd-noRegionsAtLocation");
                return;
            } else if (regions.size() != 1) {
                this.messageBridge.message(sender, "cmd-moreRegionsAtLocation");
                return;
            }
            region = regions.get(0);
        } else {
            this.messageBridge.message(sender, "cmd-automaticRegionOnlyByPlayer");
            return;
        }

        if (!(sender instanceof Player) && !sender.hasPermission("sethome.control.other")) {
            return;
        }
        if (sender instanceof Player player && !region.isOwner(player)) {
            this.messageBridge.message(sender, "togglehome-noPermission");
        }
        region.getOrCreateFeature(HomeAccessFeature.class).homeAccessType(accessType);
        this.messageBridge.message(sender, "togglehome-success", accessType.name());
    }

    @Override
    public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
        if (!sender.hasPermission("sethome.control")) {
            return Collections.emptyList();
        }
        if (start.length == 1) {
            Stream<GeneralRegion> stream = this.fileManager.getRegions().stream();
            if (!sender.hasPermission("sethome.control.other") && sender instanceof Player player) {
                stream = stream.filter(region -> region.isOwner(player.getUniqueId()));
            }
            return stream.map(GeneralRegion::getName)
                    .filter(name -> name.startsWith(start[0]))
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
        if (start.length == 2 && sender instanceof Player player) {
            return this.fileManager.getRegions().stream()
                    .filter(region -> region.isOwner(player.getUniqueId()))
                    .map(GeneralRegion::getName)
                    .filter(name -> name.startsWith(start[0]))
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
        return Collections.emptyList();
    }
}
