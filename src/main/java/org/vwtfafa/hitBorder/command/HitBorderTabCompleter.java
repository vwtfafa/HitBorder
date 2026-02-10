package org.vwtfafa.hitBorder.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HitBorderTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("hitborder")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("help");
            options.add("status");
            options.add("version");
            if (sender.hasPermission("hitborder.admin")) {
                options.add("reload");
                options.add("toggle");
                options.add("setborder");
                options.add("set");
                options.add("grow");
                options.add("hardcore");
                options.add("setspawn");
            }
            return filterPrefix(options, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("hardcore")) {
            return filterPrefix(List.of("on", "off"), args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}
