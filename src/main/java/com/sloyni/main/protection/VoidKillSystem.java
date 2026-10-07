package com.sloyni.main.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.TeleportUtil;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Removes NPCs and dropped items that fall into the void in island worlds.
 */
public class VoidKillSystem extends EntityTickingSystem<EntityStore> {
    private static final double DEFAULT_VOID_KILL_Y = 1.0D;
    private static final Set<UUID> VOID_TELEPORT_IN_PROGRESS = ConcurrentHashMap.newKeySet();

    private final Query<EntityStore> query;
    private final ComponentType<EntityStore, NPCEntity> npcComponentType;
    private final ComponentType<EntityStore, ItemComponent> itemComponentType;
    private final ComponentType<EntityStore, Player> playerComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    public VoidKillSystem() {
        this.npcComponentType = NPCEntity.getComponentType();
        this.itemComponentType = ItemComponent.getComponentType();
        this.playerComponentType = Player.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.query = (Query<EntityStore>) Query.and(
            (Query) Query.or(
                (Query) Query.or((Query) npcComponentType, (Query) itemComponentType),
                (Query) playerComponentType
            ),
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
        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null || world == null || !plugin.getIslandAccessController().isIslandRoutingWorld(world)) {
            return;
        }

        TransformComponent transform = (TransformComponent) archetypeChunk.getComponent(index, transformComponentType);
        if (transform == null) {
            return;
        }

        Vector3d position = transform.getPosition();
        if (position.y >= DEFAULT_VOID_KILL_Y) {
            return;
        }

        Player player = (Player) archetypeChunk.getComponent(index, playerComponentType);
        if (player != null) {
            // If void teleport is enabled, teleport everyone
            if (plugin.getConfigManager().getConfig().getSettings().isVoidTeleportEnabled()) {
                tryTeleportPlayerOutOfVoid(plugin, world, ref);
                return;
            }
            // Void teleport disabled — owner/team/coop die, visitors get teleported
            PlayerRef pRef = findPlayerRef(world, ref);
            if (pRef != null && pRef.getUuid() != null) {
                int px = (int) Math.floor(position.x);
                int pz = (int) Math.floor(position.z);
                IslandData islandHere = plugin.getProtectionService().getIslandAtBlock(px, pz);
                if (islandHere != null && islandHere.canModify(pRef.getUuid())) {
                    return; // canModify — die
                }
                if (islandHere == null && plugin.getIslandService().getPlayerIsland(pRef.getUuid()) != null) {
                    return; // fell off own island — die
                }
            }
            tryTeleportPlayerOutOfVoid(plugin, world, ref);
            return;
        }

        NPCEntity npc = (NPCEntity) archetypeChunk.getComponent(index, npcComponentType);
        if (npc != null && plugin.getConfigManager().getConfig().getSettings().isVoidKillEntitiesEnabled()) {
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            return;
        }

        ItemComponent item = (ItemComponent) archetypeChunk.getComponent(index, itemComponentType);
        if (item != null && plugin.getConfigManager().getConfig().getSettings().isVoidKillItemsEnabled()) {
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
        }
    }

    private void tryTeleportPlayerOutOfVoid(ISkyblockPlugin plugin, World sourceWorld, Ref<EntityStore> ref) {
        PlayerRef playerRef = findPlayerRef(sourceWorld, ref);
        if (playerRef == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null || !VOID_TELEPORT_IN_PROGRESS.add(playerUuid)) {
            return;
        }

        plugin.getIslandService().resolveIslandTeleportTarget(playerUuid)
            .thenCompose(target -> TeleportUtil.teleport(playerRef, sourceWorld, target.world(), target.x(), target.y(), target.z(), target.pitch(), target.yaw()))
            .exceptionally(ex -> {
                String lobbyWorld = plugin.getConfigManager().getConfig().getLobby().getWorldName();
                int lobbyX = plugin.getConfigManager().getConfig().getLobby().getX();
                int lobbyY = plugin.getConfigManager().getConfig().getLobby().getY();
                int lobbyZ = plugin.getConfigManager().getConfig().getLobby().getZ();
                float lobbyPitch = plugin.getConfigManager().getConfig().getLobby().getPitch();
                float lobbyYaw = plugin.getConfigManager().getConfig().getLobby().getYaw();
                World targetLobby = Universe.get().getWorld(lobbyWorld);
                if (targetLobby != null) {
                    TeleportUtil.teleport(playerRef, sourceWorld, targetLobby, lobbyX, lobbyY, lobbyZ, lobbyPitch, lobbyYaw);
                }
                return null;
            })
            .whenComplete((ignored, ex) -> VOID_TELEPORT_IN_PROGRESS.remove(playerUuid));
    }

    private PlayerRef findPlayerRef(World world, Ref<EntityStore> entityRef) {
        if (world == null || entityRef == null) {
            return null;
        }
        for (Player player : world.getPlayers()) {
            if (player == null || player.getPlayerRef() == null) {
                continue;
            }
            Ref<EntityStore> playerEntityRef = player.getPlayerRef().getReference();
            if (playerEntityRef == null) {
                continue;
            }
            if (playerEntityRef.getStore() == entityRef.getStore() && playerEntityRef.getIndex() == entityRef.getIndex()) {
                return player.getPlayerRef();
            }
        }
        return null;
    }
}
