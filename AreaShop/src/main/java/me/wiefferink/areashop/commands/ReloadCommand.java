package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

@Singleton
public class ReloadCommand extends CommandAreaShop {

	private final AreaShop plugin;

	@Inject
	public ReloadCommand(@Nonnull MessageBridge messageBridge, @Nonnull AreaShop plugin) {
		super(messageBridge);
		this.plugin = plugin;
	}
	
	@Override
	public String getCommandStart() {
		return "areashop reload";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.reload")) {
			return "help-reload";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(sender.hasPermission("areashop.reload")) {
			// Reload the configuration files and update all region flags/signs
			plugin.reload(sender);
		} else {
			messageBridge.message(sender, "reload-noPermission");
		}
	}
}
