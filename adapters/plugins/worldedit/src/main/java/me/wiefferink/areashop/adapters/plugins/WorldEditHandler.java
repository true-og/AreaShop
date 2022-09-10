package me.wiefferink.areashop.adapters.plugins;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
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

public class WorldEditHandler extends WorldEditInterface {

	public WorldEditHandler(AreaShopInterface pluginInterface) {
		super(pluginInterface);
	}

	@Override
	public WorldEditSelection getPlayerSelection(Player player) {
		try {
			Region region = pluginInterface.getWorldEdit().getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
			return new WorldEditSelection(
					player.getWorld(),
					BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint()),
					BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint())
			);
		} catch (IncompleteRegionException e) {
			return null;
		}
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
		Region region = WorldEditRegionConverter.convertToRegion(regionInterface.getRegion());
		if (region == null) {
			ProtectedRegion wgRegion = regionInterface.getRegion();
			region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
		}
		final World world = BukkitAdapter.adapt(regionInterface.getWorld());
		if (world == null) {
			pluginInterface.getLogger().warning("Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
			return false;
		}
		BlockVector3 dimensions = regionInterface.computeDimensions();
		int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
		try (InputStream is = new FileInputStream(finalFile);
			 ClipboardReader reader = finalFormat.getReader(is)) {
			Clipboard clipboard = reader.read();
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
		final ClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
		final File targetFile = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
		Region region = WorldEditRegionConverter.convertToRegion(regionInterface.getRegion());
		if (region == null) {
			ProtectedRegion wgRegion = regionInterface.getRegion();
			region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
		}
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
		Clipboard clipboard = new BlockArrayClipboard(region);
		try (editSession) {
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
}






















