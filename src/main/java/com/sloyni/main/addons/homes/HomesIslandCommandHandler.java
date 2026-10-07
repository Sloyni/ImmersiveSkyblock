package com.sloyni.main.addons.homes;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.Map;

public class HomesIslandCommandHandler {
    private final ISkyblockPlugin plugin;

    public HomesIslandCommandHandler(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleIslandHomeUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.usage"));
    }

    public void handleIslandHomeListCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        try {
            Map<String, HomeLocation> homes = plugin.getAddonManager().getHomesAddon().getService().listHomes(playerRef);
            int maxHomes = plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(playerRef.getUuid());
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.list.header", Map.of(
                "count", String.valueOf(homes.size()),
                "max", String.valueOf(maxHomes)
            )));

            if (homes.isEmpty()) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.list.empty"));
                return;
            }

            for (Map.Entry<String, HomeLocation> entry : homes.entrySet()) {
                HomeLocation location = entry.getValue();
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.list.item", Map.of(
                    "home", entry.getKey(),
                    "x", String.valueOf(location.getX()),
                    "y", String.valueOf(location.getY()),
                    "z", String.valueOf(location.getZ())
                )));
            }
        } catch (IllegalStateException ex) {
            if ("no_island".equals(ex.getMessage())) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.no_island"));
            } else {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandHomeSetCommand(PlayerRef playerRef, World sourceWorld, String homeId, int x, int y, int z, float pitch, float yaw) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        try {
            boolean created = plugin.getAddonManager().getHomesAddon().getService().setHome(playerRef, sourceWorld, homeId, x, y, z, pitch, yaw);
            int count = plugin.getAddonManager().getHomesAddon().getService().getPlayerHomeCount(playerRef.getUuid());
            int maxHomes = plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(playerRef.getUuid());
            String key = created ? "home.set.created" : "home.set.updated";
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, key, Map.of(
                "home", homeId.toLowerCase(),
                "x", String.valueOf(x),
                "y", String.valueOf(y),
                "z", String.valueOf(z),
                "count", String.valueOf(count),
                "max", String.valueOf(maxHomes)
            )));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.no_island"));
                case "not_on_own_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.set.not_on_own_island"));
                case "home_limit_reached" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.set.limit_reached", Map.of("max", String.valueOf(plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(playerRef.getUuid())))));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandHomeGoCommand(PlayerRef playerRef, World sourceWorld, String homeId) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        plugin.getAddonManager().getHomesAddon().getService().teleportToHome(playerRef, sourceWorld, homeId)
            .thenRun(() -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.go.success", Map.of("home", homeId.toLowerCase()))))
            .exceptionally(ex -> {
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                if ("no_island".equals(msg)) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.no_island"));
                } else if ("home_not_found".equals(msg)) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.not_found", Map.of("home", homeId.toLowerCase())));
                } else {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", msg == null ? "unknown" : msg)));
                }
                return null;
            });
    }

    public void handleIslandHomeRenameCommand(PlayerRef playerRef, String oldHomeId, String newHomeId) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        try {
            plugin.getAddonManager().getHomesAddon().getService().renameHome(playerRef, oldHomeId, newHomeId);
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.rename.success", Map.of("old", oldHomeId.toLowerCase(), "new", newHomeId.toLowerCase())));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.no_island"));
                case "home_not_found" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.not_found", Map.of("home", oldHomeId.toLowerCase())));
                case "home_already_exists" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.already_exists", Map.of("home", newHomeId.toLowerCase())));
                case "same_name" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.rename.same_name"));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandHomeDeleteCommand(PlayerRef playerRef, String homeId) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        try {
            plugin.getAddonManager().getHomesAddon().getService().deleteHome(playerRef, homeId);
            int count = plugin.getAddonManager().getHomesAddon().getService().getPlayerHomeCount(playerRef.getUuid());
            int maxHomes = plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(playerRef.getUuid());
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.delete.success", Map.of(
                "home", homeId.toLowerCase(),
                "count", String.valueOf(count),
                "max", String.valueOf(maxHomes)
            )));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.no_island"));
                case "home_not_found" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.not_found", Map.of("home", homeId.toLowerCase())));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }
}

