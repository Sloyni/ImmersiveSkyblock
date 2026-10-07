package com.sloyni.main.protection;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.mobsmanager.MobsManagerAddon;
import com.sloyni.main.island.IslandData;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Blocks NPC spawns in inter-island gaps and inside the optional spawn protection cube.
 */
public class MobSpawnProtectionSystem extends RefSystem<EntityStore> {
    private static final double BASELINE_RATE_PERCENT = 20.0D;
    private static final double BASELINE_KEEP_RATIO = 0.80D;
    private static final int TORCH_RADIUS_MAX = 64;
    private static final Set<String> TORCH_IDS = Set.of(
        "Furniture_Crude_Torch",
        "Furniture_Feran_Torch",
        "Furniture_Feran_Torch_Tall",
        "Furniture_Desert_Torch",
        "Furniture_Jungle_Torch",
        "Furniture_Ancient_Torch",
        "Furniture_Human_Ruins_Torch",
        "Furniture_Temple_Emerald_Torch",
        "Wood_Torch_Wall"
    );

    private final Query<EntityStore> query;
    private final ComponentType<EntityStore, NPCEntity> npcComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    public MobSpawnProtectionSystem() {
        this.npcComponentType = NPCEntity.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.query = (Query<EntityStore>) Query.and((Query) npcComponentType, (Query) transformComponentType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref,
                              @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (reason != AddReason.SPAWN) {
            return;
        }

        EntityStore entityStore = store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
        if (plugin == null || world == null || !plugin.getIslandAccessController().isIslandRoutingWorld(world)) {
            return;
        }
        NPCEntity npc = store.getComponent(ref, npcComponentType);
        TransformComponent transform = store.getComponent(ref, transformComponentType);
        if (npc == null || transform == null) {
            return;
        }

        Vector3d pos = transform.getPosition();
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);

        MobsManagerAddon mobsManagerAddon = plugin.getAddonManager() == null ? null : plugin.getAddonManager().getMobsManagerAddon();
        boolean mobsManagerEnabled = mobsManagerAddon != null && mobsManagerAddon.isEnabled();
        boolean reduceMobsEnabled = mobsManagerEnabled && mobsManagerAddon.isReduceMobsEnabled();
        boolean torchProtectionEnabled = mobsManagerEnabled && mobsManagerAddon.isTorchProtectionEnabled();
        boolean islandWallsEnabled = plugin.getAddonManager() != null && plugin.getAddonManager().isIslandWallsEnabled();
        boolean spawnProtectionEnabled = plugin.getConfigManager().getConfig().getSettings().isSpawnProtectionEnabled();

        IslandData island = plugin.getProtectionService().getIslandAtBlock(x, z);

        if (reduceMobsEnabled) {
            if (islandWallsEnabled
                && (plugin.getIslandService().isProtectedWallBlock(world, x, y, z)
                || plugin.getIslandService().isProtectedWallBlock(world, x, y - 1, z)
                || plugin.getIslandService().isProtectedWallBlock(world, x, y + 1, z))) {
                scheduleRemoval(world, ref);
                return;
            }

            if (island == null) {
                scheduleRemoval(world, ref);
                return;
            }

            int ratePercent = mobsManagerAddon == null ? 20 : mobsManagerAddon.getReduceMobsRatePercent();
            double effectiveRemovalChance = computeEffectiveRemovalChance(ratePercent);
            if (shouldRemoveByReduceMobsRate(effectiveRemovalChance)) {
                scheduleRemoval(world, ref);
                return;
            }
        }

        if (torchProtectionEnabled && island != null) {
            int torchRadius = mobsManagerAddon == null
                ? 5
                : Math.max(1, Math.min(TORCH_RADIUS_MAX, mobsManagerAddon.getTorchProtectionRadius()));
            if (hasTorchNearby(world, x, y, z, torchRadius)) {
                scheduleRemoval(world, ref);
                return;
            }
        }

        if (!spawnProtectionEnabled || island == null) {
            return;
        }

        if (isInsideProtectedZone(plugin, island, x, y, z)) {
            scheduleRemoval(world, ref);
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref,
                               @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    private static void scheduleRemoval(@Nonnull World world, @Nonnull Ref<EntityStore> ref) {
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            Store<EntityStore> refStore = ref.getStore();
            if (refStore == null) {
                return;
            }
            try {
                refStore.removeEntity(ref, RemoveReason.REMOVE);
            } catch (Exception ignored) {
            }
        });
    }

    private boolean shouldRemoveByReduceMobsRate(double effectiveRemovalChance) {
        if (effectiveRemovalChance <= 0.0D) {
            return false;
        }
        if (effectiveRemovalChance >= 1.0D) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < effectiveRemovalChance;
    }

