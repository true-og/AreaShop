package me.wiefferink.areashop.adapters.plugins.essentials;

import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.extensions.AreashopExtension;
import me.wiefferink.areashop.features.homeaccess.AccessControlValidator;
import me.wiefferink.areashop.features.homeaccess.OwnershipControlValidator;
import org.bukkit.Server;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EssentialsExtension implements AreashopExtension {

    private boolean enabled;

    @Override
    public void init(@NonNull AreaShop plugin, @NonNull Injector injector) {
        if (this.enabled) {
            return;
        }
        Injector childInjector = injector.createChildInjector(new FactoryModuleBuilder().build(HomeListenerFactory.class));
        Server server = childInjector.getInstance(Server.class);
        if (!server.getPluginManager().isPluginEnabled("Essentials")) {
            return;
        }
        AreaShop.info("EssentialsX detected; binding implementation for home access");
        HomeListenerFactory factory = childInjector.getInstance(HomeListenerFactory.class);
        AccessControlValidator controlValidator = new OwnershipControlValidator();
        HomeModificationListener listener = factory.createListener(controlValidator);
        server.getPluginManager().registerEvents(listener, plugin);
        this.enabled = true;
    }
}
