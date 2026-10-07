package com.sloyni.main.addons.levels;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.Chat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LevelsIslandCommandHandler {
    private final ISkyblockPlugin plugin;
    private final LevelsAddon levelsAddon;

    public LevelsIslandCommandHandler(ISkyblockPlugin plugin, LevelsAddon levelsAddon) {
        this.plugin = plugin;
        this.levelsAddon = levelsAddon;
    }

    public void handleIslandValueCommand(PlayerRef playerRef) {
        if (!levelsAddon.isEnabled()) {
            return;
        }
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.no_island"));
            return;
        }

        World islandWorld = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = islandWorld != null
            ? CompletableFuture.completedFuture(islandWorld)
            : plugin.getWorldManager().createOrLoadVoidWorld(island.getWorldName());

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.calculating"));
        worldFuture
            .thenCompose(world -> levelsAddon.getIslandValueService().calculateIslandValue(island, world, true))
            .thenAccept(result -> {
                String templatePenaltySigned = formatDecimal(-Math.abs(result.templatePenalty()));
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.result", Map.of(
                    "value", formatDecimal(result.netValue()),
                    "level", String.valueOf(result.level()),
                    "raw", formatDecimal(result.rawValue()),
                    "template", templatePenaltySigned
                )));
                if (result.fromCache()) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.cached"));
                }
            })
            .exceptionally(ex -> {
                String error = ex.getMessage() == null ? "unknown" : ex.getMessage();
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.failed", Map.of("error", error)));
                return null;
            });
    }

    public void handleIslandLevelCommand(PlayerRef playerRef, String targetName) {
        if (!levelsAddon.isEnabled()) {
            return;
        }
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        UUID targetUuid;
        try {
            targetUuid = targetName == null || targetName.isBlank()
                ? playerRef.getUuid()
                : plugin.getIslandService().resolveKnownPlayerUuid(targetName);
        } catch (IllegalStateException ex) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.level.target_not_found", Map.of("player", String.valueOf(targetName))));
            return;
        }

        plugin.getIslandService().getIslandLevel(targetUuid, levelsAddon.getIslandValueService())
            .thenAccept(result -> {
                IslandData island = plugin.getIslandService().getPlayerIsland(targetUuid);
                if (island == null) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.no_island"));
                    return;
                }
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.level.result", Map.of(
                    "player", island.getOwnerName() == null ? "unknown" : island.getOwnerName(),
                    "island", plugin.getIslandService().getIslandDisplayName(island),
                    "level", String.valueOf(result.level()),
                    "value", formatDecimal(result.netValue())
                )));
                if (result.fromCache()) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.cached"));
                }
            })
            .exceptionally(ex -> {
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                if ("no_island".equals(msg)) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.level.no_island_target", Map.of(
                        "player", targetName == null ? playerRef.getUsername() : targetName
                    )));
                } else {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.failed", Map.of(
                        "error", msg == null ? "unknown" : msg
                    )));
                }
                return null;
            });
    }

    public void handleIslandTopCommand(PlayerRef playerRef) {
        if (!levelsAddon.isEnabled()) {
            return;
        }

        var topEntries = levelsAddon.getIslandValueService().getTopCachedIslands(10);
        if (topEntries.isEmpty()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.top.empty"));
            return;
        }

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.top.header"));
        int rank = 1;
        for (var entry : topEntries) {
            String islandName = entry.islandName() == null || entry.islandName().isBlank()
                ? plugin.getIslandService().buildDefaultIslandName(entry.ownerName())
                : entry.islandName();
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.top.item", Map.of(
                "rank", String.valueOf(rank++),
                "player", entry.ownerName() == null ? "unknown" : entry.ownerName(),
                "island", islandName,
                "level", String.valueOf(entry.level()),
                "value", formatDecimal(entry.netValue())
            )));
        }
    }

    private String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
