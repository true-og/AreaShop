package me.wiefferink.areashop.adapters.plugins;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.google.common.annotations.Beta;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.interfaces.GeneralRegionInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

// Future: this class could use the schematic save/paste api of FAWE: https://github.com/boy0001/FastAsyncWorldedit/wiki/Pasting-a-schematic
// This would not allow for shaping the schematic to a polygon region though
public class FastAsyncWorldEditHandler extends WorldEditInterface {

	public FastAsyncWorldEditHandler(AreaShopInterface pluginInterface) {
		super(pluginInterface);
	}

	private static class RegionBoundMask implements Mask {

		private final BoundingBox boundingBox;

		public RegionBoundMask(final ProtectedRegion region) {
			final BlockVector3 min = region.getMinimumPoint();
			final BlockVector3 max = region.getMaximumPoint();
			this.boundingBox = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
		}

		public RegionBoundMask(final BoundingBox boundingBox) {
			this.boundingBox = boundingBox.clone();
		}

		@Override
		public boolean test(BlockVector3 vector) {
			return boundingBox.contains(vector.getX(), vector.getY(), vector.getZ());
		}

		@Override
		public Mask2D toMask2D() {
			return null;
		}

		@Override
		public Mask copy() {
			return new RegionBoundMask(boundingBox);
		}
	}

	@Override
	public boolean supportsAsyncOperations() {
		return true;
	}

