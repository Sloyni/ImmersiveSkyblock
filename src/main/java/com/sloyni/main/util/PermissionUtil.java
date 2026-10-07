package com.sloyni.main.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(Player player, String permission) {
        return player != null && has(player.getPlayerRef(), permission);
    }

    public static boolean has(PlayerRef playerRef, String permission) {
        return playerRef != null && playerRef.hasPermission(permission);
    }
}
