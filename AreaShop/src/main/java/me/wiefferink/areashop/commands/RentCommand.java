package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

@Singleton
public class RentCommand extends AreashopCommandBean {

    private final CommandFlag<RentRegion> rentRegionFlag;

    @Inject
    public RentCommand(@Nonnull IFileManager fileManager) {
        this.rentRegionFlag = RegionParseUtil.createDefaultRent(fileManager);
    }

    @Override
    public String stringDescription() {
        return "Allows you to rent a region";
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if(target.hasPermission("areashop.rent")) {
            return "help-rent";
        }
        return null;
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("rent");
    }


    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder
                .literal("rent")
                .flag(this.rentRegionFlag)
                .senderType(Player.class)
                .handler(this::handleCommand);
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        if (!context.hasPermission("areashop.rent")) {
            throw new AreaShopCommandException("rent-noPermission");
        }
        RentRegion region = RegionParseUtil.getOrParseRentRegion(context, this.rentRegionFlag);
        region.rent(context.sender());
    }

}