	@Override
	public CompletableFuture<Boolean> restoreRegionBlocksAsync(File rawFile, GeneralRegionInterface regionInterface) {
		File file = null;
		for (ClipboardFormat formatOption : BuiltInClipboardFormat.values()) {
			if (new File(rawFile.getAbsolutePath() + "." + formatOption.getPrimaryFileExtension())
					.exists()) {
				file = new File(
						rawFile.getAbsolutePath() + "." + formatOption.getPrimaryFileExtension());
				break;
			}
		}
		if (file == null) {
			pluginInterface.getLogger().info("Did not restore region " + regionInterface.getName()
					+ ", schematic file does not exist: " + rawFile.getAbsolutePath());
			return CompletableFuture.completedFuture(false);
		}
		ClipboardFormat format = ClipboardFormats.findByFile(file);
		if (format == null) {
			pluginInterface.debugI("Did not restore region " + regionInterface.getName() + ", could not find a valid clipboard format");
			return CompletableFuture.completedFuture(false);
		}
		pluginInterface.debugI("Trying to restore region", regionInterface.getName(), " from file",
				file.getAbsolutePath(), "with format", format.getName());

		com.sk89q.worldedit.world.World world = null;
		if (regionInterface.getName() != null) {
			world = BukkitAdapter.adapt(regionInterface.getWorld());
		}
		if (world == null) {
			pluginInterface.getLogger().info(
					"Did not restore region " + regionInterface.getName() + ", world not found: "
							+ regionInterface.getWorldName());
			return CompletableFuture.completedFuture(false);
		}
		final FaweLimit limit = FaweLimit.MAX.copy();
		limit.MAX_CHANGES = pluginInterface.getConfig().getInt("maximumBlocks");
		limit.MAX_ENTITIES = pluginInterface.getConfig().getInt("maximumEntities", (int) (limit.MAX_CHANGES / 1000));
		EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit()
				.newEditSessionBuilder()
				.world(world)
				.limit(limit)
				.build();
		editSession.setReorderMode(EditSession.ReorderMode.MULTI_STAGE);
		ProtectedRegion region = regionInterface.getRegion();
		// Get the origin and size of the region
		BlockVector3 origin = BlockVector3
				.at(region.getMinimumPoint().getBlockX(),
						region.getMinimumPoint().getBlockY(),
						region.getMinimumPoint().getBlockZ());
		final File finalFile = file;
		final String regionName = regionInterface.getName();
		final double regionWidth = regionInterface.getWidth(), regionHeight = regionInterface.getHeight(), regionDepth = regionInterface.getDepth();
		final LocalSession session =
				new LocalSession(pluginInterface.getWorldEdit().getLocalConfiguration());
		final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
		final RegionType regionType = region.getType();
		final Mask mask = new RegionBoundMask(region);
		FaweAPI.getTaskManager().async(() -> {
			boolean result = true;
			// Read the schematic and paste it into the world
			try (Closer closer = Closer.create(); editSession) {
				final FileInputStream fis = closer.register(new FileInputStream(finalFile));
				final ClipboardReader reader = format.getReader(fis);
				final Clipboard clipboard = reader.read();

				if (clipboard.getDimensions().getY() != regionHeight
						|| clipboard.getDimensions().getX() != regionWidth
						|| clipboard.getDimensions().getZ() != regionDepth) {
					pluginInterface.getLogger().warning(
							"Size of the region " + regionName
									+ " is not the same as the schematic to restore!");
					pluginInterface.debugI(
							"schematic|region, x:" + clipboard.getDimensions().getX() + "|"
									+ regionWidth + ", y:" + clipboard.getDimensions().getY()
									+ "|" + regionHeight + ", z:" + clipboard.getDimensions()
									.getZ() + "|" + regionDepth);
				}
				clipboard.setOrigin(clipboard.getMinimumPoint());
				ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
				session.setBlockChangeLimit((int) limit.MAX_CHANGES);
				session.setClipboard(clipboardHolder);

				// Build operation

				BlockTransformExtent extent = new BlockTransformExtent(clipboard);
				ForwardExtentCopy copy =
						new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(),
								editSession, origin);
				copy.setCopyingEntities(false);
				copy.setTransform(clipboardHolder.getTransform());
				// Mask to region (for polygon and other weird shaped regions)
				// TODO make this more efficient (especially for polygon regions)
				if (regionType != RegionType.CUBOID) {
					copy.setSourceMask(mask);
				}
				Operations.completeLegacy(copy);
			} catch (MaxChangedBlocksException e) {
				pluginInterface.getLogger().warning(
						"exceeded the block limit while restoring schematic of " + regionInterface.getName()
								+ ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: "
								+ pluginInterface.getConfig().getInt("maximumBlocks"));
				result = false;
			} catch (IOException e) {
				pluginInterface.getLogger().warning(
						"An error occurred while restoring schematic of " + regionInterface.getName()
								+ ", enable debug to see the complete stacktrace");
				pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
				result = false;
			} catch (Exception e) {
				pluginInterface.getLogger()
						.warning("crashed during restore of " + regionInterface.getName());
				pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
				result = false;
			}
			completableFuture.complete(result);
		});
		return completableFuture;
	}

	@Override
	public CompletableFuture<Boolean> saveRegionBlocksAsync(File file, GeneralRegionInterface regionInterface) {
		ClipboardFormat format = BuiltInClipboardFormat.FAST;

		file = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
		pluginInterface.debugI("Trying to save region", regionInterface.getName(), " to file",
				file.getAbsolutePath(), "with format", format.getName());
		com.sk89q.worldedit.world.World world = null;
		if (regionInterface.getWorld() != null) {
			world = BukkitAdapter.adapt(regionInterface.getWorld());
		}
		if (world == null) {
			pluginInterface.getLogger().warning(
					"Did not save region " + regionInterface.getName() + ", world not found: "
							+ regionInterface.getWorldName());
			return CompletableFuture.completedFuture(false);
		}
		final int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
		EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
				.world(world)
				.relightMode(RelightMode.OPTIMAL)
				.maxBlocks(maxBlocks)
				.build();
		final ProtectedRegion region = regionInterface.getRegion();
		final BlockVector3 min = region.getMinimumPoint();
		final BlockVector3 max = region.getMaximumPoint();
		final String regionName = regionInterface.getName();
		final World finalWorld = world;
		final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
		final File finalFile = file;
		FaweAPI.getTaskManager().async(() -> {
			// Create a clipboard
			CuboidRegion selection =
					new CuboidRegion(finalWorld, min, max);
			Clipboard clipboard = new MemoryOptimizedClipboard(selection);
			clipboard.setOrigin(min);
			ForwardExtentCopy copy = new ForwardExtentCopy(editSession, selection.clone(), clipboard, min);
			copy.setCopyingEntities(false);
			try(editSession) {
				Operations.completeLegacy(copy);
			} catch (MaxChangedBlocksException e) {
				pluginInterface.getLogger().warning(
						"Exceeded the block limit while saving schematic of " + regionName
								+ ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: "
								+ pluginInterface.getConfig().getInt("maximumBlocks"));
				completableFuture.complete(false);
				return;
			} catch (FaweException e) {
				pluginInterface.getLogger().warning(
						"Exceeded the block limit while saving schematic of " + regionName
								+ ", limit passed by AreaShop: "
								+ pluginInterface.getConfig().getInt("maximumBlocks"));
				completableFuture.complete(false);
				return;
			}

			try (Closer closer = Closer.create()) {
				FileOutputStream fos = closer.register(new FileOutputStream(finalFile));
				BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
				ClipboardWriter writer = closer.register(format.getWriter(bos));
				writer.write(clipboard);
			} catch (IOException e) {
				pluginInterface.getLogger().warning(
						"An error occurred while saving schematic of " + regionName
								+ ", enable debug to see the complete stacktrace");
				pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
				completableFuture.complete(false);
			} catch (Exception e) {
				pluginInterface.getLogger()
						.warning("Crashed during save of " + regionName);
				pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
				completableFuture.complete(false);
				return;
			}
			completableFuture.complete(true);
		});
		return completableFuture;
	}

	@Override
	public boolean restoreRegionBlocks(File rawFile, GeneralRegionInterface regionInterface) {
		File file = null;
		for (ClipboardFormat formatOption : BuiltInClipboardFormat.values()) {
			if (new File(rawFile.getAbsolutePath() + "." + formatOption.getPrimaryFileExtension())
					.exists()) {
				file = new File(
						rawFile.getAbsolutePath() + "." + formatOption.getPrimaryFileExtension());
				break;
			}
		}
		if (file == null) {
			pluginInterface.getLogger().info("Did not restore region " + regionInterface.getName()
					+ ", schematic file does not exist: " + rawFile.getAbsolutePath());
			return false;
		}
		ClipboardFormat format = ClipboardFormats.findByFile(file);
		if (format == null) {
			pluginInterface.debugI("Did not restore region " + regionInterface.getName() + ", could not find a valid clipboard format");
			return false;
		}
		pluginInterface.debugI("Trying to restore region", regionInterface.getName(), " from file",
				file.getAbsolutePath(), "with format", format.getName());

		com.sk89q.worldedit.world.World world = null;
		if (regionInterface.getName() != null) {
			world = BukkitAdapter.adapt(regionInterface.getWorld());
		}
		if (world == null) {
			pluginInterface.getLogger().info(
					"Did not restore region " + regionInterface.getName() + ", world not found: "
							+ regionInterface.getWorldName());
			return false;
		}
		final int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
		EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit()
				.newEditSessionBuilder()
				.world(world)
				.maxBlocks(maxBlocks)
				.build();
		editSession.setReorderMode(EditSession.ReorderMode.MULTI_STAGE);
		ProtectedRegion region = regionInterface.getRegion();
		// Get the origin and size of the region
		BlockVector3 origin = BlockVector3
				.at(region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(),
						region.getMinimumPoint().getBlockZ());


		// Read the schematic and paste it into the world
		try(Closer closer = Closer.create(); editSession) {
			final FileInputStream fis = closer.register(new FileInputStream(file));
			final ClipboardReader reader = format.getReader(fis);
			final Clipboard clipboard = reader.read();
			if (clipboard.getDimensions().getY() != regionInterface.getHeight()
					|| clipboard.getDimensions().getX() != regionInterface.getWidth()
					|| clipboard.getDimensions().getZ() != regionInterface.getDepth()) {
				pluginInterface.getLogger().warning(
						"Size of the region " + regionInterface.getName()
								+ " is not the same as the schematic to restore!");
				pluginInterface.debugI(
						"schematic|region, x:" + clipboard.getDimensions().getX() + "|"
								+ regionInterface.getWidth() + ", y:" + clipboard.getDimensions().getY()
								+ "|" + regionInterface.getHeight() + ", z:" + clipboard.getDimensions()
								.getZ() + "|" + regionInterface.getDepth());
			}
			clipboard.setOrigin(clipboard.getMinimumPoint());
			ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
			LocalSession session =
					new LocalSession(pluginInterface.getWorldEdit().getLocalConfiguration());
			session.setBlockChangeLimit(pluginInterface.getConfig().getInt("maximumBlocks"));
			session.setClipboard(clipboardHolder);

			// Build operation

			BlockTransformExtent extent = new BlockTransformExtent(clipboard);
			ForwardExtentCopy copy =
					new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(),
							editSession, origin);
			copy.setTransform(clipboardHolder.getTransform());
			copy.setCopyingEntities(false);
			// Mask to region (for polygon and other weird shaped regions)
			// FIXME make this more efficient (especially for polygon regions)
			if (region.getType() != RegionType.CUBOID) {
				copy.setSourceMask(new RegionBoundMask(region));
			}
			Operations.completeLegacy(copy);
		} catch (MaxChangedBlocksException e) {
			pluginInterface.getLogger().warning(
					"exceeded the block limit while restoring schematic of " + regionInterface.getName()
							+ ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: "
							+ pluginInterface.getConfig().getInt("maximumBlocks"));
			return false;
		} catch (FaweException ex) {
			pluginInterface.getLogger().warning(
					"exceeded the block entity limit while restoring schematic of " + regionInterface.getName()
							+ ", limit passed by AreaShop: "
							+ pluginInterface.getConfig().getInt("maximumEntities"));
			return false;
		}
		catch (IOException e) {
			pluginInterface.getLogger().warning(
					"An error occurred while restoring schematic of " + regionInterface.getName()
							+ ", enable debug to see the complete stacktrace");
			pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
			return false;
		} catch (Exception e) {
			pluginInterface.getLogger()
					.warning("crashed during restore of " + regionInterface.getName());
			pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
			return false;
		}
		return true;
	}

	@Override
	public boolean saveRegionBlocks(File file, GeneralRegionInterface regionInterface) {
		ClipboardFormat format = BuiltInClipboardFormat.FAST;

		file = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
		pluginInterface.debugI("Trying to save region", regionInterface.getName(), " to file",
				file.getAbsolutePath(), "with format", format.getName());
		com.sk89q.worldedit.world.World world = null;
		if (regionInterface.getWorld() != null) {
			world = BukkitAdapter.adapt(regionInterface.getWorld());
		}
		if (world == null) {
			pluginInterface.getLogger().warning(
					"Did not save region " + regionInterface.getName() + ", world not found: "
							+ regionInterface.getWorldName());
			return false;
		}
		EditSession editSession =
				pluginInterface.getWorldEdit().getWorldEdit()
						.newEditSessionBuilder()
						.world(world)
						.maxBlocks(pluginInterface.getConfig().getInt("maximumBlocks"))
						.build();

		// Create a clipboard
		CuboidRegion selection =
				new CuboidRegion(world, regionInterface.getRegion().getMinimumPoint(),
						regionInterface.getRegion().getMaximumPoint());
		BlockArrayClipboard clipboard = new BlockArrayClipboard(selection);
		clipboard.setOrigin(regionInterface.getRegion().getMinimumPoint());
		ForwardExtentCopy copy = new ForwardExtentCopy(editSession,
				new CuboidRegion(world, regionInterface.getRegion().getMinimumPoint(),
						regionInterface.getRegion().getMaximumPoint()), clipboard,
				regionInterface.getRegion().getMinimumPoint());
		try(editSession) {
			Operations.completeLegacy(copy);
		} catch (MaxChangedBlocksException e) {
			pluginInterface.getLogger().warning(
					"Exceeded the block limit while saving schematic of " + regionInterface.getName()
							+ ", limit in exception: " + e.getBlockLimit() + ", limit passed by AreaShop: "
							+ pluginInterface.getConfig().getInt("maximumBlocks"));
			return false;
		}

		try (Closer closer = Closer.create()) {
			FileOutputStream fos = closer.register(new FileOutputStream(file));
			BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
			ClipboardWriter writer = closer.register(format.getWriter(bos));
			writer.write(clipboard);
		} catch (IOException e) {
			pluginInterface.getLogger().warning(
					"An error occurred while saving schematic of " + regionInterface.getName()
							+ ", enable debug to see the complete stacktrace");
			pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
			return false;
		} catch (Exception e) {
			pluginInterface.getLogger()
					.warning("Crashed during save of " + regionInterface.getName());
			pluginInterface.debugI(ExceptionUtils.getStackTrace(e));
			return false;
		}
		return true;
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
}
