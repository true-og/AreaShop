package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.interfaces.BukkitInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.CommandManager;
import me.wiefferink.areashop.managers.FeatureManager;
import me.wiefferink.areashop.managers.FileManager;
import me.wiefferink.areashop.managers.SignErrorLogger;
import me.wiefferink.areashop.managers.SignLinkerManager;
import me.wiefferink.areashop.nms.BlockBehaviourHelper;
import me.wiefferink.areashop.nms.NMS;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class AreaShopModule extends AbstractModule {

    private final AreaShop instance;
    private final BukkitInterface bukkitInterface;
    private final WorldGuardInterface worldGuardInterface;
    private final WorldEditInterface worldEditInterface;
    private final NMS nms;
    private final DependencyModule dependencyModule;

    public AreaShopModule(@Nonnull AreaShop instance,
                          @Nonnull NMS nms,
                          @Nonnull BukkitInterface bukkitInterface,
                          @Nonnull WorldEditInterface worldEditInterface,
                          @Nonnull WorldGuardInterface worldGuardInterface,
                          @Nonnull DependencyModule dependencyModule
    ) {
        this.instance = instance;
        this.nms = nms;
        this.bukkitInterface = bukkitInterface;
        this.worldEditInterface = worldEditInterface;
        this.worldGuardInterface = worldGuardInterface;
        this.dependencyModule = dependencyModule;
    }

    @Override
    protected void configure() {
        install(this.dependencyModule);
        bind(Plugin.class).toInstance(this.instance);
        bind(AreaShop.class).toInstance(this.instance);
        bind(NMS.class).toInstance(this.nms);
        bind(BlockBehaviourHelper.class).toInstance(this.nms.blockBehaviourHelper());
        bind(BukkitInterface.class).toInstance(this.bukkitInterface);
        bind(WorldGuardInterface.class).toInstance(this.worldGuardInterface);
        bind(WorldEditInterface.class).toInstance(this.worldEditInterface);
        // Setup managers
        bind(FileManager.class).asEagerSingleton();
        bind(SignErrorLogger.class).asEagerSingleton();
        bind(FeatureManager.class).asEagerSingleton();
        bind(CommandManager.class).asEagerSingleton();
        bind(SignLinkerManager.class).asEagerSingleton();
    }
}
