package me.wiefferink.areashop.regions;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.events.ask.BuyingRegionEvent;
import me.wiefferink.areashop.events.ask.ResellingRegionEvent;
import me.wiefferink.areashop.events.ask.SellingRegionEvent;
import me.wiefferink.areashop.events.notify.BoughtRegionEvent;
import me.wiefferink.areashop.events.notify.ResoldRegionEvent;
import me.wiefferink.areashop.events.notify.SoldRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.FeatureManager;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import net.trueog.diamondbankog.DiamondBankException;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.UUID;

public class BuyRegion extends GeneralRegion {

    private final DiamondBankAPIJava economy;

    @AssistedInject
    BuyRegion(@Nonnull AreaShop plugin, @Nonnull FeatureManager featureManager,
            @Nonnull WorldEditInterface worldEditInterface, @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull MessageBridge messageBridge, @Nullable DiamondBankAPIJava economy,
            @Assisted @Nonnull YamlConfiguration config)
    {

        super(plugin, featureManager, worldEditInterface, worldGuardInterface, messageBridge, config);
        this.economy = economy;

    }

    @AssistedInject
    BuyRegion(@Nonnull AreaShop plugin, @Nonnull FeatureManager featureManager,
            @Nonnull WorldEditInterface worldEditInterface, @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull MessageBridge messageBridge, @Nullable DiamondBankAPIJava economy, @Assisted @Nonnull String name,
            @Assisted @Nonnull World world)
    {

        super(plugin, featureManager, worldEditInterface, worldGuardInterface, messageBridge, name, world);
        this.economy = economy;

    }

    @Override
    public boolean isOwner(UUID player) {

        return isBuyer(player);

    }

    @Override
    public UUID getOwner() {

        return getBuyer();

    }

    @Override
    public void setOwner(UUID player) {

        setBuyer(player);

    }

    @Override
    public RegionType getType() {

        return RegionType.BUY;

    }

    @Override
    public RegionState getState() {

        if (isSold() && isInResellingMode()) {

            return RegionState.RESELL;

        } else if (isSold() && !isInResellingMode()) {

            return RegionState.SOLD;

        } else {

            return RegionState.FORSALE;

        }

    }

    @Override
    public boolean isAvailable() {

        return !isSold();

    }

    /**
     * Get the UUID of the owner of this region.
     * 
     * @return The UUID of the owner of this region
     */
    public UUID getBuyer() {

        String buyer = config.getString("buy.buyer");
        if (buyer != null) {

            try {

                return UUID.fromString(buyer);

            } catch (IllegalArgumentException e) {

                // Incorrect UUID
            }

        }

        return null;

    }

    /**
     * Check if a player is the buyer of this region.
     * 
     * @param player Player to check
     * @return true if this player owns this region, otherwise false
     */
    public boolean isBuyer(OfflinePlayer player) {

        return player != null && isBuyer(player.getUniqueId());

    }

    public boolean isBuyer(UUID player) {

        UUID buyer = getBuyer();
        return !(buyer == null || player == null) && buyer.equals(player);

    }

    /**
     * Set the buyer of this region.
     * 
     * @param buyer The UUID of the player that should be set as buyer
     */
    public void setBuyer(UUID buyer) {

        if (buyer == null) {

            setSetting("buy.buyer", null);
            setSetting("buy.buyerName", null);

        } else {

            setSetting("buy.buyer", buyer.toString());
            setSetting("buy.buyerName", Utils.toName(buyer));

        }

    }

    /**
     * Get the name of the player that owns this region.
     * 
     * @return The name of the player that owns this region, if unavailable by UUID
     *         it will return the old cached name, if that is unavailable it will
     *         return &lt;UNKNOWN&gt;
     */
    public String getPlayerName() {

        String result = Utils.toName(getBuyer());
        if (result == null || result.isEmpty()) {

            result = getStringSetting("buy.buyerName");
            if (result == null || result.isEmpty()) {

                result = "<UNKNOWN>";

            }

        }

        return result;

    }

