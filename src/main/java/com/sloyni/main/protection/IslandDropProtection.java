package com.sloyni.main.protection;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.Chat;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Prevents players from dropping items on islands they do not own.
 */
public class IslandDropProtection extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    public IslandDropProtection() {
        super(DropItemEvent.PlayerRequest.class);
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull DropItemEvent.PlayerRequest event) {
        try {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            Player player = chunk.getComponent(index, Player.getComponentType());
            if (player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin")) {
                return;
            }

            EntityStore entityStore = (EntityStore) store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
            String worldName = world != null ? world.getName() : null;

            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (!plugin.getProtectionService().isIslandWorld(worldName)) {
                return;
            }

            if (plugin.getIslandAccessController().enforcePendingLobbyTeleport(playerRef, world)) {
                event.setCancelled(true);
                return;
            }
            if (plugin.getIslandAccessController().enforceLockedIslandAccess(playerRef, world)) {
                event.setCancelled(true);
                return;
            }

            int playerX = (int) playerRef.getTransform().getPosition().x;
            int playerZ = (int) playerRef.getTransform().getPosition().z;
            IslandData island = plugin.getProtectionService().getIslandAtBlock(playerX, playerZ);

            if (island == null) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.drop_denied"));
                return;
            }

            UUID playerUuid = playerRef.getUuid();
            if (!plugin.getProtectionService().canModify(island, playerUuid)) {
                if (island.isVisitorItemDropEnabled()) {
                    return;
                }
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.drop_denied"));
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandDropProtection error: " + ex.getMessage());
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
