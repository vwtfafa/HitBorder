package org.vwtfafa.hitBorder.config;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigManager {
    public static final String CONFIG_VERSION = "1.0";
    private static final int MAX_WARNING_DISTANCE = 100;
    private static final int MIN_GROW_TIME = 1;
    private static final int MAX_GROW_TIME = 600; // 10 minutes max
    private static final double MIN_BORDER_SIZE = 1.0;
    private static final double MAX_BORDER_SIZE = 60000000.0; // Minecraft's limit
    
    private final JavaPlugin plugin;
    private final AtomicBoolean isReloading = new AtomicBoolean(false);
    
    private volatile double initialBorderSize;
    private volatile double borderGrowAmount;
    private volatile double minBorderSize;
    private volatile double maxBorderSize;
    private volatile int borderGrowTime;
    private volatile int growthCooldown;
    private volatile boolean hardcoreMode;
    private volatile boolean enabled;
    private volatile String worldName;
    private volatile boolean affectOps;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads the configuration from disk with validation and error handling.
     * This method is thread-safe and will prevent concurrent reloads.
     */
    public synchronized void loadConfig() {
        if (isReloading.getAndSet(true)) {
            plugin.getLogger().warning("Config reload already in progress");
            return;
        }

        try {
            // Save default config if it doesn't exist
            plugin.saveDefaultConfig();
            
            // Reload the config from disk
            plugin.reloadConfig();
            FileConfiguration config = plugin.getConfig();

            // Ensure config version is set
            if (!config.contains("config-version")) {
                config.set("config-version", CONFIG_VERSION);
                plugin.saveConfig();
            }
            
            // Check config version
            String configVersion = config.getString("config-version");
            if (!CONFIG_VERSION.equals(configVersion)) {
                plugin.getLogger().warning(String.format(
                    "Config version mismatch! Expected %s but found %s. Some settings may not work as expected.",
                    CONFIG_VERSION, configVersion
                ));
                handleConfigMigration(configVersion);
            }

            // Load and validate border settings
            double newMinSize = Math.max(MIN_BORDER_SIZE, Math.min(MAX_BORDER_SIZE, 
                config.getDouble("border.min-size", 10.0)));
            double newMaxSize = Math.max(newMinSize, Math.min(MAX_BORDER_SIZE, 
                config.getDouble("border.max-size", 1000.0)));
            double newInitialSize = Math.max(newMinSize, Math.min(newMaxSize, 
                config.getDouble("border.initial-size", 100.0)));
                
            // Load other settings
            double newGrowAmount = Math.max(0.1, 
                config.getDouble("border.grow-amount", 1.0));
            int newGrowTime = Math.max(MIN_GROW_TIME, 
                Math.min(MAX_GROW_TIME, config.getInt("border.grow-time", 5)));
            int newGrowthCooldown = Math.max(0, config.getInt("border.growth-cooldown", 10));
            boolean newHardcoreMode = config.getBoolean("game.hardcore", false);
            boolean newEnabled = config.getBoolean("enabled", true);
            boolean newAffectOps = config.getBoolean("game.affect-ops", true);

            // Validate world name
            String newWorldName = config.getString("world", "world").trim();
            if (newWorldName.isEmpty()) {
                throw new IllegalArgumentException("World name cannot be empty");
            }
            
            // Apply new values atomically
            synchronized (this) {
                minBorderSize = newMinSize;
                maxBorderSize = newMaxSize;
                initialBorderSize = newInitialSize;
                borderGrowAmount = newGrowAmount;
                borderGrowTime = newGrowTime;
                growthCooldown = newGrowthCooldown;
                affectOps = newAffectOps;
                hardcoreMode = newHardcoreMode;
                enabled = newEnabled;
                worldName = newWorldName;
            }

            // Initialize world border with the new settings
            if (!initializeWorldBorder() && plugin.getServer().getWorld(worldName) == null) {
                plugin.getLogger().warning(String.format(
                    "World '%s' is not loaded. The border will be initialized when the world loads.",
                    worldName
                ));
            }
            
            plugin.getLogger().info("Configuration loaded successfully");
        } catch (Exception e) {
            String errorMsg = "Error loading config: " + e.getMessage();
            plugin.getLogger().severe(errorMsg);
            
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                e.printStackTrace();
            }
            
            // Restore defaults and notify admins
            restoreDefaults();
            notifyAdmins("Configuration error: " + e.getMessage() + " - Using default settings.");
        } finally {
            isReloading.set(false);
        }
    }
    
    /**
     * Restores default configuration values and saves them to the config file.
     */
    private synchronized void restoreDefaults() {
        try {
            // Set default values
            minBorderSize = 10.0;
            maxBorderSize = 1000.0;
            initialBorderSize = 100.0;
            borderGrowAmount = 1.0;
            borderGrowTime = 5;
            growthCooldown = 10;
            hardcoreMode = false;
            enabled = true;
            worldName = "world";
            affectOps = true;

            // Update the config file with defaults
            FileConfiguration config = plugin.getConfig();
            config.set("border.min-size", minBorderSize);
            config.set("border.max-size", maxBorderSize);
            config.set("border.initial-size", initialBorderSize);
            config.set("border.grow-amount", borderGrowAmount);
            config.set("border.grow-time", borderGrowTime);
            config.set("border.growth-cooldown", growthCooldown);
            config.set("game.hardcore", hardcoreMode);
            config.set("enabled", enabled);
            config.set("world", worldName);
            config.set("game.affect-ops", affectOps);
            config.set("config-version", CONFIG_VERSION);
            
            plugin.saveConfig();
            plugin.getLogger().info("Restored default configuration");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to restore default config: " + e.getMessage());
        }
    }

    /**
     * Initializes the world border with the current settings.
     * @return true if the border was initialized successfully, false otherwise
     */
    private boolean initializeWorldBorder() {
        String currentWorldName;
        double currentMinSize, currentMaxSize, currentInitialSize;
        
        // Get a consistent snapshot of the current settings
        synchronized (this) {
            if (worldName == null || worldName.trim().isEmpty()) {
                plugin.getLogger().warning("World name is not configured");
                return false;
            }
            currentWorldName = worldName;
            currentMinSize = minBorderSize;
            currentMaxSize = maxBorderSize;
            currentInitialSize = initialBorderSize;
        }

        World world = Bukkit.getWorld(currentWorldName);
        if (world == null) {
            plugin.getLogger().warning(String.format("World '%s' not loaded. Border will be initialized when the world loads.", currentWorldName));
            return false;
        }

        try {
            WorldBorder border = world.getWorldBorder();
            if (border == null) {
                throw new IllegalStateException("Failed to get world border");
            }

            // Get warning distance with validation
            int warningDistance = Math.min(MAX_WARNING_DISTANCE, 
                Math.max(0, plugin.getConfig().getInt("border.warning-distance", 10)));
            
            // Ensure size is within bounds
            double size = Math.max(currentMinSize, Math.min(currentMaxSize, currentInitialSize));
            
            // Set border properties atomically
            border.setCenter(0, 0);
            border.setSize(size * 2, 0); // Convert radius to diameter for Minecraft
            border.setDamageAmount(0);
            border.setDamageBuffer(0);
            border.setWarningDistance(warningDistance);
            
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info(String.format(
                    "Initialized border in %s - Radius: %.1f, Diameter: %.1f, Warning: %d blocks",
                    currentWorldName, size, size * 2, warningDistance
                ));
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe(String.format(
                "Failed to initialize border in %s: %s", 
                currentWorldName, e.getMessage()
            ));
            
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                e.printStackTrace();
            }
            
            return false;
        }
    }

    /**
     * Reloads the configuration from disk.
     * This method is thread-safe and will prevent concurrent reloads.
     * @return true if reload was successful, false if a reload was already in progress
     */
    public boolean reload() {
        if (isReloading.get()) {
            return false;
        }
        
        try {
            loadConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload config: " + e.getMessage());
            return false;
        }
    }

    // Getters
    public double getInitialBorderSize() {
        return initialBorderSize;
    }

    public double getBorderGrowAmount() {
        return borderGrowAmount;
    }

    public int getBorderGrowTime() {
        return borderGrowTime;
    }

    public int getGrowthCooldown() {
        return growthCooldown;
    }

    public double getMinBorderSize() {
        return minBorderSize;
    }

    public double getMaxBorderSize() {
        return maxBorderSize;
    }

    public boolean isHardcoreMode() {
        return hardcoreMode;
    }

    public boolean isAffectOps() {
        return affectOps;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWorldName() {
        return worldName;
    }
    
    public String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Handles configuration migration between different versions.
     * @param oldVersion The version of the existing config
     */
    private void handleConfigMigration(String oldVersion) {
        try {
            plugin.getLogger().info(String.format("Migrating config from version %s to %s", 
                oldVersion, CONFIG_VERSION));
            
            // Example migration (uncomment and modify as needed):
            // if ("1.0".equals(oldVersion)) {
            //     FileConfiguration config = plugin.getConfig();
            //     // Perform migrations
            //     plugin.saveConfig();
            // }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to migrate config: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notifies all online administrators with the given message.
     * @param message The message to send
     */
    private void notifyAdmins(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // Format message with plugin prefix
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', 
            "&8[&cHitBorder&8] &7" + message);
        
        // Send to all online players with the hitborder.admin permission
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("hitborder.admin"))
            .forEach(player -> player.sendMessage(formattedMessage));
        
        // Also log to console
        plugin.getLogger().info(ChatColor.stripColor(formattedMessage));
    }
    
    /**
     * Enables or disables the plugin.
     * @param enabled Whether the plugin should be enabled
     */
    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            
            // Update config file asynchronously to avoid blocking
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    FileConfiguration config = plugin.getConfig();
                    config.set("enabled", enabled);
                    plugin.saveConfig();
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save enabled state: " + e.getMessage());
                }
            });
            
            // Notify about the state change
            String message = enabled ? "Plugin has been enabled" : "Plugin has been disabled";
            plugin.getLogger().info(message);
            notifyAdmins("\u00A7a" + message);
        }
    }
    
    /**
     * Sets the world border size with validation and safety checks.
     * @param world The world to update (must not be null)
     * @param size The new radius of the border (must be positive and within min/max bounds)
     * @throws IllegalArgumentException if world is null or size is invalid
     */
    /**
     * Sets the world border size with validation and safety checks.
     * @param world The world to update (must not be null)
     * @param newSize The new radius of the border (must be positive and within min/max bounds)
     * @throws IllegalArgumentException if world is null or size is invalid
     * @throws IllegalStateException if the world border cannot be modified
     */
    public void setBorderSize(@NotNull World world, double newSize) {
        Objects.requireNonNull(world, "World cannot be null");
        
        // Get current bounds to ensure thread safety
        double currentMinSize, currentMaxSize;
        int currentGrowTime;
        
        synchronized (this) {
            currentMinSize = minBorderSize;
            currentMaxSize = maxBorderSize;
            currentGrowTime = borderGrowTime;
        }
        
        // Validate the new size
        if (newSize < currentMinSize || newSize > currentMaxSize) {
            throw new IllegalArgumentException(String.format(
                "Border size must be between %.1f and %.1f (got %.1f)", 
                currentMinSize, currentMaxSize, newSize
            ));
        }
        
        WorldBorder border = world.getWorldBorder();
        if (border == null) {
            throw new IllegalStateException("World border not available for " + world.getName());
        }
        
        try {
            // Update the border size with the configured grow time
            border.setSize(newSize * 2, currentGrowTime);
            
            // Log the change if debug is enabled
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info(String.format(
                    "Set border size in %s to %.1f blocks (radius) over %d seconds", 
                    world.getName(), newSize, currentGrowTime
                ));
            }
            
            // Notify online admins about the change
            String message = String.format(
                "\u00A7eBorder size in %s is now \u00A76%.1f\u00A7e blocks (radius)",
                world.getName(), newSize
            );
            notifyAdmins(message);
            
        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to set border size in %s: %s", 
                world.getName(), e.getMessage()
            );
            plugin.getLogger().severe(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }
}
