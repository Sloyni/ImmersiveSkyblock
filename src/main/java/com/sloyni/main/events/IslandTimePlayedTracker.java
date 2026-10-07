package com.sloyni.main.events;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.ISkyblockPlugin;
import java.util.UUID;

/**
 * Tracks the time players spend on their own island and commits it to
 * {@link com.sloyni.main.island.stats.IslandStatsService}.
 *
 * <p>Hooks into {@link PlayerReadyEvent} (world enter / re-enter) and
 * {@link PlayerDisconnectEvent} (session end).</p>
 */
public class IslandTimePlayedTracker {

    private final ISkyblockPlugin plugin;

    public IslandTimePlayedTracker(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called every time a player becomes ready in a world (world enter / respawn).
     * If the player just entered an island world, starts the timer.
     * If they were previously on an island world, the previous timer is committed
     * naturally because {@link com.sloyni.main.island.stats.IslandStatsService}
     * re-initialises the timestamp on each call.
     */
    public void onPlayerReady(PlayerReadyEvent event) {
        if (event == null) {
            return;
        }
        try {
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }
            PlayerRef playerRef = player.getPlayerRef();
            World world = player.getWorld();
            if (playerRef == null || playerRef.getUuid() == null || world == null) {
                return;
            }

            UUID playerUuid = playerRef.getUuid();

            if (isIslandWorld(world)) {
                // Player entered an island world — start (or restart) the timer.
                // Committing any previous session is handled by calling leave first.
                plugin.getIslandStatsService().onPlayerLeaveIslandWorld(playerUuid);
                plugin.getIslandStatsService().onPlayerEnterIslandWorld(playerUuid);
            } else {
                // Player moved to a non-island world — commit any pending island time.
                plugin.getIslandStatsService().onPlayerLeaveIslandWorld(playerUuid);
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandTimePlayedTracker.onPlayerReady error: " + ex.getMessage());
        }
    }

    /**
     * Called when a player disconnects. Commits any remaining time-played session.
     */
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null) {
            return;
        }
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        plugin.getIslandStatsService().onPlayerLeaveIslandWorld(playerRef.getUuid());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isIslandWorld(World world) {
        if (world == null || world.getName() == null) {
            return false;
        }
        String configured = plugin.getConfigManager().getConfig().getInit().getWorldName();
        return configured != null && !configured.isBlank()
            && configured.equalsIgnoreCase(world.getName());
    }
}
