package com.sloyni.main.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import javax.annotation.Nonnull;

/**
 * Listens for mob (non-player) death events on island worlds and increments
 * the island's mob-kill statistic via {@link com.sloyni.main.island.stats.IslandStatsService}.
 */
public class IslandMobKillStatsHandler extends RefChangeSystem<EntityStore, DeathComponent> {

    @Nonnull
    @Override
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return (Query<EntityStore>) DeathComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull DeathComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Count all non-player deaths in island worlds.
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            return;
        }

        EntityStore entityStore = (EntityStore) store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (world == null) {
            return;
        }
        if (transform == null) {
            return;
        }

        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null || plugin.getIslandStatsService() == null) {
            return;
        }

        if (!plugin.getIslandAccessController().isIslandRoutingWorld(world)) {
            return;
        }

        int blockX = (int) Math.floor(transform.getPosition().x);
        int blockZ = (int) Math.floor(transform.getPosition().z);
        plugin.getIslandStatsService().recordMobKillAt(blockX, blockZ);
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull DeathComponent component,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Not used
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref,
                               DeathComponent oldComponent,
                               @Nonnull DeathComponent newComponent,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Not used
    }
}
