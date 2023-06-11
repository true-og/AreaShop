package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.regions.ImportJobFactory;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ImportCommand extends CommandAreaShop {

	/* RegionGroup priority usage:
	   0: Settings from /config.yml
	   1: Settings from /worlds/<world>/config.yml
	   2: Settings from /worlds/<world>/parent-regions.yml (if their priority is set it is added to this value)
	 */
	@Inject
	private MessageBridge messageBridge;
	@Inject
	private ImportJobFactory importJobFactory;

	@Override
	public String getCommandStart() {
		return "areashop import";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.import")) {
			return "help-import";
		}
		return null;
	}

	// TODO:
	//  - Landlord?
	//  - Friends
	//  - Region flags?
	//  - Settings from the 'permissions' section in RegionForSale/config.yml?

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.import")) {
			messageBridge.message(sender, "import-noPermission");
			return;
		}

		if(args.length < 2) {
			messageBridge.message(sender, "import-help");
			return;
		}

		if(!"RegionForSale".equalsIgnoreCase(args[1])) {
			messageBridge.message(sender, "import-wrongSource");
			return;
		}

		if(!confirm(sender, args, Message.fromKey("import-confirm"))) {
			return;
		}

		importJobFactory.createImportJob(sender).execute();
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result.add("RegionForSale");
		}
		return result;
	}

}










