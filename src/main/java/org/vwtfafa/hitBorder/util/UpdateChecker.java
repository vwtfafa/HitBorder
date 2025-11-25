package org.vwtfafa.hitBorder.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String currentVersion;
    private final String projectId;
    private static final String API_URL = "https://api.modrinth.com/v2/project/%s/version";
    private static final String USER_AGENT = "HitBorder-Update-Checker/1.0";

    public UpdateChecker(JavaPlugin plugin, String projectId) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.projectId = projectId;
    }

    public void checkForUpdates() {
        if (projectId == null || projectId.isEmpty()) {
            plugin.getLogger().warning("Project ID not set, cannot check for updates");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(String.format(API_URL, projectId));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
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
                    
                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();
                    if (versions.size() == 0) {
                        plugin.getLogger().info("No versions found for this project");
                        return;
                    }

                    // Get the first (latest) version
                    JsonObject latestVersion = versions.get(0).getAsJsonObject();
                    String versionNumber = latestVersion.get("version_number").getAsString();
                    
                    if (isNewerVersion(versionNumber, currentVersion)) {
                        String downloadUrl = latestVersion.getAsJsonArray("files")
                                .get(0).getAsJsonObject()
                                .get("url").getAsString();
                                
                        plugin.getLogger().info("");
                        plugin.getLogger().info("§6[!] A new version of " + plugin.getName() + " is available!");
                        plugin.getLogger().info("§6[!] Current version: §c" + currentVersion);
                        plugin.getLogger().info("§6[!] New version: §a" + versionNumber);
                        plugin.getLogger().info("§6[!] Download: §b" + downloadUrl);
                        plugin.getLogger().info("");
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

    private boolean isNewerVersion(@NotNull String newVersion, @NotNull String currentVersion) {
        if (newVersion.equals(currentVersion)) {
            return false;
        }

        String[] current = currentVersion.replaceAll("[^0-9.]", "").split("\\.");
        String[] latest = newVersion.replaceAll("[^0-9.]", "").split("\\.");

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
