package com.sloyni.main.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.interface_.ShowEventTitle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Periodically checks player positions and shows a title when entering or
 * leaving an island's protected zone. Mimics the "area discovery" style
 * titles used elsewhere in the game.
 */
public class IslandAreaTitleSystem extends EntityTickingSystem<EntityStore> {

    private static final float FADE_IN  = 0.5f;
    private static final float DISPLAY  = 2.5f;
    private static final float FADE_OUT = 0.5f;

    /**
     * Maps player UUID → the island owner UUID they are currently inside.
     * {@code null} means they are in the wilderness (outside any island zone).
     */
    private final Map<UUID, UUID> currentZone = new ConcurrentHashMap<>();

    private final Query<EntityStore> query;
    private final ComponentType<EntityStore, Player> playerComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    public IslandAreaTitleSystem() {
        this.playerComponentType = Player.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.query = (Query<EntityStore>) Query.and(
            (Query) playerComponentType,
            (Query) transformComponentType
        );
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        EntityStore entityStore = store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null) {
            return;
        }

        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        // Only run in the island world
        if (!plugin.getProtectionService().isIslandWorld(world.getName())) {
            return;
        }

        Player player = (Player) archetypeChunk.getComponent(index, playerComponentType);
        if (player == null) {
            return;
        }

        TransformComponent transform = (TransformComponent) archetypeChunk.getComponent(index, transformComponentType);
        if (transform == null) {
            return;
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        Vector3d position = transform.getPosition();
        int px = (int) Math.floor(position.x);
        int pz = (int) Math.floor(position.z);

        IslandData islandHere = plugin.getProtectionService().getIslandAtBlock(px, pz);
        UUID newZoneOwner = (islandHere != null) ? islandHere.getOwnerUuid() : null;
        UUID oldZoneOwner = currentZone.get(playerUuid);

        // Skip if no change
        if (areEqual(newZoneOwner, oldZoneOwner)) {
            return;
        }

        // Zone changed – update tracking and send title
        if (newZoneOwner != null) {
            currentZone.put(playerUuid, newZoneOwner);
        } else {
            currentZone.remove(playerUuid);
        }

        sendZoneTitle(playerRef, islandHere, plugin);
    }

    private static boolean areEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static void sendZoneTitle(PlayerRef playerRef,
                                      IslandData island,
                                      ISkyblockPlugin plugin) {
        PacketHandler handler = playerRef.getPacketHandler();
        if (handler == null) {
            return;
        }

        if (island != null) {
            // Entering an island zone
            String islandName = island.getIslandName();
            if (islandName == null || islandName.isBlank()) {
                islandName = island.getOwnerName() + "'s Island";
            }
            String ownerName = island.getOwnerName();
            if (ownerName == null) {
                ownerName = "";
            }

            FormattedMessage primary = new FormattedMessage();
            primary.rawText = islandName;
            primary.color = "ffa500"; // orange/gold

            FormattedMessage secondary = new FormattedMessage();
            secondary.rawText = "Entering " + ownerName + "'s territory";
            secondary.color = "7ac77a"; // green

            handler.write(new ShowEventTitle(FADE_IN, FADE_OUT, DISPLAY, null, false, primary, secondary));
        } else {
            // Leaving into wilderness
            FormattedMessage primary = new FormattedMessage();
            primary.rawText = "Wilderness";
            primary.color = "aaaaaa"; // grey

            FormattedMessage secondary = new FormattedMessage();
            secondary.rawText = "Leaving protected area";
            secondary.color = "e74c3c"; // red

            handler.write(new ShowEventTitle(FADE_IN, FADE_OUT, DISPLAY, null, false, primary, secondary));
        }
    }

    /**
     * Called when a player disconnects – clean up their zone state.
     */
    public void onPlayerDisconnect(UUID playerUuid) {
        if (playerUuid != null) {
            currentZone.remove(playerUuid);
        }
    }

    /**
     * Called on plugin shutdown – clear all zone state.
     */
    public void shutdown() {
        currentZone.clear();
    }
}
