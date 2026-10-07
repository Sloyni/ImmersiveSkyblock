package com.sloyni.main.menu;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.ui.IslandMenuPage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandMenuKeybindManager {
    private final ISkyblockPlugin plugin;
    private final Map<UUID, Boolean> walkKeyState = new ConcurrentHashMap<>();

    private PacketFilter packetFilter;
    private boolean started = false;

    public IslandMenuKeybindManager(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (started) {
            return;
        }

        packetFilter = (packetHandler, packet) -> {
            if (!(packetHandler instanceof GamePacketHandler gameHandler)) {
                return false;
            }
            if (!(packet instanceof ClientMovement movement)) {
                return false;
            }
            if (movement.movementStates == null) {
                return false;
            }

            PlayerRef playerRef = gameHandler.getPlayerRef();
            if (playerRef == null || playerRef.getUuid() == null) {
                return false;
            }

            boolean walkingNow = movement.movementStates.walking
                && !movement.movementStates.running
                && !movement.movementStates.sprinting;
            Boolean previous = walkKeyState.put(playerRef.getUuid(), walkingNow);

            if (walkingNow && (previous == null || !previous)) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    EntityStore entityStore = store == null ? null : store.getExternalData();
                    World world = entityStore == null ? null : entityStore.getWorld();
                    if (world != null) {
                        world.execute(() -> openMenuForPlayer(playerRef));
                    }
                }
            }

            return false;
        };

        PacketAdapters.registerInbound(packetFilter);
        started = true;
    }

    public void shutdown() {
        if (!started) {
            return;
        }

        try {
            if (packetFilter != null) {
                PacketAdapters.deregisterInbound(packetFilter);
            }
        } catch (Exception ignored) {
        }

        walkKeyState.clear();
        packetFilter = null;
        started = false;
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null || event.getPlayerRef() == null || event.getPlayerRef().getUuid() == null) {
            return;
        }
        walkKeyState.remove(event.getPlayerRef().getUuid());
    }

    private void openMenuForPlayer(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
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

        World world = player.getWorld();
        if (world == null) {
            EntityStore entityStore = store.getExternalData();
            world = entityStore == null ? null : entityStore.getWorld();
        }
        if (world == null) {
            return;
        }

        if (plugin.getIslandService().getPlayerIsland(playerRef.getUuid()) == null) {
            plugin.getIslandCommandHandler().handleIslandCommand(player, ref, store, playerRef, world);
            return;
        }

        IslandMenuPage.open(player, ref, store, playerRef, world, plugin);
    }
}
