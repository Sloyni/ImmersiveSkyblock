package com.sloyni.main.protection;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.Chat;
import javax.annotation.Nonnull;

public class IslandPlaceProtection extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public IslandPlaceProtection() {
        super(PlaceBlockEvent.class);
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull PlaceBlockEvent event) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            if (ref == null) {
                return;
            }

            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin")) {
                return;
            }

            EntityStore ext = (EntityStore) store.getExternalData();
            World world = ext != null ? ext.getWorld() : null;
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

            Vector3i pos = event.getTargetBlock();
            IslandData island = plugin.getProtectionService().getIslandAtBlock(pos.x(), pos.z());

            if (island == null) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.outside_boundary"));
                return;
            }

            if (!plugin.getProtectionService().canModify(island, playerRef.getUuid())) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.place_denied"));
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandPlaceProtection error: " + ex.getMessage());
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
