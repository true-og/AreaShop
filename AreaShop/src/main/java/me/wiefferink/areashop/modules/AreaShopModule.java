package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import me.wiefferink.areashop.AreaShopPlugin;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.features.FeatureFactory;
import me.wiefferink.areashop.features.signs.SignsModule;
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
import me.wiefferink.areashop.regions.ImportJobFactory;
import me.wiefferink.areashop.regions.RegionModule;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class AreaShopModule extends AbstractModule {

    private final AreaShopPlugin instance;
    private final BukkitInterface bukkitInterface;
    private final WorldGuardInterface worldGuardInterface;
    private final WorldEditInterface worldEditInterface;
    private final NMS nms;
    private final MessageBridge messageBridge;
    private final SignErrorLogger signErrorLogger;
    private final DependencyModule dependencyModule;

    public AreaShopModule(@Nonnull AreaShopPlugin instance,
                          @Nonnull MessageBridge messageBridge,
                          @Nonnull NMS nms,
                          @Nonnull BukkitInterface bukkitInterface,
                          @Nonnull WorldEditInterface worldEditInterface,
                          @Nonnull WorldGuardInterface worldGuardInterface,
                          @Nonnull SignErrorLogger signErrorLogger,
                          @Nonnull DependencyModule dependencyModule
    ) {
        this.instance = instance;
        this.messageBridge = messageBridge;
        this.nms = nms;
        this.signErrorLogger = signErrorLogger;
        this.bukkitInterface = bukkitInterface;
        this.worldEditInterface = worldEditInterface;
        this.worldGuardInterface = worldGuardInterface;
        this.dependencyModule = dependencyModule;
    }

    @Override
    protected void configure() {
        install(this.dependencyModule);
        bind(Plugin.class).toInstance(this.instance);
        bind(AreaShopPlugin.class).toInstance(this.instance);
        bind(MessageBridge.class).toInstance(this.messageBridge);
        bind(NMS.class).toInstance(this.nms);
        bind(BlockBehaviourHelper.class).toInstance(this.nms.blockBehaviourHelper());
        bind(BukkitInterface.class).toInstance(this.bukkitInterface);
        bind(WorldGuardInterface.class).toInstance(this.worldGuardInterface);
        bind(WorldEditInterface.class).toInstance(this.worldEditInterface);
        bind(SignErrorLogger.class).toInstance(this.signErrorLogger);
        // Setup managers
        bind(FileManager.class).asEagerSingleton();
        bind(FeatureManager.class).in(Singleton.class);
        bind(CommandManager.class).in(Singleton.class);
        bind(SignLinkerManager.class).in(Singleton.class);
        install(new SignsModule());
        install(new RegionModule());
        install(new FactoryModuleBuilder().build(FeatureFactory.class));
        install(new FactoryModuleBuilder().build(ImportJobFactory.class));
        requestStaticInjection(Utils.class);
    }
}
