package me.wiefferink.areashop.regions;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.events.ask.RentingRegionEvent;
import me.wiefferink.areashop.events.ask.UnrentingRegionEvent;
import me.wiefferink.areashop.events.notify.RentedRegionEvent;
import me.wiefferink.areashop.events.notify.UnrentedRegionEvent;
import me.wiefferink.areashop.features.signs.SignsFeature;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static me.wiefferink.areashop.tools.Utils.millisToHumanFormat;

public class RentRegion extends GeneralRegion {

    private long warningsDoneUntil = System.currentTimeMillis();
    private final DiamondBankAPIJava economy;

    /**
     * Constructor.
     * 
     * @param config All settings of this region
     */
    @AssistedInject
    RentRegion(@Nonnull AreaShop plugin, @Nonnull FeatureManager featureManager,
            @Nonnull WorldEditInterface worldEditInterface, @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull MessageBridge messageBridge, @Nullable DiamondBankAPIJava economy,
            @Assisted @Nonnull YamlConfiguration config)
    {

        super(plugin, featureManager, worldEditInterface, worldGuardInterface, messageBridge, config);
        this.economy = economy;

    }

    /**
     * Create a new RentRegion.
     * 
     * @param name  The name of the region (correct casing)
     * @param world The world of the WorldGuard region
     */
    @AssistedInject
    RentRegion(@Nonnull AreaShop plugin, @Nonnull FeatureManager featureManager,
            @Nonnull WorldEditInterface worldEditInterface, @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull MessageBridge messageBridge, @Nullable DiamondBankAPIJava economy, @Assisted @Nonnull String name,
            @Assisted @Nonnull World world)
    {

        super(plugin, featureManager, worldEditInterface, worldGuardInterface, messageBridge, name, world);
        this.economy = economy;

    }

    @Override
    public boolean needsPeriodicUpdate() {

        if (super.needsPeriodicUpdate()) {

            return true;

        }

        return SignsFeature.exists(this) && getSignsFeature().signManager().needsPeriodicUpdate();

    }

    @Override
    public boolean isOwner(UUID player) {

        return isRenter(player);

    }

    @Override
    public UUID getOwner() {

        return getRenter();

    }

    @Override
    public void setOwner(UUID player) {

        setRenter(player);

    }

    @Override
    public RegionType getType() {

        return RegionType.RENT;

    }

    @Override
    public RegionState getState() {

        if (isRented()) {

            return RegionState.RENTED;

        } else {

            return RegionState.FORRENT;

        }

    }

    @Override
    public boolean isAvailable() {

        return !isRented();

    }

    /**
     * Get the UUID of the player renting the region.
     * 
     * @return The UUID of the renter
     */
    public UUID getRenter() {

        String renter = config.getString("rent.renter");
        if (renter != null) {

            try {

                return UUID.fromString(renter);

            } catch (IllegalArgumentException e) {

                // Incorrect UUID
            }

        }

        return null;

    }

    /**
     * Check if a player is the renter of this region.
     * 
     * @param player Player to check
     * @return true if this player rents this region, otherwise false
     */
    public boolean isRenter(Player player) {

        return player != null && isRenter(player.getUniqueId());

    }

    public boolean isRenter(UUID player) {

        UUID renter = getRenter();
        return !(player == null || renter == null) && renter.equals(player);

    }

    /**
     * Set the renter of this region.
     * 
     * @param renter The UUID of the player that should be set as the renter
     */
    public void setRenter(UUID renter) {

        if (renter == null) {

            setSetting("rent.renter", null);
            setSetting("rent.renterName", null);

        } else {

            setSetting("rent.renter", renter.toString());
            setSetting("rent.renterName", Utils.toName(renter));

        }

    }

    /**
     * Get the max number of extends of this region.
     * 
     * @return -1 if infinite otherwise the maximum number
     */
    public int getMaxExtends() {

        return getIntegerSetting("rent.maxExtends");

    }

    /**
     * Get how many times the rent has already been extended.
     * 
     * @return The number of times extended
     */
    public int getTimesExtended() {

        return config.getInt("rent.timesExtended");

    }

    /**
     * Set the number of times the region has been extended.
     * 
     * @param times The number of times the region has been extended
     */
    public void setTimesExtended(int times) {

        if (times < 0) {

            setSetting("rent.timesExtended", null);

        } else {

            setSetting("rent.timesExtended", times);

        }

    }

