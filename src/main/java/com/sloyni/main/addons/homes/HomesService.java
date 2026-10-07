package com.sloyni.main.addons.homes;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.util.TeleportUtil;
import com.sloyni.main.world.WorldManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HomesService {
    private final IslandRepository islandRepository;
    private final PlayerHomeLimitRepository playerHomeLimitRepository;
    private final IslandHomesRepository islandHomesRepository;
    private final WorldManager worldManager;
    private final PluginConfig pluginConfig;
    private final HomesAddon homesAddon;

    public HomesService(IslandRepository islandRepository,
                        PlayerHomeLimitRepository playerHomeLimitRepository,
                        IslandHomesRepository islandHomesRepository,
                        WorldManager worldManager,
                        PluginConfig pluginConfig,
                        HomesAddon homesAddon) {
        this.islandRepository = islandRepository;
        this.playerHomeLimitRepository = playerHomeLimitRepository;
        this.islandHomesRepository = islandHomesRepository;
        this.worldManager = worldManager;
        this.pluginConfig = pluginConfig;
        this.homesAddon = homesAddon;
    }

    public int getPlayerMaxHomes(UUID playerUuid) {
        int defaultLimit = Math.max(0, homesAddon.getMaxHomes());
        int groupLimit = resolveHomesByPermission(playerUuid, defaultLimit);
        int addedHomes = Math.max(0, playerHomeLimitRepository.getAddedHomes(playerUuid));
        return Math.max(0, groupLimit + addedHomes);
    }

    public int getPlayerAddedHomes(UUID playerUuid) {
        return Math.max(0, playerHomeLimitRepository.getAddedHomes(playerUuid));
    }

    public int setPlayerAddedHomes(UUID playerUuid, int addedHomes) {
        return playerHomeLimitRepository.setAddedHomes(playerUuid, Math.max(0, addedHomes));
    }

    public int addManualMaxHomes(UUID playerUuid, int amount) {
        return playerHomeLimitRepository.addAddedHomes(playerUuid, amount);
    }

    public int setManualMaxHomes(UUID playerUuid, int amount) {
        return setPlayerAddedHomes(playerUuid, amount);
    }

    public int removeManualMaxHomes(UUID playerUuid, int amount) {
        return playerHomeLimitRepository.removeAddedHomes(playerUuid, amount);
    }

    public int getPlayerHomeCount(UUID playerUuid) {
        IslandData island = islandRepository.getForPlayer(playerUuid);
        if (island == null) {
            return 0;
        }
        return islandHomesRepository.getHomeCount(island.getOwnerUuid(), playerUuid);
    }

    public Map<String, HomeLocation> listHomes(PlayerRef playerRef) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        return islandHomesRepository.getHomes(island.getOwnerUuid(), playerRef.getUuid());
    }

    public Map<String, HomeLocation> listHomesForTarget(UUID targetUuid) {
        if (targetUuid == null) {
            return new LinkedHashMap<>();
        }
        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            return new LinkedHashMap<>();
        }
        return islandHomesRepository.getHomes(island.getOwnerUuid(), targetUuid);
    }

    public boolean setHome(PlayerRef playerRef, World sourceWorld, String homeId, int x, int y, int z, float pitch, float yaw) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (sourceWorld == null || !sourceWorld.getName().equals(island.getWorldName()) || !isInsideIslandZone(island, x, z)) {
            throw new IllegalStateException("not_on_own_island");
        }

        boolean alreadyExists = islandHomesRepository.hasHome(island.getOwnerUuid(), playerRef.getUuid(), homeId);
        int currentCount = islandHomesRepository.getHomeCount(island.getOwnerUuid(), playerRef.getUuid());
        int maxHomes = getPlayerMaxHomes(playerRef.getUuid());
        if (!alreadyExists && currentCount >= maxHomes) {
            throw new IllegalStateException("home_limit_reached");
        }

        return islandHomesRepository.setHome(island.getOwnerUuid(), playerRef.getUuid(), homeId, x, y, z, pitch, yaw);
    }

    public void renameHome(PlayerRef playerRef, String oldHomeId, String newHomeId) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (oldHomeId.equalsIgnoreCase(newHomeId)) {
            throw new IllegalStateException("same_name");
        }
        if (!islandHomesRepository.hasHome(island.getOwnerUuid(), playerRef.getUuid(), oldHomeId)) {
            throw new IllegalStateException("home_not_found");
        }
        if (islandHomesRepository.hasHome(island.getOwnerUuid(), playerRef.getUuid(), newHomeId)) {
            throw new IllegalStateException("home_already_exists");
        }

        boolean renamed = islandHomesRepository.renameHome(island.getOwnerUuid(), playerRef.getUuid(), oldHomeId, newHomeId);
        if (!renamed) {
            throw new IllegalStateException("home_not_found");
        }
    }

    public void deleteHome(PlayerRef playerRef, String homeId) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        boolean removed = islandHomesRepository.deleteHome(island.getOwnerUuid(), playerRef.getUuid(), homeId);
        if (!removed) {
            throw new IllegalStateException("home_not_found");
        }
    }

    public CompletableFuture<Void> teleportToHome(PlayerRef playerRef, World sourceWorld, String homeId) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        HomeLocation home = islandHomesRepository.getHome(island.getOwnerUuid(), playerRef.getUuid(), homeId);
        if (home == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("home_not_found"));
        }

        World islandWorld = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = islandWorld == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(islandWorld);

        return worldFuture.thenCompose(world -> TeleportUtil.teleport(
            playerRef,
            sourceWorld,
            world,
            home.getX(),
            home.getY(),
            home.getZ(),
            home.getPitch(),
            home.getYaw()
        ));
    }

    public CompletableFuture<Void> teleportViewerToTargetHome(PlayerRef viewerRef,
                                                               World sourceWorld,
                                                               UUID targetUuid,
                                                               String homeId) {
        if (viewerRef == null || targetUuid == null || homeId == null || homeId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("home_not_found"));
        }

        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        HomeLocation home = islandHomesRepository.getHome(island.getOwnerUuid(), targetUuid, homeId);
        if (home == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("home_not_found"));
        }

        World islandWorld = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = islandWorld == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(islandWorld);

        return worldFuture.thenCompose(world -> TeleportUtil.teleport(
            viewerRef,
            sourceWorld,
            world,
            home.getX(),
            home.getY(),
            home.getZ(),
            home.getPitch(),
            home.getYaw()
        ));
    }

    public void applyGlobalHomeDefaultChange(int oldDefault, int newDefault) {
        // No-op with homes-by-groups model: per-player state is additive only.
    }

    public void clearHomes(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        IslandData island = islandRepository.getForPlayer(playerUuid);
        if (island == null) {
            return;
        }
        clearHomes(island.getOwnerUuid(), playerUuid);
    }

    public void clearHomes(UUID islandOwnerUuid, UUID playerUuid) {
        if (islandOwnerUuid == null || playerUuid == null) {
            return;
        }
        islandHomesRepository.clearHomes(islandOwnerUuid, playerUuid);
    }

    private boolean isInsideIslandZone(IslandData island, int x, int z) {
        int zoneSize = island.getZoneSize() > 0 ? island.getZoneSize() : pluginConfig.getInit().getIslandZoneSize();
        int spacing = Math.max(0, pluginConfig.getInit().getIslandSpacing());
        int step = Math.max(zoneSize + Math.max(0, spacing), zoneSize + 1);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = zoneSize / 2;
        int minX = anchorX - half;
        int maxX = anchorX + half;
        int minZ = anchorZ - half;
        int maxZ = anchorZ + half;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private int resolveHomesByPermission(UUID playerUuid, int defaultLimit) {
        if (playerUuid == null) {
            return Math.max(0, defaultLimit);
        }
        int resolved = Math.max(0, defaultLimit);
        for (Map.Entry<String, Integer> group : homesAddon.getHomeGroups().entrySet()) {
            String groupId = group.getKey();
            int groupLimit = Math.max(0, group.getValue() == null ? 0 : group.getValue());
            if (groupId == null || groupId.isBlank()) {
                continue;
            }
            if (hasHomesGroupPermission(playerUuid, groupId)) {
                resolved = Math.max(resolved, groupLimit);
            }
        }
        return resolved;
    }

    private boolean hasHomesGroupPermission(UUID playerUuid, String groupId) {
        String permission = "iskyblock.homes." + groupId;
        for (PlayerRef onlinePlayer : Universe.get().getPlayers()) {
            if (onlinePlayer == null || !playerUuid.equals(onlinePlayer.getUuid())) {
                continue;
            }
            Ref<EntityStore> ref = onlinePlayer.getReference();
            if (ref == null || ref.getStore() == null) {
                return false;
            }
            try {
                Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                return player != null && com.sloyni.main.util.PermissionUtil.has(player, permission);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }
}