    /**
     * Check if the region is sold.
     * 
     * @return true if the region is sold, otherwise false
     */
    public boolean isSold() {

        return getBuyer() != null;

    }

    /**
     * Check if the region is being resold.
     * 
     * @return true if the region is available for reselling, otherwise false
     */
    public boolean isInResellingMode() {

        return config.getBoolean("buy.resellMode");

    }

    /**
     * Get the price of the region.
     * 
     * @return The price of the region
     */
    public double getPrice() {

        return Math.max(0, Utils.evaluateToDouble(getStringSetting("buy.price"), this));

    }

    /**
     * Get the resell price of this region.
     * 
     * @return The resell price if isInResellingMode(), otherwise 0.0
     */
    public double getResellPrice() {

        return Math.max(0, config.getDouble("buy.resellPrice"));

    }

    /**
     * Get the formatted string of the price (includes prefix and suffix).
     * 
     * @return The formatted string of the price
     */
    public String getFormattedPrice() {

        return Utils.formatCurrency(getPrice());

    }

    /**
     * Get the formatted string of the resellprice (includes prefix and suffix).
     * 
     * @return The formatted string of the resellprice
     */
    public String getFormattedResellPrice() {

        return Utils.formatCurrency(getResellPrice());

    }

    /**
     * Change the price of the region.
     * 
     * @param price The price to set this region to
     */
    public void setPrice(Double price) {

        setSetting("buy.price", price);

    }

    /**
     * Set the region into resell mode with the given price.
     * 
     * @param price The price this region should be put up for sale
     */
    public void enableReselling(double price) {

        setSetting("buy.resellMode", true);
        setSetting("buy.resellPrice", price);

    }

    /**
     * Stop this region from being in resell mode.
     */
    public void disableReselling() {

        setSetting("buy.resellMode", null);
        setSetting("buy.resellPrice", null);

    }

    /**
     * Get the moneyBack percentage.
     * 
     * @return The % of money the player will get back when selling
     */
    public double getMoneyBackPercentage() {

        return Utils.evaluateToDouble(getStringSetting("buy.moneyBack"), this);

    }

    /**
     * Get the amount of money that should be paid to the player when selling the
     * region.
     * 
     * @return The amount of money the player should get back
     */
    public double getMoneyBackAmount() {

        return getPrice() * (getMoneyBackPercentage() / 100.0);

    }

    /**
     * Get the amount of money that should be paid to the player when selling the
     * region, in DiamondBank-OG shards. The percentage is applied in shard space so
     * the paid amount is exact.
     *
     * @return The amount of shards the player should get back
     */
    public long getMoneyBackShards() {

        return Math.max(0,
                Math.round(Utils.diamondsToShards(economy, getPrice()) * (getMoneyBackPercentage() / 100.0)));

    }

    /**
     * Get the formatted string of the amount of the moneyBack amount.
     * 
     * @return String with currency symbols and proper fractional part
     */
    public String getFormattedMoneyBackAmount() {

        return Utils.formatCurrencyShards(getMoneyBackShards());

    }

    @Override
    public Object provideReplacement(String variable) {

        return switch (variable) {

            // Color code carrying values are wrapped as Message so they are not escaped on
            // insert
            case AreaShop.tagPrice -> Message.fromString(getFormattedPrice());
            case AreaShop.tagRawPrice -> getPrice();
            case AreaShop.tagPlayerName -> getPlayerName();
            case AreaShop.tagPlayerColor -> Message.fromString(plugin.getPlayerPrefixColors(getBuyer()));
            case AreaShop.tagPlayerUUID -> getBuyer();
            case AreaShop.tagResellPrice -> Message.fromString(getFormattedResellPrice());
            case AreaShop.tagRawResellPrice -> getResellPrice();
            case AreaShop.tagMoneyBackAmount -> Message.fromString(getFormattedMoneyBackAmount());
            case AreaShop.tagRawMoneyBackAmount -> getMoneyBackAmount();
            case AreaShop.tagMoneyBackPercentage ->
                getMoneyBackPercentage() % 1.0 == 0.0 ? (int) getMoneyBackPercentage() : getMoneyBackPercentage();
            case AreaShop.tagMaxInactiveTime -> this.getFormattedInactiveTimeUntilSell();
            default -> super.provideReplacement(variable);

        };

    }

