package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

import javax.annotation.Nonnull;

public class BukkitModule extends AbstractModule {

    private final Server server;

    public BukkitModule(@Nonnull Server server) {
        this.server = server;
    }

    @Override
    protected void configure() {
        bind(Server.class).toInstance(this.server);
        bind(PluginManager.class).toInstance(this.server.getPluginManager());
        bind(BukkitScheduler.class).toInstance(this.server.getScheduler());
        bind(PluginManager.class).toInstance(this.server.getPluginManager());
        bind(ServicesManager.class).toInstance(this.server.getServicesManager());
        bind(ItemFactory.class).toInstance(this.server.getItemFactory());
        bind(ConsoleCommandSender.class).toInstance(this.server.getConsoleSender());
        bind(ScoreboardManager.class).toInstance(this.server.getScoreboardManager());
    }

}
