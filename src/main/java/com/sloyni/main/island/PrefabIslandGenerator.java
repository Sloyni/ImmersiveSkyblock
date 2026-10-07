package com.sloyni.main.island;

import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PrefabIslandGenerator {
    private final IslandTemplateManager templateManager;

    public PrefabIslandGenerator(IslandTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    public CompletableFuture<Void> generate(World world, IslandTemplate template, int x, int y, int z) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder tried = new StringBuilder();
                BlockSelection prefab = resolvePrefab(template.getPrefabName(), tried);
                if (prefab != null) {
                    world.execute(() -> {
                        try {
                            prefab.place((CommandSender) null, world, new Vector3i(x, y, z), (BlockMask) null);
                            done.complete(null);
                        } catch (Exception ex) {
                            done.completeExceptionally(ex);
                        }
                    });
                    return;
                }

                // Try loading as a native prefab using SelectionPrefabSerializer
                // (same system as builder tools copy/paste — handles blocks, rotation,
                // fluids, chest contents, sign text, and all block entity data)
                BlockSelection captured = templateManager.loadCapturedPrefabBsdon(template.getPrefabName());
                if (captured != null) {
                    world.execute(() -> {
                        try {
                            captured.place((CommandSender) null, world, new Vector3i(x, y, z), (BlockMask) null);
                            done.complete(null);
                        } catch (Exception ex) {
                            done.completeExceptionally(ex);
                        }
                    });
                    return;
                }

                // Fallback: try the legacy IslandSchematic format
                IslandSchematic legacy = templateManager.loadCapturedPrefab(template.getPrefabName());
                if (legacy != null) {
                    placeSchematicLegacy(world, legacy, x, y, z)
                        .thenAccept(unused -> done.complete(null))
                        .exceptionally(ex -> {
                            done.completeExceptionally(ex);
                            return null;
                        });
                    return;
                }

                done.completeExceptionally(new IllegalStateException(
                    "Prefab not found: " + template.getPrefabName() + " (tried: " + tried + ")"));
            } catch (Exception ex) {
                done.completeExceptionally(ex);
            }
        });
        return done;
    }

    /**
     * Legacy fallback: manually place blocks from the old IslandSchematic format.
     * Does NOT preserve block entity data, fluids, or proper rotation.
     * Kept for backward compatibility with existing schematics.
     */
    @Deprecated
    private CompletableFuture<Void> placeSchematicLegacy(World world, IslandSchematic schematic,
                                                         int originX, int originY, int originZ) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        if (schematic.getBlocks().isEmpty()) {
            done.completeExceptionally(new IllegalStateException("Schematic has no blocks"));
            return done;
        }

        Map<Long, List<IslandSchematic.BlockData>> blocksByChunk = new HashMap<>();
        for (IslandSchematic.BlockData block : schematic.getBlocks()) {
            int worldX = originX + block.getX();
            int worldZ = originZ + block.getZ();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
            blocksByChunk.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(block);
        }

        List<CompletableFuture<?>> chunkFutures = new ArrayList<>();
        for (Long chunkIndex : blocksByChunk.keySet()) {
            chunkFutures.add(world.getChunkAsync(chunkIndex));
        }

        CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> world.execute(() -> {
                try {
                    for (Map.Entry<Long, List<IslandSchematic.BlockData>> entry : blocksByChunk.entrySet()) {
                        WorldChunk chunk = world.getChunk(entry.getKey());
                        if (chunk == null) {
                            continue;
                        }

                        for (IslandSchematic.BlockData block : entry.getValue()) {
                            BlockType blockType = BlockType.getAssetMap().getAsset(block.getBlockId());
                            if (blockType == null) {
                                continue;
                            }

                            int worldX = originX + block.getX();
                            int worldY = originY + block.getY();
                            int worldZ = originZ + block.getZ();
                            int blockIndex = BlockType.getAssetMap().getIndex(blockType.getId());
                            int settings = block.getSettings() != 0 ? block.getSettings() : 68;
                            chunk.setBlock(worldX, worldY, worldZ, blockIndex, blockType,
                                block.getRotation(), block.getFiller(), settings);
                        }
                    }
                    done.complete(null);
                } catch (Exception ex) {
                    done.completeExceptionally(ex);
                }
            }))
            .exceptionally(ex -> {
                done.completeExceptionally(ex);
                return null;
            });

        return done;
    }

    private BlockSelection resolvePrefab(String configuredName, StringBuilder tried) {
        String raw = configuredName == null ? "" : configuredName.trim();
        if (raw.isEmpty()) {
            return null;
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(raw);

        String noLeadingSlash = raw.startsWith("/") ? raw.substring(1) : raw;
        candidates.add(noLeadingSlash);

        if (noLeadingSlash.endsWith(".prefab.json")) {
            String base = noLeadingSlash.substring(0, noLeadingSlash.length() - ".prefab.json".length());
            candidates.add(base);
            candidates.add(base + ".prefab");
        }

        if (noLeadingSlash.endsWith(".json")) {
            candidates.add(noLeadingSlash.substring(0, noLeadingSlash.length() - ".json".length()));
        }

        if (!noLeadingSlash.contains("/")) {
            candidates.add("prefabs/" + noLeadingSlash);
            if (!noLeadingSlash.endsWith(".prefab.json")) {
                candidates.add("prefabs/" + noLeadingSlash + ".prefab.json");
            }
        } else if (noLeadingSlash.startsWith("prefabs/")) {
            String withoutPrefix = noLeadingSlash.substring("prefabs/".length());
            candidates.add(withoutPrefix);
            if (withoutPrefix.endsWith(".prefab.json")) {
                candidates.add(withoutPrefix.substring(0, withoutPrefix.length() - ".prefab.json".length()));
            }
        }

        for (String candidate : candidates) {
            if (tried.length() > 0) {
                tried.append(", ");
            }
            tried.append(candidate);
            BlockSelection prefab = PrefabStore.get().getAssetPrefabFromAnyPack(candidate);
            if (prefab != null) {
                return prefab;
            }

            // Fallback for server-side prefab files outside asset packs.
            try {
                prefab = PrefabStore.get().getServerPrefab(candidate);
            } catch (RuntimeException ignored) {
                prefab = null;
            }

            if (prefab != null) {
                return prefab;
            }
        }

        return null;
    }
}