    /**
     * Minutes until automatic unrent when player is offline.
     * 
     * @return The number of milliseconds until the region is unrented while player
     *         is offline
     */
    public long getInactiveTimeUntilSell() {

        return Utils.getDurationFromMinutesOrStringInput(getStringSetting("buy.inactiveTimeUntilSell"));

    }

    /**
     * Get a human readable string indicating how long the player can be offline
     * until automatic unrent.
     * 
     * @return String indicating the inactive time until unrent
     */
    public String getFormattedInactiveTimeUntilSell() {

        return Utils.millisToHumanFormat(getInactiveTimeUntilSell());

    }

    /**
     * Buy a region.
     * 
     * @param offlinePlayer The player that wants to buy the region
     * @return true if it succeeded and false if not
     */
    @SuppressWarnings("deprecation")
    public boolean buy(OfflinePlayer offlinePlayer) {

        // Check if the player has permission
        if (!plugin.hasPermission(offlinePlayer, "areashop.buy")) {

            message(offlinePlayer, "buy-noPermission");
            return false;

        }

        if (economy == null) {

            message(offlinePlayer, "general-noEconomy");
            return false;

        }

        if (isEconomyTransactionInProgress()) {

            message(offlinePlayer, "general-transactionInProgress");
            return false;

        }

        if (isInResellingMode()) {

            if (!plugin.hasPermission(offlinePlayer, "areashop.buyresell")) {

                message(offlinePlayer, "buy-noPermissionResell");
                return false;

            }

        } else {

            if (!plugin.hasPermission(offlinePlayer, "areashop.buynormal")) {

                message(offlinePlayer, "buy-noPermissionNoResell");
                return false;

            }

        }

        if (getWorld() == null) {

            message(offlinePlayer, "general-noWorld");
            return false;

        }

        if (getRegion() == null) {

            message(offlinePlayer, "general-noRegion");
            return false;

        }

        if (isSold() && !(isInResellingMode() && !isBuyer(offlinePlayer))) {

            if (isBuyer(offlinePlayer)) {

                message(offlinePlayer, "buy-yours");

            } else {

                message(offlinePlayer, "buy-someoneElse");

            }

            return false;

        }

        boolean isResell = isInResellingMode();

        // Only relevant if the player is online
        Player player = offlinePlayer.getPlayer();
        if (player != null) {

            // Check if the players needs to be in the region for buying
            if (restrictedToRegion() && (!player.getWorld().getName().equals(getWorldName())
                    || !getRegion().contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                            player.getLocation().getBlockZ())))
            {

                message(offlinePlayer, "buy-restrictedToRegion");
                return false;

            }

            // Check if the players needs to be in the world for buying
            if (restrictedToWorld() && !player.getWorld().getName().equals(getWorldName())) {

                message(offlinePlayer, "buy-restrictedToWorld", player.getWorld().getName());
                return false;

            }

        }

        // Check region limits
        LimitResult limitResult = this.limitsAllow(RegionType.BUY, offlinePlayer);
        AreaShop.debug("LimitResult: " + limitResult.toString());
        if (!limitResult.actionAllowed()) {

            if (limitResult.getLimitingFactor() == LimitType.TOTAL) {

                message(offlinePlayer, "total-maximum", limitResult.getMaximum(), limitResult.getCurrent(),
                        limitResult.getLimitingGroup());
                return false;

            }

            if (limitResult.getLimitingFactor() == LimitType.BUYS) {

                message(offlinePlayer, "buy-maximum", limitResult.getMaximum(), limitResult.getCurrent(),
                        limitResult.getLimitingGroup());
                return false;

            }

            // Should not be reached, but is safe like this
            return false;

        }

