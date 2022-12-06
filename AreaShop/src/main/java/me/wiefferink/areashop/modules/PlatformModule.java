package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import me.wiefferink.areashop.adapters.platform.MinecraftPlatform;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.nms.BlockBehaviourHelper;
import me.wiefferink.areashop.nms.NMS;
import org.jetbrains.annotations.NotNull;

public class PlatformModule extends AbstractModule {

    private final MinecraftPlatform minecraftPlatform;
    private final NMS nms;

    public PlatformModule(@NotNull MinecraftPlatform platform,
                          @NotNull NMS nms) {
        this.minecraftPlatform = platform;
        this.nms = nms;
    }
    @Override
    protected void configure() {
        bind(MinecraftPlatform.class).toInstance(this.minecraftPlatform);
        bind(OfflinePlayerHelper.class).toInstance(this.minecraftPlatform.offlinePlayerHelper());
        bind(NMS.class).toInstance(this.nms);
        bind(BlockBehaviourHelper.class).toInstance(this.nms.blockBehaviourHelper());
    }
}
