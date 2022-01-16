package me.wiefferink.areashop.features.signs;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.interfaces.BukkitInterface;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.managers.SignLinkerManager;
import me.wiefferink.areashop.nms.BlockBehaviourHelper;
import me.wiefferink.areashop.regions.RegionFactory;

import javax.annotation.Nonnull;

public class SignsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SignManager.class).asEagerSingleton();
        install(new FactoryModuleBuilder().build(SignFactory.class));
    }

    @Provides
    @Singleton
    public SignListener provideSignListener(@Nonnull AreaShop plugin,
                                            @Nonnull BlockBehaviourHelper behaviourHelper,
                                            @Nonnull RegionFactory regionFactory,
                                            @Nonnull MessageBridge messageBridge,
                                            @Nonnull SignLinkerManager signLinkerManager,
                                            @Nonnull BukkitInterface bukkitInterface,
                                            @Nonnull WorldGuardInterface worldGuardInterface,
                                            @Nonnull SignManager signManager,
                                            @Nonnull IFileManager fileManager
    ) {
        final SignListener signListener = new SignListener(
                plugin,
                behaviourHelper,
                regionFactory,
                messageBridge,
                signLinkerManager,
                bukkitInterface,
                worldGuardInterface,
                signManager,
                fileManager
        );
        plugin.getServer().getPluginManager().registerEvents(signListener, plugin);
        return signListener;
    }

}