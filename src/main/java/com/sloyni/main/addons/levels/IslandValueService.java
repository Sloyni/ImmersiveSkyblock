package com.sloyni.main.addons.levels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.sloyni.main.config.LockFolderStorage;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.island.IslandSchematic;
import com.sloyni.main.island.IslandTemplate;
import com.sloyni.main.island.IslandTemplateManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IslandValueService {
    private static final long CACHE_TTL_MILLIS = 30_000L;
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 319;

    private static final Type VALUE_CACHE_TYPE = new TypeToken<Map<String, CachedValueEntry>>() {}.getType();
    private static final Type BLOCK_VALUES_TYPE = new TypeToken<Map<String, Number>>() {}.getType();

    public record IslandValueResult(double rawValue,
                                    double templatePenalty,
                                    double netValue,
                                    long level,
                                    long calculatedAt,
                                    boolean fromCache) {
    }

    public record CachedIslandTopEntry(UUID ownerUuid,
                                       String ownerName,
                                       String islandName,
                                       long level,
                                       double netValue,
                                       long calculatedAt) {
    }

    private static final class CachedValueEntry {
        double rawValue;
        double templatePenalty;
        double netValue;
        long level;
        long calculatedAt;
    }

    private static final class ChunkWindow {
        final long chunkIndex;
        final int minX;
        final int maxX;
        final int minZ;
        final int maxZ;

        private ChunkWindow(long chunkIndex, int minX, int maxX, int minZ, int maxZ) {
            this.chunkIndex = chunkIndex;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }

    private final PluginConfig.InitConfig initConfig;
    private final LevelsAddonConfig levelsConfig;
    private final IslandRepository islandRepository;
    private final IslandTemplateManager templateManager;
    private final Gson gson;
    private final File cacheFile;
    private final File blockValuesFile;

    private final Map<UUID, CachedValueEntry> valueCache = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<IslandValueResult>> inFlight = new ConcurrentHashMap<>();
    private final Map<String, Double> blockValues = new ConcurrentHashMap<>();
    private final Map<String, Double> templatePenaltyCache = new ConcurrentHashMap<>();

    private final ExecutorService calculatorExecutor = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
        runnable -> {
            Thread thread = new Thread(runnable, "ISkyblock-IslandValueCalculator");
            thread.setDaemon(true);
            return thread;
        }
    );

    public IslandValueService(File dataFolder,
                              IslandRepository islandRepository,
                              IslandTemplateManager templateManager,
                              PluginConfig.InitConfig initConfig,
                              LevelsAddonConfig levelsConfig) {
        this.initConfig = initConfig;
        this.levelsConfig = levelsConfig;
        this.islandRepository = islandRepository;
        this.templateManager = templateManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cacheFile = LockFolderStorage.resolveLockedFile(dataFolder, "island_value_cache.json");
        this.blockValuesFile = new File(dataFolder, "block_values.json");
        loadBlockValues();
        loadValueCache();
    }

    public CompletableFuture<IslandValueResult> calculateIslandValue(IslandData island, World world, boolean allowCache) {
        if (island == null || island.getOwnerUuid() == null || world == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_island_or_world"));
        }

        UUID ownerUuid = island.getOwnerUuid();
        CachedValueEntry cached = valueCache.get(ownerUuid);
        if (allowCache && isCacheFresh(cached)) {
            return CompletableFuture.completedFuture(toResult(cached, true));
        }

        return inFlight.computeIfAbsent(ownerUuid, ignored ->
            computeIslandValue(island, world)
                .whenComplete((result, throwable) -> inFlight.remove(ownerUuid))
        );
    }

    public void invalidateTemplatePenaltyCache() {
        templatePenaltyCache.clear();
    }

    public void clearValueCache() {
        valueCache.clear();
        saveValueCacheAsync();
    }

    public void invalidateCache(UUID ownerUuid) {
        if (ownerUuid != null) {
            valueCache.remove(ownerUuid);
        }
    }

    public IslandValueResult getCachedIslandValue(UUID ownerUuid) {
        CachedValueEntry cached = ownerUuid == null ? null : valueCache.get(ownerUuid);
        return cached == null ? null : toResult(cached, true);
    }

    public List<CachedIslandTopEntry> getTopCachedIslands(int limit) {
        List<CachedIslandTopEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, CachedValueEntry> entry : valueCache.entrySet()) {
            IslandData island = islandRepository.get(entry.getKey());
            CachedValueEntry cached = entry.getValue();
            if (island == null || cached == null) {
                continue;
            }
            entries.add(new CachedIslandTopEntry(
                entry.getKey(),
                island.getOwnerName(),
                island.getIslandName(),
                cached.level,
                cached.netValue,
                cached.calculatedAt
            ));
        }
        entries.sort((left, right) -> {
            int levelCompare = Long.compare(right.level(), left.level());
            if (levelCompare != 0) {
                return levelCompare;
            }
            int valueCompare = Double.compare(right.netValue(), left.netValue());
            if (valueCompare != 0) {
                return valueCompare;
            }
            return Long.compare(right.calculatedAt(), left.calculatedAt());
        });
        if (limit > 0 && entries.size() > limit) {
            return new ArrayList<>(entries.subList(0, limit));
        }
        return entries;
    }

    public Map<String, Double> getConfiguredBlockValuesSnapshot() {
        Map<String, Double> snapshot = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(blockValues.keySet());
        keys.sort(String.CASE_INSENSITIVE_ORDER);
        for (String key : keys) {
            Double value = blockValues.get(key);
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            snapshot.put(key, value);
        }
        return snapshot;
    }

    public String resolveCanonicalBlockId(String rawBlockId) {
        if (rawBlockId == null || rawBlockId.isBlank()) {
            return null;
        }
        String candidate = rawBlockId.trim();
        try {
            BlockType type = BlockType.getAssetMap().getAsset(candidate);
            if (type == null || type.getId() == null || type.getId().isBlank()) {
                return null;
            }
            return type.getId();
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean hasConfiguredBlockValue(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        String target = blockId.trim().toLowerCase(Locale.ROOT);
        for (String existing : blockValues.keySet()) {
            if (existing != null && existing.trim().toLowerCase(Locale.ROOT).equals(target)) {
                return true;
            }
        }
        return false;
    }

    public void setConfiguredBlockValue(String blockId, double value) {
        String canonical = resolveCanonicalBlockId(blockId);
        if (canonical == null) {
            return;
        }
        double safeValue = (Double.isNaN(value) || Double.isInfinite(value) || value <= 0D) ? 1D : value;
        blockValues.put(canonical, safeValue);
        saveBlockValues();
        clearValueCache();
        invalidateTemplatePenaltyCache();
    }

    public void removeConfiguredBlockValue(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return;
        }
        String target = blockId.trim().toLowerCase(Locale.ROOT);
        String matchedKey = null;
        for (String existing : blockValues.keySet()) {
            if (existing != null && existing.trim().toLowerCase(Locale.ROOT).equals(target)) {
                matchedKey = existing;
                break;
            }
        }
        if (matchedKey == null) {
            return;
        }
        blockValues.remove(matchedKey);
        saveBlockValues();
        clearValueCache();
        invalidateTemplatePenaltyCache();
    }

    private CompletableFuture<IslandValueResult> computeIslandValue(IslandData island, World world) {
        ZoneBounds bounds = computeZoneBounds(island);
        List<ChunkWindow> windows = computeChunkWindows(bounds);

        List<CompletableFuture<WorldChunk>> chunkFutures = new ArrayList<>(windows.size());
        for (ChunkWindow window : windows) {
            chunkFutures.add(world.getChunkAsync(window.chunkIndex));
        }

        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
            .thenApplyAsync(ignored -> {
                double rawValue = scanRawValue(chunkFutures, windows);
                double templatePenalty = resolveTemplatePenaltyAndPersist(island, rawValue);
                double netValue = levelsConfig.isTemplateValueEnabled() ? (rawValue - templatePenalty) : rawValue;
                int levelValue = Math.max(1, levelsConfig.getLevelValue());
                long level = (long) Math.floor(Math.max(0D, netValue) / (double) levelValue);

                // Snapshot top owners before the update so we can detect rank changes
                List<CachedIslandTopEntry> beforeTop = getTopCachedIslands(0);
                java.util.List<java.util.UUID> beforeOwners = new ArrayList<>();
                if (beforeTop != null) {
                    for (CachedIslandTopEntry e : beforeTop) {
                        if (e != null) beforeOwners.add(e.ownerUuid());
                    }
                }

                CachedValueEntry entry = new CachedValueEntry();
                entry.rawValue = rawValue;
                entry.templatePenalty = templatePenalty;
                entry.netValue = netValue;
                entry.level = level;
                entry.calculatedAt = System.currentTimeMillis();
                valueCache.put(island.getOwnerUuid(), entry);
                saveValueCacheAsync();

                try {
                    com.sloyni.main.ISkyblockPlugin plugin = com.sloyni.main.ISkyblockPlugin.getInstance();
                    // compute after snapshot and find owners whose rank changed
                    List<CachedIslandTopEntry> afterTop = getTopCachedIslands(0);
                    java.util.List<java.util.UUID> afterOwners = new ArrayList<>();
                    if (afterTop != null) {
                        for (CachedIslandTopEntry e : afterTop) {
                            if (e != null) afterOwners.add(e.ownerUuid());
                        }
                    }

                    java.util.Map<java.util.UUID, Integer> beforePos = new HashMap<>();
                    for (int i = 0; i < beforeOwners.size(); i++) {
                        beforePos.put(beforeOwners.get(i), i);
                    }
                    java.util.Map<java.util.UUID, Integer> afterPos = new HashMap<>();
                    for (int i = 0; i < afterOwners.size(); i++) {
                        afterPos.put(afterOwners.get(i), i);
                    }

                    java.util.Set<java.util.UUID> union = new java.util.HashSet<>();
                    union.addAll(beforePos.keySet());
                    union.addAll(afterPos.keySet());

                    // Always include the updated owner
                    union.add(island.getOwnerUuid());

                    for (java.util.UUID uid : union) {
                        int b = beforePos.getOrDefault(uid, -1);
                        int a = afterPos.getOrDefault(uid, -1);
                        if (a != b) {
                            try {
                                if (plugin != null && plugin.getIslandMenuHudManager() != null) {
                                    plugin.getIslandMenuHudManager().refreshHudForOwner(uid);
                                }
                            } catch (Exception ignore) {}
                        }
                    }
                } catch (Exception ignore) {
                }
                return toResult(entry, false);
            }, calculatorExecutor);
    }

    private double scanRawValue(List<CompletableFuture<WorldChunk>> chunkFutures, List<ChunkWindow> windows) {
        Map<Integer, Double> blockIndexValueCache = new HashMap<>();
        double total = 0D;

        for (int i = 0; i < windows.size(); i++) {
            WorldChunk chunk = chunkFutures.get(i).join();
            if (chunk == null) {
                continue;
            }
            ChunkWindow window = windows.get(i);

            for (int x = window.minX; x <= window.maxX; x++) {
                for (int z = window.minZ; z <= window.maxZ; z++) {
                    for (int y = MIN_Y; y <= MAX_Y; y++) {
                        int blockIndex;
                        try {
                            blockIndex = chunk.getBlock(x, y, z);
                        } catch (Exception ignored) {
                            continue;
                        }
                        if (blockIndex == 0) {
                            continue;
                        }

                        Double value = blockIndexValueCache.get(blockIndex);
                        if (value == null && !blockIndexValueCache.containsKey(blockIndex)) {
                            value = resolveBlockIndexValue(blockIndex);
                            blockIndexValueCache.put(blockIndex, value);
                        }
                        if (value != null && value != 0D) {
                            total += value;
                        }
                    }
                }
            }
        }

        return total;
    }

    private double resolveBlockIndexValue(int blockIndex) {
        try {
            BlockType type = (BlockType) BlockType.getAssetMap().getAsset(blockIndex);
            if (type == null || type.getId() == null || type.getId().isBlank()) {
                return 0D;
            }
            return resolveBlockIdValue(type.getId());
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private double resolveBlockIdValue(String blockId) {
        Double configured = blockValues.get(blockId);
        if (configured != null) {
            return configured;
        }
        double fallback = levelsConfig.getBlockDefaultValue();
        if (Double.isNaN(fallback) || Double.isInfinite(fallback) || fallback <= 0D) {
            return 1D;
        }
        return fallback;
    }

    private double getTemplatePenalty(IslandData island) {
        if (island.getTemplateId() == null || island.getTemplateId().isBlank()) {
            return 0D;
        }
        return templatePenaltyCache.computeIfAbsent(island.getTemplateId(), this::computeTemplatePenalty);
    }

    private double resolveTemplatePenaltyAndPersist(IslandData island, double rawValue) {
        if (island.isTemplateBaseValueConfigured()) {
            return Math.max(0D, island.getTemplateBaseValue());
        }

        // No template assigned: nothing to subtract, island starts counting from 0.
        if (island.getTemplateId() == null || island.getTemplateId().isBlank()) {
            island.setTemplateBaseValue(0D);
            island.setTemplateBaseValueConfigured(true);
            islandRepository.put(island);
            return 0D;
        }

        double templatePenalty = getTemplatePenalty(island);
        // Schematic could not be parsed or returned 0: use the island's current raw value as
        // the baseline (assumes this first scan happens right after island creation when only
        // template blocks are present).
        if (templatePenalty <= 0D) {
            templatePenalty = Math.max(0D, rawValue);
        }

        island.setTemplateBaseValue(templatePenalty);
        island.setTemplateBaseValueConfigured(true);
        islandRepository.put(island);
        return templatePenalty;
    }

    /**
     * Corrects islands that were incorrectly given a non-zero template penalty when they have no
     * template assigned (caused by the old rawValue failsafe). Called once at startup.
     */
    public void migrateIncorrectTemplatePenalties() {
        boolean anyFixed = false;
        for (IslandData island : islandRepository.values()) {
            if (island == null) {
                continue;
            }
            boolean hasNoTemplate = island.getTemplateId() == null || island.getTemplateId().isBlank();
            // Case 1: no template but a non-zero penalty was stored — reset to 0.
            if (hasNoTemplate && island.isTemplateBaseValueConfigured() && island.getTemplateBaseValue() > 0D) {
                island.setTemplateBaseValue(0D);
                island.setTemplateBaseValueConfigured(false);
                islandRepository.put(island);
                anyFixed = true;
            }
            // Case 2: has a template but zero penalty was stored (schematic parse failed previously
            // and the old fix incorrectly set it to 0) — mark as unconfigured so it recalculates.
            if (!hasNoTemplate && island.isTemplateBaseValueConfigured() && island.getTemplateBaseValue() == 0D) {
                island.setTemplateBaseValueConfigured(false);
                islandRepository.put(island);
                anyFixed = true;
            }
        }
        if (anyFixed) {
            valueCache.clear();
            saveValueCacheAsync();
        }
    }

    private double computeTemplatePenalty(String templateId) {
        IslandTemplate template = templateManager.get(templateId);
        if (template == null) {
            template = templateManager.getDefault();
        }
        if (template == null) {
            return 0D;
        }

        IslandSchematic schematic = null;
        String prefab = template.getPrefabName();
        if (prefab != null && !prefab.isBlank()) {
            schematic = templateManager.loadCapturedPrefab(prefab);
        }
        if (schematic == null) {
            schematic = templateManager.loadCapturedPrefab(template.getId());
        }
        if (schematic == null) {
            schematic = loadSchematicFromResource(prefab);
        }
        if (schematic == null) {
            return 0D;
        }

        double total = 0D;
        for (IslandSchematic.BlockData block : schematic.getBlocks()) {
            if (block == null || block.getBlockId() == null || block.getBlockId().isBlank()) {
                continue;
            }
            total += resolveBlockIdValue(block.getBlockId());
        }
        return total;
    }

    private IslandSchematic loadSchematicFromResource(String prefabName) {
        if (prefabName == null || prefabName.isBlank()) {
            return null;
        }

        String[] candidates = new String[] {
            prefabName,
            "Server/Prefabs/" + prefabName,
            "Server/Prefabs/island.prefab.json"
        };

        for (String path : candidates) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    IslandSchematic schematic = gson.fromJson(reader, IslandSchematic.class);
                    if (schematic != null && schematic.getBlocks() != null && !schematic.getBlocks().isEmpty()) {
                        return schematic;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean isCacheFresh(CachedValueEntry entry) {
        if (entry == null) {
            return false;
        }
        if (levelsConfig.isTemplateValueEnabled() && entry.rawValue > 0D && entry.templatePenalty <= 0D) {
            return false;
        }
        return (System.currentTimeMillis() - entry.calculatedAt) <= CACHE_TTL_MILLIS;
    }

    private IslandValueResult toResult(CachedValueEntry entry, boolean fromCache) {
        return new IslandValueResult(
            entry.rawValue,
            entry.templatePenalty,
            entry.netValue,
            entry.level,
            entry.calculatedAt,
            fromCache
        );
    }

    private ZoneBounds computeZoneBounds(IslandData island) {
        int zoneSize = Math.max(1, island.getZoneSize());
        int half = zoneSize / 2;
        int step = Math.max(1, initConfig.getIslandZoneSize()) + Math.max(0, initConfig.getIslandSpacing());
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;

        return new ZoneBounds(
            anchorX - half,
            anchorX + half,
            anchorZ - half,
            anchorZ + half
        );
    }

    private List<ChunkWindow> computeChunkWindows(ZoneBounds bounds) {
        int minChunkX = bounds.minX >> 5;
        int maxChunkX = bounds.maxX >> 5;
        int minChunkZ = bounds.minZ >> 5;
        int maxChunkZ = bounds.maxZ >> 5;

        List<ChunkWindow> windows = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int chunkMinX = chunkX << 5;
                int chunkMaxX = chunkMinX + 31;
                int chunkMinZ = chunkZ << 5;
                int chunkMaxZ = chunkMinZ + 31;

                windows.add(new ChunkWindow(
                    ChunkUtil.indexChunkFromBlock(chunkMinX, chunkMinZ),
                    Math.max(bounds.minX, chunkMinX),
                    Math.min(bounds.maxX, chunkMaxX),
                    Math.max(bounds.minZ, chunkMinZ),
                    Math.min(bounds.maxZ, chunkMaxZ)
                ));
            }
        }
        return windows;
    }

    private void loadValueCache() {
        valueCache.clear();
        if (!cacheFile.exists()) {
            return;
        }
        try (Reader reader = new FileReader(cacheFile)) {
            Map<String, CachedValueEntry> raw = gson.fromJson(reader, VALUE_CACHE_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, CachedValueEntry> entry : raw.entrySet()) {
                try {
                    UUID ownerUuid = UUID.fromString(entry.getKey());
                    if (entry.getValue() != null) {
                        valueCache.put(ownerUuid, entry.getValue());
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void saveValueCacheAsync() {
        CompletableFuture.runAsync(this::saveValueCache);
    }

    private void saveValueCache() {
        Map<String, CachedValueEntry> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, CachedValueEntry> entry : valueCache.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(cacheFile)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    private void loadBlockValues() {
        blockValues.clear();
        if (!blockValuesFile.exists()) {
            createDefaultBlockValues();
            return;
        }

        try (Reader reader = new FileReader(blockValuesFile)) {
            Map<String, Number> raw = gson.fromJson(reader, BLOCK_VALUES_TYPE);
            if (raw == null || raw.isEmpty()) {
                createDefaultBlockValues();
                return;
            }
            for (Map.Entry<String, Number> entry : raw.entrySet()) {
                String blockId = entry.getKey();
                Number value = entry.getValue();
                if (blockId == null || blockId.isBlank() || value == null) {
                    continue;
                }
                blockValues.put(blockId, value.doubleValue());
            }
            if (blockValues.isEmpty()) {
                createDefaultBlockValues();
            }
        } catch (IOException ignored) {
            createDefaultBlockValues();
        }
    }

    private void createDefaultBlockValues() {
        blockValues.clear();
        blockValues.put("Rock_Bedrock", 0D);
        blockValues.put("Soil_Grass_Deep", 1D);
        blockValues.put("Rock_Stone", 1D);
        saveBlockValues();
    }

    private void saveBlockValues() {
        try (Writer writer = new FileWriter(blockValuesFile)) {
            gson.toJson(Collections.unmodifiableMap(blockValues), writer);
        } catch (IOException ignored) {
        }
    }

    private record ZoneBounds(int minX, int maxX, int minZ, int maxZ) {
    }
}
