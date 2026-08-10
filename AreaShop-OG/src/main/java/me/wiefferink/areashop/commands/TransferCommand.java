package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.ValidatedOfflinePlayerParser;
import me.wiefferink.areashop.events.notify.TransferredRegionEvent;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TransferCommand extends AreashopCommandBean {

    private static final CloudKey<OfflinePlayer> KEY_PLAYER = CloudKey.of("player", OfflinePlayer.class);
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public TransferCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {

        ParserDescriptor<Player, GeneralRegion> regionParser = ParserDescriptor
                .of(new GeneralRegionParser<>(fileManager, this::suggestRegions), GeneralRegion.class);
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.regionFlag = CommandFlag.builder("region").withComponent(regionParser).build();

    }

    @Override
    public String getHelpKey(CommandSender target) {

        if (!target.hasPermission("areashop.transfer")) {

            return null;

        }

        return "help-transfer";

    }

    @Override
    public String stringDescription() {

        return null;

    }

    @NotNull
    @Override
    protected Command.Builder<? extends CommandSender> configureCommand(
            @NotNull Command.Builder<CommandSender> builder)
    {

        return builder.literal("transfer").senderType(Player.class)
                .required(KEY_PLAYER, ValidatedOfflinePlayerParser.validatedOfflinePlayerParser()).flag(this.regionFlag)
                .handler(this::handleCommand);

    }

    @Override
    protected @NonNull CommandProperties properties() {

        return CommandProperties.of("transfer");

    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {

        Player sender = context.sender();
        if (!sender.hasPermission("areashop.transfer")) {

            throw new AreaShopCommandException("transfer-noPermission");

        }

        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, this.regionFlag);
        if (!region.isTransferEnabled()) {

            throw new AreaShopCommandException("transfer-disabled");

        }

        OfflinePlayer targetPlayer = context.get(KEY_PLAYER);
        String targetPlayerName = targetPlayer.getName();
        if (Objects.equals(sender, targetPlayer)) {

            throw new AreaShopCommandException("transfer-transferSelf");

        }

        if (!targetPlayer.hasPlayedBefore()) {

            // Unknown player
            throw new AreaShopCommandException("transfer-noPlayer", targetPlayerName);

        }

        // Captured before the swap, listeners need the player losing the region.
        UUID previousOwner = region.getOwner();
        UUID newOwner = targetPlayer.getUniqueId();
        if (region.isLandlord(sender.getUniqueId())) {

            // Transfer ownership if same as landlord
            removeOwnerFromFriends(region, previousOwner);
            region.setOwner(newOwner);
            region.setLandlord(newOwner, targetPlayerName);
            this.messageBridge.message(sender, "transfer-transferred-owner", targetPlayerName, region);
            this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-owner", targetPlayerName, region);
            region.update();
            region.saveRequired();
            announceTransfer(region, previousOwner, newOwner, true);
            return;

        }

        if (!region.isOwner(sender.getUniqueId())) {

            // Cannot transfer tenant if we aren't the current tenant
            throw new AreaShopCommandException("transfer-notCurrentTenant");

        }

        removeOwnerFromFriends(region, previousOwner);
        // Swap the owner/occupant (renter or buyer)
        region.setOwner(newOwner);

        this.messageBridge.message(sender, "transfer-transferred-tenant", targetPlayerName, region);
        this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-tenant", targetPlayerName, region);
        region.update();
        region.saveRequired();
        announceTransfer(region, previousOwner, newOwner, false);

    }

    // A landlord can transfer an unoccupied region, which has no owner to drop.
    private void removeOwnerFromFriends(@Nonnull GeneralRegion region, UUID owner) {

        if (owner == null) {

            return;

        }

        region.getFriendsFeature().deleteFriend(owner, null);

    }

    // Fired after the region is updated and saved, so listeners see final state.
    private void announceTransfer(@Nonnull GeneralRegion region, UUID from, UUID to, boolean landlordTransfer) {

        Bukkit.getPluginManager().callEvent(new TransferredRegionEvent(region, from, to, landlordTransfer));

    }

    private CompletableFuture<Iterable<Suggestion>> suggestRegions(@Nonnull CommandContext<Player> context,
            @Nonnull CommandInput input)
    {

        String text = input.peekString();
        UUID uuid = context.sender().getUniqueId();
        List<Suggestion> suggestions = this.fileManager.getRegions().stream()
                .filter(region -> region.isOwner(uuid) || region.isLandlord(uuid)).map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text)).map(Suggestion::suggestion).toList();
        return CompletableFuture.completedFuture(suggestions);

    }

}
