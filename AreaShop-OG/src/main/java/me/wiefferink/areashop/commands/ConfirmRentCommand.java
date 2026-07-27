package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.features.confirmation.RentOptionsGui;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

// Opens the confirmation screen used by chat and sign rent actions.
@Singleton
public class ConfirmRentCommand extends AreashopCommandBean {

    private final CommandFlag<RentRegion> regionFlag;
    private final MessageBridge messageBridge;
    private final RentOptionsGui rentOptionsGui;

    @Inject
    public ConfirmRentCommand(@Nonnull IFileManager fileManager, @Nonnull MessageBridge messageBridge,
            @Nonnull RentOptionsGui rentOptionsGui)
    {

        this.regionFlag = RegionParseUtil.createDefaultRent(fileManager);
        this.messageBridge = messageBridge;
        this.rentOptionsGui = rentOptionsGui;

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

        return CommandProperties.of("confirmrent");

    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(
            @Nonnull Command.Builder<CommandSender> builder)
    {

        return builder.literal("confirmrent").flag(regionFlag).senderType(Player.class).handler(this::handleCommand);

    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {

        Player player = context.sender();
        if (!player.hasPermission("areashop.rent")) {

            messageBridge.message(player, "rent-noPermission");
            return;

        }

        RentRegion region = RegionParseUtil.getOrParseRentRegion(context, regionFlag);
        // A rented shop is managed from the rent menu instead
        if (region.isRented()) {

            if (region.isRenter(player)) {

                rentOptionsGui.openRentOptions(player, region);

            } else {

                rentOptionsGui.openPayRentConfirmation(player, region);

            }

            return;

        }

        // Tell the player about their rank limit before showing a price they
        // cannot accept
        if (!region.checkLimitsAndInform(player, GeneralRegion.RegionType.RENT, false)) {

            return;

        }

        rentOptionsGui.openRentConfirmation(player, region);

    }

}
