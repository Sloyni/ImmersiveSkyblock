package com.sloyni.main.island;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import org.joml.Vector3i;
import com.sloyni.main.addons.levels.IslandValueService;
import com.sloyni.main.addons.warps.IslandWarpData;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.util.Chat;
import com.sloyni.main.util.TeleportUtil;
import com.sloyni.main.world.WorldManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandService {
    public record SpawnUpdateResult(int x, int y, int z, boolean fallbackUsed, float pitch, float yaw) {}
    public record TeleportTarget(World world, int x, int y, int z, float pitch, float yaw) {}
    public record IslandSizeUpdateResult(int requestedSize,
                                         int playerSize,
                                         boolean islandFound,
                                         int islandSize,
                                         boolean islandSizeChanged) {}

    private final IslandRepository islandRepository;
    private final IslandTemplateManager templateManager;
    private final GridAllocator gridAllocator;
    private final PrefabIslandGenerator generator;
    private final WorldManager worldManager;
    private final PluginConfig pluginConfig;
    private final PlayerResetRepository playerResetRepository;
    private final PlayerIslandSizeRepository playerIslandSizeRepository;

    public IslandService(IslandRepository islandRepository,
                         IslandTemplateManager templateManager,
                         PlayerResetRepository playerResetRepository,
                         PlayerIslandSizeRepository playerIslandSizeRepository,
                         WorldManager worldManager,
                         PluginConfig pluginConfig) {
        this.islandRepository = islandRepository;
        this.templateManager = templateManager;
        this.playerResetRepository = playerResetRepository;
        this.playerIslandSizeRepository = playerIslandSizeRepository;
        this.worldManager = worldManager;
        this.pluginConfig = pluginConfig;
        this.gridAllocator = new GridAllocator();
        this.gridAllocator.bootstrap(islandRepository.values());
        this.generator = new PrefabIslandGenerator(templateManager);
    }

    public IslandData getPlayerIsland(UUID playerUuid) {
        return islandRepository.getForPlayer(playerUuid);
    }

    public IslandData getOwnedIsland(UUID ownerUuid) {
        return islandRepository.get(ownerUuid);
    }

    public List<String> getSupportedWallPatternIds() {
        return getIslandWallsService().getSupportedWallPatternIds();
    }

    public String normalizeWallPatternId(String wallPatternId) {
        return getIslandWallsService().normalizeWallPatternId(wallPatternId);
    }

    public List<String> getSupportedWallColorNames() {
        return getIslandWallsService().getSupportedWallColorNames();
    }

    public String normalizeWallColorName(String wallColorName) {
        return getIslandWallsService().normalizeWallColorName(wallColorName);
    }

    public CompletableFuture<com.sloyni.main.addons.islandwalls.IslandWallState> updateIslandWallColor(PlayerRef leaderRef, String wallColorName) {
        return getIslandWallsService().updateIslandWallColor(leaderRef, wallColorName);
    }

    public CompletableFuture<com.sloyni.main.addons.islandwalls.IslandWallState> updateIslandWallAlternateColors(PlayerRef leaderRef, List<String> colorNames) {
        return getIslandWallsService().updateIslandWallAlternateColors(leaderRef, colorNames);
    }

    public CompletableFuture<com.sloyni.main.addons.islandwalls.IslandWallState> updateIslandWalls(PlayerRef leaderRef, String wallPatternId, int wallPatternSize) {
        return getIslandWallsService().updateIslandWalls(leaderRef, wallPatternId, wallPatternSize);
    }

    public CompletableFuture<com.sloyni.main.addons.islandwalls.IslandWallState> disableIslandWalls(UUID actorUuid) {
        return getIslandWallsService().disableIslandWalls(actorUuid);
    }

    public CompletableFuture<com.sloyni.main.addons.islandwalls.IslandWallState> enableIslandWalls(UUID actorUuid) {
        return getIslandWallsService().enableIslandWalls(actorUuid);
    }

    public boolean isIslandWallsDisabled(UUID islandOwnerUuid) {
        return getIslandWallsService().isWallsDisabled(islandOwnerUuid);
    }

    public IslandWarpData setIslandWarp(PlayerRef leaderRef, World sourceWorld, int x, int y, int z, float pitch, float yaw) {
        return getWarpsService().setIslandWarp(leaderRef, sourceWorld, x, y, z, pitch, yaw);
    }

    public IslandWarpData setIslandWarpEnabled(PlayerRef leaderRef, boolean enabled) {
        return getWarpsService().setIslandWarpEnabled(leaderRef, enabled);
    }

    public boolean deleteIslandWarp(PlayerRef leaderRef) {
        return getWarpsService().deleteIslandWarp(leaderRef);
    }

    public IslandWarpData renameIslandWarp(PlayerRef leaderRef, String warpName) {
        return getWarpsService().renameIslandWarp(leaderRef, warpName);
    }

    public List<IslandWarpData> listIslandWarps() {
        return getWarpsService().listIslandWarps();
    }

    public CompletableFuture<Void> teleportToIslandWarp(PlayerRef visitorRef, World sourceWorld, String targetNameOrUuid) {
        return getWarpsService().teleportToIslandWarp(visitorRef, sourceWorld, targetNameOrUuid);
    }

    private com.sloyni.main.addons.warps.WarpsService getWarpsService() {
        return com.sloyni.main.ISkyblockPlugin.getInstance().getAddonManager().getWarpsAddon().getService();
    }

    private com.sloyni.main.addons.homes.HomesService getHomesService() {
        return com.sloyni.main.ISkyblockPlugin.getInstance().getAddonManager().getHomesAddon().getService();
    }

    private com.sloyni.main.addons.islandwalls.IslandWallsService getIslandWallsService() {
        return com.sloyni.main.ISkyblockPlugin.getInstance().getAddonManager().getIslandWallsAddon().getService();
    }

    @SuppressWarnings("deprecation")
    private void checkAndClearWallsIfNoPermission(IslandData island) {
        if (island == null || !com.sloyni.main.ISkyblockPlugin.getInstance().getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        World islandWorld = Universe.get().getWorld(island.getWorldName());
        if (islandWorld == null) {
            return;
        }
        boolean anyOnline = false;
        for (com.hypixel.hytale.server.core.entity.entities.Player player : islandWorld.getPlayers()) {
            if (player == null || player.getPlayerRef() == null || player.getPlayerRef().getUuid() == null) {
                continue;
            }
            UUID uuid = player.getPlayerRef().getUuid();
            if (!island.isOnTeam(uuid)) {
                continue;
            }
            anyOnline = true;
            if (com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island_walls")) {
                return;
            }
        }
        if (anyOnline) {
            getIslandWallsService().clearWallsUnconditionally(island);
        }
    }

    public IslandData setIslandLocked(UUID leaderUuid, boolean locked) {
        IslandData island = islandRepository.getForPlayer(leaderUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(leaderUuid, IslandMemberPermission.LOCK_ISLAND)) {
            throw new IllegalStateException("not_leader");
        }
        if (island.isLocked() == locked) {
            throw new IllegalStateException(locked ? "already_locked" : "already_unlocked");
        }
        island.setLocked(locked);
        islandRepository.put(island);
        return island;
    }

    public IslandData setIslandVisitorItemDropEnabled(UUID actorUuid, boolean enabled) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(actorUuid, IslandMemberPermission.CHANGE_ISLAND_SETTINGS)) {
            throw new IllegalStateException("no_permission");
        }
        island.setVisitorItemDropEnabled(enabled);
        islandRepository.put(island);
        return island;
    }

    public IslandData setIslandVisitorMobDamageEnabled(UUID actorUuid, boolean enabled) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(actorUuid, IslandMemberPermission.CHANGE_ISLAND_SETTINGS)) {
            throw new IllegalStateException("no_permission");
        }
        island.setVisitorMobDamageEnabled(enabled);
        islandRepository.put(island);
        return island;
    }

    public boolean hasIslandActionPermission(UUID actorUuid, IslandMemberPermission permission) {
        if (actorUuid == null || permission == null) {
            return false;
        }
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            return false;
        }
        return island.hasMemberPermission(actorUuid, permission);
    }

    public List<String> getDefaultMemberPermissionKeys(UUID actorUuid) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.isLeader(actorUuid)) {
            throw new IllegalStateException("not_leader");
        }
        return new ArrayList<>(island.getDefaultMemberPermissionKeys());
    }

    public void setDefaultMemberPermissionKeys(UUID actorUuid, List<String> permissionKeys) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.isLeader(actorUuid)) {
            throw new IllegalStateException("not_leader");
        }
        island.setDefaultMemberPermissionKeys(permissionKeys);
        islandRepository.put(island);
    }

    public boolean isMemberFollowingDefaultPermissions(UUID actorUuid, UUID memberUuid) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.isLeader(actorUuid)) {
            throw new IllegalStateException("not_leader");
        }
        if (memberUuid == null || !island.isMember(memberUuid)) {
            throw new IllegalStateException("member_not_found");
        }
        return island.isMemberFollowingDefaultPermissions(memberUuid);
    }

    public List<String> getMemberExplicitPermissionKeys(UUID actorUuid, UUID memberUuid) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.isLeader(actorUuid)) {
            throw new IllegalStateException("not_leader");
        }
        if (memberUuid == null || !island.isMember(memberUuid)) {
            throw new IllegalStateException("member_not_found");
        }
        return new ArrayList<>(island.getMemberExplicitPermissionKeys(memberUuid));
    }

    public void setMemberFollowDefaultPermissions(UUID actorUuid, UUID memberUuid, boolean followDefault) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.isLeader(actorUuid)) {
            throw new IllegalStateException("not_leader");
        }
        if (memberUuid == null || !island.isMember(memberUuid)) {
            throw new IllegalStateException("member_not_found");
        }
        island.setMemberFollowDefaultPermissions(memberUuid, followDefault);
        islandRepository.put(island);
    }

    public void setMemberExplicitPermissionKeys(UUID actorUuid, UUID memberUuid, List<String> permissionKeys) {
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.isLeader(actorUuid)) {
            throw new IllegalStateException("not_leader");
        }
        if (memberUuid == null || !island.isMember(memberUuid)) {
            throw new IllegalStateException("member_not_found");
        }
        island.setMemberExplicitPermissionKeys(memberUuid, permissionKeys);
        islandRepository.put(island);
    }

    public CompletableFuture<SpawnUpdateResult> setIslandSpawnAndTeleportByActor(PlayerRef actorRef,
                                                                                  World sourceWorld,
                                                                                  int preferredX,
                                                                                  int preferredY,
                                                                                  int preferredZ,
                                                                                  float preferredPitch,
                                                                                  float preferredYaw) {
        if (actorRef == null || actorRef.getUuid() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        IslandData island = islandRepository.getForPlayer(actorRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.hasMemberPermission(actorRef.getUuid(), IslandMemberPermission.SET_SPAWN)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }
        return setIslandSpawnAndTeleport(actorRef, sourceWorld, preferredX, preferredY, preferredZ, preferredPitch, preferredYaw);
    }

    public CompletableFuture<Void> resetIslandAndTeleportToLobby(PlayerRef actorRef,
                                                                 World sourceWorld,
                                                                 String lobbyWorldName,
                                                                 int lobbyX,
                                                                 int lobbyY,
                                                                 int lobbyZ,
                                                                 float lobbyPitch,
                                                                 float lobbyYaw) {
        if (actorRef == null || actorRef.getUuid() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        IslandData island = islandRepository.getForPlayer(actorRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.isLeader(actorRef.getUuid())) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        int memberCount = island.getMemberCount();
        if (memberCount > 0) {
            return CompletableFuture.failedFuture(new IllegalStateException("leader_has_members"));
        }

        return deleteIslandAndTeleportToLobby(island, actorRef, sourceWorld, lobbyWorldName, lobbyX, lobbyY, lobbyZ, lobbyPitch, lobbyYaw);
    }

    public CompletableFuture<Void> teleportToIsland(PlayerRef playerRef, World sourceWorld) {
        return resolveIslandTeleportTarget(playerRef.getUuid())
            .thenCompose(target -> TeleportUtil.teleport(
                playerRef,
                sourceWorld,
                target.world(),
                target.x(),
                target.y(),
                target.z(),
                target.pitch(),
                target.yaw()
            ));
    }

    public CompletableFuture<TeleportTarget> resolveIslandTeleportTarget(UUID playerUuid) {
        IslandData island = islandRepository.getForPlayer(playerUuid);
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No island"));
        }

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        return worldFuture.thenCompose(target -> loadIslandZoneChunks(target, island)
            .thenApply(v -> {
                IslandTemplate template = resolveTemplate(island.getTemplateId());
                int[] preferredSpawn = getOrInitializePreferredSpawn(island, template);
                SpawnUpdateResult resolved = resolveAndPersistSpawn(target, island, preferredSpawn);
                return new TeleportTarget(target, resolved.x(), resolved.y(), resolved.z(), resolved.pitch(), resolved.yaw());
            })
        );
    }

    public CompletableFuture<SpawnUpdateResult> setIslandSpawnAndTeleport(PlayerRef playerRef,
                                                                           World sourceWorld,
                                                                           int preferredX,
                                                                           int preferredY,
                                                                           int preferredZ,
                                                                           float preferredPitch,
                                                                           float preferredYaw) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No island"));
        }

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        return worldFuture.thenCompose(target -> loadIslandZoneChunks(target, island)
            .thenCompose(v -> {
                island.setPreferredSpawnX(preferredX);
                island.setPreferredSpawnY(preferredY);
                island.setPreferredSpawnZ(preferredZ);
                island.setPreferredSpawnPitch(preferredPitch);
                island.setPreferredSpawnYaw(preferredYaw);
                island.setPreferredSpawnConfigured(true);
                islandRepository.put(island);

                SpawnUpdateResult resolved = resolveAndPersistSpawn(target, island, new int[] {preferredX, preferredY, preferredZ});
                return TeleportUtil.teleport(playerRef, sourceWorld, target, resolved.x(), resolved.y(), resolved.z(), resolved.pitch(), resolved.yaw())
                    .thenApply(unused -> resolved);
            })
        );
    }

    public CompletableFuture<IslandData> createIsland(PlayerRef playerRef, World sourceWorld, String templateId) {
        IslandData existing = islandRepository.getForPlayer(playerRef.getUuid());
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        int defaultResets = Math.max(0, pluginConfig.getSettings().getMaxResets());
        int remainingBefore = playerResetRepository.getRemaining(playerRef.getUuid(), defaultResets);
        if (remainingBefore <= 0) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_resets_left"));
        }

        playerResetRepository.consumeOneCreate(playerRef.getUuid(), defaultResets);

        IslandTemplate selectedTemplate = templateManager.get(templateId);
        if (selectedTemplate == null || !selectedTemplate.isEnabled()) {
            playerResetRepository.addSystemResets(playerRef.getUuid(), 1, defaultResets);
            return CompletableFuture.failedFuture(new IllegalStateException("Selected island template is unavailable"));
        }
        final IslandTemplate template = selectedTemplate;

        GridAllocator.GridPos pos = gridAllocator.allocate();
        int zoneSize = resolveConfiguredIslandZoneSize(playerRef.getUuid());
        int step = getGridStep(zoneSize);
        int y = pluginConfig.getGrid().getIslandY();
        int anchorX = pos.x * step;
        int anchorZ = pos.z * step;

        int originX = anchorX;
        int originZ = anchorZ;
        // Try native prefab first (SelectionPrefabSerializer BlockSelection)
        BlockSelection prefabBsdon = templateManager.loadCapturedPrefabBsdon(template.getPrefabName());
        if (prefabBsdon != null) {
            Vector3i min = prefabBsdon.getSelectionMin();
            Vector3i max = prefabBsdon.getSelectionMax();
            int width = max.x() - min.x() + 1;
            int length = max.z() - min.z() + 1;
            originX = anchorX - (width / 2);
            originZ = anchorZ - (length / 2);
        } else {
            IslandSchematic captured = templateManager.loadCapturedPrefab(template.getPrefabName());
            if (captured != null) {
                // Captured schematics are stored from selection min (0..size-1); center them in the island cell.
                originX = anchorX - (captured.getWidth() / 2);
                originZ = anchorZ - (captured.getLength() / 2);
            }
        }
        final int finalOriginX = originX;
        final int finalOriginZ = originZ;

        return worldManager.createOrLoadVoidWorld(pluginConfig.getInit().getWorldName())
            .thenCompose(world -> world.getChunkAsync(com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(finalOriginX, finalOriginZ))
                .thenCompose(chunk -> generator.generate(world, template, finalOriginX, y, finalOriginZ)
                    .thenCompose(v -> {
                        IslandData pending = new IslandData();
                        pending.setGridX(pos.x);
                        pending.setGridZ(pos.z);
                        pending.setZoneSize(zoneSize);
                        return loadIslandZoneChunks(world, pending);
                    })
                    .thenCompose(v -> {
                        int spawnX = finalOriginX + template.getSpawnOffsetX();
                        int spawnY = y + template.getSpawnOffsetY();
                        int spawnZ = finalOriginZ + template.getSpawnOffsetZ();
                        float spawnPitch = template.getSpawnOffsetPitch();
                        float spawnYaw = template.getSpawnOffsetYaw();

                        IslandData island = new IslandData();
                        island.setOwnerUuid(playerRef.getUuid());
                        island.setOwnerName(playerRef.getUsername());
                        island.setIslandName(buildDefaultIslandName(playerRef.getUsername()));
                        island.setWorldName(world.getName());
                        island.setGridX(pos.x);
                        island.setGridZ(pos.z);
                        island.setZoneSize(zoneSize);
                        island.setTemplateId(template.getId());
                        island.setGenerated(true);

                        island.setPreferredSpawnX(spawnX);
                        island.setPreferredSpawnY(spawnY);
                        island.setPreferredSpawnZ(spawnZ);
                        island.setPreferredSpawnPitch(spawnPitch);
                        island.setPreferredSpawnYaw(spawnYaw);
                        island.setPreferredSpawnConfigured(true);
                        island.setLeaderUuid(playerRef.getUuid());

                        int[] safeSpawn = resolveSafeSpawn(world, island, new int[] {spawnX, spawnY, spawnZ});
                        island.setCenterX(safeSpawn[0]);
                        island.setCenterY(safeSpawn[1]);
                        island.setCenterZ(safeSpawn[2]);

                        islandRepository.put(island);
                        return TeleportUtil.teleport(playerRef, sourceWorld, world, safeSpawn[0], safeSpawn[1], safeSpawn[2], spawnPitch, spawnYaw).thenApply(unused -> island);
                    })
                )
            ).whenComplete((createdIsland, ex) -> {
                if (ex != null) {
                    // Roll back consumed reset on creation failure.
                    playerResetRepository.addSystemResets(playerRef.getUuid(), 1, defaultResets);
                }
            });
    }

    public boolean banPlayer(PlayerRef leaderRef, PlayerRef targetRef) {
        if (leaderRef == null || targetRef == null || targetRef.getUuid() == null) {
            throw new IllegalStateException("target_not_found");
        }
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_BANS)) {
            throw new IllegalStateException("not_leader");
        }

        UUID targetUuid = targetRef.getUuid();
        if (targetUuid.equals(leaderRef.getUuid())) throw new IllegalStateException("self");
        if (island.isOnTeam(targetUuid)) throw new IllegalStateException("cannot_ban_member");
        if (island.isBanned(targetUuid)) throw new IllegalStateException("already_banned");

        // If coop member, auto-remove from coop before banning
        island.removeCoopMember(targetUuid);

        boolean added = island.addBannedPlayer(targetUuid, targetRef.getUsername());
        if (added) {
            islandRepository.put(island);
        }
        return added;
    }

    public Map<String, String> listBannedPlayers(PlayerRef leaderRef) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_BANS)) {
            throw new IllegalStateException("not_leader");
        }
        return new LinkedHashMap<>(island.getBannedNames());
    }

    public String unbanPlayer(PlayerRef leaderRef, String targetName) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_BANS)) {
            throw new IllegalStateException("not_leader");
        }
        if (targetName == null || targetName.isBlank()) throw new IllegalStateException("not_banned");

        String targetUuidStr = null;
        String targetDisplayName = null;
        for (Map.Entry<String, String> entry : island.getBannedNames().entrySet()) {
            String name = entry.getValue();
            if (name != null && name.equalsIgnoreCase(targetName)) {
                targetUuidStr = entry.getKey();
                targetDisplayName = name;
                break;
            }
        }

        if (targetUuidStr == null) {
            for (String uuidStr : island.getBannedUuidStrings()) {
                if (uuidStr.equalsIgnoreCase(targetName)) {
                    targetUuidStr = uuidStr;
                    break;
                }
            }
        }

        if (targetUuidStr == null) throw new IllegalStateException("not_banned");

        UUID targetUuid = UUID.fromString(targetUuidStr);
        boolean removed = island.removeBannedPlayer(targetUuid);
        if (!removed) throw new IllegalStateException("not_banned");
        islandRepository.put(island);
        return (targetDisplayName == null || targetDisplayName.isBlank()) ? targetName : targetDisplayName;
    }

    public CompletableFuture<Void> leaveIslandAndTeleportToLobby(PlayerRef playerRef,
                                                                 World sourceWorld,
                                                                 String lobbyWorldName,
                                                                 int lobbyX,
                                                                 int lobbyY,
                                                                 int lobbyZ,
                                                                 float lobbyPitch,
                                                                 float lobbyYaw) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        // Non-leader member: just remove from team and teleport to lobby, island stays.
        if (island.isMember(playerRef.getUuid())) {
            UUID leavingUuid = playerRef.getUuid();
            getWarpsService().removeWarp(leavingUuid);
            getHomesService().clearHomes(island.getOwnerUuid(), leavingUuid);
            island.removeMember(leavingUuid);

            // Safety: if the leaving member is still the repository owner key
            // (e.g. an old owner who was demoted without ownership transfer),
            // re-key the island under the current leader so the leaver is fully detached.
            if (leavingUuid.equals(island.getOwnerUuid())) {
                UUID newOwner = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
                if (newOwner != null && !newOwner.equals(leavingUuid)) {
                    String newOwnerName = island.getMemberNames().get(newOwner.toString());
                    if (newOwnerName == null || newOwnerName.isBlank()) {
                        newOwnerName = getAllKnownPlayers().get(newOwner);
                    }
                    if (newOwnerName == null || newOwnerName.isBlank()) {
                        newOwnerName = island.getOwnerName();
                    }
                    islandRepository.remove(leavingUuid);
                    island.setOwnerUuid(newOwner);
                    island.setOwnerName(newOwnerName != null ? newOwnerName : "Leader");
                    island.setLeaderUuid(null);
                }
            }

            checkAndClearWallsIfNoPermission(island);
            islandRepository.put(island);
            World lobbyWorld = Universe.get().getWorld(lobbyWorldName);
            if (lobbyWorld == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Lobby world not loaded: " + lobbyWorldName));
            }
            return TeleportUtil.teleport(playerRef, sourceWorld, lobbyWorld, lobbyX, lobbyY, lobbyZ, lobbyPitch, lobbyYaw);
        }

        // Leader with remaining members: refuse.
        if (island.getMemberCount() > 0) {
            return CompletableFuture.failedFuture(new IllegalStateException("leader_has_members"));
        }

        // Leader with no members: full delete.
        return deleteIslandAndTeleportToLobby(island, playerRef, sourceWorld, lobbyWorldName, lobbyX, lobbyY, lobbyZ, lobbyPitch, lobbyYaw);
    }

    private CompletableFuture<Void> deleteIslandAndTeleportToLobby(IslandData island,
                                                                    PlayerRef playerRef,
                                                                    World sourceWorld,
                                                                    String lobbyWorldName,
                                                                    int lobbyX, int lobbyY, int lobbyZ,
                                                                    float lobbyPitch, float lobbyYaw) {
        clearIslandPlayerData(island);

        World islandWorld = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<Void> clearFuture;
        if (islandWorld == null) {
            clearFuture = CompletableFuture.completedFuture(null);
        } else {
            clearFuture = loadIslandZoneChunks(islandWorld, island).thenCompose(v -> clearIslandZone(islandWorld, island));
        }

        return clearFuture.thenCompose(v -> {
            islandRepository.remove(island.getOwnerUuid());
            gridAllocator.release(island.getGridX(), island.getGridZ());

            World lobbyWorld = Universe.get().getWorld(lobbyWorldName);
            if (lobbyWorld == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Lobby world not loaded: " + lobbyWorldName));
            }

            return TeleportUtil.teleport(playerRef, sourceWorld, lobbyWorld, lobbyX, lobbyY, lobbyZ, lobbyPitch, lobbyYaw);
        });
    }

    /**
     * Removes a member (by name) from the leader's island and teleports them to lobby if online.
     * @param kickedMessage message sent to the kicked player (if online).
     * @return future that completes when the kicked player has been teleported (or immediately if offline).
     */
    public CompletableFuture<Void> kickMember(PlayerRef leaderRef,
                                              String memberName,
                                              Message kickedMessage,
                                              Message queuedMessage,
                                              String lobbyWorldName,
                                              int lobbyX, int lobbyY, int lobbyZ,
                                              float lobbyPitch, float lobbyYaw) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_REMOVE_MEMBER)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        // Resolve member UUID from name
        String targetUuidStr = null;
        for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(memberName)) {
                targetUuidStr = entry.getKey();
                break;
            }
        }
        if (targetUuidStr == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("member_not_found"));
        }

        UUID targetUuid = UUID.fromString(targetUuidStr);
        getWarpsService().removeWarp(targetUuid);
        getHomesService().clearHomes(island.getOwnerUuid(), targetUuid);
        island.removeMember(targetUuid);
        checkAndClearWallsIfNoPermission(island);

        // Teleport kicked player to lobby if online
        PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
        World lobbyWorld = Universe.get().getWorld(lobbyWorldName);
        World islandWorld = Universe.get().getWorld(island.getWorldName());
        if (targetRef != null) {
            if (lobbyWorld != null && islandWorld != null) {
                island.clearPendingLobbyTeleport(targetUuid);
                islandRepository.put(island);
                if (kickedMessage != null) {
                    Chat.send(targetRef, kickedMessage);
                }
                return TeleportUtil.teleport(targetRef, islandWorld, lobbyWorld, lobbyX, lobbyY, lobbyZ, lobbyPitch, lobbyYaw);
            }

            if (queuedMessage != null) {
                Chat.send(targetRef, queuedMessage);
            }
        }

        island.markPendingLobbyTeleport(targetUuid);
        islandRepository.put(island);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Transfers island leadership to a current member.
     * The old leader becomes a regular member.
     * @return the display name of the new leader (for success message formatting).
     */
    public String makeLeader(PlayerRef leaderRef, String memberName, Message notifyNewLeaderMessage) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_TRANSFER_LEADERSHIP)) {
            throw new IllegalStateException("not_leader");
        }

        String targetUuidStr = null;
        String targetDisplayName = null;
        for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(memberName)) {
                targetUuidStr = entry.getKey();
                targetDisplayName = entry.getValue();
                break;
            }
        }
        if (targetUuidStr == null) throw new IllegalStateException("not_member");

        UUID newLeaderUuid = UUID.fromString(targetUuidStr);
        UUID oldLeaderUuid = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
        String oldLeaderName = leaderRef.getUsername();
        if (oldLeaderUuid != null) {
            if (oldLeaderUuid.equals(island.getOwnerUuid()) && island.getOwnerName() != null && !island.getOwnerName().isBlank()) {
                oldLeaderName = island.getOwnerName();
            } else {
                String mappedOldLeaderName = island.getMemberNames().get(oldLeaderUuid.toString());
                if (mappedOldLeaderName != null && !mappedOldLeaderName.isBlank()) {
                    oldLeaderName = mappedOldLeaderName;
                } else {
                    String knownOldLeaderName = getAllKnownPlayers().get(oldLeaderUuid);
                    if (knownOldLeaderName != null && !knownOldLeaderName.isBlank()) {
                        oldLeaderName = knownOldLeaderName;
                    }
                }
            }
        }

        getWarpsService().removeWarp(oldLeaderUuid);
        getWarpsService().removeWarp(newLeaderUuid);

        UUID previousOwner = island.getOwnerUuid();

        island.removeMember(newLeaderUuid);
        island.setLeaderUuid(newLeaderUuid);
        if (oldLeaderUuid != null && !oldLeaderUuid.equals(newLeaderUuid)) {
            island.addMember(oldLeaderUuid, oldLeaderName);
        }

        // If the island was keyed by the previous owner UUID, move the repository key
        // to the new leader so the original owner no longer resolves to this island
        // after they leave. This prevents a former owner (now a member) from still
        // being treated as the island owner.
        if (previousOwner != null && !previousOwner.equals(newLeaderUuid)) {
            island.setOwnerUuid(newLeaderUuid);
            island.setOwnerName(targetDisplayName);
            islandRepository.remove(previousOwner);
        }

        islandRepository.put(island);

        // Notify new leader if online
        PlayerRef newLeaderRef = Universe.get().getPlayer(newLeaderUuid);
        if (newLeaderRef != null && notifyNewLeaderMessage != null) {
            Chat.send(newLeaderRef, notifyNewLeaderMessage);
        }
        return targetDisplayName;
    }

    /**
     * Adds the acceptor as a team member on the inviter's island.
     */
    public void addTeamMember(UUID inviterUuid, PlayerRef acceptorRef) {
        IslandData island = islandRepository.getForPlayer(inviterUuid);
        if (island == null) throw new IllegalStateException("no_island");
        getWarpsService().removeWarp(acceptorRef.getUuid());
        island.addMember(acceptorRef.getUuid(), acceptorRef.getUsername());
        islandRepository.put(island);
        tryGrowIslandForJoiningPlayer(island, acceptorRef.getUuid());
    }

    public boolean hasPendingLobbyTeleport(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        for (IslandData island : islandRepository.values()) {
            if (island.hasPendingLobbyTeleport(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean clearPendingLobbyTeleport(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        for (IslandData island : islandRepository.values()) {
            if (island.clearPendingLobbyTeleport(playerUuid)) {
                islandRepository.put(island);
                return true;
            }
        }
        return false;
    }

    public int getRemainingResets(UUID playerUuid) {
        int defaults = Math.max(0, pluginConfig.getSettings().getMaxResets());
        return playerResetRepository.getRemaining(playerUuid, defaults);
    }

    public int addManualResets(UUID playerUuid, int amount) {
        int defaults = Math.max(0, pluginConfig.getSettings().getMaxResets());
        return playerResetRepository.addResets(playerUuid, amount, defaults);
    }

    public int setManualResets(UUID playerUuid, int amount) {
        int defaults = Math.max(0, pluginConfig.getSettings().getMaxResets());
        return playerResetRepository.setResets(playerUuid, amount, defaults);
    }

    public int removeManualResets(UUID playerUuid, int amount) {
        int defaults = Math.max(0, pluginConfig.getSettings().getMaxResets());
        return playerResetRepository.removeResets(playerUuid, amount, defaults);
    }

    public UUID resolveKnownPlayerUuid(String targetNameOrUuid) {
        if (targetNameOrUuid == null || targetNameOrUuid.isBlank()) {
            throw new IllegalStateException("target_not_found");
        }

        String target = targetNameOrUuid.trim();

        try {
            return UUID.fromString(target);
        } catch (IllegalArgumentException ignored) {
        }

        for (PlayerRef online : Universe.get().getPlayers()) {
            if (online != null && online.getUuid() != null && online.getUsername() != null
                && online.getUsername().equalsIgnoreCase(target)) {
                return online.getUuid();
            }
        }

        for (Map.Entry<UUID, String> entry : getAllKnownPlayers().entrySet()) {
            String username = entry.getValue();
            if (username != null && username.equalsIgnoreCase(target)) {
                return entry.getKey();
            }
        }

        for (IslandData island : islandRepository.values()) {
            if (island == null) {
                continue;
            }

            if (island.getOwnerUuid() != null && island.getOwnerName() != null
                && island.getOwnerName().equalsIgnoreCase(target)) {
                return island.getOwnerUuid();
            }

            for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
                if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(target)) {
                    return UUID.fromString(entry.getKey());
                }
            }

            for (Map.Entry<String, String> entry : island.getCoopNames().entrySet()) {
                if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(target)) {
                    return UUID.fromString(entry.getKey());
                }
            }

            for (Map.Entry<String, String> entry : island.getBannedNames().entrySet()) {
                if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(target)) {
                    return UUID.fromString(entry.getKey());
                }
            }
        }

        throw new IllegalStateException("target_not_found");
    }

    public Map<UUID, String> getAllKnownPlayers() {
        Map<UUID, String> known = new LinkedHashMap<>();

        for (Map.Entry<UUID, String> entry : com.sloyni.main.ISkyblockPlugin.getInstance().getKnownPlayerRepository().all().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isBlank()) {
                known.put(entry.getKey(), entry.getValue());
            }
        }

        for (PlayerRef online : Universe.get().getPlayers()) {
            if (online != null && online.getUuid() != null && online.getUsername() != null && !online.getUsername().isBlank()) {
                known.put(online.getUuid(), online.getUsername());
            }
        }

        for (IslandData island : islandRepository.values()) {
            if (island == null) {
                continue;
            }

            if (island.getOwnerUuid() != null && island.getOwnerName() != null && !island.getOwnerName().isBlank()) {
                known.put(island.getOwnerUuid(), island.getOwnerName());
            }

            for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    String name = entry.getValue();
                    if (name != null && !name.isBlank()) {
                        known.put(uuid, name);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            for (Map.Entry<String, String> entry : island.getCoopNames().entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    String name = entry.getValue();
                    if (name != null && !name.isBlank()) {
                        known.put(uuid, name);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            for (Map.Entry<String, String> entry : island.getBannedNames().entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    String name = entry.getValue();
                    if (name != null && !name.isBlank()) {
                        known.put(uuid, name);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return known;
    }

    public String buildDefaultIslandName(String ownerName) {
        String safeOwnerName = ownerName == null || ownerName.isBlank() ? "Player" : ownerName.trim();
        String defaultName = safeOwnerName + "'s Island";
        return defaultName.length() <= 16 ? defaultName : defaultName.substring(0, 16).trim();
    }

    public String getIslandDisplayName(IslandData island) {
        if (island == null) {
            return "Unknown Island";
        }
        String islandName = island.getIslandName();
        if (islandName != null && !islandName.isBlank()) {
            return islandName;
        }
        return buildDefaultIslandName(island.getOwnerName());
    }

    public void setIslandName(PlayerRef playerRef, String islandName) {
        IslandData island = islandRepository.getForPlayer(playerRef.getUuid());
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        if (!island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.RENAME_ISLAND)) {
            throw new IllegalStateException("not_leader");
        }
        island.setIslandName(islandName);
        islandRepository.put(island);
    }

    public void setIslandNameByTarget(UUID targetUuid, String islandName) {
        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        island.setIslandName(islandName);
        islandRepository.put(island);
    }

    public void resetIslandNameByTarget(UUID targetUuid) {
        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        island.setIslandName(buildDefaultIslandName(island.getOwnerName()));
        islandRepository.put(island);
    }

    public CompletableFuture<IslandValueService.IslandValueResult> getIslandLevel(UUID targetUuid, IslandValueService islandValueService) {
        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        World islandWorld = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = islandWorld == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(islandWorld);

        return worldFuture.thenCompose(world -> islandValueService.calculateIslandValue(island, world, true));
    }

    public Map<String, IslandData> getNeighboringIslands(UUID playerUuid) {
        IslandData island = islandRepository.getForPlayer(playerUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }

        Map<String, IslandData> neighbors = new LinkedHashMap<>();
        neighbors.put("north", islandRepository.getByGrid(island.getGridX(), island.getGridZ() - 1));
        neighbors.put("south", islandRepository.getByGrid(island.getGridX(), island.getGridZ() + 1));
        neighbors.put("east", islandRepository.getByGrid(island.getGridX() + 1, island.getGridZ()));
        neighbors.put("west", islandRepository.getByGrid(island.getGridX() - 1, island.getGridZ()));
        return neighbors;
    }

    public void deleteIslandByTarget(UUID targetUuid) {
        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        scheduleIslandWorldCleanup(island);
        clearIslandPlayerData(island);
        islandRepository.remove(island.getOwnerUuid());
        gridAllocator.release(island.getGridX(), island.getGridZ());
    }

    private void clearIslandPlayerData(IslandData island) {
        if (island == null) {
            return;
        }

        Set<UUID> playersToClear = new LinkedHashSet<>();
        if (island.getOwnerUuid() != null) {
            playersToClear.add(island.getOwnerUuid());
        }
        if (island.getLeaderUuid() != null) {
            playersToClear.add(island.getLeaderUuid());
        }
        for (String memberUuid : island.getMemberNames().keySet()) {
            try {
                playersToClear.add(UUID.fromString(memberUuid));
            } catch (Exception ignored) {
            }
        }

        for (UUID playerUuid : playersToClear) {
            getWarpsService().removeWarp(playerUuid);
            getHomesService().clearHomes(island.getOwnerUuid(), playerUuid);
        }

        UUID ownerUuid = island.getOwnerUuid();
        if (ownerUuid != null) {
            com.sloyni.main.ISkyblockPlugin pluginInstance = com.sloyni.main.ISkyblockPlugin.getInstance();
            if (pluginInstance != null && pluginInstance.getIslandStatsService() != null) {
                pluginInstance.getIslandStatsService().clearIslandStats(ownerUuid);
            }
        }
    }

    public IslandData getIslandForTarget(UUID targetUuid) {
        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            throw new IllegalStateException("no_island");
        }
        return island;
    }

    public void addTeamMemberAsAdmin(UUID islandTargetUuid, UUID memberUuid, String memberName) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        if (memberUuid == null) {
            throw new IllegalStateException("target_not_found");
        }
        if (getPlayerIsland(memberUuid) != null) {
            throw new IllegalStateException("already_has_island");
        }
        if (island.isOnTeam(memberUuid)) {
            throw new IllegalStateException("already_member");
        }
        island.addMember(memberUuid, memberName);
        islandRepository.put(island);
        tryGrowIslandForJoiningPlayer(island, memberUuid);
    }

    public void removeTeamMemberAsAdmin(UUID islandTargetUuid, String memberNameOrUuid) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        String targetUuidStr = null;
        for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(memberNameOrUuid)) {
                targetUuidStr = entry.getKey();
                break;
            }
        }
        if (targetUuidStr == null) {
            try {
                targetUuidStr = UUID.fromString(memberNameOrUuid).toString();
            } catch (Exception ignored) {
            }
        }
        if (targetUuidStr == null) {
            throw new IllegalStateException("member_not_found");
        }
        UUID memberUuid = UUID.fromString(targetUuidStr);
        getWarpsService().removeWarp(memberUuid);
        getHomesService().clearHomes(island.getOwnerUuid(), memberUuid);
        island.removeMember(memberUuid);
        checkAndClearWallsIfNoPermission(island);
        islandRepository.put(island);
    }

    public void makeLeaderAsAdmin(UUID islandTargetUuid, String memberNameOrUuid) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        String targetUuidStr = null;
        String targetDisplayName = null;
        for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(memberNameOrUuid)) {
                targetUuidStr = entry.getKey();
                targetDisplayName = entry.getValue();
                break;
            }
        }
        if (targetUuidStr == null) {
            try {
                targetUuidStr = UUID.fromString(memberNameOrUuid).toString();
            } catch (Exception ignored) {
            }
        }
        if (targetUuidStr == null) {
            throw new IllegalStateException("member_not_found");
        }
        // Fallback: resolve display name from known players if not found in members
        if (targetDisplayName == null || targetDisplayName.isBlank()) {
            targetDisplayName = getAllKnownPlayers().get(UUID.fromString(targetUuidStr));
        }
        if (targetDisplayName == null || targetDisplayName.isBlank()) {
            targetDisplayName = memberNameOrUuid;
        }

        UUID newLeaderUuid = UUID.fromString(targetUuidStr);
        UUID oldLeaderUuid = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
        String oldLeaderName = oldLeaderUuid != null && oldLeaderUuid.equals(island.getOwnerUuid()) ? island.getOwnerName() : island.getMemberNames().getOrDefault(String.valueOf(oldLeaderUuid), "Leader");

        getWarpsService().removeWarp(oldLeaderUuid);
        getWarpsService().removeWarp(newLeaderUuid);

        UUID previousOwner = island.getOwnerUuid();

        island.removeMember(newLeaderUuid);
        island.setLeaderUuid(newLeaderUuid);
        if (oldLeaderUuid != null && !oldLeaderUuid.equals(newLeaderUuid)) {
            island.addMember(oldLeaderUuid, oldLeaderName);
        }

        // Transfer ownership to the new leader so the previous owner no longer
        // resolves as the island owner. This mirrors the player makeLeader flow.
        if (previousOwner != null && !previousOwner.equals(newLeaderUuid)) {
            island.setOwnerUuid(newLeaderUuid);
            island.setOwnerName(targetDisplayName);
            islandRepository.remove(previousOwner);
        }

        islandRepository.put(island);
    }

    public void addCoopMemberAsAdmin(UUID islandTargetUuid, UUID coopUuid, String coopName) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        if (coopUuid == null) {
            throw new IllegalStateException("target_not_found");
        }
        if (island.isOnTeam(coopUuid)) {
            throw new IllegalStateException("already_member");
        }
        if (island.isCoopMember(coopUuid)) {
            throw new IllegalStateException("already_coop");
        }
        island.addCoopMember(coopUuid, coopName);
        islandRepository.put(island);
        tryGrowIslandForJoiningPlayer(island, coopUuid);
    }

    public void removeCoopMemberAsAdmin(UUID islandTargetUuid, String coopNameOrUuid) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        String targetUuidStr = null;
        for (Map.Entry<String, String> entry : island.getCoopNames().entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(coopNameOrUuid)) {
                targetUuidStr = entry.getKey();
                break;
            }
        }
        if (targetUuidStr == null) {
            try {
                targetUuidStr = UUID.fromString(coopNameOrUuid).toString();
            } catch (Exception ignored) {
            }
        }
        if (targetUuidStr == null || !island.removeCoopMember(UUID.fromString(targetUuidStr))) {
            throw new IllegalStateException("not_coop");
        }
        islandRepository.put(island);
    }

    public Map<String, String> listCoopMembersAsAdmin(UUID islandTargetUuid) {
        return new LinkedHashMap<>(getIslandForTarget(islandTargetUuid).getCoopNames());
    }

    public void banPlayerAsAdmin(UUID islandTargetUuid, UUID targetUuid, String targetName) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        if (targetUuid == null) {
            throw new IllegalStateException("target_not_found");
        }
        if (island.isOnTeam(targetUuid)) {
            throw new IllegalStateException("cannot_ban_member");
        }
        if (island.isBanned(targetUuid)) {
            throw new IllegalStateException("already_banned");
        }
        island.removeCoopMember(targetUuid);
        island.addBannedPlayer(targetUuid, targetName);
        islandRepository.put(island);
    }

    public void unbanPlayerAsAdmin(UUID islandTargetUuid, String targetNameOrUuid) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        String targetUuidStr = null;
        for (Map.Entry<String, String> entry : island.getBannedNames().entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(targetNameOrUuid)) {
                targetUuidStr = entry.getKey();
                break;
            }
        }
        if (targetUuidStr == null) {
            try {
                targetUuidStr = UUID.fromString(targetNameOrUuid).toString();
            } catch (Exception ignored) {
            }
        }
        if (targetUuidStr == null || !island.removeBannedPlayer(UUID.fromString(targetUuidStr))) {
            throw new IllegalStateException("not_banned");
        }
        islandRepository.put(island);
    }

    public Map<String, String> listBannedPlayersAsAdmin(UUID islandTargetUuid) {
        return new LinkedHashMap<>(getIslandForTarget(islandTargetUuid).getBannedNames());
    }

    public void setIslandLockedAsAdmin(UUID islandTargetUuid, boolean locked) {
        IslandData island = getIslandForTarget(islandTargetUuid);
        island.setLocked(locked);
        islandRepository.put(island);
    }

    public int getDefaultIslandZoneSize() {
        return Math.max(1, pluginConfig.getInit().getIslandZoneSize());
    }

    public int getMinUpgradeableIslandZoneSize() {
        return getDefaultIslandZoneSize() + 1;
    }

    public int getMaxIslandExtension() {
        int gap = Math.max(0, pluginConfig.getInit().getIslandSpacing());
        return Math.max(0, (gap / 2) - 2);
    }

    public int getMaxAllowedIslandZoneSize() {
        return getDefaultIslandZoneSize() + getMaxIslandExtension();
    }

    public CompletableFuture<IslandSizeUpdateResult> setPlayerIslandSize(UUID targetUuid, int requestedSize) {
        if (targetUuid == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("target_not_found"));
        }

        int minSize = getMinUpgradeableIslandZoneSize();
        int maxSize = getMaxAllowedIslandZoneSize();
        if (requestedSize < minSize) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_island_size_min"));
        }
        if (requestedSize > maxSize) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_island_size_max"));
        }

        int playerSize = playerIslandSizeRepository.setZoneSizeMax(targetUuid, requestedSize, getDefaultIslandZoneSize());
        int normalizedPlayerSize = Math.max(getDefaultIslandZoneSize(), Math.min(playerSize, maxSize));

        IslandData island = islandRepository.getForPlayer(targetUuid);
        if (island == null) {
            return CompletableFuture.completedFuture(new IslandSizeUpdateResult(
                requestedSize,
                normalizedPlayerSize,
                false,
                0,
                false
            ));
        }

        int currentSize = normalizeIslandZoneSize(island.getZoneSize());
        int targetSize = Math.max(currentSize, normalizedPlayerSize);
        if (targetSize <= currentSize) {
            return CompletableFuture.completedFuture(new IslandSizeUpdateResult(
                requestedSize,
                normalizedPlayerSize,
                true,
                currentSize,
                false
            ));
        }

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        return worldFuture.thenCompose(targetWorld -> loadIslandZoneChunks(targetWorld, island, targetSize)
            .thenCompose(v -> {
                island.setZoneSize(targetSize);
                return getIslandWallsService().applyWallsAfterZoneResize(targetWorld, island, currentSize, targetSize)
                    .thenApply(unused -> {
                        islandRepository.put(island);
                        return null;
                    });
            })
            .thenApply(v -> new IslandSizeUpdateResult(
                requestedSize,
                normalizedPlayerSize,
                true,
                targetSize,
                true
            ))
        );
    }

    public int getPlayerMaxHomes(UUID playerUuid) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .getPlayerMaxHomes(playerUuid);
    }

    public int addManualMaxHomes(UUID playerUuid, int amount) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .addManualMaxHomes(playerUuid, amount);
    }

    public int setManualMaxHomes(UUID playerUuid, int amount) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .setManualMaxHomes(playerUuid, amount);
    }

    public int removeManualMaxHomes(UUID playerUuid, int amount) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .removeManualMaxHomes(playerUuid, amount);
    }

    public int getPlayerHomeCount(UUID playerUuid) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .getPlayerHomeCount(playerUuid);
    }

    public Map<String, com.sloyni.main.addons.homes.HomeLocation> listHomes(PlayerRef playerRef) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .listHomes(playerRef);
    }

    public boolean setHome(PlayerRef playerRef, World sourceWorld, String homeId, int x, int y, int z, float pitch, float yaw) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .setHome(playerRef, sourceWorld, homeId, x, y, z, pitch, yaw);
    }

    public void renameHome(PlayerRef playerRef, String oldHomeId, String newHomeId) {
        com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .renameHome(playerRef, oldHomeId, newHomeId);
    }

    public void deleteHome(PlayerRef playerRef, String homeId) {
        com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .deleteHome(playerRef, homeId);
    }

    public CompletableFuture<Void> teleportToHome(PlayerRef playerRef, World sourceWorld, String homeId) {
        return com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .teleportToHome(playerRef, sourceWorld, homeId);
    }

    public void applyGlobalResetDefaultChange(int oldDefault, int newDefault) {
        playerResetRepository.applyGlobalDefaultChange(Math.max(0, oldDefault), Math.max(0, newDefault));
    }

    public void applyGlobalHomeDefaultChange(int oldDefault, int newDefault) {
        com.sloyni.main.ISkyblockPlugin.getInstance()
            .getAddonManager()
            .getHomesAddon()
            .getService()
            .applyGlobalHomeDefaultChange(oldDefault, newDefault);
    }

    public void addCoopMember(PlayerRef leaderRef, PlayerRef targetRef) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_COOP)) {
            throw new IllegalStateException("not_leader");
        }
        if (targetRef == null || targetRef.getUuid() == null) throw new IllegalStateException("target_not_found");

        UUID targetUuid = targetRef.getUuid();
        if (targetUuid.equals(leaderRef.getUuid())) throw new IllegalStateException("self");
        if (island.isOnTeam(targetUuid)) throw new IllegalStateException("already_member");
        if (island.isCoopMember(targetUuid)) throw new IllegalStateException("already_coop");

        island.addCoopMember(targetUuid, targetRef.getUsername());
        islandRepository.put(island);
        tryGrowIslandForJoiningPlayer(island, targetUuid);
    }

    public String removeCoopMember(PlayerRef leaderRef, String targetName) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_COOP)) {
            throw new IllegalStateException("not_leader");
        }

        String targetUuidStr = null;
        String targetDisplayName = null;
        for (Map.Entry<String, String> entry : island.getCoopNames().entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(targetName)) {
                targetUuidStr = entry.getKey();
                targetDisplayName = entry.getValue();
                break;
            }
        }
        if (targetUuidStr == null) throw new IllegalStateException("not_coop");

        UUID targetUuid = UUID.fromString(targetUuidStr);
        island.removeCoopMember(targetUuid);
        islandRepository.put(island);
        return targetDisplayName;
    }

    public boolean removeCoopMember(PlayerRef leaderRef, UUID targetUuid) {
        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_COOP)) {
            throw new IllegalStateException("not_leader");
        }
        if (targetUuid == null) throw new IllegalStateException("not_coop");

        boolean removed = island.removeCoopMember(targetUuid);
        if (!removed) throw new IllegalStateException("not_coop");

        islandRepository.put(island);
        return true;
    }

    public Map<String, String> listCoopMembers(PlayerRef requesterRef) {
        IslandData island = islandRepository.getForPlayer(requesterRef.getUuid());
        if (island == null) throw new IllegalStateException("no_island");
        if (!island.hasMemberPermission(requesterRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_COOP)) {
            throw new IllegalStateException("not_leader");
        }
        return new LinkedHashMap<>(island.getCoopNames());
    }

    private IslandTemplate resolveTemplate(String templateId) {
        IslandTemplate template = templateManager.get(templateId);
        if (template == null) {
            template = templateManager.getDefault();
        }
        if (template != null) {
            template.applyDefaults();
        }
        return template;
    }

    private int[] resolveAndMaybeMigrateSpawn(IslandData island, IslandTemplate template) {
        int currentX = island.getCenterX();
        int currentY = island.getCenterY();
        int currentZ = island.getCenterZ();

        if (template == null) {
            return new int[] {currentX, currentY, currentZ};
        }

        int zoneSize = island.getZoneSize() > 0 ? island.getZoneSize() : pluginConfig.getInit().getIslandZoneSize();
        int step = getGridStep(zoneSize);
        int baseY = pluginConfig.getGrid().getIslandY();
        int legacyBaseX = island.getGridX() * step;
        int legacyBaseZ = island.getGridZ() * step;

        // Older saves used the generation anchor as center; move them to configured spawn offset.
        if (currentX == legacyBaseX && currentY == baseY && currentZ == legacyBaseZ) {
            int migratedX = legacyBaseX + template.getSpawnOffsetX();
            int migratedY = baseY + template.getSpawnOffsetY();
            int migratedZ = legacyBaseZ + template.getSpawnOffsetZ();
            island.setCenterX(migratedX);
            island.setCenterY(migratedY);
            island.setCenterZ(migratedZ);
            islandRepository.put(island);
            return new int[] {migratedX, migratedY, migratedZ};
        }

        return new int[] {currentX, currentY, currentZ};
    }

    private boolean isInsideIslandZone(IslandData island, int x, int z) {
        if (island == null) {
            return false;
        }
        int zoneSize = island.getZoneSize() > 0 ? island.getZoneSize() : pluginConfig.getInit().getIslandZoneSize();
        int step = getGridStep(zoneSize);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = zoneSize / 2;
        int minX = anchorX - half;
        int maxX = anchorX + half;
        int minZ = anchorZ - half;
        int maxZ = anchorZ + half;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private int[] getOrInitializePreferredSpawn(IslandData island, IslandTemplate template) {
        if (island.isPreferredSpawnConfigured()) {
            return new int[] {
                island.getPreferredSpawnX(),
                island.getPreferredSpawnY(),
                island.getPreferredSpawnZ()
            };
        }

        int[] initial = resolveAndMaybeMigrateSpawn(island, template);
        island.setPreferredSpawnX(initial[0]);
        island.setPreferredSpawnY(initial[1]);
        island.setPreferredSpawnZ(initial[2]);
        island.setPreferredSpawnConfigured(true);
        islandRepository.put(island);
        return initial;
    }

    private CompletableFuture<Void> loadIslandZoneChunks(World world, IslandData island) {
        int zoneSize = island == null ? getDefaultIslandZoneSize() : normalizeIslandZoneSize(island.getZoneSize());
        return loadIslandZoneChunks(world, island, zoneSize);
    }

    private CompletableFuture<Void> loadIslandZoneChunks(World world, IslandData island, int zoneSize) {
        if (world == null || island == null) {
            return CompletableFuture.completedFuture(null);
        }

        int safeZoneSize = Math.max(1, zoneSize);
        int step = getGridStep(safeZoneSize);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = safeZoneSize / 2;

        int minX = anchorX - half - 1;
        int maxX = anchorX + half + 1;
        int minZ = anchorZ - half - 1;
        int maxZ = anchorZ + half + 1;

        int minChunkX = minX >> 5;
        int maxChunkX = maxX >> 5;
        int minChunkZ = minZ >> 5;
        int maxChunkZ = maxZ >> 5;

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                futures.add(world.getChunkAsync(ChunkUtil.indexChunkFromBlock(cx << 5, cz << 5)));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private int[] resolveSafeSpawn(World world, IslandData island, int[] preferred) {
        if (preferred == null || preferred.length < 3) {
            return preferred;
        }

        int x = preferred[0];
        int y = preferred[1];
        int z = preferred[2];
        if (isSafeSpawn(world, x, y, z)) {
            return preferred;
        }

        int[] bedrock = findFirstBedrockInIslandZone(world, island);
        if (bedrock == null) {
            return preferred;
        }

        int highestSolidY = findHighestSolidY(world, bedrock[0], bedrock[2], bedrock[1], bedrock[1] + 128);
        int spawnY = highestSolidY >= bedrock[1] ? highestSolidY + 1 : bedrock[1] + 1;
        for (int testY = spawnY; testY <= spawnY + 16; testY++) {
            if (isSafeSpawn(world, bedrock[0], testY, bedrock[2])) {
                return new int[] {bedrock[0], testY, bedrock[2]};
            }
        }

        return new int[] {bedrock[0], spawnY, bedrock[2]};
    }

    private SpawnUpdateResult resolveAndPersistSpawn(World world, IslandData island, int[] preferred) {
        int[] safe = resolveSafeSpawn(world, island, preferred);
        if (safe == null || safe.length < 3) {
            throw new IllegalStateException("Unable to resolve island spawn");
        }

        boolean fallbackUsed = preferred != null
            && preferred.length >= 3
            && (safe[0] != preferred[0] || safe[1] != preferred[1] || safe[2] != preferred[2]);

        if (safe[0] != island.getCenterX() || safe[1] != island.getCenterY() || safe[2] != island.getCenterZ()) {
            island.setCenterX(safe[0]);
            island.setCenterY(safe[1]);
            island.setCenterZ(safe[2]);
            islandRepository.put(island);
        }

        return new SpawnUpdateResult(safe[0], safe[1], safe[2], fallbackUsed, island.getPreferredSpawnPitch(), island.getPreferredSpawnYaw());
    }

    private int[] findFirstBedrockInIslandZone(World world, IslandData island) {
        if (world == null || island == null) {
            return null;
        }

        int zoneSize = island.getZoneSize() > 0 ? island.getZoneSize() : pluginConfig.getInit().getIslandZoneSize();
        int step = getGridStep(zoneSize);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = zoneSize / 2;

        int minX = anchorX - half;
        int maxX = anchorX + half;
        int minZ = anchorZ - half;
        int maxZ = anchorZ + half;
        int minY = Math.max(1, pluginConfig.getGrid().getIslandY() - 64);
        int maxY = pluginConfig.getGrid().getIslandY() + 128;

        for (int xx = minX; xx <= maxX; xx++) {
            for (int zz = minZ; zz <= maxZ; zz++) {
                for (int yy = minY; yy <= maxY; yy++) {
                    BlockType type = getBlockType(world, xx, yy, zz);
                    if (type != null && IslandSchematic.isBedrockId(type.getId())) {
                        return new int[] {xx, yy, zz};
                    }
                }
            }
        }
        return null;
    }

    private int findHighestSolidY(World world, int x, int z, int minY, int maxY) {
        int highest = -1;
        for (int yy = minY; yy <= maxY; yy++) {
            BlockType type = getBlockType(world, x, yy, z);
            if (type != null && !isFluid(type)) {
                highest = yy;
            }
        }
        return highest;
    }

    private boolean isSafeSpawn(World world, int x, int y, int z) {
        if (y <= 1) {
            return false;
        }
        BlockType ground = getBlockType(world, x, y - 1, z);
        if (ground == null || isFluid(ground)) {
            return false;
        }
        if (getBlockType(world, x, y, z) != null) {
            return false;
        }
        return getBlockType(world, x, y + 1, z) == null;
    }

    private boolean isFluid(BlockType type) {
        if (type == null || type.getId() == null) {
            return false;
        }
        String id = type.getId().toLowerCase();
        return id.contains("water") || id.contains("lava") || id.contains("fluid");
    }

    private BlockType getBlockType(World world, int x, int y, int z) {
        if (world == null || y < 0) {
            return null;
        }
        try {
            WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                return null;
            }
            int index = chunk.getBlock(x, y, z);
            if (index == 0) {
                return null;
            }
            return (BlockType) BlockType.getAssetMap().getAsset(index);
        } catch (Exception ignored) {
            return null;
        }
    }

    private CompletableFuture<Void> clearIslandZone(World world, IslandData island) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        if (world == null || island == null) {
            done.complete(null);
            return done;
        }

        world.execute(() -> {
            try {
                BlockType airType = (BlockType) BlockType.getAssetMap().getAsset(0);
                if (airType == null) {
                    done.completeExceptionally(new IllegalStateException("Air block type unavailable"));
                    return;
                }
                int airIndex = BlockType.getAssetMap().getIndex(airType.getId());

                int zoneSize = island.getZoneSize() > 0 ? island.getZoneSize() : pluginConfig.getInit().getIslandZoneSize();
                int step = getGridStep(zoneSize);
                int anchorX = island.getGridX() * step;
                int anchorZ = island.getGridZ() * step;
                int half = zoneSize / 2;
                int minX = anchorX - half - 1;
                int maxX = anchorX + half + 1;
                int minZ = anchorZ - half - 1;
                int maxZ = anchorZ + half + 1;

                int minY = 0;
                int maxY = 319;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                        if (chunk == null) {
                            continue;
                        }

                        Ref<ChunkStore> columnRef = chunk.getReference();
                        Store<ChunkStore> chunkStore = columnRef != null ? columnRef.getStore() : null;
                        ChunkColumn column = (chunkStore != null && columnRef != null)
                            ? (ChunkColumn) chunkStore.getComponent(columnRef, ChunkColumn.getComponentType())
                            : null;

                        for (int y = minY; y <= maxY; y++) {
                            // Always clear fluid, even if block is already air.
                            if (column != null && chunkStore != null) {
                                try {
                                    int sectionIndex = ChunkUtil.chunkCoordinate(y);
                                    Ref<ChunkStore> sectionRef = column.getSection(sectionIndex);
                                    if (sectionRef != null) {
                                        FluidSection fluidSection = (FluidSection) chunkStore.getComponent(sectionRef, FluidSection.getComponentType());
                                        if (fluidSection != null) {
                                            fluidSection.setFluid(x, y, z, 0, (byte) 0);
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // Keep deletion resilient when section/fluid data is absent.
                                }
                            }

                            int current = chunk.getBlock(x, y, z);
                            if (current == 0) {
                                continue;
                            }
                            chunk.setBlock(x, y, z, airIndex, airType, 0, 0, 196);
                        }
                    }
                }

                done.complete(null);
            } catch (Exception ex) {
                done.completeExceptionally(ex);
            }
        });

        return done;
    }

    public boolean isProtectedWallBlock(World world, int x, int y, int z) {
        if (!com.sloyni.main.ISkyblockPlugin.getInstance().getAddonManager().isIslandWallsEnabled()) {
            return false;
        }
        return getIslandWallsService().isProtectedWallBlock(world, x, y, z);
    }

    private void scheduleIslandWorldCleanup(IslandData island) {
        if (island == null || island.getWorldName() == null || island.getWorldName().isBlank()) {
            return;
        }

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        worldFuture
            .thenCompose(targetWorld -> loadIslandZoneChunks(targetWorld, island).thenCompose(v -> clearIslandZone(targetWorld, island)))
            .exceptionally(ex -> null);
    }

    public boolean isHoneycombBorderBlock(World world, int x, int y, int z) {
        return getIslandWallsService().isHoneycombBorderBlock(world, x, y, z);
    }

    private void tryGrowIslandForJoiningPlayer(IslandData island, UUID playerUuid) {
        if (island == null || playerUuid == null) return;

        int currentSize = normalizeIslandZoneSize(island.getZoneSize());
        int playerSize = resolveConfiguredIslandZoneSize(playerUuid);
        int targetSize = Math.max(currentSize, playerSize);
        if (targetSize <= currentSize) return;

        // Persist the new size immediately so it survives even if async chunk/wall
        // operations fail or the server restarts before they complete.
        island.setZoneSize(targetSize);
        islandRepository.put(island);

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        worldFuture.thenCompose(targetWorld -> loadIslandZoneChunks(targetWorld, island, targetSize)
            .thenCompose(v -> getIslandWallsService().applyWallsAfterZoneResize(targetWorld, island, currentSize, targetSize))
            .thenApply(unused -> {
                // Re-save in case the in-memory island was reloaded between the first save and now
                islandRepository.put(island);
                return null;
            }))
            .exceptionally(ex -> null);
    }

    private int normalizeIslandZoneSize(int islandZoneSize) {
        int defaultSize = getDefaultIslandZoneSize();
        if (islandZoneSize <= 0) {
            return defaultSize;
        }
        int maxSize = getMaxAllowedIslandZoneSize();
        return Math.max(defaultSize, Math.min(islandZoneSize, maxSize));
    }

    private int resolveConfiguredIslandZoneSize(UUID playerUuid) {
        int defaultSize = getDefaultIslandZoneSize();
        int configured = playerIslandSizeRepository.getZoneSize(playerUuid, defaultSize);
        return normalizeIslandZoneSize(configured);
    }

    private int getGridStep(int zoneSize) {
        int gap = Math.max(0, pluginConfig.getInit().getIslandSpacing());
        // Keep grid anchors stable; all islands are allocated on the default grid step.
        int safeZone = Math.max(1, pluginConfig.getInit().getIslandZoneSize());
        return safeZone + gap;
    }
}
