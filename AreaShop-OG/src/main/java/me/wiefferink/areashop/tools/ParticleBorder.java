// Adapted from the ParticleBorder minifeature of AdvancedRegionMarket
// (https://github.com/alex9849/advanced-region-market), Copyright alex9849,
// licensed under the Apache License, Version 2.0:
//     http://www.apache.org/licenses/LICENSE-2.0
// Modifications for AreaShop: takes a plugin instance instead of using the ARM
// singleton, outlines cuboids only (polygonal regions use their bounding box),
// always uses the purple witch particle effect and restarts cleanly when recreated.
package me.wiefferink.areashop.tools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

// Shows the edges of a cuboid region to a single player with purple particles.
public class ParticleBorder {

    private final Plugin plugin;
    private final Player player;
    private final World world;
    private final Vector min;
    private final Vector max;
    private Integer taskID;
    private Integer cancelerID;

    // Create a particle border for a cuboid volume that only the given player will
    // see.
    public ParticleBorder(Plugin plugin, Player player, World world, Vector pos1, Vector pos2) {

        this.plugin = plugin;
        this.player = player;
        this.world = world;
        this.min = Vector.getMinimum(pos1, pos2);
        this.max = Vector.getMaximum(pos1, pos2);

    }

    private List<Location> getCuboidParticleLocations() {

        List<Location> particleLoc = new ArrayList<>();

        for (int i = min.getBlockX(); i <= max.getBlockX(); i++) {

            particleLoc.add(new Location(world, i, min.getBlockY(), min.getBlockZ()));
            particleLoc.add(new Location(world, i, min.getBlockY(), max.getBlockZ()));
            particleLoc.add(new Location(world, i, max.getBlockY(), min.getBlockZ()));
            particleLoc.add(new Location(world, i, max.getBlockY(), max.getBlockZ()));

        }

        for (int i = min.getBlockY(); i <= max.getBlockY(); i++) {

            particleLoc.add(new Location(world, min.getBlockX(), i, min.getBlockZ()));
            particleLoc.add(new Location(world, min.getBlockX(), i, max.getBlockZ()));
            particleLoc.add(new Location(world, max.getBlockX(), i, min.getBlockZ()));
            particleLoc.add(new Location(world, max.getBlockX(), i, max.getBlockZ()));

        }

        for (int i = min.getBlockZ(); i <= max.getBlockZ(); i++) {

            particleLoc.add(new Location(world, min.getBlockX(), min.getBlockY(), i));
            particleLoc.add(new Location(world, min.getBlockX(), max.getBlockY(), i));
            particleLoc.add(new Location(world, max.getBlockX(), min.getBlockY(), i));
            particleLoc.add(new Location(world, max.getBlockX(), max.getBlockY(), i));

        }

        return particleLoc;

    }

    // Start showing the border to the player for the given number of ticks.
    public void createParticleBorder(int ticks) {

        removeBorder();

        final List<Location> particleSpawnPoints = getCuboidParticleLocations();
        this.taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

            if (!player.isOnline() || !player.getWorld().equals(world)) {

                return;

            }

            for (Location location : particleSpawnPoints) {

                player.spawnParticle(Particle.SPELL_WITCH, location.getX() + 0.5, location.getY() + 0.5,
                        location.getZ() + 0.5, 6, 0, 0, 0);

            }

        }, 0, 20);
        this.cancelerID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

            if (taskID != null) {

                Bukkit.getScheduler().cancelTask(taskID);

            }

            cancelerID = null;
            taskID = null;

        }, ticks);

    }

    // Stop showing the border.
    public void removeBorder() {

        if (taskID == null || cancelerID == null) {

            return;

        }

        Bukkit.getScheduler().cancelTask(cancelerID);
        Bukkit.getScheduler().cancelTask(taskID);
        cancelerID = null;
        taskID = null;

    }

}
