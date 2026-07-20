package me.wiefferink.areashop.features.confirmation;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;

import javax.annotation.Nonnull;

/**
 * Registers the inventory listener used for buy and sell-back confirmations.
 */
public class ConfirmationModule extends AbstractModule {

    @Provides
    @Singleton
    public ShopConfirmationGui provideShopConfirmationGui(@Nonnull AreaShop plugin) {

        ShopConfirmationGui confirmationGui = new ShopConfirmationGui(plugin);
        plugin.getServer().getPluginManager().registerEvents(confirmationGui, plugin);
        return confirmationGui;

    }

    @Provides
    @Singleton
    public RentOptionsGui provideRentOptionsGui(@Nonnull AreaShop plugin, @Nonnull MessageBridge messageBridge) {

        RentOptionsGui rentOptionsGui = new RentOptionsGui(plugin, messageBridge);
        plugin.getServer().getPluginManager().registerEvents(rentOptionsGui, plugin);
        return rentOptionsGui;

    }

}
