package com.sloyni.main.addons.warps;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.List;
import java.util.Map;

public class WarpsIslandCommandHandler {
    private final ISkyblockPlugin plugin;

    public WarpsIslandCommandHandler(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleIslandVisitUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.tr(playerRef, "island.visit.usage"));
    }

    public void handleIslandVisitCommand(PlayerRef playerRef, World sourceWorld, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.not_initialized"));
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.visit.usage"));
            return;
        }

        plugin.getAddonManager().getWarpsAddon().getService().teleportToIslandWarp(playerRef, sourceWorld, targetName)
            .thenRun(() -> Chat.send(playerRef, plugin.tr(playerRef, "island.visit.success", Map.of("player", targetName))))
            .exceptionally(ex -> {
                String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                if ("target_not_found".equals(message)) {
                    Chat.send(playerRef, plugin.tr(playerRef, "island.level.target_not_found", Map.of("player", targetName)));
                } else if ("warp_not_found".equals(message)) {
                    Chat.send(playerRef, plugin.tr(playerRef, "island.visit.not_found", Map.of("player", targetName)));
                } else {
                    Chat.send(playerRef, plugin.tr(playerRef, "island.leave.failed", Map.of("error", message == null ? "unknown" : message)));
                }
                return null;
            });
    }

    public void handleIslandWarpsCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.not_initialized"));
            return;
        }

        List<IslandWarpData> warps = plugin.getAddonManager().getWarpsAddon().getService().listIslandWarps();
        if (warps.isEmpty()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.warps.empty"));
            return;
        }

        Chat.send(playerRef, plugin.tr(playerRef, "island.warps.header"));
        for (IslandWarpData warp : warps) {
            String owner = warp.getOwnerName() == null || warp.getOwnerName().isBlank() ? "unknown" : warp.getOwnerName();
            String warpName = warp.getWarpName() == null || warp.getWarpName().isBlank() ? owner : warp.getWarpName();
            Chat.send(playerRef, plugin.tr(playerRef, "island.warps.item", Map.of(
                "player", owner,
                "name", warpName
            )));
        }
    }

    public void handleIslandWarpUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.tr(playerRef, "island.warp.usage"));
    }

    public void handleIslandWarpSetCommand(PlayerRef playerRef, World sourceWorld, int x, int y, int z, float pitch, float yaw) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.not_initialized"));
            return;
        }

        try {
            IslandWarpData warp = plugin.getAddonManager().getWarpsAddon().getService().setIslandWarp(playerRef, sourceWorld, x, y, z, pitch, yaw);
            Chat.send(playerRef, plugin.tr(playerRef, "island.warp.set.success", Map.of(
                "name", warp.getWarpName() == null ? "" : warp.getWarpName(),
                "x", String.valueOf(warp.getX()),
                "y", String.valueOf(warp.getY()),
                "z", String.valueOf(warp.getZ())
            )));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.tr(playerRef, "island.walls.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_leader"));
                case "not_on_own_island" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_on_own_island"));
                default -> Chat.send(playerRef, plugin.tr(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandWarpStateCommand(PlayerRef playerRef, boolean enabled) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.not_initialized"));
            return;
        }

        try {
            plugin.getAddonManager().getWarpsAddon().getService().setIslandWarpEnabled(playerRef, enabled);
            Chat.send(playerRef, plugin.tr(playerRef, enabled ? "island.warp.enabled" : "island.warp.disabled"));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.tr(playerRef, "island.walls.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_leader"));
                case "warp_not_set" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_set"));
                default -> Chat.send(playerRef, plugin.tr(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandWarpDeleteCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.not_initialized"));
            return;
        }

        try {
            plugin.getAddonManager().getWarpsAddon().getService().deleteIslandWarp(playerRef);
            Chat.send(playerRef, plugin.tr(playerRef, "island.warp.deleted"));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.tr(playerRef, "island.walls.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_leader"));
                case "warp_not_set" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_set"));
                default -> Chat.send(playerRef, plugin.tr(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandWarpRenameCommand(PlayerRef playerRef, String warpName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.not_initialized"));
            return;
        }

        try {
            IslandWarpData warp = plugin.getAddonManager().getWarpsAddon().getService().renameIslandWarp(playerRef, warpName);
            Chat.send(playerRef, plugin.tr(playerRef, "island.warp.rename.success", Map.of(
                "name", warp.getWarpName() == null ? "" : warp.getWarpName()
            )));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.tr(playerRef, "island.walls.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_leader"));
                case "warp_not_set" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.not_set"));
                case "warp_name_invalid" -> Chat.send(playerRef, plugin.tr(playerRef, "island.warp.rename.invalid", Map.of("max", "22")));
                default -> Chat.send(playerRef, plugin.tr(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }
}

