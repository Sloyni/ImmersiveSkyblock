package com.sloyni.main.interactions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandSchematic;
import com.sloyni.main.util.Chat;

/**
 * Shared protection check logic used by all interaction codec overrides.
 */
final class IslandInteractionCheck {
    private IslandInteractionCheck() {}

    /**
     * Returns true if the player is allowed to interact at the given block coordinates.
     * Returns true on any internal error (fail-open).
     */
    static boolean isAllowed(InteractionContext context, int blockX, int blockZ) {
        try {
            Ref<EntityStore> entityRef = context.getEntity();
            Store<EntityStore> entityStore = entityRef.getStore();
            Player player = entityStore.getComponent(entityRef, Player.getComponentType());
            PlayerRef playerRef = entityStore.getComponent(entityRef, PlayerRef.getComponentType());
            if (player == null || playerRef == null) {
                return true;
            }
            World world = player.getWorld();
            if (world == null) {
                return true;
            }
            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (!plugin.getProtectionService().isIslandWorld(world.getName())) {
                return true;
            }
            if (plugin.getIslandAccessController().enforcePendingLobbyTeleport(playerRef, world)) {
                return false;
            }
            if (plugin.getIslandAccessController().enforceLockedIslandAccess(playerRef, world)) {
                return false;
            }
            if (com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin")) {
                return true;
            }
            IslandData island = plugin.getProtectionService().getIslandAtBlock(blockX, blockZ);
            if (island == null) {
                return false;
            }
            return plugin.getProtectionService().canModify(island, playerRef.getUuid());
        } catch (Exception e) {
            System.err.println("[ISkyblock] IslandInteractionCheck error: " + e.getMessage());
            return true;
        }
    }

    /** Cancels the interaction chain, sets state to Failed, and notifies the player. */
    static void reject(InteractionContext context, String messageKey) {
        try {
            if (context.getState() != null) {
                context.getState().state = InteractionState.Failed;
            }
        } catch (Exception ignored) {}
        try {
            InteractionChain chain = context.getChain();
            InteractionManager mgr = context.getInteractionManager();
            if (chain != null && mgr != null) {
                mgr.cancelChains(chain);
            }
        } catch (Exception ignored) {}
        try {
            Ref<EntityStore> entityRef = context.getEntity();
            PlayerRef playerRef = entityRef.getStore().getComponent(entityRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                // Use trNoPrefix for protection messages (no plugin prefix)
                Chat.send(playerRef, ISkyblockPlugin.getInstance().trNoPrefix(playerRef, messageKey));
            }
        } catch (Exception ignored) {}
    }

    /** Extracts block target coordinates from context via getTargetBlock() + reflection fallback. */
    static int[] extractCoords(InteractionContext context) {
        try {
            Object target = context.getTargetBlock();
            if (target == null) {
                return null;
            }
            // Try cast to Vector3i first (the normal API type)
            if (target instanceof org.joml.Vector3i) {
                org.joml.Vector3i v = (org.joml.Vector3i) target;
                return new int[]{v.x(), v.y(), v.z()};
            }
            // Reflection fallback for obfuscated or wrapped types
            try {
                java.lang.reflect.Field fx = target.getClass().getField("x");
                java.lang.reflect.Field fy = target.getClass().getField("y");
                java.lang.reflect.Field fz = target.getClass().getField("z");
                return new int[]{((Number) fx.get(target)).intValue(), ((Number) fy.get(target)).intValue(), ((Number) fz.get(target)).intValue()};
            } catch (Exception ignored) {}
            // Method fallback
            try {
                int x = ((Number) target.getClass().getMethod("getX").invoke(target)).intValue();
                int y = ((Number) target.getClass().getMethod("getY").invoke(target)).intValue();
                int z = ((Number) target.getClass().getMethod("getZ").invoke(target)).intValue();
                return new int[]{x, y, z};
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println("[ISkyblock] extractCoords error: " + e.getMessage());
        }
        return null;
    }

    static boolean isBedrockAt(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                return false;
            }
            int blockIndex = chunk.getBlock(x, y, z);
            if (blockIndex == 0) {
                return false;
            }
            BlockType type = (BlockType) BlockType.getAssetMap().getAsset(blockIndex);
            return type != null && IslandSchematic.isBedrockId(type.getId());
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean isProtectedWallAt(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        try {
            return ISkyblockPlugin.getInstance().getIslandService().isProtectedWallBlock(world, x, y, z);
        } catch (Exception ignored) {
            return false;
        }
    }
}
