package com.sloyni.main.island;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.config.ConfigManager;
import com.sloyni.main.lang.MessageManager;
import com.sloyni.main.util.Chat;
import com.sloyni.main.util.TeleportUtil;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IslandAccessController {
    public record RoutingDestination(World world, int x, int y, int z, float pitch, float yaw) {}

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final IslandService islandService;
    private final IslandProtectionService protectionService;

    private final Set<UUID> pendingKickTeleportInProgress = ConcurrentHashMap.newKeySet();
    private final Set<UUID> lockedIslandTeleportInProgress = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bannedIslandTeleportInProgress = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lockedIslandTeleportCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bannedIslandTeleportCooldowns = new ConcurrentHashMap<>();
    private final ScheduledExecutorService lockedIslandScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ISkyblock-LockedIslandEnforcer");
        thread.setDaemon(true);
        return thread;
    });

    public IslandAccessController(ConfigManager configManager,
                                  MessageManager messageManager,
                                  IslandService islandService,
                                  IslandProtectionService protectionService) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.islandService = islandService;
        this.protectionService = protectionService;
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        try {
            if (event == null) {
                return;
            }
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }
            PlayerRef playerRef = player.getPlayerRef();
            World world = player.getWorld();
            if (playerRef == null || world == null) {
                return;
            }
            enforcePlayerAccessRules(player, playerRef, world);
        } catch (Exception ex) {
            System.err.println("[ISkyblock] Failed to handle PlayerReadyEvent: " + ex.getMessage());
        }
    }

    public void startLockedIslandEnforcement() {
        lockedIslandScheduler.scheduleAtFixedRate(this::runLockedIslandEnforcement, 1000L, 500L, TimeUnit.MILLISECONDS);
    }

    private void runLockedIslandEnforcement() {
        try {
            if (!configManager.getConfig().isInitialized()) {
                return;
            }
            String islandWorldName = configManager.getConfig().getInit().getWorldName();
            if (islandWorldName == null || islandWorldName.isBlank()) {
                return;
            }
            World islandWorld = Universe.get().getWorld(islandWorldName);
            if (islandWorld == null) {
                return;
            }
            try {
                islandWorld.execute(() -> {
                    try {
                        for (Player player : islandWorld.getPlayers()) {
                            if (player == null || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin")) {
                                continue;
                            }
                            PlayerRef playerRef = player.getPlayerRef();
                            if (playerRef != null) {
                                enforcePlayerAccessRules(player, playerRef, islandWorld);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[ISkyblock] Locked island enforcement iteration failed: " + ex.getMessage());
                    }
                });
            } catch (IllegalThreadStateException ignored) {
                // Le monde est en cours d'arrêt — son thread n'accepte plus de tâches.
                // C'est un état normal lors d'un shutdown, on ignore silencieusement.
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] Failed to run locked island enforcement: " + ex.getMessage());
        }
    }

    private boolean enforcePlayerAccessRules(Player player, PlayerRef playerRef, World world) {
        if (player != null && (com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin"))) {
            return false;
        }
        if (enforcePendingLobbyTeleport(playerRef, world)) {
            return true;
        }
        if (enforceBannedIslandAccess(playerRef, world)) {
            return true;
        }
        return enforceLockedIslandAccess(playerRef, world);
    }

    public boolean enforceBannedIslandAccess(PlayerRef playerRef, World sourceWorld) {
        if (playerRef == null || sourceWorld == null || !protectionService.isIslandWorld(sourceWorld.getName())) {
            return false;
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return false;
        }
        if (isBannedIslandTeleportOnCooldown(playerUuid)) {
            return true;
        }

        // Skip enforcement for admins and moderators
        try {
            if (playerRef.getHolder() != null) {
                Player player = playerRef.getHolder().getComponent(Player.getComponentType());
                if (player != null && (com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin"))) {
                    return false;
                }
            }
        } catch (Exception ignored) {
        }

        var transform = playerRef.getTransform();
        if (transform == null) {
            return false;
        }

        int playerX = (int) Math.floor(transform.getPosition().x);
        int playerZ = (int) Math.floor(transform.getPosition().z);
        IslandData islandAtLocation = protectionService.getIslandAtBlock(playerX, playerZ);
        if (islandAtLocation == null || !islandAtLocation.isBanned(playerUuid) || islandAtLocation.canModify(playerUuid)) {
            return false;
        }
        if (!bannedIslandTeleportInProgress.add(playerUuid)) {
            return true;
        }

        bannedIslandTeleportCooldowns.put(playerUuid, System.currentTimeMillis());
        Chat.send(playerRef, trNoPrefix(playerRef, "island.ban.blocked"));

        routePlayerToIslandOrLobby(playerRef, sourceWorld)
            .whenComplete((ignored, ex) -> bannedIslandTeleportInProgress.remove(playerUuid));
        return true;
    }

    public boolean enforceLockedIslandAccess(PlayerRef playerRef, World sourceWorld) {
        if (playerRef == null || sourceWorld == null || !protectionService.isIslandWorld(sourceWorld.getName())) {
            return false;
        }
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return false;
        }
        if (isLockedIslandTeleportOnCooldown(playerUuid)) {
            return true;
        }

        // Skip enforcement for admins and moderators
        try {
            if (playerRef.getHolder() != null) {
                Player player = playerRef.getHolder().getComponent(Player.getComponentType());
                if (player != null && (com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin"))) {
                    return false;
                }
            }
        } catch (Exception ignored) {
        }

        var transform = playerRef.getTransform();
        if (transform == null) {
            return false;
        }
        int playerX = (int) Math.floor(transform.getPosition().x);
        int playerZ = (int) Math.floor(transform.getPosition().z);
        IslandData islandAtLocation = protectionService.getIslandAtBlock(playerX, playerZ);
        if (islandAtLocation == null || !islandAtLocation.isLocked() || islandAtLocation.canModify(playerUuid)) {
            return false;
        }
        if (!lockedIslandTeleportInProgress.add(playerUuid)) {
            return true;
        }

        lockedIslandTeleportCooldowns.put(playerUuid, System.currentTimeMillis());
        Chat.send(playerRef, trNoPrefix(playerRef, "island.lock.visitor_removed"));

        routePlayerToIslandOrLobby(playerRef, sourceWorld)
            .whenComplete((ignored, ex) -> lockedIslandTeleportInProgress.remove(playerUuid));
        return true;
    }

    public boolean isIslandRoutingWorld(World world) {
        return world != null && protectionService.isIslandWorld(world.getName());
    }

    public CompletableFuture<Void> routePlayerToIslandOrLobby(PlayerRef playerRef, World sourceWorld) {
        return resolveRoutingDestination(playerRef, sourceWorld)
            .thenCompose(destination -> {
                if (destination == null) {
                    Chat.send(playerRef, tr(playerRef, "lobby.not_set"));
                    return CompletableFuture.completedFuture(null);
                }
                return TeleportUtil.teleport(
                    playerRef,
                    sourceWorld,
                    destination.world(),
                    destination.x(),
                    destination.y(),
                    destination.z(),
                    destination.pitch(),
                    destination.yaw()
                );
            });
    }

    public CompletableFuture<RoutingDestination> resolveRoutingDestination(PlayerRef playerRef, World sourceWorld) {
        if (playerRef == null || sourceWorld == null) {
            return CompletableFuture.completedFuture(null);
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return CompletableFuture.completedFuture(null);
        }

        IslandData destinationIsland = islandService.getPlayerIsland(playerUuid);
        if (destinationIsland != null) {
            return islandService.resolveIslandTeleportTarget(playerUuid)
                .thenApply(target -> new RoutingDestination(target.world(), target.x(), target.y(), target.z(), target.pitch(), target.yaw()))
                .exceptionally(ex -> getLobbyDestination());
        }

        return CompletableFuture.completedFuture(getLobbyDestination());
    }

    public void ejectLockedIslandVisitors(IslandData island) {
        if (island == null || island.getWorldName() == null || island.getWorldName().isBlank()) {
            return;
        }
        World islandWorld = Universe.get().getWorld(island.getWorldName());
        if (islandWorld == null) {
            return;
        }
        islandWorld.execute(() -> {
            try {
                for (Player player : islandWorld.getPlayers()) {
                    if (player == null || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin")) {
                        continue;
                    }
                    PlayerRef playerRef = player.getPlayerRef();
                    if (playerRef == null || island.canModify(playerRef.getUuid())) {
                        continue;
                    }
                    var transform = playerRef.getTransform();
                    if (transform == null) {
                        continue;
                    }
                    int playerX = (int) Math.floor(transform.getPosition().x);
                    int playerZ = (int) Math.floor(transform.getPosition().z);
                    IslandData islandAtLocation = protectionService.getIslandAtBlock(playerX, playerZ);
                    if (islandAtLocation != null
                        && islandAtLocation.getOwnerUuid() != null
                        && islandAtLocation.getOwnerUuid().equals(island.getOwnerUuid())) {
                        enforceLockedIslandAccess(playerRef, islandWorld);
                    }
                }
            } catch (Exception ex) {
                System.err.println("[ISkyblock] Failed to eject locked-island visitors: " + ex.getMessage());
            }
        });
    }

    public boolean enforcePendingLobbyTeleport(PlayerRef playerRef, World sourceWorld) {
        if (playerRef == null || sourceWorld == null) {
            return false;
        }
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return false;
        }
        if (!islandService.hasPendingLobbyTeleport(playerUuid)) {
            return false;
        }
        if (!pendingKickTeleportInProgress.add(playerUuid)) {
            return true;
        }

        var lobby = configManager.getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || lobby.getWorldName() == null || lobby.getWorldName().isBlank()) {
            pendingKickTeleportInProgress.remove(playerUuid);
            Chat.send(playerRef, tr(playerRef, "lobby.not_set"));
            return true;
        }

        World lobbyWorld = Universe.get().getWorld(lobby.getWorldName());
        if (lobbyWorld == null) {
            pendingKickTeleportInProgress.remove(playerUuid);
            Chat.send(playerRef, tr(playerRef, "lobby.not_set"));
            return true;
        }

        Chat.send(playerRef, tr(playerRef, "team.remove.kicked_offline_message"));
        TeleportUtil.teleport(playerRef, sourceWorld, lobbyWorld, lobby.getX(), lobby.getY(), lobby.getZ(), lobby.getPitch(), lobby.getYaw())
            .thenRun(() -> islandService.clearPendingLobbyTeleport(playerUuid))
            .whenComplete((ignored, ex) -> pendingKickTeleportInProgress.remove(playerUuid));

        return true;
    }

    private RoutingDestination getLobbyDestination() {
        var lobby = configManager.getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || lobby.getWorldName() == null || lobby.getWorldName().isBlank()) {
            return null;
        }
        World lobbyWorld = Universe.get().getWorld(lobby.getWorldName());
        if (lobbyWorld == null) {
            return null;
        }
        return new RoutingDestination(lobbyWorld, lobby.getX(), lobby.getY(), lobby.getZ(), lobby.getPitch(), lobby.getYaw());
    }

    private boolean isLockedIslandTeleportOnCooldown(UUID playerUuid) {
        Long lastTeleport = lockedIslandTeleportCooldowns.get(playerUuid);
        return lastTeleport != null && (System.currentTimeMillis() - lastTeleport) < 2000L;
    }

    private boolean isBannedIslandTeleportOnCooldown(UUID playerUuid) {
        Long lastTeleport = bannedIslandTeleportCooldowns.get(playerUuid);
        return lastTeleport != null && (System.currentTimeMillis() - lastTeleport) < 2000L;
    }

    private Message tr(PlayerRef playerRef, String key) {
        return ISkyblockPlugin.getInstance().tr(playerRef, key);
    }

    private Message tr(PlayerRef playerRef, String key, Map<String, String> vars) {
        return ISkyblockPlugin.getInstance().tr(playerRef, key, vars);
    }

    private Message trNoPrefix(PlayerRef playerRef, String key) {
        return ISkyblockPlugin.getInstance().trNoPrefix(playerRef, key);
    }

    private Message trNoPrefix(PlayerRef playerRef, String key, Map<String, String> vars) {
        return ISkyblockPlugin.getInstance().trNoPrefix(playerRef, key, vars);
    }
}