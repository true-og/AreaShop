package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.luckperms.api.LuckPerms;
import net.trueog.diamondbankog.api.DiamondBankAPIJava;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DependencyModule extends AbstractModule {

    private final WorldEditPlugin worldEditPlugin;
    private final WorldGuardPlugin worldGuardPlugin;
    private final DiamondBankAPIJava economy;
    private final LuckPerms permission;

    public DependencyModule(@Nonnull WorldEditPlugin worldEditPlugin, @Nonnull WorldGuardPlugin worldGuardPlugin,
            @Nullable DiamondBankAPIJava economy, @Nullable LuckPerms permission)
    {

        this.worldEditPlugin = worldEditPlugin;
        this.worldGuardPlugin = worldGuardPlugin;
        this.economy = economy;
        this.permission = permission;

    }

    @Override
    protected void configure() {

        bind(WorldEditPlugin.class).toInstance(this.worldEditPlugin);
        bind(WorldGuardPlugin.class).toInstance(this.worldGuardPlugin);
        bind(DiamondBankAPIJava.class).toInstance(this.economy);
        bind(LuckPerms.class).toInstance(this.permission);

    }

}