    @Override
    public Object provideReplacement(String variable) {

        return switch (variable) {

            // Color code carrying values are wrapped as Message so they are not escaped on
            // insert
            case AreaShop.tagPrice -> Message.fromString(getFormattedPrice());
            case AreaShop.tagRawPrice -> getPrice();
            case AreaShop.tagDuration -> getDurationString();
            case AreaShop.tagDurationShort -> Utils.millisToCompactFormat(getDuration());
            case AreaShop.tagPlayerName -> getPlayerName();
            case AreaShop.tagPlayerColor -> Message.fromString(plugin.getPlayerPrefixColors(getRenter()));
            case AreaShop.tagPlayerUUID -> getRenter();
            case AreaShop.tagRentedUntil ->
                new SimpleDateFormat(plugin.getConfig().getString("timeFormatChat")).format(new Date(getRentedUntil()));
            case AreaShop.tagRentedUntilShort ->
                new SimpleDateFormat(plugin.getConfig().getString("timeFormatSign")).format(new Date(getRentedUntil()));
            case AreaShop.tagTimeLeft -> getTimeLeftString();
            case AreaShop.tagMoneyBackAmount -> Message.fromString(getFormattedMoneyBackAmount());
            case AreaShop.tagRawMoneyBackAmount -> getMoneyBackAmount();
            case AreaShop.tagMoneyBackPercentage ->
                (getMoneyBackPercentage() % 1.0) == 0.0 ? (int) getMoneyBackPercentage() : getMoneyBackPercentage();
            case AreaShop.tagTimesExtended -> this.getTimesExtended();
            case AreaShop.tagMaxExtends -> this.getMaxExtends();
            case AreaShop.tagExtendsLeft -> getMaxExtends() - getTimesExtended();
            case AreaShop.tagMaxRentTime -> millisToHumanFormat(getMaxRentTime());
            // Note for the shop entry greeting: during the jubilee one payment rents the
            // shop for the maximum rent time, so tell the player up front
            case AreaShop.tagJubilee -> {

                if (!plugin.isJubilee()) {

                    yield "";

                }

                long maxRentTime = getMaxRentTime();
                if (maxRentTime == -1) {

                    yield Message.fromString(" &6(jubilee: extending is free)");

                }

                yield Message.fromString(
                        " &6(jubilee: one payment rents it for &2" + millisToHumanFormat(maxRentTime) + "&6)");

            }
            case AreaShop.tagMaxInactiveTime -> this.getFormattedInactiveTimeUntilUnrent();
            default -> super.provideReplacement(variable);

        };

    }

    /**
     * Check if the region is rented.
     * 
     * @return true if the region is rented, otherwise false
     */
    public boolean isRented() {

        return getRenter() != null;

    }

    /**
     * Get the name of the player renting this region.
     * 
     * @return Name of the player renting this region, if unavailable by UUID it
     *         will return the old cached name, if that is unavailable it will
     *         return &lt;UNKNOWN&gt;
     */
    public String getPlayerName() {

        String result = Utils.toName(getRenter());
        if (result == null || result.isEmpty()) {

            result = config.getString("rent.renterName");
            if (result == null || result.isEmpty()) {

                result = "<UNKNOWN>";

            }

        }

        return result;

    }

    /**
     * Get the time until this region is rented (time from 1970 epoch).
     * 
     * @return The epoch time until which this region is rented
     */
    public long getRentedUntil() {

        return getLongSetting("rent.rentedUntil");

    }

    /**
     * Set the time until the region is rented (milliseconds from 1970, system
     * time).
     * 
     * @param rentedUntil The time until the region is rented
     */
    public void setRentedUntil(Long rentedUntil) {

        if (rentedUntil == null) {

            setSetting("rent.rentedUntil", null);

        } else {

            setSetting("rent.rentedUntil", rentedUntil);

        }

    }

