package com.sloyni.main.protection;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandSchematic;
import com.sloyni.main.util.Chat;
import javax.annotation.Nonnull;

public class IslandBreakProtection extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public IslandBreakProtection() {
        super(BreakBlockEvent.class);
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull BreakBlockEvent event) {
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

            if (isBedrock(world, pos.x(), pos.y(), pos.z())) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.bedrock_denied"));
                return;
            }

            if (plugin.getAddonManager().isIslandWallsEnabled()
                && plugin.getIslandService().isProtectedWallBlock(world, pos.x(), pos.y(), pos.z())) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.wall_denied"));
                return;
            }

            IslandData island = plugin.getProtectionService().getIslandAtBlock(pos.x(), pos.z());

            if (island == null) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.outside_boundary"));
                return;
            }

            if (!plugin.getProtectionService().canModify(island, playerRef.getUuid())) {
                event.setCancelled(true);
                Chat.send(playerRef, plugin.tr(playerRef, "protection.break_denied"));
            }
        } catch (Exception ex) {
            System.err.println("[ISkyblock] IslandBreakProtection error: " + ex.getMessage());
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private boolean isBedrock(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                return false;
            }
            int index = chunk.getBlock(x, y, z);
            if (index == 0) {
                return false;
            }
            BlockType type = (BlockType) BlockType.getAssetMap().getAsset(index);
            return type != null && IslandSchematic.isBedrockId(type.getId());
        } catch (Exception ignored) {
            return false;
        }
    }
}