        // Buying and reselling are free during the jubilee, otherwise the price is
        // charged
        boolean jubilee = plugin.isJubilee();
        UUID oldOwner = getBuyer();
        if (isResell && oldOwner != null) {

            // Broadcast and check event
            ResellingRegionEvent event = new ResellingRegionEvent(this, offlinePlayer);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {

                message(offlinePlayer, "general-cancelled", event.getReason());
                return false;

            }

            double resellPrice = getResellPrice();
            long resellPriceShards = Utils.diamondsToShards(economy, resellPrice);
            OfflinePlayer oldOwnerPlayer = Bukkit.getOfflinePlayer(oldOwner);
            String oldOwnerName = getPlayerName();
            if (oldOwnerPlayer != null && oldOwnerPlayer.getName() != null) {

                oldOwnerName = oldOwnerPlayer.getName();

            }

            // Pay the old owner through DiamondBank-OG, free during the jubilee
            if (jubilee || resellPriceShards <= 0) {

                completeResell(offlinePlayer, oldOwner, oldOwnerName, resellPrice);
                return true;

            }

            Player payingPlayer = offlinePlayer.getPlayer();
            if (payingPlayer == null) {

                message(offlinePlayer, "buy-payError");
                return false;

            }

            // Pay on an async thread: the DiamondBank-OG API blocks and may not be
            // called on the main thread (see AreaShop#runEconomyTask)
            if (!beginEconomyTransaction()) {

                message(offlinePlayer, "general-transactionInProgress");
                return false;

            }

            final String finalOldOwnerName = oldOwnerName;
            plugin.runEconomyTask(() -> {

                try {

                    economy.playerPayPlayer(payingPlayer.getUniqueId(), oldOwner, resellPriceShards,
                            "AreaShop resell: " + getName(), null);
                    return ChargeResult.ofSuccess();

                } catch (DiamondBankException.InsufficientFundsException e) {

                    return ChargeResult.ofLowMoney(getBalanceString(payingPlayer));

                } catch (DiamondBankException e) {

                    return ChargeResult.ofError();

                }

            }, result -> {

                endEconomyTransaction();
                if (result.lowMoney()) {

                    message(offlinePlayer, "buy-lowMoneyResell", result.balance());
                    return;

                }

                if (!result.success()) {

                    message(offlinePlayer, "buy-payError");
                    return;

                }

                // The payment succeeded but the region may have changed while it
                // ran; in that case reverse the payment instead of granting the
                // region
                if (isDeleted() || !isInResellingMode() || !oldOwner.equals(getBuyer())) {

                    reversePayment(payingPlayer.getUniqueId(), oldOwner, resellPriceShards,
                            "AreaShop resell reversal: " + getName());
                    message(offlinePlayer, "buy-payError");
                    return;

                }

                completeResell(offlinePlayer, oldOwner, finalOldOwnerName, resellPrice);

            });

            return true;

        } else {

            // Broadcast and check event
            BuyingRegionEvent event = new BuyingRegionEvent(this, offlinePlayer);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {

                message(offlinePlayer, "general-cancelled", event.getReason());
                return false;

            }

            // Charge the price through DiamondBank-OG, free during the jubilee
            long priceShards = Utils.diamondsToShards(economy, getPrice());
            if (jubilee || priceShards <= 0) {

                completeBuy(offlinePlayer);
                return true;

            }

            Player payingPlayer = offlinePlayer.getPlayer();
            if (payingPlayer == null) {

                message(offlinePlayer, "buy-payError");
                return false;

            }

            // Charge on an async thread: the DiamondBank-OG API blocks and may not
            // be called on the main thread (see AreaShop#runEconomyTask)
            if (!beginEconomyTransaction()) {

                message(offlinePlayer, "general-transactionInProgress");
                return false;

            }

            UUID landlord = getLandlord();
            plugin.runEconomyTask(() -> {

                try {

                    economy.consumeFromPlayer(payingPlayer.getUniqueId(), priceShards, "AreaShop buy: " + getName(),
                            null);

                } catch (DiamondBankException.InsufficientFundsException e) {

                    return ChargeResult.ofLowMoney(getBalanceString(payingPlayer));

                } catch (DiamondBankException e) {

                    return ChargeResult.ofError();

                }

                // Pay the landlord if there is one, otherwise the diamonds stay consumed.
                // If the landlord payout fails the buyer is refunded and the buy is cancelled.
                if (landlord != null) {

                    try {

                        economy.addToPlayerBankShards(landlord, priceShards, "AreaShop buy: " + getName(), null);

                    } catch (DiamondBankException e) {

                        try {

                            economy.addToPlayerBankShards(payingPlayer.getUniqueId(), priceShards,
                                    "AreaShop buy refund: " + getName(), null);

                        } catch (DiamondBankException refundError) {

                            AreaShop.warn("Could not refund " + Utils.shardsToDisplay(economy, priceShards)
                                    + " Diamonds to " + payingPlayer.getName() + " after a failed landlord payout for "
                                    + getName());

                        }

                        return ChargeResult.ofError();

                    }

                }

                return ChargeResult.ofSuccess();

            }, result -> {

                endEconomyTransaction();
                if (result.lowMoney()) {

                    message(offlinePlayer, "buy-lowMoney", result.balance());
                    return;

                }

                if (!result.success()) {

                    message(offlinePlayer, "buy-payError");
                    return;

                }

                // The charge succeeded but the region may have changed while it
                // ran; in that case refund the payment instead of granting the buy
                if (isDeleted() || isSold()) {

                    refundShards(payingPlayer.getUniqueId(), priceShards, "AreaShop buy refund: " + getName());
                    message(offlinePlayer, "buy-payError");
                    return;

                }

                completeBuy(offlinePlayer);

            });

            return true;

        }

    }

    /**
     * Apply a successful (or free) resell to the region: transfer the owner, fire
     * events and send messages. Runs on the main thread after the payment to the
     * old owner went through.
     *
     * @param offlinePlayer The player that bought the region
     * @param oldOwner      The previous owner that got paid
     * @param oldOwnerName  Display name of the previous owner
     * @param resellPrice   The resell price, for the seller message
     */
    private void completeResell(OfflinePlayer offlinePlayer, UUID oldOwner, String oldOwnerName, double resellPrice) {

        getFriendsFeature().clearFriends();

        // Set the owner
        setBuyer(offlinePlayer.getUniqueId());
        updateLastActiveTime();

        // Update everything
        handleSchematicEvent(RegionEvent.RESELL);

        // Notify about updates
        this.notifyAndUpdate(new ResoldRegionEvent(this, oldOwner));

        // Resell is done, disable that now
        disableReselling();

        // Send message to the player
        message(offlinePlayer, "buy-successResale", oldOwnerName);
        Player seller = Bukkit.getPlayer(oldOwner);
        if (seller != null) {

            message(seller, "buy-successSeller", Utils.formatCurrency(resellPrice));

        }

        // Announce the takeover to the configured worlds
        broadcast(plugin.isJubilee() ? "broadcast-resoldJubilee" : "broadcast-resold", oldOwnerName,
                Utils.formatCurrency(resellPrice));

        celebrateNewOwner(offlinePlayer.getPlayer());

    }

    /**
     * Apply a successful (or free) buy to the region: set the owner, fire events
     * and send messages. Runs on the main thread after the charge went through.
     *
     * @param offlinePlayer The player that bought the region
     */
    private void completeBuy(OfflinePlayer offlinePlayer) {

        // Set the owner
        setBuyer(offlinePlayer.getUniqueId());
        updateLastActiveTime();

        // Send message to the player
        message(offlinePlayer, "buy-succes");

        // Update everything
        handleSchematicEvent(RegionEvent.BOUGHT);

        // Notify about updates
        this.notifyAndUpdate(new BoughtRegionEvent(this));

        // Announce the new owner to the configured worlds
        broadcast(plugin.isJubilee() ? "broadcast-boughtJubilee" : "broadcast-bought");

        celebrateNewOwner(offlinePlayer.getPlayer());

    }

    /**
     * Sell a buyed region, get part of the money back.
     * 
     * @param giveMoneyBack true if the player should be given money back, otherwise
     *                      false
     * @param executor      CommandSender to receive a message when the sell fails,
     *                      or null
     * @return true if the region has been sold, otherwise false
     */
    @SuppressWarnings("deprecation")
    public boolean sell(boolean giveMoneyBack, CommandSender executor) {

        boolean own = executor instanceof Player player && this.isBuyer(player);
        if (executor != null) {

            if (!executor.hasPermission("areashop.sell") && !own) {

                message(executor, "sell-noPermissionOther");
                return false;

            }

            if (!executor.hasPermission("areashop.sell") && !executor.hasPermission("areashop.sellown") && own) {

                message(executor, "sell-noPermission");
                return false;

            }

            if (!executor.hasPermission("areashop.sell") && executor.hasPermission("areashop.sellown") && own
                    && getBooleanSetting("buy.sellDisabled"))
            {

                message(executor, "sell-disabled");
                return false;

            }

        }

        if (economy == null) {

            return false;

        }

        if (isEconomyTransactionInProgress()) {

            message(executor, "general-transactionInProgress");
            return false;

        }

        // Broadcast and check event
        SellingRegionEvent event = new SellingRegionEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {

            message(executor, "general-cancelled", event.getReason());
            return false;

        }

        // Pay back (part of) the price, jubilee mode pays nothing back.
        // If the payback fails the sell is cancelled completely.
        double moneyBack = getMoneyBackAmount();
        long moneyBackShards = giveMoneyBack && !plugin.isJubilee() ? getMoneyBackShards() : 0;
        UUID payBackTo = getBuyer();
        if (moneyBackShards <= 0 || payBackTo == null) {

            finishSell(executor, moneyBack);
            return true;

        }

        // A region being deleted must finish selling synchronously, so the refund
        // becomes fire-and-forget instead of cancelling the sell
        if (isDeleted()) {

            refundShards(payBackTo, moneyBackShards, "AreaShop sell: " + getName());
            finishSell(executor, moneyBack);
            return true;

        }

        // Pay back on an async thread: the DiamondBank-OG API blocks and may not
        // be called on the main thread (see AreaShop#runEconomyTask)
        if (!beginEconomyTransaction()) {

            message(executor, "general-transactionInProgress");
            return false;

        }

        plugin.runEconomyTask(() -> {

            try {

                economy.addToPlayerBankShards(payBackTo, moneyBackShards, "AreaShop sell: " + getName(), null);
                return true;

            } catch (DiamondBankException e) {

                return false;

            }

        }, success -> {

            endEconomyTransaction();
            if (!success) {

                AreaShop.warn("Could not pay back " + Utils.shardsToDisplay(economy, moneyBackShards) + " Diamonds to "
                        + getPlayerName() + " for selling " + getName() + ", sell cancelled");
                message(executor, "sell-payError");
                return;

            }

            if (isSold()) {

                finishSell(executor, moneyBack);

            }

        });

        return true;

    }

    /**
     * Apply the sell to the region: fire events, send messages and clear the owner.
     * Runs on the main thread after any payback has been deposited.
     *
     * @param executor  The CommandSender that gets the result message, or null
     * @param moneyBack The amount of money paid back, for the event and messages
     */
    private void finishSell(CommandSender executor, double moneyBack) {

        disableReselling();

        // Handle schematic save/restore (while %uuid% is still available)
        handleSchematicEvent(RegionEvent.SOLD);

        // Send message: before actual removal of the buyer so that it is still
        // available for variables
        message(executor, "sell-sold");

        // Remove friends and the owner
        getFriendsFeature().clearFriends();
        UUID oldBuyer = getBuyer();
        setBuyer(null);
        removeLastActiveTime();

        // Notify about updates
        this.notifyAndUpdate(new SoldRegionEvent(this, oldBuyer, Math.max(moneyBack, 0)));

    }

    /**
     * Deposit shards into a player's bank asynchronously, logging when it fails.
     * Used for refunds that must not block or cancel the surrounding action.
     *
     * @param target The player to deposit to
     * @param shards The amount of shards to deposit
     * @param reason The transaction reason for the DiamondBank-OG log
     */
    private void refundShards(UUID target, long shards, String reason) {

        plugin.runEconomyTask(() -> {

            try {

                economy.addToPlayerBankShards(target, shards, reason, null);
                return true;

            } catch (DiamondBankException e) {

                return false;

            }

        }, success -> {

            if (!success) {

                AreaShop.warn("Could not deposit " + Utils.shardsToDisplay(economy, shards) + " Diamonds to " + target
                        + " (" + reason + "), staff should compensate manually.");

            }

        });

    }

    /**
     * Best-effort reversal of a resell payment for when the region changed while
     * the payment ran: take the amount back from the receiver's bank and return it
     * to the payer's bank. Logs for staff when either half fails.
     *
     * @param payer    The player that paid and should get the amount back
     * @param receiver The player that received the payment
     * @param shards   The amount of shards that was paid
     * @param reason   The transaction reason for the DiamondBank-OG log
     */
    private void reversePayment(UUID payer, UUID receiver, long shards, String reason) {

        plugin.runEconomyTask(() -> {

            try {

                economy.subtractFromPlayerBankShards(receiver, shards, reason, null);

            } catch (DiamondBankException e) {

                return false;

            }

            try {

                economy.addToPlayerBankShards(payer, shards, reason, null);

            } catch (DiamondBankException e) {

                return false;

            }

            return true;

        }, success -> {

            if (!success) {

                AreaShop.warn(
                        "Could not reverse a payment of " + Utils.shardsToDisplay(economy, shards) + " Diamonds from "
                                + payer + " to " + receiver + " (" + reason + "), staff should compensate manually.");

            }

        });

    }

    private String getBalanceString(Player player) {

        try {

            return Utils.shardsToDisplay(economy, economy.getTotalShards(player.getUniqueId()));

        } catch (DiamondBankException e) {

            return Utils.shardsToDisplay(economy, 0);

        }

    }

    @Override
    public boolean checkInactive() {

        if (isDeleted() || !isSold()) {

            return false;

        }

        long inactiveSetting = getInactiveTimeUntilSell();
        OfflinePlayer player = Bukkit.getOfflinePlayer(getBuyer());
        if (inactiveSetting <= 0 || player.isOp()) {

            return false;

        }

        long lastPlayed = getLastActiveTime();
        // AreaShop.debug("currentTime=" + Calendar.getInstance().getTimeInMillis() + ",
        // getLastPlayed()=" + lastPlayed + ", timeInactive=" +
        // (Calendar.getInstance().getTimeInMillis()-player.getLastPlayed()) + ",
        // inactiveSetting=" + inactiveSetting);
        if (Calendar.getInstance().getTimeInMillis() > (lastPlayed + inactiveSetting)) {

            AreaShop.info("Region " + getName() + " unrented because of inactivity for player " + getPlayerName());
            AreaShop.debug("currentTime=" + Calendar.getInstance().getTimeInMillis() + ", getLastPlayed()=" + lastPlayed
                    + ", timeInactive=" + (Calendar.getInstance().getTimeInMillis() - player.getLastPlayed())
                    + ", inactiveSetting=" + inactiveSetting);
            return this.sell(true, null);

        }

        return false;

    }

}
