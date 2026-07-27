package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionGroupParser;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import net.trueog.diamondbankog.DiamondBankException;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.EnumParser;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
public class FindCommand extends AreashopCommandBean {

    private static final CloudKey<GeneralRegion.RegionType> KEY_REGION_TYPE = CloudKey.of("regionType",
            GeneralRegion.RegionType.class);
    private static final CloudKey<Double> KEY_PRICE = CloudKey.of("maxPrice", Double.class);

    private final AreaShop plugin;
    private final DiamondBankAPIJava economy;
    private final IFileManager fileManager;
    private final CommandFlag<RegionGroup> regionGroupFlag;

    private final MessageBridge messageBridge;

    @Inject
    public FindCommand(@Nonnull AreaShop plugin, @Nonnull MessageBridge messageBridge,
            @Nonnull DiamondBankAPIJava economy, @Nonnull IFileManager fileManager)
    {

        this.plugin = plugin;
        this.messageBridge = messageBridge;
        this.economy = economy;
        this.fileManager = fileManager;
        this.regionGroupFlag = CommandFlag.builder("region")
                .withComponent(
                        ParserDescriptor.of(new RegionGroupParser<>(fileManager, "find-wrongGroup"), RegionGroup.class))
                .build();

    }

    @Override
    public String stringDescription() {

        return null;

    }

    @Override
    public String getHelpKey(CommandSender target) {

        if (target.hasPermission("areashop.find")) {

            return "help-find";

        }

        return null;

    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(
            @Nonnull Command.Builder<CommandSender> builder)
    {

        return builder.literal("find").senderType(Player.class)
                .required(KEY_REGION_TYPE, EnumParser.enumParser(GeneralRegion.RegionType.class))
                .optional(KEY_PRICE, DoubleParser.doubleParser(0)).flag(this.regionGroupFlag)
                .handler(this::handleCommand);

    }

    @Override
    protected @Nonnull CommandProperties properties() {

        return CommandProperties.of("find");

    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {

        Player sender = context.sender();
        if (!sender.hasPermission("areashop.find")) {

            throw new AreaShopCommandException("find-noPermission");

        }

        boolean maxPriceSet = context.contains(KEY_PRICE);
        double maxPrice = context.getOrDefault(KEY_PRICE, Double.MAX_VALUE);
        RegionGroup group = context.flags().get(this.regionGroupFlag);
        GeneralRegion.RegionType regionType = context.get(KEY_REGION_TYPE);
        Message onlyInGroup;
        if (group != null) {

            onlyInGroup = Message.fromKey("find-onlyInGroup").replacements(group.getName());

        } else {

            onlyInGroup = Message.empty();

        }

        // With an explicit maximum price the player's balance is not needed, so
        // the blocking balance lookup can be skipped entirely
        if (maxPriceSet || economy == null) {

            long limitShards = maxPriceSet ? Utils.diamondsToShards(economy, maxPrice) : 0;
            String limitDisplay = Utils.formatCurrency(maxPriceSet ? maxPrice : 0);
            find(sender, regionType, limitShards, limitDisplay, maxPriceSet, onlyInGroup, group);
            return;

        }

        // The balance lookup blocks on the database, so run it off the main
        // thread and continue the search on the main thread afterwards
        plugin.runEconomyTask(() -> {

            try {

                return economy.getTotalShards(sender.getUniqueId());

            } catch (DiamondBankException e) {

                return 0L;

            }

        }, balanceShards -> find(sender, regionType, balanceShards, Utils.shardsToDisplay(economy, balanceShards),
                false, onlyInGroup, group));

    }

    private void find(@Nonnull Player sender, @Nonnull GeneralRegion.RegionType regionType, long limitShards,
            @Nonnull String limitDisplay, boolean maxPriceSet, @Nonnull Message onlyInGroup, RegionGroup group)
    {

        switch (regionType) {

            case BUY -> handleBuy(sender, limitShards, limitDisplay, maxPriceSet, onlyInGroup, group);
            case RENT -> handleRent(sender, limitShards, limitDisplay, maxPriceSet, onlyInGroup, group);

        }

    }

    private void handleBuy(@Nonnull Player sender, long limitShards, @Nonnull String limitDisplay, boolean maxPriceSet,
            @Nonnull Message onlyInGroup, RegionGroup group)
    {

        Collection<BuyRegion> regions = fileManager.getBuysRef();
        List<BuyRegion> results = new LinkedList<>();
        for (BuyRegion region : regions) {

            if (!region.isSold() && Utils.diamondsToShards(economy, region.getPrice()) <= limitShards
                    && (group == null || group.isMember(region)) && (region.getBooleanSetting("general.findCrossWorld")
                            || sender.getWorld().equals(region.getWorld())))
            {

                results.add(region);

            }

        }

        if (results.isEmpty()) {

            String key = maxPriceSet ? "find-noneFoundMax" : "find-noneFound";
            this.messageBridge.message(sender, key, "buy", limitDisplay, onlyInGroup);
            return;

        }

        // Draw a random one
        BuyRegion region = results.get(ThreadLocalRandom.current().nextInt(results.size()));
        // Teleport
        String key = maxPriceSet ? "find-successMax" : "find-success";
        this.messageBridge.message(sender, key, "buy", limitDisplay, onlyInGroup, region);
        boolean tpToSign = region.getBooleanSetting("general.findTeleportToSign");
        region.getTeleportFeature().teleportPlayer(sender, tpToSign, false);

    }

    private void handleRent(@Nonnull Player sender, long limitShards, @Nonnull String limitDisplay, boolean maxPriceSet,
            @Nonnull Message onlyInGroup, RegionGroup group)
    {

        Collection<RentRegion> regions = fileManager.getRentsRef();
        List<RentRegion> results = new LinkedList<>();
        for (RentRegion region : regions) {

            if (!region.isRented() && Utils.diamondsToShards(economy, region.getPrice()) <= limitShards
                    && (group == null || group.isMember(region)) && (region.getBooleanSetting("general.findCrossWorld")
                            || sender.getWorld().equals(region.getWorld())))
            {

                results.add(region);

            }

        }

        if (results.isEmpty()) {

            String key = maxPriceSet ? "find-noneFoundMax" : "find-noneFound";
            this.messageBridge.message(sender, key, "rent", limitDisplay, onlyInGroup);
            return;

        }

        // Draw a random one
        RentRegion region = results.get(ThreadLocalRandom.current().nextInt(results.size()));
        // Teleport
        String key = maxPriceSet ? "find-successMax" : "find-success";
        this.messageBridge.message(sender, key, "rent", limitDisplay, onlyInGroup, region);
        boolean tpToSign = region.getBooleanSetting("general.findTeleportToSign");
        region.getTeleportFeature().teleportPlayer(sender, tpToSign, false);

    }

}
