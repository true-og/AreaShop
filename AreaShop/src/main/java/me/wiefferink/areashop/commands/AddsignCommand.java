package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.features.signs.RegionSign;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Materials;
import me.wiefferink.areashop.tools.SignUtils;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
public class AddsignCommand extends CommandAreaShop {
	private final IFileManager fileManager;
	private final SignManager signManager;
	private final Plugin plugin;

    @Inject
    public AddsignCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull SignManager signManager,
            @Nonnull Plugin plugin
    ) {
        super(messageBridge);
        this.fileManager = fileManager;
        this.signManager = signManager;
        this.plugin = plugin;
    }

	@Override
	public String getCommandStart() {
		return "areashop addsign";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.addsign")) {
			return "help-addsign";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.addsign")) {
			messageBridge.message(sender, "addsign-noPermission");
			return;
		}
		if(!(sender instanceof Player player)) {
			messageBridge.message(sender, "cmd-onlyByPlayer");
			return;
		}

		// Get the sign
		Block block = null;
		BlockIterator blockIterator = new BlockIterator(player, 100);
		while(blockIterator.hasNext() && block == null) {
			Block next = blockIterator.next();
			if(next.getType() != Material.AIR) {
				block = next;
			}
		}
		if(block == null || !Materials.isSign(block.getType())) {
			messageBridge.message(sender, "addsign-noSign");
			return;
		}

		GeneralRegion region;
		if(args.length > 1) {
			// Get region by argument
			region = fileManager.getRegion(args[1]);
			if(region == null) {
				messageBridge.message(sender, "cmd-notRegistered", args[1]);
				return;
			}
		} else {
			// Get region by sign position
			List<GeneralRegion> regions = Utils.getImportantRegions(block.getLocation());
			if(regions.isEmpty()) {
				messageBridge.message(sender, "addsign-noRegions");
				return;
			} else if(regions.size() > 1) {
				messageBridge.message(sender, "addsign-couldNotDetect", regions.get(0).getName(), regions.get(1).getName());
				return;
			}
			region = regions.get(0);
		}
		String profile = null;
		if(args.length > 2) {
			profile = args[2];
			Set<String> profiles = plugin.getConfig().getConfigurationSection("signProfiles").getKeys(false);
			if(!profiles.contains(profile)) {
				messageBridge.message(sender, "addsign-wrongProfile", Utils.createCommaSeparatedList(profiles), region);
				return;
			}
		}
		Optional<RegionSign> optionalRegionSign = signManager.signFromLocation(block.getLocation());
		if(optionalRegionSign.isPresent()) {
			RegionSign regionSign = optionalRegionSign.get();
			messageBridge.message(sender, "addsign-alreadyRegistered", regionSign.getRegion());
			return;
		}

		region.getSignsFeature().addSign(block.getLocation(), block.getType(), SignUtils.getSignFacing(block), profile);
		if(profile == null) {
			messageBridge.message(sender, "addsign-success", region);
		} else {
			messageBridge.message(sender, "addsign-successProfile", profile, region);
		}
		region.update();
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result.addAll(fileManager.getRegionNames());
		} else if(toComplete == 3) {
			result.addAll(plugin.getConfig().getStringList("signProfiles"));
		}
		return result;
	}

}










