package com.sloyni.main.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerWorldData;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public class IslandRespawnRoutingHandler extends RefChangeSystem<EntityStore, DeathComponent> {
    @Nonnull
    @Override
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull DeathComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        EntityStore entityStore = (EntityStore) store.getExternalData();
        World sourceWorld = entityStore == null ? null : entityStore.getWorld();
        if (playerRef == null || player == null || sourceWorld == null) {
            return;
        }

        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null || !plugin.getIslandAccessController().isIslandRoutingWorld(sourceWorld)) {
            return;
        }

        plugin.getIslandAccessController().resolveRoutingDestination(playerRef, sourceWorld)
            .thenAccept(destination -> {
                if (destination == null) {
                    return;
                }
                if (!sourceWorld.getName().equals(destination.world().getName())) {
                    return;
                }

                try {
                    PlayerWorldData perWorldData = player.getPlayerConfigData().getPerWorldData(sourceWorld.getName());
                    Vector3d respawnPosition = new Vector3d(destination.x(), destination.y(), destination.z());
                    Vector3i blockPosition = new Vector3i(
                        (int) Math.floor(destination.x()),
                        (int) Math.floor(destination.y()) - 1,
                        (int) Math.floor(destination.z())
                    );
                    perWorldData.setRespawnPoints(new PlayerRespawnPointData[] {
                        new PlayerRespawnPointData(blockPosition, respawnPosition, "ISkyblock")
                    });
                } catch (Exception ex) {
                    System.err.println("[ISkyblock] Failed to set respawn point: " + ex.getMessage());
                }
            });
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull DeathComponent component,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        EntityStore entityStore = (EntityStore) store.getExternalData();
        World sourceWorld = entityStore == null ? null : entityStore.getWorld();
        if (playerRef == null || sourceWorld == null) {
            return;
        }

        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null || !plugin.getIslandAccessController().isIslandRoutingWorld(sourceWorld)) {
            return;
        }

        plugin.getIslandAccessController().resolveRoutingDestination(playerRef, sourceWorld).thenAccept(destination -> {
            if (destination == null) {
                return;
            }

            CompletableFuture.delayedExecutor(200L, TimeUnit.MILLISECONDS).execute(() -> {
                try {
                    plugin.getIslandAccessController().routePlayerToIslandOrLobby(playerRef, sourceWorld);
                } catch (Exception ex) {
                    System.err.println("[ISkyblock] Failed to route respawned player: " + ex.getMessage());
                }
            });
        });
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref,
                               DeathComponent oldComponent,
                               @Nonnull DeathComponent newComponent,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}