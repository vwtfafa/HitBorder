package org.vwtfafa.hitBorder.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.vwtfafa.hitBorder.HitBorder;
import org.vwtfafa.hitBorder.config.ConfigManager;

public class PlayerSpawnListener implements Listener {
    private final HitBorder plugin;
    private final ConfigManager configManager;

    public PlayerSpawnListener(HitBorder plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!configManager.isEnabled()) {
            return;
        }

        World world = player.getWorld();
        if (!world.getName().equals(configManager.getWorldName())) {
            return;
        }

        ensureInsideBorder(player, player.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        Location respawnLocation = event.getRespawnLocation();
        World world = respawnLocation.getWorld();
        if (world == null || !world.getName().equals(configManager.getWorldName())) {
            return;
        }

        Location safeLocation = getSafeSpawnLocation(world, respawnLocation);
        if (!safeLocation.equals(respawnLocation)) {
            event.setRespawnLocation(safeLocation);
        }
    }

    private void ensureInsideBorder(Player player, Location currentLocation) {
        World world = currentLocation.getWorld();
        if (world == null) {
            return;
        }

        WorldBorder border = world.getWorldBorder();
        if (border != null && !border.isInside(currentLocation)) {
            Location safeLocation = getSafeSpawnLocation(world, currentLocation);
            player.teleport(safeLocation);
        }
    }

    private Location getSafeSpawnLocation(World world, Location fallback) {
        Location baseSpawn = configManager.getSpawnLocation(world);
        WorldBorder border = world.getWorldBorder();
        if (border == null) {
            return baseSpawn;
        }

        Location chosen = border.isInside(baseSpawn) ? baseSpawn : border.getCenter();
        int safeY = world.getHighestBlockYAt(chosen.getBlockX(), chosen.getBlockZ()) + 1;
        return new Location(world, chosen.getBlockX() + 0.5, safeY, chosen.getBlockZ() + 0.5);
    }
}