    /**
     * Get the price of the region.
     * 
     * @return The price of the region
     */
    public double getPrice() {

        return Math.max(0, Utils.evaluateToDouble(getStringSetting("rent.price"), this));

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
     * Get the duration of 1 rent period.
     * 
     * @return The duration in milliseconds of 1 rent period
     */
    public long getDuration() {

        return Utils.durationStringToLong(getDurationString());

    }

    /**
     * Get the duration string, includes 'number indentifier'.
     * 
     * @return The duration string
     */
    public String getDurationString() {

        return getStringSetting("rent.duration");

    }

    /**
     * Get the time that is left on the region.
     * 
     * @return The time left on the region
     */
    public long getTimeLeft() {

        if (isRented()) {

            return this.getRentedUntil() - Calendar.getInstance().getTimeInMillis();

        } else {

            return 0;

        }

    }

    /**
     * Get a formatted string indicating the rent time that is left.
     * 
     * @return Time left on the rent, for example '29 days', '3 months', '1 second'
     */
    public String getTimeLeftString() {

        return Utils.millisToHumanFormat(getTimeLeft());

    }

    /**
     * Minutes until automatic unrent when player is offline.
     * 
     * @return The number of milliseconds until the region is unrented while player
     *         is offline
     */
    public long getInactiveTimeUntilUnrent() {

        return Utils.getDurationFromMinutesOrStringInput(getStringSetting("rent.inactiveTimeUntilUnrent"));

    }

    /**
     * Get a human readable string indicating how long the player can be offline
     * until automatic unrent.
     * 
     * @return String indicating the inactive time until unrent
     */
    public String getFormattedInactiveTimeUntilUnrent() {

        return Utils.millisToHumanFormat(getInactiveTimeUntilUnrent());

    }

    /**
     * Change the price of the region.
     * 
     * @param price The price of the region
     */
    public void setPrice(Double price) {

        setSetting("rent.price", price);

    }

    /**
     * Set the duration of the rent.
     * 
     * @param duration The duration of the rent (as specified on the documentation
     *                 pages)
     */
    public void setDuration(String duration) {

        setSetting("rent.duration", duration);

    }

    /**
     * Get the moneyBack percentage.
     * 
     * @return The % of money the player will get back when unrenting
     */
    public double getMoneyBackPercentage() {

        return Utils.evaluateToDouble(getStringSetting("rent.moneyBack"), this);

    }

    /**
     * Get the amount of money that should be paid to the player when unrenting the
     * region.
     * 
     * @return The amount of money the player should get back
     */
    public double getMoneyBackAmount() {

        long currentTime = Calendar.getInstance().getTimeInMillis();
        Double timeLeft = (double) (getRentedUntil() - currentTime);
        double percentage = (getMoneyBackPercentage()) / 100.0;
        Double timePeriod = (double) (getDuration());
        double periods = timeLeft / timePeriod;
        return Math.max(0, periods * getPrice() * percentage);

    }

    /**
     * Get the amount of money that should be paid to the player when unrenting the
     * region, in DiamondBank-OG shards. The percentage and proration are applied in
     * shard space so the paid amount is exact.
     *
     * @return The amount of shards the player should get back
     */
    public long getMoneyBackShards() {

        long currentTime = Calendar.getInstance().getTimeInMillis();
        double timeLeft = getRentedUntil() - currentTime;
        double percentage = getMoneyBackPercentage() / 100.0;
        double periods = timeLeft / getDuration();
        return Math.max(0, Math.round(periods * Utils.diamondsToShards(economy, getPrice()) * percentage));

    }

    /**
     * Get the formatted string of the amount of the moneyBack amount.
     * 
     * @return String with currency symbols and proper fractional part
     */
    public String getFormattedMoneyBackAmount() {

        return Utils.formatCurrencyShards(getMoneyBackShards());

    }

    /**
     * Get the maximum time the player can rent the region in advance
     * (milliseconds).
     * 
     * @return The maximum rent time in milliseconds
     */
    public long getMaxRentTime() {

        return Utils.getDurationFromMinutesOrStringInput(getStringSetting("rent.maxRentTime"));

    }

    /**
     * Check if the rent should expire.
     * 
     * @return true if the rent has expired and has been unrented, false otherwise
     */
    public boolean checkExpiration() {

        long now = Calendar.getInstance().getTimeInMillis();
        if (!isDeleted() && isRented() && now > getRentedUntil()) {

            // Extend rent if configured for that
            if (getBooleanSetting("rent.autoExtend") && extend()) {

                return false;

            }

            // Send message to the player if online
            Player player = Bukkit.getPlayer(getRenter());
            if (unRent(false, null)) {

                if (player != null) {

                    message(player, "unrent-expired");

                }

                return true;

            }

        }

        return false;

    }

    /**
     * Send the expiration warnings from the selected profile which is specified in
     * the config. Sends all warnings since previous call until (now + normal
     * delay), delay can be found in the config as well.
     */
    public void sendExpirationWarnings() {

        // Send from warningsDoneUntil to current+delay
        if (isDeleted() || !isRented()) {

            return;

        }

        ConfigurationSection profileSection = getConfigurationSectionSetting("rent.expirationWarningProfile",
                "expirationWarningProfiles");
        if (profileSection == null) {

            return;

        }

        // Check if a warning needs to be send for each defined point in time
        Player player = Bukkit.getPlayer(getRenter());
        long sendUntil = Calendar.getInstance().getTimeInMillis()
                + (plugin.getConfig().getInt("expireWarning.delay") * 60 * 1000);
        for (String timeBefore : profileSection.getKeys(false)) {

            long timeBeforeParsed = Utils.durationStringToLong(timeBefore);
            if (timeBeforeParsed <= 0) {

                return;

            }

            long checkTime = getRentedUntil() - timeBeforeParsed;

            if (checkTime > warningsDoneUntil && checkTime <= sendUntil) {

                List<String> commands;
                if (profileSection.isConfigurationSection(timeBefore)) {

                    /*
                     * Legacy config layout: "1 minute": warnPlayer: true commands: ["say hi"]
                     */
                    commands = profileSection.getStringList(timeBefore + ".commands");
                    // Warn player
                    if (profileSection.getBoolean(timeBefore + ".warnPlayer") && player != null) {

                        message(player, "rent-expireWarning");

                    }

                } else {

                    commands = profileSection.getStringList(timeBefore);

                }

                this.runCommands(Bukkit.getConsoleSender(), commands);

            }

        }

        warningsDoneUntil = sendUntil;

    }

    /**
     * Try to extend the rent for the current owner, respecting all restrictions.
     * 
     * @return true if successful, otherwise false
     */
    public boolean extend() {

        if (!isRented()) {

            return false;

        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(getRenter());
        return rent(offlinePlayer);

    }

    /**
     * Rent a region.
     *
     * @param offlinePlayer The player that wants to rent the region
     * @return true if it succeeded and false if not
     */
    public boolean rent(OfflinePlayer offlinePlayer) {

        return rent(offlinePlayer, null);

    }

    /**
     * Rent a region, optionally charged to another player.
     *
     * @param offlinePlayer The player the rent is for
     * @param payer         The online player paying the Diamonds, or null to charge
     *                      the renter themselves
     * @return true if it succeeded and false if not
     */
    public boolean rent(OfflinePlayer offlinePlayer, Player payer) {

        if (economy == null) {

            message(offlinePlayer, "general-noEconomy");
            return false;

        }

        if (isEconomyTransactionInProgress()) {

            message(offlinePlayer, "general-transactionInProgress");
            return false;

        }

        // Check if the player has permission
        if (!plugin.hasPermission(offlinePlayer, "areashop.rent")) {

            message(offlinePlayer, "rent-noPermission");
            return false;

        }

        // Check location restrictions
        if (getWorld() == null) {

            message(offlinePlayer, "general-noWorld");
            return false;

        }

        if (getRegion() == null) {

            message(offlinePlayer, "general-noRegion");
            return false;

        }

        boolean extend = false;
        if (getRenter() != null && offlinePlayer.getUniqueId().equals(getRenter())) {

            extend = true;

        }

        // Check if available or extending
        if (isRented() && !extend) {

            message(offlinePlayer, "rent-someoneElse");
            return false;

        }

        // These checks are only relevant for online players doing the renting/buying
        // themselves
        Player player = offlinePlayer.getPlayer();
        if (player != null) {

            // Check if the players needs to be in the region for renting
            if (restrictedToRegion() && (!player.getWorld().getName().equals(getWorldName())
                    || !getRegion().contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                            player.getLocation().getBlockZ())))
            {

                message(offlinePlayer, "rent-restrictedToRegion");
                return false;

            }

            // Check if the players needs to be in the world for renting
            if (restrictedToWorld() && !player.getWorld().getName().equals(getWorldName())) {

                message(offlinePlayer, "rent-restrictedToWorld", player.getWorld().getName());
                return false;

            }

        }

        // Check region limits if this is not extending
        if (!(extend && config.getBoolean("allowRegionExtendsWhenAboveLimits"))) {

            LimitResult limitResult;
            if (extend) {

                limitResult = this.limitsAllow(RegionType.RENT, offlinePlayer, true);

            } else {

                limitResult = this.limitsAllow(RegionType.RENT, offlinePlayer);

            }

            AreaShop.debug("LimitResult: " + limitResult.toString());
            if (!limitResult.actionAllowed()) {

                if (limitResult.getLimitingFactor() == LimitType.TOTAL) {

                    message(offlinePlayer, "total-maximum", limitResult.getMaximum(), limitResult.getCurrent(),
                            limitResult.getLimitingGroup());
                    return false;

                }

                if (limitResult.getLimitingFactor() == LimitType.RENTS) {

                    message(offlinePlayer, "rent-maximum", limitResult.getMaximum(), limitResult.getCurrent(),
                            limitResult.getLimitingGroup());
                    return false;

                }

                if (limitResult.getLimitingFactor() == LimitType.EXTEND) {

                    message(offlinePlayer, "rent-maximumExtend", limitResult.getMaximum(), limitResult.getCurrent() + 1,
                            limitResult.getLimitingGroup());
                    return false;

                }

                return false;

            }

        }

        // Check if the player can still extend this rent
        if (extend && !plugin.hasPermission(offlinePlayer, "areashop.rentextendbypass")) {

            if (getMaxExtends() >= 0 && getTimesExtended() >= getMaxExtends()) {

                message(offlinePlayer, "rent-maxExtends");
                return false;

            }

        }

        // Check if there is enough time left before hitting maxRentTime
        boolean extendToMax = false;
        long timeNow = Calendar.getInstance().getTimeInMillis();
        long timeRented = 0;
        long maxRentTime = getMaxRentTime();
        if (isRented()) {

            timeRented = getRentedUntil() - timeNow;

        }

        // The price is tracked in shards (the atomic DiamondBank-OG unit) so the
        // proration below stays exact
        long priceShards = Utils.diamondsToShards(economy, getPrice());
        if ((timeRented + getDuration()) > (maxRentTime)
                && !plugin.hasPermission(offlinePlayer, "areashop.renttimebypass") && maxRentTime != -1)
        {

            // Extend to the maximum instead of adding a full period
            if (getBooleanSetting("rent.extendToFullWhenAboveMaxRentTime")) {

                if (timeRented >= maxRentTime) {

                    message(offlinePlayer, "rent-alreadyAtFull");
                    return false;

                } else {

                    long toRentPart = maxRentTime - timeRented;
                    extendToMax = true;
                    priceShards = Math.round(((double) toRentPart) / getDuration() * priceShards);

                }

            } else {

                message(offlinePlayer, "rent-maxRentTime");
                return false;

            }

        }

        // During the jubilee only the first rent is charged, otherwise every rent and
        // extend costs the (possibly prorated) price. Payment is taken from the
        // paying player's physical diamonds through DiamondBank-OG, so the payer
        // must be online. Rent revenue is an intentional money sink: the consumed
        // diamonds are not paid out to anyone.
        boolean charged = priceShards > 0 && (!extend || !plugin.isJubilee());
        Player payingPlayer = payer != null ? payer : offlinePlayer.getPlayer();
        if (charged && payingPlayer == null) {

            message(offlinePlayer, "rent-payError");
            return false;

        }

        // Broadcast and check event
        RentingRegionEvent event = new RentingRegionEvent(this, offlinePlayer, extend);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {

            message(offlinePlayer, "general-cancelled", event.getReason());
            return false;

        }

        if (!charged) {

            completeRent(offlinePlayer, payer, extend, extendToMax, maxRentTime);
            return true;

        }

        // Charge on an async thread: the DiamondBank-OG API blocks and may not be
        // called on the main thread (see AreaShop#runEconomyTask). The transaction
        // guard blocks other economy actions on this region until completion.
        if (!beginEconomyTransaction()) {

            message(offlinePlayer, "general-transactionInProgress");
            return false;

        }

        final long chargedShards = priceShards;
        final boolean finalExtend = extend;
        final boolean finalExtendToMax = extendToMax;
        final Player finalPayingPlayer = payingPlayer;
        plugin.runEconomyTask(() -> {

            try {

                economy.consumeFromPlayer(finalPayingPlayer.getUniqueId(), chargedShards, "AreaShop rent: " + getName(),
                        null);
                return ChargeResult.ofSuccess();

            } catch (DiamondBankException.InsufficientFundsException e) {

                return ChargeResult.ofLowMoney(balanceString(finalPayingPlayer));

            } catch (DiamondBankException e) {

                AreaShop.debug("Something went wrong with getting money from " + finalPayingPlayer.getName()
                        + " while renting " + getName() + ": " + e.getMessage());
                return ChargeResult.ofError();

            }

        }, result -> {

            endEconomyTransaction();
            if (result.lowMoney()) {

                message(finalPayingPlayer, finalExtend ? "rent-lowMoneyExtend" : "rent-lowMoneyRent", result.balance());
                return;

            }

            if (!result.success()) {

                message(finalPayingPlayer, "rent-payError");
                return;

            }

            // The charge succeeded but the region may have changed while it ran;
            // in that case refund the payment instead of granting the rent
            if (isDeleted() || (isRented() && !offlinePlayer.getUniqueId().equals(getRenter()))) {

                refundShards(finalPayingPlayer.getUniqueId(), chargedShards, "AreaShop rent refund: " + getName());
                message(finalPayingPlayer, "rent-payError");
                return;

            }

            completeRent(offlinePlayer, payer, finalExtend, finalExtendToMax, maxRentTime);

        });

        return true;

    }

    /**
     * Apply a successful (or free) rent to the region: set the renter and time,
     * fire events and send messages. Runs on the main thread after the payment has
     * been taken.
     *
     * @param offlinePlayer The player the rent is for
     * @param payer         The player that paid, or null when the renter paid
     * @param extend        true if this is an extension of a running rent
     * @param extendToMax   true if the rent is topped up to the maximum time
     * @param maxRentTime   The maximum rent time in milliseconds
     */
    private void completeRent(OfflinePlayer offlinePlayer, Player payer, boolean extend, boolean extendToMax,
            long maxRentTime)
    {

        // Get the time until the region will be rented
        Calendar calendar = Calendar.getInstance();
        if (plugin.isJubilee() && maxRentTime != -1) {

            // Jubilee grants the maximum rent time immediately
            calendar.setTimeInMillis(calendar.getTimeInMillis() + maxRentTime);

        } else if (extendToMax) {

            calendar.setTimeInMillis(calendar.getTimeInMillis() + getMaxRentTime());

        } else if (extend) {

            calendar.setTimeInMillis(getRentedUntil() + getDuration());

        } else {

            calendar.setTimeInMillis(calendar.getTimeInMillis() + getDuration());

        }

        // Add values to the rent and send it to FileManager
        setRentedUntil(calendar.getTimeInMillis());
        setRenter(offlinePlayer.getUniqueId());
        updateLastActiveTime();

        // Fire schematic event and updated times extended
        if (!extend) {

            this.handleSchematicEvent(RegionEvent.RENTED);
            setTimesExtended(0);

        } else {

            setTimesExtended(getTimesExtended() + 1);

        }

        // Send message to the player
        if (extendToMax) {

            message(offlinePlayer, "rent-extendedToMax");

        } else if (extend) {

            message(offlinePlayer, "rent-extended");

        } else if (plugin.isJubilee()) {

            message(offlinePlayer, "rent-rentedJubilee");

        } else {

            message(offlinePlayer, "rent-rented");

        }

        // Confirm to the payer when they paid for someone else's rent
        if (payer != null && !payer.getUniqueId().equals(offlinePlayer.getUniqueId())) {

            message(payer, "payrent-paidOther");

        }

        // Notify about updates
        this.notifyAndUpdate(new RentedRegionEvent(this, extend));

    }

    /**
     * Get a player's total balance formatted for display. Only call from an async
     * thread: the DiamondBank-OG API blocks.
     *
     * @param player The player to get the balance of
     * @return The balance formatted through the DiamondBank-OG API
     */
    private String balanceString(Player player) {

        try {

            return Utils.shardsToDisplay(economy, economy.getTotalShards(player.getUniqueId()));

        } catch (DiamondBankException e) {

            return Utils.shardsToDisplay(economy, 0);

        }

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
     * Unrent a region, reset to unrented.
     * 
     * @param giveMoneyBack true if money should be given back to the player, false
     *                      otherwise
     * @param executor      The CommandSender that should get the cancelled message
     *                      if there is any, or null
     * @return true if unrenting succeeded, othwerwise false
     */
    @SuppressWarnings("deprecation")
    public boolean unRent(boolean giveMoneyBack, CommandSender executor) {

        boolean own = executor instanceof Player && this.isRenter((Player) executor);
        if (executor != null) {

            if (!executor.hasPermission("areashop.unrent") && !own) {

                message(executor, "unrent-noPermissionOther");
                return false;

            }

            if (!executor.hasPermission("areashop.unrent") && !executor.hasPermission("areashop.unrentown") && own) {

                message(executor, "unrent-noPermission");
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
        UnrentingRegionEvent unrentingRegionEvent = new UnrentingRegionEvent(this);
        Bukkit.getPluginManager().callEvent(unrentingRegionEvent);
        if (unrentingRegionEvent.isCancelled()) {

            message(executor, "general-cancelled", unrentingRegionEvent.getReason());
            return false;

        }

        // Pay back (part of) the price for the unused time, jubilee mode pays
        // nothing back. If the payback fails the unrent is cancelled completely.
        double moneyBack = getMoneyBackAmount();
        long moneyBackShards = giveMoneyBack && !plugin.isJubilee() ? getMoneyBackShards() : 0;
        UUID payBackTo = getRenter();
        if (moneyBackShards <= 0 || payBackTo == null) {

            finishUnrent(executor, moneyBack);
            return true;

        }

        // A region being deleted must finish unrenting synchronously, so the
        // refund becomes fire-and-forget instead of cancelling the unrent
        if (isDeleted()) {

            refundShards(payBackTo, moneyBackShards, "AreaShop unrent: " + getName());
            finishUnrent(executor, moneyBack);
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

                economy.addToPlayerBankShards(payBackTo, moneyBackShards, "AreaShop unrent: " + getName(), null);
                return true;

            } catch (DiamondBankException e) {

                return false;

            }

        }, success -> {

            endEconomyTransaction();
            if (!success) {

                AreaShop.warn("Could not pay back " + Utils.shardsToDisplay(economy, moneyBackShards) + " Diamonds to "
                        + getPlayerName() + " for unrenting " + getName() + ", unrent cancelled");
                message(executor, "unrent-payError");
                return;

            }

            if (isRented()) {

                finishUnrent(executor, moneyBack);

            }

        });

        return true;

    }

    /**
     * Apply the unrent to the region: fire events, send messages and clear the
     * renter. Runs on the main thread after any payback has been deposited.
     *
     * @param executor  The CommandSender that gets the result message, or null
     * @param moneyBack The amount of money paid back, for the event and messages
     */
    private void finishUnrent(CommandSender executor, double moneyBack) {

        // Handle schematic save/restore (while %uuid% is still available)
        handleSchematicEvent(RegionEvent.UNRENTED);

        // Send message: before actual removal of the renter so that it is still
        // available for variables
        message(executor, "unrent-unrented");

        // Remove friends, the owner and renteduntil values
        getFriendsFeature().clearFriends();
        UUID oldRenter = getRenter();
        setRentedUntil(null);
        setTimesExtended(-1);
        removeLastActiveTime();

        // Notify about updates
        Bukkit.getPluginManager().callEvent(new UnrentedRegionEvent(this, oldRenter, Math.max(0, moneyBack)));
        // Placed here so when event is passed, the player renting can still be accessed
        setRenter(null);
        // Update world (has to be after setting renter to null)
        this.update();

    }

    @Override
    public boolean checkInactive() {

        if (isDeleted() || !isRented()) {

            return false;

        }

        long inactiveSetting = getInactiveTimeUntilUnrent();
        OfflinePlayer player = Bukkit.getOfflinePlayer(getRenter());
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
            return this.unRent(true, null);

        }

        return false;

    }

}
