package me.wiefferink.areashop.features.confirmation;

import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.regions.BuyRegion;
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
 * ARM-style, one-row warning screens for shop purchases and sell-backs.
 */
public class ShopConfirmationGui implements Listener {

    private static final int INVENTORY_SIZE = 9;
    private static final int CONFIRM_SLOT = 0;
    private static final int CANCEL_SLOT = INVENTORY_SIZE - 1;
    private final AreaShop plugin;

    ShopConfirmationGui(AreaShop plugin) {

        this.plugin = plugin;

    }

    /**
     * Show a confirmation screen before a player buys a shop.
     *
     * @param player The player buying the shop
     * @param region The shop to buy
     */
    public void openBuyConfirmation(Player player, BuyRegion region) {

        openConfirmation(player, region, Transaction.BUY);

    }

    /**
     * Show a confirmation screen before a player sells their shop back.
     *
     * @param player The player selling the shop
     * @param region The shop to sell
     */
    public void openSellConfirmation(Player player, BuyRegion region) {

        openConfirmation(player, region, Transaction.SELL);

    }

    private void openConfirmation(Player player, BuyRegion region, Transaction transaction) {

        ConfirmationInventoryHolder holder = new ConfirmationInventoryHolder(region, transaction);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Utils.applyColors(transaction.title()));
        holder.setInventory(inventory);

        inventory.setItem(CONFIRM_SLOT, createConfirmItem(holder));
        for (int slot = CONFIRM_SLOT + 1; slot < CANCEL_SLOT; slot++) {

            inventory.setItem(slot, createFillerItem());

        }

        inventory.setItem(CANCEL_SLOT, createCancelItem());
        player.openInventory(inventory);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof ConfirmationInventoryHolder holder)) {

            return;

        }

        // The dialog is read-only, including the player's lower inventory.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {

            return;

        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= INVENTORY_SIZE) {

            return;

        }

        if (rawSlot == CANCEL_SLOT) {

            player.closeInventory();
            return;

        }

        if (rawSlot == CONFIRM_SLOT) {

            player.closeInventory();
            if (holder.transaction() == Transaction.BUY) {

                holder.region().buy(player);

            } else {

                // This is the normal sell-back path, including Diamond payback.
                holder.region().sell(true, player);

            }

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder() instanceof ConfirmationInventoryHolder) {

            event.setCancelled(true);

        }

    }

    private ItemStack createConfirmItem(ConfirmationInventoryHolder holder) {

        BuyRegion region = holder.region();
        boolean buying = holder.transaction() == Transaction.BUY;
        boolean willPayBack = !buying && !plugin.isJubilee();
        String price = buying
                ? (region.isInResellingMode() ? region.getFormattedResellPrice() : region.getFormattedPrice())
                : region.getFormattedMoneyBackAmount();

        ItemStack item = new ItemStack(Material.MELON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors(buying ? "&aYes, buy this shop" : "&aYes, sell this shop"));
        List<String> lore = new ArrayList<>(
                List.of(Utils.applyColors("&7" + (buying ? "Buy " : "Sell ") + "&6" + region.getName()),
                        Utils.applyColors("&7" + (buying ? "Cost: " : "Diamonds returned: ")
                                + (willPayBack || buying ? price : Utils.formatCurrency(0)))));
        if (!buying && !willPayBack) {

            lore.add(Utils.applyColors("&cNo Diamonds are returned during the jubilee"));

        }

        lore.add(Utils.applyColors("&8Click to confirm"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;

    }

    private ItemStack createCancelItem() {

        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.applyColors("&cNo, cancel"));
        meta.setLore(List.of(Utils.applyColors("&7Keep the shop unchanged")));
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

    private enum Transaction {

        BUY("&2Buy this shop?"), SELL("&4Sell this shop?");

        private final String title;

        Transaction(String title) {

            this.title = title;

        }

        String title() {

            return title;

        }

    }

    private static final class ConfirmationInventoryHolder implements InventoryHolder {

        private final BuyRegion region;
        private final Transaction transaction;
        private Inventory inventory;

        private ConfirmationInventoryHolder(BuyRegion region, Transaction transaction) {

            this.region = region;
            this.transaction = transaction;

        }

        private BuyRegion region() {

            return region;

        }

        private Transaction transaction() {

            return transaction;

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
