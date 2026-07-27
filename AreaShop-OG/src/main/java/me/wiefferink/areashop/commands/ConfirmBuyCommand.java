package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.features.confirmation.ShopConfirmationGui;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

/**
 * Opens the confirmation screen used by chat and sign buy actions.
 */
@Singleton
public class ConfirmBuyCommand extends AreashopCommandBean {

    private final CommandFlag<BuyRegion> regionFlag;
    private final MessageBridge messageBridge;
    private final ShopConfirmationGui confirmationGui;

    @Inject
    public ConfirmBuyCommand(@Nonnull IFileManager fileManager, @Nonnull MessageBridge messageBridge,
            @Nonnull ShopConfirmationGui confirmationGui)
    {

        this.regionFlag = RegionParseUtil.createDefaultBuy(fileManager);
        this.messageBridge = messageBridge;
        this.confirmationGui = confirmationGui;

    }

    @Override
    public String getHelpKey(CommandSender target) {

        return null;

    }

    @Override
    public String stringDescription() {

        return null;

    }

    @Override
    protected @Nonnull CommandProperties properties() {

        return CommandProperties.of("confirmbuy");

    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(
            @Nonnull Command.Builder<CommandSender> builder)
    {

        return builder.literal("confirmbuy").flag(regionFlag).senderType(Player.class).handler(this::handleCommand);

    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {

        Player player = context.sender();
        if (!player.hasPermission("areashop.buy")) {

            messageBridge.message(player, "buy-noPermission");
            return;

        }

        BuyRegion region = RegionParseUtil.getOrParseBuyRegion(context, regionFlag);
        // Tell the player about their rank limit before showing a price they
        // cannot accept
        if (!region.isOwner(player) && !region.checkLimitsAndInform(player, GeneralRegion.RegionType.BUY, false)) {

            return;

        }

        confirmationGui.openBuyConfirmation(player, region);

    }

}
