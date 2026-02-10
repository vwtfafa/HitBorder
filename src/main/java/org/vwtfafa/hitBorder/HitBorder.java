package org.vwtfafa.hitBorder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.vwtfafa.hitBorder.command.HitBorderCommand;
import org.vwtfafa.hitBorder.command.HitBorderTabCompleter;
import org.vwtfafa.hitBorder.config.ConfigManager;
import org.vwtfafa.hitBorder.listener.BlockBreakListener;
import org.vwtfafa.hitBorder.listener.PlayerDamageListener;
import org.vwtfafa.hitBorder.listener.PlayerSpawnListener;
import org.vwtfafa.hitBorder.util.LuckPermsHook;
import org.vwtfafa.hitBorder.util.UpdateChecker;

import java.util.Objects;
import java.util.logging.Level;

public final class HitBorder extends JavaPlugin {
    private static HitBorder instance;
    private ConfigManager configManager;
    private PlayerDamageListener damageListener;
    private BlockBreakListener blockBreakListener;
    private PlayerSpawnListener spawnListener;
    private LuckPermsHook luckPermsHook;
    private static final String GITHUB_REPO = "vwtfafa/HitBorder";
    private boolean isEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        try {
            // Initialize config manager
            this.configManager = new ConfigManager(this);
            
            // Initialize integrations
            this.luckPermsHook = new LuckPermsHook();
            if (luckPermsHook.isAvailable()) {
                getLogger().info("LuckPerms detected and linked.");
            }

            // Register commands
            Objects.requireNonNull(getCommand("hitborder"), "Failed to register commands. Check plugin.yml")
                .setExecutor(new HitBorderCommand(this));
            Objects.requireNonNull(getCommand("hitborder"), "Failed to register commands. Check plugin.yml")
                .setTabCompleter(new HitBorderTabCompleter());
            
            // Register event listeners
            this.damageListener = new PlayerDamageListener(this);
            this.blockBreakListener = new BlockBreakListener(this);
            this.spawnListener = new PlayerSpawnListener(this);
            
            getServer().getPluginManager().registerEvents(damageListener, this);
            getServer().getPluginManager().registerEvents(blockBreakListener, this);
            getServer().getPluginManager().registerEvents(spawnListener, this);
            
            // Initialize bStats metrics
            if (getConfig().getBoolean("metrics.enabled", true)) {
                int bstatsId = getConfig().getInt("metrics.bstats-id", 29463);
                if (bstatsId > 0) {
                    Metrics metrics = new Metrics(this, bstatsId);
                    metrics.addCustomChart(new SimplePie("hardcore_mode", () ->
                            configManager.isHardcoreMode() ? "enabled" : "disabled"));
                } else {
                    getLogger().warning("metrics.bstats-id must be > 0 to enable bStats");
                }
            }

            // Set up update checker if enabled in config
            if (getConfig().getBoolean("update-checker.enabled", true)) {
                String repo = getConfig().getString("update-checker.github-repo", GITHUB_REPO);
                new UpdateChecker(this, repo).checkForUpdates();
            }
            
            getLogger().info("HitBorder v" + getDescription().getVersion() + " has been enabled!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to enable HitBorder: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Stacktrace:", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("HitBorder has been disabled!");
    }
    
    public static HitBorder getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public void reloadPlugin() {
        reloadConfig();
        if (configManager != null) configManager.reload();

        // Unregister previous listeners to avoid duplicate handling
        try {
            if (damageListener != null) org.bukkit.event.HandlerList.unregisterAll(damageListener);
            if (blockBreakListener != null) org.bukkit.event.HandlerList.unregisterAll(blockBreakListener);
            if (spawnListener != null) org.bukkit.event.HandlerList.unregisterAll(spawnListener);
        } catch (Exception e) {
            getLogger().warning("Failed to unregister old listeners: " + e.getMessage());
        }

        // Re-initialize listeners to apply new config and register them
        damageListener = new PlayerDamageListener(this);
        blockBreakListener = new BlockBreakListener(this);
        spawnListener = new PlayerSpawnListener(this);
        getServer().getPluginManager().registerEvents(damageListener, this);
        getServer().getPluginManager().registerEvents(blockBreakListener, this);
        getServer().getPluginManager().registerEvents(spawnListener, this);

        getLogger().info("HitBorder configuration reloaded!");
    }
    
    
    public BlockBreakListener getBlockBreakListener() {
        return blockBreakListener;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }
}
