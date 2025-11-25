package org.vwtfafa.hitBorder.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.vwtfafa.hitBorder.HitBorder;
import org.vwtfafa.hitBorder.config.ConfigManager;

public class BlockBreakListener implements Listener {
    private final HitBorder plugin;
    private final ConfigManager configManager;
    private Location spawnLocation;
    private int spawnProtectionRadius;
    private boolean spawnProtectionEnabled;

    public BlockBreakListener(HitBorder plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        loadSpawnSettings();
    }

    private void loadSpawnSettings() {
        spawnProtectionRadius = plugin.getConfig().getInt("spawn.protection-radius", 8); // 8 blocks = 1/2 chunk radius
        spawnProtectionEnabled = true; // Always enable spawn protection
        
        String worldName = configManager.getWorldName();
        World world = plugin.getServer().getWorld(worldName);
        if (world != null) {
            // Get chunk-aligned spawn coordinates
            int centerX = plugin.getConfig().getInt("spawn.x", 8);
            int centerY = plugin.getConfig().getInt("spawn.y", 100);
            int centerZ = plugin.getConfig().getInt("spawn.z", 8);
            
            // Ensure spawn is at the center of a chunk
            spawnLocation = new Location(world, centerX, centerY, centerZ);
            
            plugin.getLogger().info(String.format("Spawn protection enabled at chunk [%d, %d] with radius %d blocks", 
                spawnLocation.getChunk().getX(), 
                spawnLocation.getChunk().getZ(),
                spawnProtectionRadius));
        } else {
            plugin.getLogger().warning("Failed to enable spawn protection - world not found: " + worldName);
            spawnProtectionEnabled = false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!spawnProtectionEnabled || !configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Allow breaking blocks in the spawn area
        if (isInSpawnArea(block.getLocation())) {
            event.setCancelled(false);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!spawnProtectionEnabled || !configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Allow placing blocks in the spawn area
        if (isInSpawnArea(block.getLocation())) {
            event.setCancelled(false);
        }
    }

    private boolean isInSpawnArea(Location location) {
        if (!spawnProtectionEnabled || spawnLocation == null || !location.getWorld().equals(spawnLocation.getWorld())) {
            return false;
        }
        
        // Check if location is within the protected chunk
        return Math.abs(location.getBlockX() - spawnLocation.getBlockX()) <= spawnProtectionRadius &&
               Math.abs(location.getBlockZ() - spawnLocation.getBlockZ()) <= spawnProtectionRadius;
    }

    public void reload() {
        loadSpawnSettings();
    }
}