    private double computeEffectiveRemovalChance(int ratePercent) {
        if (ratePercent <= 0) {
            return 0.0D;
        }

        double keepRatio;
        if (ratePercent <= (int) BASELINE_RATE_PERCENT) {
            keepRatio = 1.0D - (ratePercent / 100.0D);
        } else {
            // Above baseline, increase intensity smoothly using inverse keep-ratio scaling.
            keepRatio = BASELINE_KEEP_RATIO * (BASELINE_RATE_PERCENT / (double) ratePercent);
        }

        keepRatio = Math.max(0.0D, Math.min(1.0D, keepRatio));
        double removalChance = 1.0D - keepRatio;

        // Make high rates noticeably harsher near 80-100% without a hard jump.
        if (ratePercent > 80) {
            double highRateFactor = (ratePercent - 80) / 20.0D;
            double boost = (1.0D - removalChance) * 0.75D * Math.pow(highRateFactor, 1.4D);
            removalChance += boost;
        }

        return Math.max(0.0D, Math.min(1.0D, removalChance));
    }

    private static boolean hasTorchNearby(@Nonnull World world, int centerX, int centerY, int centerZ, int radius) {
        int cappedRadius = Math.max(1, Math.min(TORCH_RADIUS_MAX, radius));
        int radiusSquared = cappedRadius * cappedRadius;
        Map<Long, WorldChunk> chunkCache = new HashMap<>();
        long centerChunkIndex = getChunkIndex(centerX, centerZ);

        for (int dx = -cappedRadius; dx <= cappedRadius; dx++) {
            for (int dy = -cappedRadius; dy <= cappedRadius; dy++) {
                for (int dz = -cappedRadius; dz <= cappedRadius; dz++) {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;

                    long chunkIndex = getChunkIndex(x, z);
                    if (chunkIndex != centerChunkIndex) {
                        continue;
                    }

                    WorldChunk chunk = getChunk(chunkCache, world, chunkIndex);
                    if (chunk == null) {
                        continue;
                    }

                    int blockTypeId;
                    try {
                        blockTypeId = chunk.getBlock(x, y, z);
                    } catch (Exception ignored) {
                        continue;
                    }

                    if (blockTypeId == 0) {
                        continue;
                    }

                    BlockType blockType = (BlockType) BlockType.getAssetMap().getAsset(blockTypeId);
                    if (blockType == null) {
                        continue;
                    }

                    if (isTorchBlockId(blockType.getId())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isTorchBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }

        if (TORCH_IDS.contains(blockId)) {
            return true;
        }

        String lower = blockId.toLowerCase(Locale.ROOT);
        return lower.contains("torch")
            || lower.contains("lantern")
            || lower.contains("brazier")
            || lower.contains("sconce");
    }

    private static WorldChunk getChunk(Map<Long, WorldChunk> chunkCache, World world, long chunkIndex) {
        if (chunkCache.containsKey(chunkIndex)) {
            return chunkCache.get(chunkIndex);
        }

        try {
            WorldChunk chunk = world.getChunk(chunkIndex);
            chunkCache.put(chunkIndex, chunk);
            return chunk;
        } catch (Exception e) {
            chunkCache.put(chunkIndex, null);
            return null;
        }
    }

    private static long getChunkIndex(int blockX, int blockZ) {
        int chunkX = blockX >> 5;
        int chunkZ = blockZ >> 5;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private boolean isInsideProtectedZone(ISkyblockPlugin plugin, IslandData island, int x, int y, int z) {
        ProtectedBounds bounds = resolveProtectedBounds(plugin, island);
        if (bounds == null) {
            return false;
        }

        return x >= bounds.minX && x <= bounds.maxX
            && y >= bounds.minY && y <= bounds.maxY
            && z >= bounds.minZ && z <= bounds.maxZ;
    }

    private ProtectedBounds resolveProtectedBounds(ISkyblockPlugin plugin, IslandData island) {
        int size = Math.max(1, plugin.getConfigManager().getConfig().getSettings().getSpawnProtectionRadius());
        int centerX = island.getCenterX();
        int centerY = island.getCenterY();
        int centerZ = island.getCenterZ();

        int half = size / 2;
        int minX = centerX - half;
        int minY = centerY - half;
        int minZ = centerZ - half;
        int maxX = minX + size - 1;
        int maxY = minY + size - 1;
        int maxZ = minZ + size - 1;

        return new ProtectedBounds(
            minX,
            maxX,
            minY,
            maxY,
            minZ,
            maxZ
        );
    }

    private record ProtectedBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    }
}
