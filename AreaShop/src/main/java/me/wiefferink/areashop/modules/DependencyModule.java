package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import javax.annotation.Nonnull;

public class DependencyModule extends AbstractModule {

    private final WorldEditPlugin worldEditPlugin;
    private final WorldGuardPlugin worldGuardPlugin;
    private final Economy economy;
    private final Permission permission;

   public DependencyModule(@Nonnull WorldEditPlugin worldEditPlugin,
                           @Nonnull WorldGuardPlugin worldGuardPlugin,
                           @Nonnull Economy economy,
                           @Nonnull Permission permission
   ) {
       this.worldEditPlugin = worldEditPlugin;
       this.worldGuardPlugin = worldGuardPlugin;
       this.economy = economy;
       this.permission = permission;
   }

    @Override
    protected void configure() {
        bind(WorldEditPlugin.class).toInstance(this.worldEditPlugin);
        bind(WorldGuardPlugin.class).toInstance(this.worldGuardPlugin);
        bind(Economy.class).toInstance(this.economy);
        bind(Permission.class).toInstance(this.permission);
    }
}
