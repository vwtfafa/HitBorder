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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Sound;
import java.util.stream.Collectors;

public class PlayerDamageListener implements Listener {
    private final HitBorder plugin;
    private final ConfigManager configManager;
    private Set<EntityDamageEvent.DamageCause> allowedDamageCauses;
    private final ConcurrentMap<UUID, Long> lastGrowthByPlayer = new ConcurrentHashMap<>();

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

        // Skip if ops should not affect the border
        if (!configManager.isAffectOps() && player.isOp()) {
            if (plugin.getConfig().getBoolean("debug.log-damage-events", false)) {
                plugin.getLogger().info(String.format(
                        "Damage event for %s ignored - ops do not affect border growth",
                        player.getName()
                ));
            }
            return;
        }

        int growthCooldownSeconds = configManager.getGrowthCooldown();
        if (growthCooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            long lastGrowth = lastGrowthByPlayer.getOrDefault(player.getUniqueId(), 0L);
            long elapsedMillis = now - lastGrowth;
            if (elapsedMillis < growthCooldownSeconds * 1000L) {
                if (plugin.getConfig().getBoolean("debug.log-damage-events", false)) {
                    plugin.getLogger().info(String.format(
                            "Damage event for %s ignored - cooldown active (%.2fs remaining)",
                            player.getName(),
                            (growthCooldownSeconds * 1000L - elapsedMillis) / 1000.0
                    ));
                }
                return;
            }
        }

        WorldBorder border = world.getWorldBorder();
        double currentSize = border.getSize();
        double maxSize = configManager.getMaxBorderSize() * 2; // Convert to diameter
        boolean atMaxSize = currentSize >= maxSize - 0.1;
        if (atMaxSize) {
            if (configManager.isHardcoreMode()) {
                killForHardcore(player, world, maxSize / 2);
            }
            return;
        }

        double finalDamage = event.getFinalDamage();
        if (finalDamage <= 0) {
            return;
        }
        int halfHearts = Math.max(1, (int) Math.ceil(finalDamage));
        double growAmount = configManager.getBorderGrowAmount() * 2 * halfHearts; // Convert to diameter
        double newSize = currentSize + growAmount;

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
        atMaxSize = false;
        if (newSize >= maxSize) {
            newSize = maxSize;
            atMaxSize = true;

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
        lastGrowthByPlayer.put(player.getUniqueId(), System.currentTimeMillis());

        // Notify players with permission (chat + optional sound ping)
        String message = configManager.getMessage("border-grow");
        if (message != null && !message.isEmpty()) {
            final String finalMessage = ChatColor.translateAlternateColorCodes('&',
                            configManager.getMessage("prefix") + message)
                    .replace("%size%", String.format("%.1f", finalNewSize / 2));

            world.getPlayers().stream()
                    .filter(p -> p.hasPermission("hitborder.notify"))
                    .forEach(p -> {
                        p.sendMessage(finalMessage);
                        playNotificationSound(p);
                    });
        }

        if (atMaxSize) {
            String maxMessage = configManager.getMessage("border-max");
            if (maxMessage != null && !maxMessage.isEmpty()) {
                final String finalMaxMessage = ChatColor.translateAlternateColorCodes('&',
                                configManager.getMessage("prefix") + maxMessage)
                        .replace("%size%", String.format("%.1f", finalNewSize / 2));

                world.getPlayers().stream()
                        .filter(p -> p.hasPermission("hitborder.notify"))
                        .forEach(p -> {
                            p.sendMessage(finalMaxMessage);
                            playNotificationSound(p);
                        });
            }
            if (configManager.isHardcoreMode()) {
                killForHardcore(player, world, finalNewSize / 2);
            }
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

    private void killForHardcore(Player player, World world, double size) {
        player.setHealth(0);
        String deathMessage = configManager.getMessage("hardcore-death");
        if (deathMessage != null && !deathMessage.isEmpty()) {
            deathMessage = ChatColor.translateAlternateColorCodes('&',
                            configManager.getMessage("prefix") + deathMessage)
                    .replace("%player%", player.getName())
                    .replace("%size%", String.format("%.1f", size));

            String finalDeathMessage = deathMessage;
            world.getPlayers().stream()
                    .filter(p -> p.hasPermission("hitborder.notify"))
                    .forEach(p -> p.sendMessage(finalDeathMessage));
        }
    }

    private void playNotificationSound(Player player) {
        if (!plugin.getConfig().getBoolean("game.notification-sound.enabled", true)) {
            return;
        }
        String soundName = plugin.getConfig().getString("game.notification-sound.name", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) plugin.getConfig().getDouble("game.notification-sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("game.notification-sound.pitch", 1.2);
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(java.util.Locale.ROOT));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid notification sound configured: " + soundName);
        }
    }
}
