package me.wiefferink.areashop.features.signs;

import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.events.notify.UpdateRegionEvent;
import me.wiefferink.areashop.features.RegionFeature;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

public class SignsFeature extends RegionFeature {

	private static final SignListener signListener = new SignListener(AreaShop.getInstance()
			.getSignManager());
	private final SignManager signManager = new SignManager();

	@Deprecated
	public SignsFeature() {
		Bukkit.getServer().getPluginManager().registerEvents(signListener, AreaShop.getInstance());
	}

	/**
	 * Constructor.
	 * @param region The region to bind to
	 */
	public SignsFeature(GeneralRegion region) {
		setRegion(region);
		// Setup current signs
		ConfigurationSection signSection = region.getConfig().getConfigurationSection("general.signs");
		if(signSection != null) {
			for(String signKey : signSection.getKeys(false)) {
				RegionSign sign = new RegionSign(this, signKey);
				Location location = sign.getLocation();
				if(location == null) {
					AreaShop.warn("Sign with key " + signKey + " of region " + region.getName() + " does not have a proper location");
					continue;
				}
				plugin.getSignManager().cacheForWorld(location.getWorld()).addSign(sign);
				this.signManager.addSign(sign);
			}
		}
	}


	public static void shutdownGlobalState() {
		HandlerList.unregisterAll(signListener);
	}

	/**
	 * Convert a location to a string to use as map key.
	 * @param location The location to get the key for
	 * @return A string to use in a map for a location
	 */
	public static String locationToString(Location location) {
		return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
	}

	/**
	 * Convert a chunk to a string to use as map key.
	 * @param location The location to get the key for
	 * @return A string to use in a map for a chunk
	 */
	public static String chunkToString(Location location) {
		return location.getWorld().getName() + ";" + (location.getBlockX() >> 4) + ";" + (location.getBlockZ() >> 4);
	}

	@Override
	public void shutdown() {

	}

	public SignManager signManager() {
		return this.signManager;
	}

	@Deprecated
	public boolean needsPeriodicUpdate() {
		return this.signManager.needsPeriodicUpdate();
	}

	@Deprecated
	public boolean update() {
		return this.signManager.update();
	}

	@EventHandler
	public void regionUpdate(UpdateRegionEvent event) {
		if (event.getRegion() == this.getRegion()) {
			this.signManager.update();
		}
	}

	/**
	 * Add a sign to this region.
	 * @param location The location of the sign
	 * @param signType The type of the sign (WALL_SIGN or SIGN_POST)
	 * @param facing   The orientation of the sign
	 * @param profile  The profile to use with this sign (null for default)
	 */
	public void addSign(Location location, Material signType, BlockFace facing, String profile) {
		int i = 0;
		while(getRegion().getConfig().isSet("general.signs." + i)) {
			i++;
		}
		String signPath = "general.signs." + i + ".";
		getRegion().setSetting(signPath + "location", Utils.locationToConfig(location));
		getRegion().setSetting(signPath + "facing", facing != null ? facing.name() : null);
		getRegion().setSetting(signPath + "signType", signType != null ? signType.name() : null);
		if(profile != null && !profile.isEmpty()) {
			getRegion().setSetting(signPath + "profile", profile);
		}
		// Add to the map
		RegionSign regionSign = new RegionSign(this, String.valueOf(i));
		plugin.getSignManager().addSign(regionSign);
		this.signManager.addSign(regionSign);
	}

}
