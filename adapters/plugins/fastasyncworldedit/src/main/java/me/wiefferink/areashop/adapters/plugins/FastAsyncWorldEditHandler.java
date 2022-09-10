package me.wiefferink.areashop.adapters.plugins;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.interfaces.GeneralRegionInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class FastAsyncWorldEditHandler extends WorldEditInterface {

    public FastAsyncWorldEditHandler(AreaShopInterface pluginInterface) {
        super(pluginInterface);
    }

    @Override
    public boolean supportsAsyncOperations() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> restoreRegionBlocksAsync(File rawFile, GeneralRegionInterface regionInterface) {
        ClipboardFormat format = null;
        File targetFile = null;
        for (ClipboardFormat formatOption : ClipboardFormats.getAll()) {
            for (String extension : formatOption.getFileExtensions()) {
                File fileOption = new File(rawFile.getAbsolutePath() + "." + extension);
                if (fileOption.exists()) {
                    targetFile = fileOption;
                    format = formatOption;
                }
            }
        }
        if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
            pluginInterface.getLogger().info("Not restoring region. Schematic not found: " + rawFile);
            return CompletableFuture.completedFuture(false);
        }
        File finalFile = targetFile;
        ClipboardFormat finalFormat = format;
        Region convertedRegion = WorldEditRegionConverter.convertToRegion(regionInterface.getRegion());
        Region region;
        if (convertedRegion == null) {
            ProtectedRegion wgRegion = regionInterface.getRegion();
            region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        } else {
            region = convertedRegion;
        }
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning("Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return CompletableFuture.completedFuture(false);
        }
        BlockVector3 dimensions = regionInterface.computeDimensions();
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        if (failedClearEntities(maxBlocks, world, region, regionInterface)) {
            return CompletableFuture.completedFuture(false);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        FaweAPI.getTaskManager().async(() -> {
            try (InputStream is = new FileInputStream(finalFile);
                 ClipboardReader reader = finalFormat.getReader(is);
                 Clipboard clipboard = reader.read()) {
                if (!clipboard.getDimensions().equals(dimensions)) {
                    pluginInterface.getLogger().warning(() -> "Size of the region " + regionInterface.getName() + " is not the same as the schematic to restore!");
                    pluginInterface.debugI("schematic|region, x:" + clipboard.getDimensions().getX() + "|" + regionInterface.getWidth() + ", y:" + clipboard.getDimensions().getY() + "|" + regionInterface.getHeight() + ", z:" + clipboard.getDimensions().getZ() + "|" + regionInterface.getDepth());
                    future.complete(false);
                    return;
                }
                final EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                        .world(world)
                        .build();
                try (editSession) {
                    ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, region, editSession, region.getMinimumPoint());
                    Operations.complete(copy);
                }
                future.complete(true);
            } catch (IOException ex) {
                pluginInterface.getLogger().warning("An error occurred while restoring schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
                pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
                future.complete(false);
            } catch (MaxChangedBlocksException ex) {
                pluginInterface.getLogger().warning(() -> "exceeded the block limit while restoring schematic of " + regionInterface.getName() + ", limit in exception: " + ex.getBlockLimit() + ", limit passed by AreaShop: " + pluginInterface.getConfig().getInt("maximumBlocks"));
                future.complete(false);
            } catch (Exception ex) {
                pluginInterface.getLogger().warning(() -> "crashed during restore of " + regionInterface.getName());
                pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
                future.complete(false);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> saveRegionBlocksAsync(File file, GeneralRegionInterface regionInterface) {
        final ClipboardFormat format = BuiltInClipboardFormat.FAST;
        final File targetFile = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
        final Region convertedRegion = WorldEditRegionConverter.convertToRegion(regionInterface.getRegion());
        final Region region;
        if (convertedRegion == null) {
            ProtectedRegion wgRegion = regionInterface.getRegion();
            region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        } else {
            region = convertedRegion;
        }
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning("Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return CompletableFuture.completedFuture(false);
        }
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        pluginInterface.debugI("Trying to save region", regionInterface.getName(), " to file", file.getAbsolutePath(), "with format", format.getName());
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        FaweAPI.getTaskManager().async(() -> {
            EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                    .world(world)
                    .maxBlocks(maxBlocks)
                    .build();
            try (Clipboard clipboard = new MemoryOptimizedClipboard(region); editSession) {
                final ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, clipboard.getMinimumPoint());
                Operations.complete(copy);
                try (OutputStream os = new FileOutputStream(targetFile);
                     ClipboardWriter writer = format.getWriter(os)) {
                    writer.write(clipboard);
                }
                future.complete(true);
            } catch (MaxChangedBlocksException e) {
                pluginInterface.getLogger().warning("Exceeded the block limit while saving schematic of " + regionInterface.getName() + ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: " + pluginInterface.getConfig().getInt("maximumBlocks"));
                future.complete(false);
            } catch (IOException ex) {
                pluginInterface.getLogger().warning("An error occurred while saving schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
                pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
                future.complete(false);
                ;
            } catch (Exception ex) {
                pluginInterface.getLogger().warning("crashed during save of " + regionInterface.getName());
                pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
                future.complete(false);
            }
        });
        return future;
    }

    @Override
    public boolean restoreRegionBlocks(File rawFile, GeneralRegionInterface regionInterface) {
        ClipboardFormat format = null;
        File targetFile = null;
        for (ClipboardFormat formatOption : ClipboardFormats.getAll()) {
            for (String extension : formatOption.getFileExtensions()) {
                File fileOption = new File(rawFile.getAbsolutePath() + "." + extension);
                if (fileOption.exists()) {
                    targetFile = fileOption;
                    format = formatOption;
                }
            }
        }
        if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
            pluginInterface.getLogger().info("Not restoring region. Schematic not found: " + rawFile);
            return false;
        }
        File finalFile = targetFile;
        ClipboardFormat finalFormat = format;
        final Region convertedRegion = WorldEditRegionConverter.convertToRegion(regionInterface.getRegion());
        final Region region;
        if (convertedRegion == null) {
            ProtectedRegion wgRegion = regionInterface.getRegion();
            region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        } else {
            region = convertedRegion;
        }
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning("Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return false;
        }
        BlockVector3 dimensions = regionInterface.computeDimensions();
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        if (failedClearEntities(maxBlocks, world, region, regionInterface)) {
            return false;
        }
        try (InputStream is = new FileInputStream(finalFile);
             ClipboardReader reader = finalFormat.getReader(is);
             Clipboard clipboard = reader.read()) {
            if (!clipboard.getDimensions().equals(dimensions)) {
                pluginInterface.getLogger().warning(() -> "Size of the region " + regionInterface.getName() + " is not the same as the schematic to restore!");
                pluginInterface.debugI("schematic|region, x:" + clipboard.getDimensions().getX() + "|" + regionInterface.getWidth() + ", y:" + clipboard.getDimensions().getY() + "|" + regionInterface.getHeight() + ", z:" + clipboard.getDimensions().getZ() + "|" + regionInterface.getDepth());
                return false;
            }
            final EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                    .world(world)
                    .maxBlocks(maxBlocks)
                    .build();
            try (editSession) {
                ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, region, editSession, region.getMinimumPoint());
                Operations.complete(copy);
            }
            return true;
        } catch (IOException ex) {
            pluginInterface.getLogger().warning("An error occurred while restoring schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
            pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
        } catch (MaxChangedBlocksException ex) {
            pluginInterface.getLogger().warning(() -> "exceeded the block limit while restoring schematic of " + regionInterface.getName() + ", limit in exception: " + ex.getBlockLimit() + ", limit passed by AreaShop: " + pluginInterface.getConfig().getInt("maximumBlocks"));
        } catch (Exception ex) {
            pluginInterface.getLogger().warning(() -> "crashed during restore of " + regionInterface.getName());
            pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
        }
        return false;
    }

    @Override
    public boolean saveRegionBlocks(File file, GeneralRegionInterface regionInterface) {
        final ClipboardFormat format = BuiltInClipboardFormat.FAST;
        final File targetFile = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
        final Region region = WorldEditRegionConverter.convertToRegion(regionInterface.getRegion());
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning("Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return false;
        }
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        pluginInterface.debugI("Trying to save region", regionInterface.getName(), " to file", file.getAbsolutePath(), "with format", format.getName());
        EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                .world(world)
                .maxBlocks(maxBlocks)
                .build();
        try (Clipboard clipboard = new MemoryOptimizedClipboard(region); editSession) {
            final ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, clipboard.getMinimumPoint());
            Operations.complete(copy);
            try (OutputStream os = new FileOutputStream(targetFile);
                 ClipboardWriter writer = format.getWriter(os)) {
                writer.write(clipboard);
            }
            return true;
        } catch (MaxChangedBlocksException e) {
            pluginInterface.getLogger().warning("Exceeded the block limit while saving schematic of " + regionInterface.getName() + ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: " + pluginInterface.getConfig().getInt("maximumBlocks"));

        } catch (IOException ex) {
            pluginInterface.getLogger().warning("An error occurred while saving schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
            pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
        } catch (Exception ex) {
            pluginInterface.getLogger().warning("crashed during save of " + regionInterface.getName());
            pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
        }
        return false;
    }

    @Override
    public WorldEditSelection getPlayerSelection(Player player) {
        try {
            Region region = pluginInterface.getWorldEdit().getSession(player)
                    .getSelection(BukkitAdapter.adapt(player.getWorld()));
            return new WorldEditSelection(player.getWorld(),
                    BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint()),
                    BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint()));
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    private boolean failedClearEntities(int maxBlocks, World world, Region region, GeneralRegionInterface regionInterface) {
        EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                .world(world)
                .maxBlocks(maxBlocks)
                .build();
        try (editSession) {
            EntityFunction function = entity -> {
                if (entity.getType().equals(EntityTypes.LEASH_KNOT)) {
                    return entity.remove();
                }
                EntityProperties properties = entity.getFacet(EntityProperties.class);
                if (properties == null) {
                    return false;
                }
                if (properties.isItemFrame() || properties.isPainting()) {
                    return entity.remove();
                }
                return false;
            };
            EntityVisitor visitor = new EntityVisitor(editSession.getEntities(region).iterator(), function);
            Operations.complete(visitor);
            return false;
        } catch (MaxChangedBlocksException e) {
            pluginInterface.getLogger().warning("Exceeded the block limit while saving schematic of " + regionInterface.getName() + ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: " + pluginInterface.getConfig().getInt("maximumBlocks"));
            return true;
        } catch (WorldEditException ex) {
            pluginInterface.getLogger().warning("crashed during save of " + regionInterface.getName());
            pluginInterface.debugI(ExceptionUtils.getStackTrace(ex));
            return true;
        }
    }
}
