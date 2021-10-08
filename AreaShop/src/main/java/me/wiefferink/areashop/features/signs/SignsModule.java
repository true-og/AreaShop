package me.wiefferink.areashop.features.signs;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.nms.BlockBehaviourHelper;

import javax.annotation.Nonnull;

public class SignsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SignManager.class).asEagerSingleton();
        bind(SignListener.class).asEagerSingleton();
    }

    @Provides
    public SignListener provideSignListener(@Nonnull AreaShop plugin,
                                            @Nonnull BlockBehaviourHelper behaviourHelper,
                                            @Nonnull SignManager signManager) {
        SignListener signListener = new SignListener(behaviourHelper, plugin, signManager);
        plugin.getServer().getPluginManager().registerEvents(signListener, plugin);
        return signListener;
    }

}
