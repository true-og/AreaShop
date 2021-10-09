package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.managers.CommandManager;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public class HelpCommand extends CommandAreaShop {

	private final CommandManager commandManager;

	public HelpCommand(@Nonnull CommandManager commandManager) {
		this.commandManager = commandManager;
	}

	@Override
	public String getCommandStart() {
		return "areashop help";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.help")) {
			return "help-help";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		commandManager.showHelp(sender);
	}
}
