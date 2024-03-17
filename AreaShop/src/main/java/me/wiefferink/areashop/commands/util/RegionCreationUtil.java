package me.wiefferink.areashop.commands.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RegionCreationUtil {

    private final WorldEditInterface worldEditInterface;
    private final Plugin plugin;
    private final IFileManager fileManager;
    private final Server server;
    private final WorldGuardInterface worldGuardInterface;

    @Inject
    public RegionCreationUtil(
            @Nonnull WorldEditInterface worldEditInterface,
            @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull IFileManager fileManager,
            @Nonnull Server server,
            @Nonnull Plugin plugin
    ) {
        this.worldEditInterface = worldEditInterface;
        this.worldGuardInterface = worldGuardInterface;
        this.fileManager = fileManager;
        this.plugin = plugin;
        this.server = server;
    }

    public CompletableFuture<ProtectedRegion> createRegion(
            @Nonnull CommandContext<Player> context,
            @Nonnull CloudKey<String> regionKey
    ) {
        Player player = context.sender();
        WorldEditSelection selection = this.worldEditInterface.getPlayerSelection(player);
        if (selection == null) {
            return CompletableFuture.failedFuture(new AreaShopCommandException("cmd-noSelection"));
        }
        List<ProtectedRegion> protectedRegions = Utils.getWorldEditRegionsInSelection(selection);
        if (!protectedRegions.isEmpty()) {
            return CompletableFuture.failedFuture(new AreaShopCommandException("cmd-moreRegionsAtLocation"));
        }
        World world = player.getWorld();
        if (!Objects.equals(selection.getWorld(), world)) {
            return CompletableFuture.failedFuture(new AreaShopCommandException("setup-selectionWorldMismatch"));
        }
        String regionName = context.get(regionKey);
        if (this.fileManager.getRegion(regionName) != null) {
            return CompletableFuture.failedFuture(new AreaShopCommandException("add-failed", regionName));
        }
        this.server.dispatchCommand(player, String.format("rg define %s", regionName));
        CompletableFuture<ProtectedRegion> future = new CompletableFuture<>();
        this.server.getScheduler().runTaskLater(this.plugin, () -> {
            ProtectedRegion region = this.worldGuardInterface.getRegionManager(world).getRegion(regionName);
            if (region == null) {
                future.completeExceptionally(new AreaShopCommandException("quickadd-failedCreateWGRegion"));
                return;
            }
            future.complete(region);
        }, 10);
        return future;
    }

}
