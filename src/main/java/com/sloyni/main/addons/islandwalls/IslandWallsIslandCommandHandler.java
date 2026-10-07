package com.sloyni.main.addons.islandwalls;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.List;
import java.util.Map;

public class IslandWallsIslandCommandHandler {
    private final ISkyblockPlugin plugin;

    public IslandWallsIslandCommandHandler(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleIslandWallsUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.usage"));
    }

    public void handleIslandWallsListCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.list", Map.of(
            "patterns", String.join(", ", plugin.getAddonManager().getIslandWallsAddon().getService().getSupportedWallPatternIds())
        )));
    }

    public void handleIslandWallsCommand(PlayerRef playerRef, String wallType, String sizeInput) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        String normalizedType = plugin.getAddonManager().getIslandWallsAddon().getService().normalizeWallPatternId(wallType);
        if (normalizedType == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.invalid_type", Map.of("type", wallType == null ? "" : wallType)));
            return;
        }

        int wallSize = 0;
        if (!"none".equals(normalizedType)) {
            if (sizeInput == null || sizeInput.isBlank()) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.usage"));
                return;
            }
            try {
                wallSize = Integer.parseInt(sizeInput);
            } catch (NumberFormatException ex) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.invalid_size", Map.of("value", sizeInput)));
                return;
            }
        }

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.updating"));
        plugin.getAddonManager().getIslandWallsAddon().getService().updateIslandWalls(playerRef, normalizedType, wallSize)
            .thenAccept(state -> {
                if ("none".equals(state.getWallPatternId())) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.disabled"));
                    return;
                }
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.updated", Map.of(
                    "type", state.getWallPatternId(),
                    "size", String.valueOf(state.getWallPatternSize())
                )));
            })
            .exceptionally(ex -> {
                String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                switch (message) {
                    case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.no_island"));
                    case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.not_leader"));
                    case "invalid_wall_type" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.invalid_type", Map.of("type", wallType == null ? "" : wallType)));
                    case "invalid_wall_size" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.invalid_size", Map.of("value", sizeInput == null ? "" : sizeInput)));
                    default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", message == null ? "unknown" : message)));
                }
                return null;
            });
    }

    public void handleIslandWallsColorUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.usage"));
    }

    public void handleIslandWallsColorAlternateUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color_alternate.usage"));
    }

    public void handleIslandWallsColorListCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.list", Map.of(
            "colors", String.join(", ", plugin.getAddonManager().getIslandWallsAddon().getService().getSupportedWallColorNames())
        )));
    }

    public void handleIslandWallsColorCommand(PlayerRef playerRef, String colorName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        String normalizedColor = plugin.getAddonManager().getIslandWallsAddon().getService().normalizeWallColorName(colorName);
        if (normalizedColor == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.invalid", Map.of("color", colorName == null ? "" : colorName)));
            return;
        }

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.updating"));
        plugin.getAddonManager().getIslandWallsAddon().getService().updateIslandWallColor(playerRef, normalizedColor)
            .thenAccept(state -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.updated", Map.of(
                "color", state.getWallColorName()
            ))))
            .exceptionally(ex -> {
                String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                switch (message) {
                    case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.no_island"));
                    case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.not_leader"));
                    case "invalid_wall_color" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.invalid", Map.of("color", colorName == null ? "" : colorName)));
                    default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", message == null ? "unknown" : message)));
                }
                return null;
            });
    }

    public void handleIslandWallsColorAlternateCommand(PlayerRef playerRef, String[] colors) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        if (colors == null || colors.length < 2) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color_alternate.usage"));
            return;
        }

        List<String> normalized = new java.util.ArrayList<>();
        String invalid = null;
        for (String color : colors) {
            String normalizedColor = plugin.getAddonManager().getIslandWallsAddon().getService().normalizeWallColorName(color);
            if (normalizedColor == null) {
                invalid = color;
                break;
            }
            if (normalized.isEmpty() || !normalized.get(normalized.size() - 1).equals(normalizedColor)) {
                normalized.add(normalizedColor);
            }
        }

        if (invalid != null || normalized.size() < 2) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.invalid_multi", Map.of(
                "color", invalid == null ? "" : invalid
            )));
            return;
        }

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color_alternate.updating"));
        plugin.getAddonManager().getIslandWallsAddon().getService().updateIslandWallAlternateColors(playerRef, normalized)
            .thenAccept(state -> {
                List<String> cycle = state.getWallAlternateColorNames().isEmpty()
                    ? List.of(state.getWallColorName())
                    : state.getWallAlternateColorNames();
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color_alternate.updated", Map.of(
                    "colorA", state.getWallColorName(),
                    "colorB", String.join(", ", cycle)
                )));
            })
            .exceptionally(ex -> {
                String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                switch (message) {
                    case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.no_island"));
                    case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls.not_leader"));
                    case "invalid_wall_color", "invalid_wall_color_alternate" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.walls_color.invalid_multi", Map.of(
                        "color", ""
                    )));
                    default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", message == null ? "unknown" : message)));
                }
                return null;
            });
    }
}

