package com.sloyni.main.menu;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandMenuHudManager {
    private final ISkyblockPlugin plugin;
    private final Map<UUID, IslandMenuHud> activeHuds = new ConcurrentHashMap<>();

    public IslandMenuHudManager(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void refreshHudForOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return;
        }
        try {
            for (IslandMenuHud hud : activeHuds.values()) {
                if (hud == null) continue;
                PlayerRef ref = hud.getPlayerRef();
                if (ref == null || ref.getUuid() == null) continue;
                IslandData myIsland = plugin.getIslandService().getPlayerIsland(ref.getUuid());
                if (myIsland == null) continue;
                if (!ownerUuid.equals(myIsland.getOwnerUuid())) continue;
                refreshHudForRef(ref);
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] Failed to refresh HUDs for owner " + ownerUuid + ": " + ex.getMessage());
        }
    }

    public void refreshHudForPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        try {
            IslandMenuHud hud = activeHuds.get(playerUuid);
            if (hud == null) {
                return;
            }
            PlayerRef ref = hud.getPlayerRef();
            if (ref == null || ref.getUuid() == null) {
                return;
            }
            refreshHudForRef(ref);
        } catch (Exception ex) {
            System.err.println("[ISkyblock] Failed to refresh HUD for player " + playerUuid + ": " + ex.getMessage());
        }
    }

    public void hideHudForPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        try {
            IslandMenuHud hud = activeHuds.remove(playerUuid);
            if (hud != null) {
                hideHud(hud);
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] Failed to hide HUD for player " + playerUuid + ": " + ex.getMessage());
        }
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
            if (playerRef == null || playerRef.getUuid() == null || world == null) {
                return;
            }

            if (!isIslandWorld(world)) {
                return;
            }

            showHud(playerRef, player);
        } catch (Exception ex) {
            System.err.println("[ISkyblock] Failed to handle PlayerReadyEvent for IslandMenuHudManager: " + ex.getMessage());
        }
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null) {
            return;
        }
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        IslandMenuHud hud = activeHuds.remove(playerRef.getUuid());
        if (hud != null) {
            hideHud(hud);
        }
    }

    public void shutdown() {
        for (IslandMenuHud hud : activeHuds.values()) {
            hideHud(hud);
        }
        activeHuds.clear();
    }

    private boolean isIslandWorld(World world) {
        if (world == null || world.getName() == null) {
            return false;
        }
        String configured = plugin.getConfigManager().getConfig().getInit().getWorldName();
        return configured != null && !configured.isBlank() && configured.equalsIgnoreCase(world.getName());
    }

    private void showHud(PlayerRef playerRef, Player player) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }

        IslandMenuHud existing = activeHuds.get(uuid);
        if (existing != null && !existing.isDestroyed()) {
            return;
        }

        IslandMenuHud hud = new IslandMenuHud(playerRef);
        activeHuds.put(uuid, hud);

        HudManager hudManager = player.getHudManager();
        if (hudManager != null) {
            hudManager.addCustomHud(playerRef, hud);
        }
    }

    private void hideHud(IslandMenuHud hud) {
        if (hud == null || hud.isDestroyed()) {
            return;
        }

        hud.markDestroyed();
        PlayerRef playerRef = hud.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        World world = resolveWorld(playerRef);
        if (world != null) {
            try {
                world.execute(() -> clearHudOnWorldThread(playerRef));
            } catch (Exception ex) {
                System.err.println("[ISkyblock] Failed to schedule HUD removal for disconnect: " + ex.getMessage());
            }
        }
    }

    private void clearHudOnWorldThread(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        HudManager hudManager = player.getHudManager();
        if (hudManager != null) {
            hudManager.removeCustomHud(playerRef, IslandMenuHud.KEY);
        }
    }

    private void refreshHudForRef(PlayerRef ref) {
        World world = resolveWorld(ref);
        if (world == null) {
            return;
        }

        World scheduledWorld = world;
        PlayerRef scheduledRef = ref;
        try {
            scheduledWorld.execute(() -> {
                try {
                    Ref<EntityStore> refObj = scheduledRef.getReference();
                    if (refObj == null || !refObj.isValid()) return;
                    Store<EntityStore> store = refObj.getStore();
                    if (store == null) return;
                    Player player = store.getComponent(refObj, Player.getComponentType());
                    if (player == null) return;
                    HudManager hudManager = player.getHudManager();
                    if (hudManager == null) return;

                    IslandMenuHud newHud = new IslandMenuHud(scheduledRef);
                    activeHuds.put(scheduledRef.getUuid(), newHud);
                    hudManager.removeCustomHud(scheduledRef, IslandMenuHud.KEY);
                    hudManager.addCustomHud(scheduledRef, newHud);
                } catch (Exception ignore) {}
            });
        } catch (Exception ignore) {
        }
    }

    private World resolveWorld(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef == null ? null : playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return null;
        }

        EntityStore entityStore = store.getExternalData();
        return entityStore == null ? null : entityStore.getWorld();
    }
}
