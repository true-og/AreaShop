package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SetrestoreCommand extends CommandAreaShop {

	private final IFileManager fileManager;

	@Inject
	public SetrestoreCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
		super(messageBridge);
		this.fileManager = fileManager;
	}
	
	@Override
	public String getCommandStart() {
		return "areashop setrestore";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.setrestore")) {
			return "help-setrestore";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.setrestore")) {
			messageBridge.message(sender, "setrestore-noPermission");
			return;
		}
		if(args.length <= 2 || args[1] == null || args[2] == null) {
			messageBridge.message(sender, "setrestore-help");
			return;
		}
		GeneralRegion region = fileManager.getRegion(args[1]);
		if(region == null) {
			messageBridge.message(sender, "setrestore-notRegistered", args[1]);
			return;
		}
		Boolean value = null;
		if(args[2].equalsIgnoreCase("true")) {
			value = true;
		} else if(args[2].equalsIgnoreCase("false")) {
			value = false;
		}
		region.setRestoreSetting(value);
		String valueString = "general";
		if(value != null) {
			valueString = value + "";
		}
		if(args.length > 3) {
			region.setSchematicProfile(args[3]);
			messageBridge.message(sender, "setrestore-successProfile", valueString, args[3], region);
		} else {
			messageBridge.message(sender, "setrestore-success", valueString, region);
		}
		region.update();
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result = fileManager.getRegionNames();
		} else if(toComplete == 3) {
			result.add("true");
			result.add("false");
			result.add("general");
		} else if(toComplete == 4) {
			ConfigurationSection schemProfiles = fileManager.getConfig().getConfigurationSection("schematicProfiles");
			if (schemProfiles != null) {
				result.addAll(schemProfiles.getKeys(false));
			}
		}
		return result;
	}
}
