package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;

import javax.annotation.Nonnull;

@Singleton
public class InfoBaseCommand extends AreashopCommandBean {

    private final MessageBridge messageBridge;

    @Inject
    public InfoBaseCommand(
            @Nonnull MessageBridge messageBridge
    ) {
        this.messageBridge = messageBridge;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.info")) {
            return "help-info";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder.literal("info").handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("info");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.info")) {
            messageBridge.message(sender, "info-noPermission");
            return;
        }
        this.messageBridge.message(sender, "info-help");
    }

}


























