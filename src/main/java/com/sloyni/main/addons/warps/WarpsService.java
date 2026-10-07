package com.sloyni.main.addons.warps;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandMemberPermission;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.util.TeleportUtil;
import com.sloyni.main.world.WorldManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WarpsService {
    private static final int WARP_NAME_MAX_LENGTH = 22;

    private final IslandRepository islandRepository;
    private final IslandWarpRepository islandWarpRepository;
    private final WorldManager worldManager;
    private final PluginConfig pluginConfig;
    private final IslandService islandService;

    public WarpsService(IslandRepository islandRepository,
                        IslandWarpRepository islandWarpRepository,
                        WorldManager worldManager,
                        PluginConfig pluginConfig,
                        IslandService islandService) {
        this.islandRepository = islandRepository;
        this.islandWarpRepository = islandWarpRepository;
        this.worldManager = worldManager;
        this.pluginConfig = pluginConfig;
        this.islandService = islandService;
    }

    public IslandWarpData setIslandWarp(PlayerRef leaderRef, World sourceWorld, int x, int y, int z, float pitch, float yaw) {
        if (leaderRef == null || leaderRef.getUuid() == null) {
            throw new IllegalStateException("no_island");
        }

        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.MANAGE_WARPS)) {
            throw new IllegalStateException("not_leader");
        }
        UUID warpOwnerUuid = resolveWarpOwnerUuid(island);
        if (warpOwnerUuid == null) {
            throw new IllegalStateException("no_island");
        }
        if (sourceWorld == null || !sourceWorld.getName().equals(island.getWorldName()) || !isInsideIslandZone(island, x, z)) {
            throw new IllegalStateException("not_on_own_island");
        }

        IslandWarpData warp = islandWarpRepository.get(warpOwnerUuid);
        if (warp == null) {
            warp = new IslandWarpData();
            warp.setOwnerUuid(warpOwnerUuid);
            warp.setWarpName(buildDefaultWarpName(island));
        }

        warp.setOwnerName(resolveWarpOwnerName(island, warpOwnerUuid, leaderRef));
        warp.setIslandOwnerUuid(island.getOwnerUuid());
        warp.setWorldName(island.getWorldName());
        warp.setX(x);
        warp.setY(y);
        warp.setZ(z);
        warp.setPitch(pitch);
        warp.setYaw(yaw);
        warp.setEnabled(true);
        if (warp.getWarpName() == null || warp.getWarpName().isBlank()) {
            warp.setWarpName(buildDefaultWarpName(island));
        }

        islandWarpRepository.put(warp);
        return warp;
    }

    public IslandWarpData setIslandWarpEnabled(PlayerRef leaderRef, boolean enabled) {
        if (leaderRef == null || leaderRef.getUuid() == null) {
            throw new IllegalStateException("no_island");
        }

        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.MANAGE_WARPS)) {
            throw new IllegalStateException("not_leader");
        }
        UUID warpOwnerUuid = resolveWarpOwnerUuid(island);
        if (warpOwnerUuid == null) {
            throw new IllegalStateException("no_island");
        }

        IslandWarpData warp = islandWarpRepository.get(warpOwnerUuid);
        if (warp == null) {
            throw new IllegalStateException("warp_not_set");
        }

        warp.setEnabled(enabled);
        warp.setOwnerName(resolveWarpOwnerName(island, warpOwnerUuid, leaderRef));
        warp.setIslandOwnerUuid(island.getOwnerUuid());
        islandWarpRepository.put(warp);
        return warp;
    }

    public boolean deleteIslandWarp(PlayerRef leaderRef) {
        if (leaderRef == null || leaderRef.getUuid() == null) {
            throw new IllegalStateException("no_island");
        }

        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.MANAGE_WARPS)) {
            throw new IllegalStateException("not_leader");
        }
        UUID warpOwnerUuid = resolveWarpOwnerUuid(island);
        if (warpOwnerUuid == null) {
            throw new IllegalStateException("no_island");
        }

        IslandWarpData removed = islandWarpRepository.remove(warpOwnerUuid);
        if (removed == null) {
            throw new IllegalStateException("warp_not_set");
        }
        return true;
    }

    public IslandWarpData renameIslandWarp(PlayerRef leaderRef, String warpName) {
        if (leaderRef == null || leaderRef.getUuid() == null) {
            throw new IllegalStateException("no_island");
        }
        String normalizedName = normalizeWarpName(warpName);
        if (normalizedName == null) {
            throw new IllegalStateException("warp_name_invalid");
        }

        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.MANAGE_WARPS)) {
            throw new IllegalStateException("not_leader");
        }
        UUID warpOwnerUuid = resolveWarpOwnerUuid(island);
        if (warpOwnerUuid == null) {
            throw new IllegalStateException("no_island");
        }

        IslandWarpData warp = islandWarpRepository.get(warpOwnerUuid);
        if (warp == null) {
            throw new IllegalStateException("warp_not_set");
        }

        warp.setWarpName(normalizedName);
        islandWarpRepository.put(warp);
        return warp;
    }

    public List<IslandWarpData> listIslandWarps() {
        List<IslandWarpData> list = new ArrayList<>();
        for (IslandWarpData warp : islandWarpRepository.values()) {
            IslandWarpData valid = normalizeAndValidateWarp(warp, true);
            if (valid != null) {
                list.add(valid);
            }
        }

        list.sort(Comparator.comparing(warp -> {
            String ownerName = warp.getOwnerName();
            return ownerName == null ? "" : ownerName.toLowerCase(Locale.ROOT);
        }));
        return list;
    }

    public IslandWarpData getIslandWarp(UUID ownerUuid) {
        if (ownerUuid == null) {
            return null;
        }
        return normalizeAndValidateWarp(islandWarpRepository.get(ownerUuid), false);
    }

    public List<IslandWarpData> listDiscoverableWarps(String searchQuery, int limit) {
        String normalizedQuery = normalizeSearchQuery(searchQuery);
        List<IslandWarpData> list = new ArrayList<>();

        for (IslandWarpData warp : islandWarpRepository.values()) {
            IslandWarpData valid = normalizeAndValidateWarp(warp, true);
            if (valid == null) {
                continue;
            }
            if (!normalizedQuery.isBlank() && !matchesSearch(valid, normalizedQuery)) {
                continue;
            }
            list.add(valid);
        }

        list.sort(
            Comparator.comparingLong((IslandWarpData warp) -> Math.max(0L, warp.getVisitCount())).reversed()
                .thenComparing(warp -> safeLower(warp.getOwnerName()))
                .thenComparing(warp -> safeLower(warp.getWarpName()))
        );

        if (limit > 0 && list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    public CompletableFuture<Void> teleportToIslandWarp(PlayerRef visitorRef, World sourceWorld, String targetNameOrUuid) {
        UUID targetUuid = islandService.resolveKnownPlayerUuid(targetNameOrUuid);
        IslandWarpData warp = normalizeAndValidateWarp(islandWarpRepository.get(targetUuid), true);
        if (warp == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("warp_not_found"));
        }

        World world = Universe.get().getWorld(warp.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(warp.getWorldName())
            : CompletableFuture.completedFuture(world);

        // Determine if the visitor is a member of this island (skip visit count)
        UUID visitorUuid = visitorRef != null ? visitorRef.getUuid() : null;
        boolean isMember = false;
        if (visitorUuid != null && warp.getOwnerUuid() != null) {
            IslandData warpIsland = warp.getIslandOwnerUuid() != null
                ? islandRepository.get(warp.getIslandOwnerUuid())
                : islandRepository.getForPlayer(warp.getOwnerUuid());
            if (warpIsland != null) {
                isMember = visitorUuid.equals(warpIsland.getOwnerUuid())
                    || visitorUuid.equals(warpIsland.getLeaderUuid())
                    || warpIsland.isOnTeam(visitorUuid)
                    || warpIsland.isCoopMember(visitorUuid);
            }
        }

        boolean finalIsMember = isMember;
        return worldFuture.thenCompose(targetWorld -> TeleportUtil.teleport(
            visitorRef,
            sourceWorld,
            targetWorld,
            warp.getX(),
            warp.getY(),
            warp.getZ(),
            warp.getPitch(),
            warp.getYaw()
        )).thenRun(() -> {
            if (!finalIsMember) {
                incrementVisitCount(warp);
            }
        });
    }

    public void removeWarp(UUID ownerUuid) {
        if (ownerUuid == null) {
            return;
        }
        islandWarpRepository.remove(ownerUuid);
    }

    private UUID resolveWarpOwnerUuid(IslandData island) {
        if (island == null) {
            return null;
        }
        if (island.getLeaderUuid() != null) {
            return island.getLeaderUuid();
        }
        return island.getOwnerUuid();
    }

    private String resolveWarpOwnerName(IslandData island, UUID warpOwnerUuid, PlayerRef actorRef) {
        if (warpOwnerUuid != null) {
            String known = islandService.getAllKnownPlayers().get(warpOwnerUuid);
            if (known != null && !known.isBlank()) {
                return known;
            }
            if (island != null
                && island.getOwnerUuid() != null
                && island.getOwnerUuid().equals(warpOwnerUuid)
                && island.getOwnerName() != null
                && !island.getOwnerName().isBlank()) {
                return island.getOwnerName();
            }
        }
        if (actorRef != null && actorRef.getUsername() != null && !actorRef.getUsername().isBlank()) {
            return actorRef.getUsername();
        }
        return "";
    }

    private boolean isInsideIslandZone(IslandData island, int x, int z) {
        if (island == null) {
            return false;
        }
        int zoneSize = island.getZoneSize() > 0 ? island.getZoneSize() : pluginConfig.getInit().getIslandZoneSize();
        int step = Math.max(zoneSize + Math.max(0, pluginConfig.getInit().getIslandSpacing()), zoneSize + 1);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = zoneSize / 2;
        int minX = anchorX - half;
        int maxX = anchorX + half;
        int minZ = anchorZ - half;
        int maxZ = anchorZ + half;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private String normalizeWarpName(String warpName) {
        if (warpName == null) {
            return null;
        }
        String normalized = warpName.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isBlank() || normalized.length() > WARP_NAME_MAX_LENGTH) {
            return null;
        }
        return normalized;
    }

    private String buildDefaultWarpName(IslandData island) {
        String base = normalizeWarpName(islandService.getIslandDisplayName(island));
        if (base == null) {
            base = normalizeWarpName(island.getOwnerName());
        }
        return base == null ? "warp" : base;
    }

    private IslandWarpData normalizeAndValidateWarp(IslandWarpData warp, boolean requireEnabled) {
        if (warp == null || warp.getOwnerUuid() == null) {
            return null;
        }
        if (requireEnabled && !warp.isEnabled()) {
            return null;
        }

        IslandData island = warp.getIslandOwnerUuid() == null
            ? islandRepository.getForPlayer(warp.getOwnerUuid())
            : islandRepository.get(warp.getIslandOwnerUuid());
        if (island == null || !island.isLeader(warp.getOwnerUuid())) {
            islandWarpRepository.remove(warp.getOwnerUuid());
            return null;
        }

        boolean changed = false;
        if (warp.getIslandOwnerUuid() == null || !warp.getIslandOwnerUuid().equals(island.getOwnerUuid())) {
            warp.setIslandOwnerUuid(island.getOwnerUuid());
            changed = true;
        }
        if (warp.getWarpName() == null || warp.getWarpName().isBlank()) {
            warp.setWarpName(buildDefaultWarpName(island));
            changed = true;
        }
        if (warp.getVisitCount() < 0L) {
            warp.setVisitCount(0L);
            changed = true;
        }

        if (changed) {
            islandWarpRepository.put(warp);
        }
        return warp;
    }

    private void incrementVisitCount(IslandWarpData warp) {
        if (warp == null || warp.getOwnerUuid() == null) {
            return;
        }
        long current = Math.max(0L, warp.getVisitCount());
        if (current == Long.MAX_VALUE) {
            return;
        }
        warp.setVisitCount(current + 1L);
        islandWarpRepository.put(warp);
    }

    private String normalizeSearchQuery(String searchQuery) {
        return safeLower(searchQuery).trim();
    }

    private boolean matchesSearch(IslandWarpData warp, String query) {
        String owner = safeLower(warp.getOwnerName());
        String warpName = safeLower(warp.getWarpName());
        return owner.contains(query) || warpName.contains(query);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
