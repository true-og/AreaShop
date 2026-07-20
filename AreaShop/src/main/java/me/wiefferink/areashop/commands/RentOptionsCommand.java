package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.features.confirmation.RentOptionsGui;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

/**
 * Opens rent management for the current renter, or a pay-rent confirmation for
 * another player.
 */
@Singleton
public class RentOptionsCommand extends AreashopCommandBean {

    private final CommandFlag<RentRegion> regionFlag;
    private final RentOptionsGui rentOptionsGui;

    @Inject
    public RentOptionsCommand(@Nonnull IFileManager fileManager, @Nonnull RentOptionsGui rentOptionsGui) {

        this.regionFlag = RegionParseUtil.createDefaultRent(fileManager);
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

        return CommandProperties.of("rentoptions");

    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(
            @Nonnull Command.Builder<CommandSender> builder)
    {

        return builder.literal("rentoptions").flag(regionFlag).senderType(Player.class).handler(this::handleCommand);

    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {

        Player player = context.sender();
        RentRegion region = RegionParseUtil.getOrParseRentRegion(context, regionFlag);
        if (region.isRenter(player)) {

            rentOptionsGui.openRentOptions(player, region);

        } else {

            rentOptionsGui.openPayRentConfirmation(player, region);

        }

    }

}
