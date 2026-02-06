package org.vwtfafa.hitBorder.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.vwtfafa.hitBorder.HitBorder;
import org.vwtfafa.hitBorder.config.ConfigManager;


public class HitBorderCommand implements CommandExecutor {
    private final HitBorder plugin;
    private final ConfigManager configManager;

    public HitBorderCommand(HitBorder plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                return handleReload(sender);
                
            case "toggle":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                return handleToggle(sender);
                
            case "setborder":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sendMessage(sender, "usage-setborder", label);
                    return true;
                }
                try {
                    double size = Double.parseDouble(args[1]);
                    return handleSetBorder(sender, size);
                } catch (NumberFormatException e) {
                    sendMessage(sender, "invalid-number");
                    return true;
                }

            case "set":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sendMessage(sender, "usage-set", label);
                    return true;
                }
                try {
                    double size = Double.parseDouble(args[1]);
                    return handleSetBorder(sender, size);
                } catch (NumberFormatException e) {
                    sendMessage(sender, "invalid-number");
                    return true;
                }

            case "grow":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    sendMessage(sender, "usage-grow", label);
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    return handleGrow(sender, amount);
                } catch (NumberFormatException e) {
                    sendMessage(sender, "invalid-number");
                    return true;
                }
                
            case "status":
                return handleStatus(sender);

            case "hardcore":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                return handleHardcore(sender, args);

            case "setspawn":
                if (!sender.hasPermission("hitborder.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                return handleSetSpawn(sender);

            case "version":
                return handleVersion(sender);
                
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        configManager.reload();
        sendMessage(sender, "reloaded");
        return true;
    }
    
    private boolean handleToggle(CommandSender sender) {
        boolean newState = !configManager.isEnabled();
        configManager.setEnabled(newState);
        
        String messageKey = newState ? "toggle-enabled" : "toggle-disabled";
        sendMessage(sender, messageKey);
        return true;
    }
    
    private boolean handleSetBorder(CommandSender sender, double size) {
        World world = Bukkit.getWorld(configManager.getWorldName());
        if (world == null) {
            sendMessage(sender, "world-not-found");
            return true;
        }
        
        // Validate size
        if (size < configManager.getMinBorderSize()) {
            sendMessage(sender, "border-too-small", String.valueOf(configManager.getMinBorderSize()));
            return true;
        }
        
        if (size > configManager.getMaxBorderSize()) {
            sendMessage(sender, "border-too-big", String.valueOf(configManager.getMaxBorderSize()));
            return true;
        }
        
        // Set the new border size
        configManager.setBorderSize(world, size);
        sendMessage(sender, "border-set", String.format("%.1f", size));
        
        // Notify all players
        String message = configManager.getMessage("border-set-broadcast")
            .replace("%size%", String.format("%.1f", size));
        if (sender instanceof Player) {
            message = message.replace("%player%", sender.getName());
        } else {
            message = message.replace("by %player%", "by Console");
        }
        
        String finalMessage = ChatColor.translateAlternateColorCodes('&', 
            configManager.getMessage("prefix") + message);
        
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(finalMessage));
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        World world = Bukkit.getWorld(configManager.getWorldName());
        if (world == null) {
            sendMessage(sender, "world-not-found");
            return true;
        }
        
        double currentSize = world.getWorldBorder().getSize() / 2; // Convert to radius
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "HitBorder Status" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Current border size: " + ChatColor.WHITE + String.format("%.1f blocks", currentSize));
        sender.sendMessage(ChatColor.YELLOW + "Min border size: " + ChatColor.WHITE + configManager.getMinBorderSize() + " blocks");
        sender.sendMessage(ChatColor.YELLOW + "Max border size: " + ChatColor.WHITE + configManager.getMaxBorderSize() + " blocks");
        sender.sendMessage(ChatColor.YELLOW + "Border growth per damage: " + ChatColor.WHITE + configManager.getBorderGrowAmount() + " blocks");
        sender.sendMessage(ChatColor.YELLOW + "Plugin enabled: " + (configManager.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.YELLOW + "Hardcore mode: " + (configManager.isHardcoreMode() ? ChatColor.RED + "Enabled" : ChatColor.GREEN + "Disabled"));
        
        return true;
    }

    private boolean handleGrow(CommandSender sender, double amount) {
        World world = Bukkit.getWorld(configManager.getWorldName());
        if (world == null) {
            sendMessage(sender, "world-not-found");
            return true;
        }

        double currentSize = world.getWorldBorder().getSize() / 2;
        double newSize = currentSize + amount;

        if (newSize < configManager.getMinBorderSize()) {
            sendMessage(sender, "border-too-small", String.valueOf(configManager.getMinBorderSize()));
            return true;
        }

        if (newSize > configManager.getMaxBorderSize()) {
            sendMessage(sender, "border-too-big", String.valueOf(configManager.getMaxBorderSize()));
            return true;
        }

        configManager.setBorderSize(world, newSize);
        sendMessage(sender, "border-grew", String.format("%.1f", amount), String.format("%.1f", newSize));
        return true;
    }

    private boolean handleHardcore(CommandSender sender, String[] args) {
        boolean newState;
        if (args.length < 2) {
            newState = !configManager.isHardcoreMode();
        } else {
            String value = args[1].toLowerCase();
            if ("on".equals(value) || "true".equals(value) || "enable".equals(value)) {
                newState = true;
            } else if ("off".equals(value) || "false".equals(value) || "disable".equals(value)) {
                newState = false;
            } else {
                sendMessage(sender, "usage-hardcore", "hitborder");
                return true;
            }
        }

        configManager.setHardcoreMode(newState);
        sendMessage(sender, newState ? "hardcore-enabled" : "hardcore-disabled");
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        configManager.setSpawnLocation(player.getLocation());
        plugin.getBlockBreakListener().reload();
        sendMessage(sender, "spawn-set");
        return true;
    }

    private boolean handleVersion(CommandSender sender) {
        sendMessage(sender, "version", plugin.getDescription().getVersion());
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        java.util.List<String> helpMessages = new java.util.ArrayList<>();
        helpMessages.add("&6=== &eHitBorder Commands &6===");
        helpMessages.add("&e/hitborder help &7- Show this help message");
        helpMessages.add("&e/hitborder status &7- Show current border status");
        helpMessages.add("&e/hitborder version &7- Show plugin version");

        if (sender.hasPermission("hitborder.admin")) {
            helpMessages.add("&6=== &eAdmin Commands &6===");
            helpMessages.add("&e/hitborder reload &7- Reload configuration");
            helpMessages.add("&e/hitborder toggle &7- Toggle the border growth");
            helpMessages.add("&e/hitborder setborder <size> &7- Set border size");
            helpMessages.add("&e/hitborder set <size> &7- Set border size");
            helpMessages.add("&e/hitborder grow <amount> &7- Grow or shrink border");
            helpMessages.add("&e/hitborder hardcore [on|off] &7- Toggle hardcore mode");
            helpMessages.add("&e/hitborder setspawn &7- Set spawn to your location");
        }

        for (String message : helpMessages) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
    
    private void sendMessage(CommandSender sender, String key, String... replacements) {
        String message = configManager.getMessage(key);
        
        if (message.isEmpty()) {
            message = "&cMessage not found: " + key;
        }
        
        // Apply replacements
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("%s" + (i + 1), replacements[i]);
        }
        
        // Add prefix and color codes
        message = configManager.getMessage("prefix") + message;
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        sender.sendMessage(message);
    }

}
