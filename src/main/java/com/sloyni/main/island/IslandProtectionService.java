package com.sloyni.main.island;

import com.sloyni.main.config.PluginConfig;
import java.util.UUID;

public class IslandProtectionService {
    private final IslandRepository repository;
    private final PluginConfig config;

    public IslandProtectionService(IslandRepository repository, PluginConfig config) {
        this.repository = repository;
        this.config = config;
    }

    /**
     * Returns true if the given world name is the configured island world.
     */
    public boolean isIslandWorld(String worldName) {
        if (worldName == null) {
            return false;
        }
        String configured = config.getInit().getWorldName();
        return configured != null && !configured.isBlank() && configured.equals(worldName);
    }

    /**
     * Finds the island whose zone contains the given world XZ coordinate.
     * Returns null if the position is outside every island's zone (open void).
     */
    public IslandData getIslandAtBlock(int blockX, int blockZ) {
        for (IslandData island : repository.values()) {
            if (isInZone(island, blockX, blockZ)) {
                return island;
            }
        }
        return null;
    }

    /**
     * Returns true if the given player UUID may modify blocks on the island.
     * Both the island leader and all team members are allowed to modify.
     */
    public boolean canModify(IslandData island, UUID playerUuid) {
        if (island == null || playerUuid == null) {
            return false;
        }
        return island.canModify(playerUuid);
    }

    // -------------------------------------------------------------------------

    private boolean isInZone(IslandData island, int x, int z) {
        int zoneSize = Math.max(1, island.getZoneSize());
        int half = zoneSize / 2;
        if (half <= 0) {
            return false;
        }
        int step = Math.max(1, config.getInit().getIslandZoneSize()) + Math.max(0, config.getInit().getIslandSpacing());
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        return x >= anchorX - half && x <= anchorX + half
            && z >= anchorZ - half && z <= anchorZ + half;
    }
}
