package org.vwtfafa.hitBorder.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class LuckPermsHook {
    private final LuckPerms luckPerms;

    public LuckPermsHook() {
        LuckPerms resolved;
        try {
            resolved = LuckPermsProvider.get();
        } catch (IllegalStateException ignored) {
            resolved = null;
        }
        this.luckPerms = resolved;
    }

    public boolean isAvailable() {
        return luckPerms != null;
    }

    public boolean isInAnyGroup(Player player, List<String> groupNames) {
        if (!isAvailable() || player == null || groupNames == null || groupNames.isEmpty()) {
            return false;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null || user.getQueryOptions() == null) {
            return false;
        }

        Set<String> playerGroups = user.getInheritedGroups(user.getQueryOptions()).stream()
                .map(group -> group.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (String configured : groupNames) {
            if (configured != null && playerGroups.contains(configured.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
