package com.sloyni.main.island.stats;

import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandProtectionService;
import com.sloyni.main.island.IslandRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central service for island statistics (mobs killed, player deaths, time played).
 *
 * <p>Stats are held in-memory and flushed to {@link IslandStatsRepository} on a
 * fixed schedule and on explicit {@link #save()} calls.</p>
 */
public class IslandStatsService {

    private static final long AUTO_SAVE_MINUTES = 5;

    private final IslandStatsRepository repository;
    private final IslandRepository islandRepository;
    private final IslandProtectionService protectionService;

    /** playerUuid → System.currentTimeMillis() when they entered an island world */
    private final Map<UUID, Long> islandJoinTimestamp = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "iskyblock-stats-save");
        t.setDaemon(true);
        return t;
    });

    public IslandStatsService(IslandStatsRepository repository,
                              IslandRepository islandRepository,
                              IslandProtectionService protectionService) {
        this.repository = repository;
        this.islandRepository = islandRepository;
        this.protectionService = protectionService;
        scheduler.scheduleAtFixedRate(this::save, AUTO_SAVE_MINUTES, AUTO_SAVE_MINUTES, TimeUnit.MINUTES);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns stats for an island. Never null; creates a blank entry if absent.
     */
    public IslandStats getStats(UUID islandOwnerUuid) {
        return repository.getOrCreate(islandOwnerUuid);
    }

    /**
     * Increments the mob-kill counter for the island identified by {@code worldName}.
     */
    public void recordMobKillAt(int blockX, int blockZ) {
        UUID ownerUuid = resolveOwnerAt(blockX, blockZ);
        if (ownerUuid == null) {
            return;
        }
        repository.getOrCreate(ownerUuid).incrementMobsKilled();
    }

    /**
     * Increments the death counter for the island whose world the player died on.
     * Only counts the island owner's own deaths; visitors' deaths are ignored.
     */
    public void recordPlayerDeathAt(UUID playerUuid, int blockX, int blockZ) {
        UUID ownerUuid = resolveOwnerAt(blockX, blockZ);
        if (ownerUuid == null) {
            return;
        }
        // Count all deaths that happen on the island (owner + members + coop)
        repository.getOrCreate(ownerUuid).incrementDeaths();
    }

    /**
     * Called when a player enters an island world. Starts the time-played timer.
     */
    public void onPlayerEnterIslandWorld(UUID playerUuid) {
        islandJoinTimestamp.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * Called when a player leaves an island world (world change or disconnect).
     * Commits the elapsed time to the player's own island stats.
     */
    public void onPlayerLeaveIslandWorld(UUID playerUuid) {
        Long start = islandJoinTimestamp.remove(playerUuid);
        if (start == null) {
            return;
        }
        long elapsedSeconds = (System.currentTimeMillis() - start) / 1000L;
        if (elapsedSeconds <= 0) {
            return;
        }
        // Attribute time played to the player's own island
        IslandData island = islandRepository.get(playerUuid);
        if (island != null) {
            repository.getOrCreate(playerUuid).addTimePlayedSeconds(elapsedSeconds);
        }
    }

    /**
     * Clears all persisted counters for an island.
     *
     * <p>Used when an island is deleted/reset so new islands start at zero.</p>
     */
    public void clearIslandStats(UUID islandOwnerUuid) {
        if (islandOwnerUuid == null) {
            return;
        }
        repository.remove(islandOwnerUuid);
        save();
    }

    /** Flush stats to disk. */
    public void save() {
        repository.save();
    }

    /** Shut down the auto-save scheduler. */
    public void shutdown() {
        scheduler.shutdownNow();
        save();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UUID resolveOwnerAt(int blockX, int blockZ) {
        if (protectionService == null) {
            return null;
        }

        IslandData island = protectionService.getIslandAtBlock(blockX, blockZ);
        if (island == null) {
            return null;
        }

        return island.getOwnerUuid();
    }
}
