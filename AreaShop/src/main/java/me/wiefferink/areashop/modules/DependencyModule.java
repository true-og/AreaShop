package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import javax.annotation.Nonnull;

public class DependencyModule extends AbstractModule {

    private final WorldEditPlugin worldEditPlugin;
    private final WorldGuardPlugin worldGuardPlugin;

   public DependencyModule(@Nonnull WorldEditPlugin worldEditPlugin,
                           @Nonnull WorldGuardPlugin worldGuardPlugin
   ) {
       this.worldEditPlugin = worldEditPlugin;
       this.worldGuardPlugin = worldGuardPlugin;
   }

    @Override
    protected void configure() {
        bind(WorldEditPlugin.class).toInstance(this.worldEditPlugin);
        bind(WorldGuardPlugin.class).toInstance(this.worldGuardPlugin);
    }
}
