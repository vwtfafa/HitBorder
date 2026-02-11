package org.vwtfafa.hitBorder.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class LuckPermsHook {
    private final boolean available;

    public LuckPermsHook() {
        this.available = isClassPresent("net.luckperms.api.LuckPermsProvider");
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isInAnyGroup(Player player, List<String> groupNames) {
        if (!available || player == null || groupNames == null || groupNames.isEmpty()) {
            return false;
        }

        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            if (luckPerms == null) {
                return false;
            }

            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            if (userManager == null) {
                return false;
            }

            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
            if (user == null) {
                return false;
            }

            Method getQueryOptions = user.getClass().getMethod("getQueryOptions");
            Object queryOptions = getQueryOptions.invoke(user);
            if (queryOptions == null) {
                return false;
            }

            Class<?> queryOptionsClass = Class.forName("net.luckperms.api.query.QueryOptions");
            Method getInheritedGroups = user.getClass().getMethod("getInheritedGroups", queryOptionsClass);
            Iterable<?> groups = (Iterable<?>) getInheritedGroups.invoke(user, queryOptions);

            for (Object group : groups) {
                if (group == null) {
                    continue;
                }
                String groupName = String.valueOf(group.getClass().getMethod("getName").invoke(group))
                        .toLowerCase(Locale.ROOT);
                for (String configured : groupNames) {
                    if (configured != null && groupName.equals(configured.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
