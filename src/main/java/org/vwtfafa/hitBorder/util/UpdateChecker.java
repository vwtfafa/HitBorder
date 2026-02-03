package org.vwtfafa.hitBorder.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String currentVersion;
    private final String githubRepo;
    private static final String API_URL = "https://api.github.com/repos/%s/releases/latest";
    private static final String USER_AGENT = "HitBorder-Update-Checker/1.0";

    public UpdateChecker(JavaPlugin plugin, String githubRepo) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.githubRepo = githubRepo;
    }

    public void checkForUpdates() {
        if (githubRepo == null || githubRepo.isEmpty()) {
            plugin.getLogger().warning("GitHub repo not set, cannot check for updates");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(String.format(API_URL, githubRepo));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    plugin.getLogger().warning("Update check failed: HTTP " + responseCode);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    JsonObject latestRelease = JsonParser.parseReader(reader).getAsJsonObject();
                    String tagName = latestRelease.get("tag_name").getAsString();
                    String versionNumber = tagName.replaceFirst("^[vV]", "");
                    String htmlUrl = latestRelease.get("html_url").getAsString();

                    if (isNewerVersion(versionNumber, currentVersion)) {
                        plugin.getLogger().info("");
                        plugin.getLogger().info("§6[!] A new version of " + plugin.getName() + " is available!");
                        plugin.getLogger().info("§6[!] Current version: §c" + currentVersion);
                        plugin.getLogger().info("§6[!] New version: §a" + versionNumber);
                        plugin.getLogger().info("§6[!] Release: §b" + htmlUrl);
                        plugin.getLogger().info("");

                        if (plugin.getConfig().getBoolean("update-checker.notify-ops", true)) {
                            notifyOps(versionNumber, htmlUrl);
                        }
                    } else if (plugin.getConfig().getBoolean("update-checker.verbose", false)) {
                        plugin.getLogger().info("You are running the latest version of " + plugin.getName() + " (v" + currentVersion + ")");
                    }
                }
            } catch (IOException e) {
                if (plugin.getConfig().getBoolean("update-checker.verbose", false)) {
                    plugin.getLogger().log(Level.WARNING, "Could not check for updates", e);
                } else {
                    plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
                }
            }
        });
    }

    private void notifyOps(String versionNumber, String htmlUrl) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("hitborder.admin"))
                .forEach(player -> {
                    player.sendMessage(ChatColor.GOLD + "[HitBorder] " + ChatColor.YELLOW + "New version available!");
                    player.sendMessage(ChatColor.GRAY + "Current: " + ChatColor.RED + currentVersion);
                    player.sendMessage(ChatColor.GRAY + "New: " + ChatColor.GREEN + versionNumber);
                    player.sendMessage(ChatColor.GRAY + "Release: " + ChatColor.AQUA + htmlUrl);
                }));
    }

    private boolean isNewerVersion(@NotNull String newVersion, @NotNull String currentVersion) {
        if (newVersion.equals(currentVersion)) {
            return false;
        }

        String[] current = Objects.requireNonNull(currentVersion).replaceAll("[^0-9.]", "").split("\\.");
        String[] latest = Objects.requireNonNull(newVersion).replaceAll("[^0-9.]", "").split("\\.");

        for (int i = 0; i < Math.min(current.length, latest.length); i++) {
            try {
                int currentPart = Integer.parseInt(current[i]);
                int latestPart = Integer.parseInt(latest[i]);
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // If version part is not a number, compare as strings
                int comparison = latest[i].compareTo(current[i]);
                if (comparison > 0) return true;
                if (comparison < 0) return false;
            }
        }
        
        return latest.length > current.length;
    }
}
