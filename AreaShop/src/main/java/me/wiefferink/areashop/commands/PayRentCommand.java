package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

@Singleton
public class PayRentCommand extends AreashopCommandBean {

    private final AreaShop plugin;
    private final CommandFlag<RentRegion> rentRegionFlag;

    @Inject
    public PayRentCommand(@Nonnull AreaShop plugin, @Nonnull IFileManager fileManager) {

        this.plugin = plugin;
        this.rentRegionFlag = RegionParseUtil.createDefaultRent(fileManager);

    }

    @Override
    public String stringDescription() {

        return "Allows you to pay the rent of a region, your own or someone else's";

    }

    @Override
    public String getHelpKey(CommandSender target) {

        if (target.hasPermission("areashop.payrent")) {

            return "help-payrent";

        }

        return null;

    }

    @Override
    protected @Nonnull CommandProperties properties() {

        return CommandProperties.of("payrent");

    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(
            @Nonnull Command.Builder<CommandSender> builder)
    {

        return builder.literal("payrent").flag(this.rentRegionFlag).senderType(Player.class)
                .handler(this::handleCommand);

    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {

        if (!context.hasPermission("areashop.payrent")) {

            throw new AreaShopCommandException("payrent-noPermission");

        }

        RentRegion region = RegionParseUtil.getOrParseRentRegion(context, this.rentRegionFlag);
        if (!region.isRented()) {

            throw new AreaShopCommandException("payrent-notRented", region.getName());

        }

        // During the jubilee only the first rent is ever charged, extends are
        // free and automatic, so there is no rent to pay
        if (plugin.isJubilee()) {

            throw new AreaShopCommandException("payrent-jubilee", region.getName());

        }

        Player payer = context.sender();
        if (payer.getUniqueId().equals(region.getRenter())) {

            region.rent(payer);
            return;

        }

        region.rent(Bukkit.getOfflinePlayer(region.getRenter()), payer);

    }

}
