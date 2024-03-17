package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.ValidatedOfflinePlayerParser;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Singleton
public class QuickBuyCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_REGION = CloudKey.of("region", String.class);
    private static final CloudKey<Double> KEY_PRICE = CloudKey.of("price", Double.class);
    private static final CloudKey<OfflinePlayer> KEY_LANDLORD = CloudKey.of("landlord", OfflinePlayer.class);
    private final WorldGuardInterface worldGuardInterface;
    private final WorldEditInterface worldEditInterface;
    private final RegionFactory regionFactory;
    private final IFileManager fileManager;
    private final Server server;
    private final Plugin plugin;
    private final MessageBridge messageBridge;

    @Inject
    public QuickBuyCommand(
            @NonNull MessageBridge messageBridge,
            @NonNull WorldGuardInterface worldGuardInterface,
            @NonNull RegionFactory regionFactory,
            @NonNull IFileManager fileManager,
            @NonNull WorldEditInterface worldEditInterface,
            @NonNull Server server,
            @NonNull Plugin plugin
    ) {
        this.messageBridge = messageBridge;
        this.regionFactory = regionFactory;
        this.fileManager = fileManager;
        this.worldGuardInterface = worldGuardInterface;
        this.worldEditInterface = worldEditInterface;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @NotNull
    @Override
    protected Command.Builder<? extends CommandSender> configureCommand(@NotNull Command.Builder<CommandSender> builder) {
        return builder.literal("quickbuy")
                .permission("permission")
                .senderType(Player.class)
                .required(KEY_REGION, StringParser.stringParser())
                .required(KEY_PRICE, DoubleParser.doubleParser(0))
                .required(KEY_LANDLORD, ValidatedOfflinePlayerParser.validatedOfflinePlayerParser())
                .handler(this::handleCommand);
    }

    private void handleCommand(@NonNull CommandContext<Player> context) {
        Player player = context.sender();
        if (!player.hasPermission("areashop.quickbuy")) {
            player.sendMessage("Insufficient permission");
            return;
        }
        WorldEditSelection selection = this.worldEditInterface.getPlayerSelection(player);
        if (selection == null) {
            this.messageBridge.message(player, "cmd-noSelection");
            return;
        }
        List<ProtectedRegion> protectedRegions = Utils.getWorldEditRegionsInSelection(selection);
        if (!protectedRegions.isEmpty()) {
            this.messageBridge.message(player, "cmd-moreRegionsAtLocation");
            return;
        }
        World world = player.getWorld();
        if (!Objects.equals(selection.getWorld(), world)) {
            this.messageBridge.message(player, "setup-selectionWorldMismatch");
        }
        String regionName = context.get(KEY_REGION);
        if (this.fileManager.getRegion(regionName) != null) {
            this.messageBridge.message(player, "add-failed", regionName);
            return;
        }
        this.server.dispatchCommand(player, String.format("rg define %s", regionName));
        this.server.getScheduler().runTaskLater(this.plugin, () -> {
            ProtectedRegion region = this.worldGuardInterface.getRegionManager(world).getRegion(regionName);
            if (region == null) {
                player.sendMessage("Failed to create WG region!");
                return;
            }
            double price = context.get(KEY_PRICE);

            OfflinePlayer landlord = context.get(KEY_LANDLORD);
            // on scheduler thread
            if (!landlord.hasPlayedBefore()) {
                this.messageBridge.message(player, "me-noPlayer");
                return;
            }
            // on main thread
            BuyRegion buyRegion = this.regionFactory.createBuyRegion(regionName, world);
            buyRegion.setPrice(price);
            buyRegion.setLandlord(landlord.getUniqueId(), landlord.getName());
            this.fileManager.addRegion(buyRegion);
            this.messageBridge.message(player, "add-success", "buy", regionName);
        }, 10);
    }

    @Nullable
    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.quickbuy")) {
            return "help-quickbuy";
        }
        return null;
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("quickbuy");
    }
}