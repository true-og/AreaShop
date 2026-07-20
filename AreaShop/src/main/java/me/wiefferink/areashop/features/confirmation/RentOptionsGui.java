package me.wiefferink.areashop.features.confirmation;

import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.UnrentCommand;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * ARM-style rent management and unrent confirmation screens.
 */
public class RentOptionsGui implements Listener {

    private static final int INVENTORY_SIZE = 9;
    private static final int PRIMARY_SLOT = 0;
    private static final int CANCEL_SLOT = INVENTORY_SIZE / 2;
    private static final int SECONDARY_SLOT = INVENTORY_SIZE - 1;

    private final AreaShop plugin;
    private final MessageBridge messageBridge;

    RentOptionsGui(AreaShop plugin, MessageBridge messageBridge) {

        this.plugin = plugin;
        this.messageBridge = messageBridge;

    }

    /**
     * Show the renter's choices to pay, decline payment, or stop renting.
     *
     * @param player The current renter
     * @param region The rented shop
     */
    public void openRentOptions(Player player, RentRegion region) {

        open(player, region, Screen.OPTIONS);

    }

    /**
     * Ask a player to confirm whether they want to pay this shop's rent.
     *
     * @param player The prospective payer
     * @param region The rented shop
     */
    public void openPayRentConfirmation(Player player, RentRegion region) {

        open(player, region, Screen.PAY_RENT_CONFIRMATION);

    }

    // Ask a player to confirm whether they want to start renting a free shop
    public void openRentConfirmation(Player player, RentRegion region) {

        open(player, region, Screen.RENT_CONFIRMATION);

    }

    /**
     * Pay rent on behalf of the renter, using the same checks as /as payrent.
     *
     * @param payer  The player paying
     * @param region The rented shop
     */
    public void payRent(Player payer, RentRegion region) {

        if (!payer.hasPermission("areashop.payrent")) {

            messageBridge.message(payer, "payrent-noPermission");
            return;

        }

        if (!region.isRented()) {

            messageBridge.message(payer, "payrent-notRented", region.getName());
            return;

        }

        if (plugin.isJubilee()) {

            messageBridge.message(payer, "payrent-jubilee", region.getName());
            return;

        }

        if (region.isRenter(payer)) {

            region.rent(payer);

        } else {

            region.rent(Bukkit.getOfflinePlayer(region.getRenter()), payer);

        }

    }

    private void open(Player player, RentRegion region, Screen screen) {

        RentInventoryHolder holder = new RentInventoryHolder(region, screen);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Utils.applyColors(screen.title()));
        holder.setInventory(inventory);

        inventory.setItem(PRIMARY_SLOT, switch (screen) {

            case OPTIONS -> createPayRentItem(region, true);
            case PAY_RENT_CONFIRMATION -> createPayRentItem(region, false);
            case RENT_CONFIRMATION -> createRentItem(region);
            case UNRENT_CONFIRMATION -> createUnrentItem(region);

        });
        for (int slot = PRIMARY_SLOT + 1; slot < SECONDARY_SLOT; slot++) {

            inventory.setItem(slot,
                    screen == Screen.OPTIONS && slot == CANCEL_SLOT ? createDoNotPayItem() : createFillerItem());

        }

