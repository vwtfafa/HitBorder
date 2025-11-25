package org.vwtfafa.hitBorder.listener;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.vwtfafa.hitBorder.HitBorder;
import org.vwtfafa.hitBorder.config.ConfigManager;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerDamageListener implements Listener {
    private final HitBorder plugin;
    private final ConfigManager configManager;
    private Set<EntityDamageEvent.DamageCause> allowedDamageCauses;

    public PlayerDamageListener(HitBorder plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        loadDamageCauses();
    }

    private void loadDamageCauses() {
        try {
            List<String> damageCauseNames = plugin.getConfig().getStringList("game.damage-types");
            this.allowedDamageCauses = damageCauseNames.stream()
                    .map(String::toUpperCase)
                    .map(this::parseDamageCause)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (allowedDamageCauses.isEmpty()) {
                plugin.getLogger().warning("No valid damage types configured. Using default damage types.");
                allowedDamageCauses = Set.of(
                        EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                        EntityDamageEvent.DamageCause.PROJECTILE,
                        EntityDamageEvent.DamageCause.FALL,
                        EntityDamageEvent.DamageCause.FIRE,
                        EntityDamageEvent.DamageCause.LAVA
                );
            }

            plugin.getLogger().info("Loaded " + allowedDamageCauses.size() + " damage types that trigger border growth");
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading damage types: " + e.getMessage());
            allowedDamageCauses = Set.of(EntityDamageEvent.DamageCause.values());
        }
    }

    private EntityDamageEvent.DamageCause parseDamageCause(String name) {
        try {
            return EntityDamageEvent.DamageCause.valueOf(name);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid damage cause in config: " + name);
            return null;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        // Only process player damage
        if (event.getEntityType() != EntityType.PLAYER || !configManager.isEnabled()) {
            return;
        }

        Player player = (Player) event.getEntity();
        World world = player.getWorld();

        // Check if damage cause is allowed
        if (!allowedDamageCauses.contains(event.getCause())) {
            if (plugin.getConfig().getBoolean("debug.log-damage-events", false)) {
                plugin.getLogger().info(String.format(
                        "Damage event %s from %s ignored - not in allowed damage types",
                        event.getCause(),
                        player.getName()
                ));
            }
            return;
        }

        // Skip if not the configured world
        String targetWorld = configManager.getWorldName();
        if (!world.getName().equals(targetWorld)) {
            if (plugin.getConfig().getBoolean("debug.log-damage-events", false)) {
                plugin.getLogger().info(String.format(
                        "Damage event in world %s ignored - expected %s",
                        world.getName(),
                        targetWorld
                ));
            }
            return;
        }

        // Skip if player has bypass permission (except in hardcore mode)
        if (player.hasPermission("hitborder.bypass") && !configManager.isHardcoreMode()) {
            if (plugin.getConfig().getBoolean("debug.log-damage-events", false)) {
                plugin.getLogger().info(String.format(
                        "Damage event for %s ignored - has bypass permission",
                        player.getName()
                ));
            }
            return;
        }

        WorldBorder border = world.getWorldBorder();
        double currentSize = border.getSize();
        double growAmount = configManager.getBorderGrowAmount() * 2; // Convert to diameter
        double newSize = currentSize + growAmount;
        double maxSize = configManager.getMaxBorderSize() * 2; // Convert to diameter

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.log-border-changes", false)) {
            plugin.getLogger().info(String.format(
                    "Processing damage event: player=%s, cause=%s, currentSize=%.1f, growAmount=%.1f, newSize=%.1f, maxSize=%.1f",
                    player.getName(),
                    event.getCause(),
                    currentSize / 2,
                    growAmount / 2,
                    newSize / 2,
                    maxSize / 2
            ));
        }

        // Ensure border doesn't exceed maximum size
        boolean atMaxSize = false;
        if (newSize >= maxSize) {
            newSize = maxSize;
            atMaxSize = true;

            // Kill player in hardcore mode if border reaches maximum size
            if (configManager.isHardcoreMode()) {
                player.setHealth(0);

                // Broadcast death message
                String deathMessage = configManager.getMessage("hardcore-death");
                if (deathMessage != null && !deathMessage.isEmpty()) {
                    deathMessage = ChatColor.translateAlternateColorCodes('&',
                                    configManager.getMessage("prefix") + deathMessage)
                            .replace("%player%", player.getName())
                            .replace("%size%", String.format("%.1f", newSize / 2));

                    String finalDeathMessage = deathMessage;
                    world.getPlayers().stream()
                            .filter(p -> p.hasPermission("hitborder.notify"))
                            .forEach(p -> p.sendMessage(finalDeathMessage));
                }
            }

            // Don't grow border if already at max size
            if (Math.abs(currentSize - maxSize) < 0.1) {
                if (plugin.getConfig().getBoolean("debug.log-border-changes", false)) {
                    plugin.getLogger().info("Border already at maximum size, not growing further");
                }
                return;
            }
        }

        // Store the final size for use in lambda
        final double finalNewSize = newSize;

        // Apply new border size with smooth transition
        int growTime = configManager.getBorderGrowTime();
        border.setSize(finalNewSize, growTime);

        // Notify players with permission
        String message = configManager.getMessage("border-grow");
        if (message != null && !message.isEmpty()) {
            final String finalMessage = ChatColor.translateAlternateColorCodes('&',
                            configManager.getMessage("prefix") + message)
                    .replace("%size%", String.format("%.1f", finalNewSize / 2));

            world.getPlayers().stream()
                    .filter(p -> p.hasPermission("hitborder.notify"))
                    .forEach(p -> p.sendMessage(finalMessage));
        }

        // Debug logging
        if (plugin.getConfig().getBoolean("debug.log-border-changes", false)) {
            plugin.getLogger().info(String.format(
                    "Border growing from %.1f to %.1f (radius) over %d seconds",
                    currentSize / 2,
                    finalNewSize / 2,
                    growTime
            ));
        }
    }

    public void reload() {
        loadDamageCauses();
    }
}