        inventory.setItem(SECONDARY_SLOT, switch (screen) {

            case OPTIONS -> createSellBackItem(region);
            case PAY_RENT_CONFIRMATION -> createDoNotPayItem();
            case RENT_CONFIRMATION -> createDoNotRentItem();
            case UNRENT_CONFIRMATION -> createKeepRentingItem();

        });
        player.openInventory(inventory);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof RentInventoryHolder holder)) {

            return;

        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {

            return;

        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= INVENTORY_SIZE) {

            return;

        }

        switch (holder.screen()) {

            case OPTIONS -> handleOptionsClick(player, holder.region(), rawSlot);
            case PAY_RENT_CONFIRMATION -> handlePayRentConfirmationClick(player, holder.region(), rawSlot);
            case RENT_CONFIRMATION -> handleRentConfirmationClick(player, holder.region(), rawSlot);
            case UNRENT_CONFIRMATION -> handleUnrentConfirmationClick(player, holder.region(), rawSlot);

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder() instanceof RentInventoryHolder) {

            event.setCancelled(true);

        }

    }

    private void handleOptionsClick(Player player, RentRegion region, int rawSlot) {

        if (rawSlot == PRIMARY_SLOT) {

            open(player, region, Screen.PAY_RENT_CONFIRMATION);
            return;

        }

        if (rawSlot == CANCEL_SLOT) {

            player.closeInventory();
            return;

        }

        if (rawSlot == SECONDARY_SLOT) {

            if (!UnrentCommand.canUse(player, region)) {

                messageBridge.message(player, "unrent-noPermission");
                return;

            }

            open(player, region, Screen.UNRENT_CONFIRMATION);

        }

    }

    private void handlePayRentConfirmationClick(Player player, RentRegion region, int rawSlot) {

        if (rawSlot == SECONDARY_SLOT) {

            player.closeInventory();
            return;

        }

        if (rawSlot == PRIMARY_SLOT) {

            player.closeInventory();
            payRent(player, region);

        }

    }

    private void handleRentConfirmationClick(Player player, RentRegion region, int rawSlot) {

        if (rawSlot == SECONDARY_SLOT) {

            player.closeInventory();
            return;

        }

        if (rawSlot == PRIMARY_SLOT) {

            player.closeInventory();
            region.rent(player);

        }

    }

    private void handleUnrentConfirmationClick(Player player, RentRegion region, int rawSlot) {

        if (rawSlot == SECONDARY_SLOT) {

            player.closeInventory();
            return;

        }

        if (rawSlot == PRIMARY_SLOT) {

            player.closeInventory();
            region.unRent(true, player);

        }

    }

    private ItemStack createPayRentItem(RentRegion region, boolean requiresConfirmation) {

        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&aPay rent"));
        List<String> lore = new ArrayList<>(
                List.of(Utils.applyColors("&7Extend &6" + region.getName()), Utils.applyColors(
                        "&7Cost: " + (plugin.isJubilee() ? Utils.formatCurrency(0) : region.getFormattedPrice()))));
        if (plugin.isJubilee()) {

            lore.add(Utils.applyColors("&8The jubilee includes all extensions"));

        }

        lore.add(Utils.applyColors(requiresConfirmation ? "&8Click to continue" : "&8Click to pay"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createRentItem(RentRegion region) {

        // The first rent is charged during the jubilee too, but it immediately
        // grants the maximum rent time instead of a single period
        boolean jubilee = plugin.isJubilee() && region.getMaxRentTime() > 0;
        String period = jubilee ? Utils.millisToHumanFormat(region.getMaxRentTime()) : region.getDurationString();
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&aYes, rent this shop"));
        List<String> lore = new ArrayList<>(List.of(Utils.applyColors("&7Rent &6" + region.getName()),
                Utils.applyColors("&7Cost: " + region.getFormattedPrice() + " &7for " + period)));
        if (jubilee) {

            lore.add(Utils.applyColors("&8The jubilee includes all extensions"));

        }

        lore.add(Utils.applyColors("&8Click to confirm"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createDoNotRentItem() {

        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&cNo, do not rent"));
        meta.setLore(List.of(Utils.applyColors("&7Leave this shop for someone else")));
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createSellBackItem(RentRegion region) {

        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&cStop renting and sell back"));
        String refund = plugin.isJubilee() ? Utils.formatCurrency(0) : region.getFormattedMoneyBackAmount();
        List<String> lore = new ArrayList<>(List.of(Utils.applyColors("&7Release &6" + region.getName()),
                Utils.applyColors("&7Diamonds returned: " + refund)));
        if (plugin.isJubilee()) {

            lore.add(Utils.applyColors("&cNo Diamonds are returned during the jubilee"));

        }

        lore.add(Utils.applyColors("&8Click to continue"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createUnrentItem(RentRegion region) {

        ItemStack item = new ItemStack(Material.MELON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&aYes, stop renting"));
        String refund = plugin.isJubilee() ? Utils.formatCurrency(0) : region.getFormattedMoneyBackAmount();
        List<String> lore = new ArrayList<>(List.of(Utils.applyColors("&7Release &6" + region.getName()),
                Utils.applyColors("&7Diamonds returned: " + refund)));
        if (plugin.isJubilee()) {

            lore.add(Utils.applyColors("&cNo Diamonds are returned during the jubilee"));

        }

        lore.add(Utils.applyColors("&8Click to confirm"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createKeepRentingItem() {

        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&cNo, keep renting"));
        meta.setLore(List.of(Utils.applyColors("&7Return to your rented shop")));
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createDoNotPayItem() {

        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&cNo, do not pay"));
        meta.setLore(List.of(Utils.applyColors("&7Return without paying rent")));
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createFillerItem() {

        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&8 "));
        item.setItemMeta(meta);
        return item;

    }

    private enum Screen {

        OPTIONS("&2Rent options"), PAY_RENT_CONFIRMATION("&2Pay rent?"), RENT_CONFIRMATION("&2Rent this shop?"),
        UNRENT_CONFIRMATION("&4Stop renting this shop?");

        private final String title;

        Screen(String title) {

            this.title = title;

        }

        String title() {

            return title;

        }

    }

    private static final class RentInventoryHolder implements InventoryHolder {

        private final RentRegion region;
        private final Screen screen;
        private Inventory inventory;

        private RentInventoryHolder(RentRegion region, Screen screen) {

            this.region = region;
            this.screen = screen;

        }

        private RentRegion region() {

            return region;

        }

        private Screen screen() {

            return screen;

        }

        private void setInventory(Inventory inventory) {

            this.inventory = inventory;

        }

        @Override
        public Inventory getInventory() {

            return inventory;

        }

    }

}
