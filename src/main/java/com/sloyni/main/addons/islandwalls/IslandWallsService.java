package com.sloyni.main.addons.islandwalls;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandMemberPermission;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.world.WorldManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandWallsService {
    private enum WallPattern {
        NONE("none"),
        HONEYCOMB("honeycomb"),
        DIAMOND("diamond"),
        GRID("grid"),
        TRIANGLES("triangles"),
        CIRCLES("circles"),
        HEART("heart"),
        DIAGONAL("diagonal"),
        WAVES("waves"),
        STARS("stars");

        private final String id;

        WallPattern(String id) {
            this.id = id;
        }

        static WallPattern fromInput(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "none", "off", "disable", "disabled" -> NONE;
                case "honeycomb", "hex", "hexagon", "hexagons" -> HONEYCOMB;
                case "diamond", "diamonds" -> DIAMOND;
                case "grid", "square", "squares" -> GRID;
                case "triangles", "triangle", "zigzag", "chevron" -> TRIANGLES;
                case "circles", "circle", "rings" -> CIRCLES;
                case "heart", "hearts", "love" -> HEART;
                case "diagonal", "diagonals", "slash", "stripes" -> DIAGONAL;
                case "waves", "wave", "sine" -> WAVES;
                case "stars", "star" -> STARS;
                default -> null;
            };
        }
    }

    private record WallBounds(int minX, int maxX, int minZ, int maxZ) {}

    private static final List<String> SUPPORTED_WALL_PATTERN_IDS = List.of(
        "none", "honeycomb", "diamond", "grid", "triangles", "circles", "heart", "diagonal", "waves", "stars"
    );
    private static final int WALL_MIN_Y = 0;
    private static final int WALL_MAX_Y = 319;
    private static final int WALL_MIN_SIZE = 2;
    private static final int WALL_MAX_SIZE = 16;
    private static final String DEFAULT_WALL_COLOR = "red";
    private static final Map<String, String> WALL_COLOR_BLOCK_IDS = createWallColorBlockIds();
    private static final List<String> SUPPORTED_WALL_COLOR_NAMES = List.copyOf(WALL_COLOR_BLOCK_IDS.keySet());

    private final IslandRepository islandRepository;
    private final IslandWallsRepository wallsRepository;
    private final WorldManager worldManager;
    private final PluginConfig pluginConfig;

    public IslandWallsService(IslandRepository islandRepository,
                              IslandWallsRepository wallsRepository,
                              WorldManager worldManager,
                              PluginConfig pluginConfig) {
        this.islandRepository = islandRepository;
        this.wallsRepository = wallsRepository;
        this.worldManager = worldManager;
        this.pluginConfig = pluginConfig;
    }

    public List<String> getSupportedWallPatternIds() {
        return SUPPORTED_WALL_PATTERN_IDS;
    }

    public String normalizeWallPatternId(String wallPatternId) {
        WallPattern pattern = WallPattern.fromInput(wallPatternId);
        return pattern == null ? null : pattern.id;
    }

    public List<String> getSupportedWallColorNames() {
        return SUPPORTED_WALL_COLOR_NAMES;
    }

    public String normalizeWallColorName(String wallColorName) {
        if (wallColorName == null || wallColorName.isBlank()) {
            return null;
        }
        String normalized = wallColorName.trim().toLowerCase(Locale.ROOT);
        return WALL_COLOR_BLOCK_IDS.containsKey(normalized) ? normalized : null;
    }

    public CompletableFuture<IslandWallState> updateIslandWallColor(PlayerRef leaderRef, String wallColorName) {
        return updateIslandWallColors(leaderRef, wallColorName, List.of(), false);
    }

    public CompletableFuture<IslandWallState> updateIslandWallAlternateColors(PlayerRef leaderRef, List<String> colorNames) {
        if (colorNames == null || colorNames.size() < 2) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_wall_color_alternate"));
        }
        String primaryColorName = colorNames.get(0);
        List<String> alternates = new ArrayList<>();
        for (int i = 1; i < colorNames.size(); i++) {
            alternates.add(colorNames.get(i));
        }
        return updateIslandWallColors(leaderRef, primaryColorName, alternates, true);
    }

    public CompletableFuture<IslandWallState> updateIslandWalls(PlayerRef leaderRef, String wallPatternId, int wallPatternSize) {
        if (leaderRef == null || leaderRef.getUuid() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.MANAGE_WALLS)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        WallPattern pattern = WallPattern.fromInput(wallPatternId);
        if (pattern == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_wall_type"));
        }

        int normalizedSize = 0;
        if (pattern != WallPattern.NONE) {
            if (wallPatternSize < WALL_MIN_SIZE || wallPatternSize > WALL_MAX_SIZE) {
                return CompletableFuture.failedFuture(new IllegalStateException("invalid_wall_size"));
            }
            normalizedSize = wallPatternSize;
        }

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        int finalSize = normalizedSize;
        return worldFuture.thenCompose(target -> loadIslandZoneChunks(target, island)
            .thenCompose(v -> clearIslandBorderWalls(target, island))
            .thenCompose(v -> {
                IslandWallState state = getWallState(island);
                if (pattern == WallPattern.NONE) {
                    state.setWallPatternId(pattern.id);
                    state.setWallPatternSize(0);
                    if (normalizeWallColorName(state.getWallColorName()) == null) {
                        state.setWallColorName(DEFAULT_WALL_COLOR);
                    }
                    wallsRepository.put(state);
                    return CompletableFuture.completedFuture(state);
                }
                if (normalizeWallColorName(state.getWallColorName()) == null) {
                    state.setWallColorName(DEFAULT_WALL_COLOR);
                }
                state.setWallsDisabled(false);
                return generateBorderWalls(target, island, pattern, finalSize)
                    .thenApply(unused -> {
                        state.setWallPatternId(pattern.id);
                        state.setWallPatternSize(finalSize);
                        wallsRepository.put(state);
                        return state;
                    });
            })
        );
    }

    public CompletableFuture<Void> applyWallsAfterZoneResize(World world, IslandData island, int currentSize, int targetSize) {
        IslandWallState state = getWallState(island);
        WallPattern activePattern = WallPattern.fromInput(state.getWallPatternId());
        int activePatternSize = state.getWallPatternSize();
        boolean hasActiveWalls = activePattern != null && activePattern != WallPattern.NONE && activePatternSize >= WALL_MIN_SIZE && !state.isWallsDisabled();

        return loadIslandZoneChunks(world, island, targetSize)
            .thenCompose(v -> clearIslandBorderWalls(world, island, currentSize))
            .thenCompose(v -> {
                if (!hasActiveWalls) {
                    return CompletableFuture.completedFuture(null);
                }
                return clearIslandBorderWalls(world, island, targetSize)
                    .thenCompose(unused -> generateBorderWalls(world, island, activePattern, activePatternSize))
                    .thenApply(unused -> null);
            });
    }

    public boolean isProtectedWallBlock(World world, int x, int y, int z) {
        if (world == null || y < WALL_MIN_Y || y > WALL_MAX_Y) {
            return false;
        }

        for (IslandData candidate : islandRepository.values()) {
            IslandWallState state = getWallState(candidate);
            WallPattern pattern = WallPattern.fromInput(state.getWallPatternId());
            if (pattern == null || pattern == WallPattern.NONE) {
                continue;
            }

            int size = state.getWallPatternSize();
            if (size < WALL_MIN_SIZE) {
                continue;
            }

            WallBounds bounds = resolveWallBounds(candidate);
            boolean onXBoundary = (x == bounds.minX() || x == bounds.maxX()) && z >= bounds.minZ() && z <= bounds.maxZ();
            boolean onZBoundary = (z == bounds.minZ() || z == bounds.maxZ()) && x >= bounds.minX() && x <= bounds.maxX();
            if (!onXBoundary && !onZBoundary) {
                continue;
            }

            int localU = onXBoundary ? (z - bounds.minZ()) : (x - bounds.minX());
            if (!isWallOutline(pattern, localU, y - WALL_MIN_Y, size)) {
                continue;
            }

            BlockType type = getBlockType(world, x, y, z);
            return type != null && isWallMaterialId(type.getId());
        }

        return false;
    }

    public boolean isHoneycombBorderBlock(World world, int x, int y, int z) {
        return isProtectedWallBlock(world, x, y, z);
    }

    private CompletableFuture<IslandWallState> updateIslandWallColors(PlayerRef leaderRef,
                                                                 String primaryColorName,
                                                                 List<String> alternateColorNames,
                                                                 boolean setAlternate) {
        if (leaderRef == null || leaderRef.getUuid() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }

        IslandData island = islandRepository.getForPlayer(leaderRef.getUuid());
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.hasMemberPermission(leaderRef.getUuid(), IslandMemberPermission.MANAGE_WALLS)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        String normalizedPrimary = normalizeWallColorName(primaryColorName);
        if (normalizedPrimary == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_wall_color"));
        }

        List<String> normalizedAlternates = new ArrayList<>();
        if (setAlternate) {
            if (alternateColorNames == null || alternateColorNames.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("invalid_wall_color_alternate"));
            }
            for (String alternateColorName : alternateColorNames) {
                String normalizedAlternate = normalizeWallColorName(alternateColorName);
                if (normalizedAlternate == null) {
                    return CompletableFuture.failedFuture(new IllegalStateException("invalid_wall_color_alternate"));
                }
                if (!normalizedPrimary.equals(normalizedAlternate)
                    && (normalizedAlternates.isEmpty() || !normalizedAlternates.get(normalizedAlternates.size() - 1).equals(normalizedAlternate))) {
                    normalizedAlternates.add(normalizedAlternate);
                }
            }
        }

        IslandWallState state = getWallState(island);
        state.setWallColorName(normalizedPrimary);
        if (setAlternate) {
            state.setWallAlternateColorNames(normalizedAlternates);
        } else {
            state.setWallAlternateColorNames(List.of());
        }

        WallPattern activePattern = WallPattern.fromInput(state.getWallPatternId());
        int activeSize = state.getWallPatternSize();
        if (activePattern == null || activePattern == WallPattern.NONE || activeSize < WALL_MIN_SIZE) {
            wallsRepository.put(state);
            return CompletableFuture.completedFuture(state);
        }

        if (state.isWallsDisabled()) {
            wallsRepository.put(state);
            return CompletableFuture.completedFuture(state);
        }

        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);

        return worldFuture.thenCompose(target -> loadIslandZoneChunks(target, island)
            .thenCompose(v -> clearIslandBorderWalls(target, island))
            .thenCompose(v -> generateBorderWalls(target, island, activePattern, activeSize))
            .thenApply(v -> {
                wallsRepository.put(state);
                return state;
            })
        );
    }

    private CompletableFuture<Void> loadIslandZoneChunks(World world, IslandData island) {
        int zoneSize = island == null ? getDefaultIslandZoneSize() : normalizeIslandZoneSize(island.getZoneSize());
        return loadIslandZoneChunks(world, island, zoneSize);
    }

    private CompletableFuture<Void> loadIslandZoneChunks(World world, IslandData island, int zoneSize) {
        if (world == null || island == null) {
            return CompletableFuture.completedFuture(null);
        }

        int safeZoneSize = Math.max(1, zoneSize);
        int step = getGridStep(safeZoneSize);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = safeZoneSize / 2;

        int minX = anchorX - half - 1;
        int maxX = anchorX + half + 1;
        int minZ = anchorZ - half - 1;
        int maxZ = anchorZ + half + 1;

        int minChunkX = minX >> 5;
        int maxChunkX = maxX >> 5;
        int minChunkZ = minZ >> 5;
        int maxChunkZ = maxZ >> 5;

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                futures.add(world.getChunkAsync(ChunkUtil.indexChunkFromBlock(cx << 5, cz << 5)));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> clearIslandBorderWalls(World world, IslandData island) {
        int zoneSize = island == null ? getDefaultIslandZoneSize() : normalizeIslandZoneSize(island.getZoneSize());
        return clearIslandBorderWalls(world, island, zoneSize);
    }

    private CompletableFuture<Void> clearIslandBorderWalls(World world, IslandData island, int zoneSize) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        if (world == null || island == null) {
            done.complete(null);
            return done;
        }

        world.execute(() -> {
            try {
                BlockType airType = (BlockType) BlockType.getAssetMap().getAsset(0);
                if (airType == null) {
                    done.completeExceptionally(new IllegalStateException("Air block type unavailable"));
                    return;
                }
                int airIndex = BlockType.getAssetMap().getIndex(airType.getId());
                WallBounds bounds = resolveWallBounds(island, zoneSize);

                for (int y = WALL_MIN_Y; y <= WALL_MAX_Y; y++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        clearWallBlock(world, bounds.minX(), y, z, airIndex, airType);
                        if (bounds.maxX() != bounds.minX()) {
                            clearWallBlock(world, bounds.maxX(), y, z, airIndex, airType);
                        }
                    }
                    for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                        clearWallBlock(world, x, y, bounds.minZ(), airIndex, airType);
                        if (bounds.maxZ() != bounds.minZ()) {
                            clearWallBlock(world, x, y, bounds.maxZ(), airIndex, airType);
                        }
                    }
                }

                done.complete(null);
            } catch (Exception ex) {
                done.completeExceptionally(ex);
            }
        });

        return done;
    }

    private CompletableFuture<Void> generateBorderWalls(World world, IslandData island, WallPattern pattern, int size) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        if (world == null || island == null || pattern == null || pattern == WallPattern.NONE) {
            done.complete(null);
            return done;
        }

        world.execute(() -> {
            try {
                List<String> colorCycle = resolveWallColorCycle(island);
                List<BlockType> cycleTypes = new ArrayList<>();
                List<Integer> cycleIndexes = new ArrayList<>();
                for (String colorName : colorCycle) {
                    BlockType cycleType = resolveWallColorType(colorName);
                    if (cycleType == null) {
                        continue;
                    }
                    cycleTypes.add(cycleType);
                    cycleIndexes.add(BlockType.getAssetMap().getIndex(cycleType.getId()));
                }
                if (cycleTypes.isEmpty()) {
                    done.completeExceptionally(new IllegalStateException("Wall color block type unavailable"));
                    return;
                }
                WallBounds bounds = resolveWallBounds(island);

                for (int y = WALL_MIN_Y; y <= WALL_MAX_Y; y++) {
                    int localY = y - WALL_MIN_Y;

                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        int local = z - bounds.minZ();
                        if (isWallOutline(pattern, local, localY, size)) {
                            int cycleIndex = resolveWallColorCycleIndex(local, localY, size, cycleTypes.size());
                            int wallIndex = cycleIndexes.get(cycleIndex);
                            BlockType wallType = cycleTypes.get(cycleIndex);
                            setWallBlock(world, bounds.minX(), y, z, wallIndex, wallType);
                            if (bounds.maxX() != bounds.minX()) {
                                setWallBlock(world, bounds.maxX(), y, z, wallIndex, wallType);
                            }
                        }
                    }

                    for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                        int local = x - bounds.minX();
                        if (isWallOutline(pattern, local, localY, size)) {
                            int cycleIndex = resolveWallColorCycleIndex(local, localY, size, cycleTypes.size());
                            int wallIndex = cycleIndexes.get(cycleIndex);
                            BlockType wallType = cycleTypes.get(cycleIndex);
                            setWallBlock(world, x, y, bounds.minZ(), wallIndex, wallType);
                            if (bounds.maxZ() != bounds.minZ()) {
                                setWallBlock(world, x, y, bounds.maxZ(), wallIndex, wallType);
                            }
                        }
                    }
                }

                done.complete(null);
            } catch (Exception ex) {
                done.completeExceptionally(ex);
            }
        });

        return done;
    }

    private WallBounds resolveWallBounds(IslandData island) {
        int zoneSize = normalizeIslandZoneSize(island.getZoneSize());
        return resolveWallBounds(island, zoneSize);
    }

    private WallBounds resolveWallBounds(IslandData island, int zoneSize) {
        int safeZoneSize = Math.max(1, zoneSize);
        int step = getGridStep(safeZoneSize);
        int anchorX = island.getGridX() * step;
        int anchorZ = island.getGridZ() * step;
        int half = safeZoneSize / 2;
        return new WallBounds(anchorX - half - 1, anchorX + half + 1, anchorZ - half - 1, anchorZ + half + 1);
    }

    private void clearWallBlock(World world, int x, int y, int z, int airIndex, BlockType airType) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return;
        }
        int current = chunk.getBlock(x, y, z);
        if (current == 0) {
            return;
        }

        Ref<ChunkStore> columnRef = chunk.getReference();
        Store<ChunkStore> chunkStore = columnRef != null ? columnRef.getStore() : null;
        ChunkColumn column = (chunkStore != null && columnRef != null)
            ? (ChunkColumn) chunkStore.getComponent(columnRef, ChunkColumn.getComponentType())
            : null;

        if (column != null && chunkStore != null) {
            try {
                int sectionIndex = ChunkUtil.chunkCoordinate(y);
                Ref<ChunkStore> sectionRef = column.getSection(sectionIndex);
                if (sectionRef != null) {
                    FluidSection fluidSection = (FluidSection) chunkStore.getComponent(sectionRef, FluidSection.getComponentType());
                    if (fluidSection != null) {
                        fluidSection.setFluid(x, y, z, 0, (byte) 0);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        chunk.setBlock(x, y, z, airIndex, airType, 0, 0, 196);
    }

    private void setWallBlock(World world, int x, int y, int z, int blockIndex, BlockType blockType) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return;
        }
        chunk.setBlock(x, y, z, blockIndex, blockType, 0, 0, 196);
    }

    private boolean isWallOutline(WallPattern pattern, int u, int v, int size) {
        return switch (pattern) {
            case NONE -> false;
            case HONEYCOMB -> isHoneycombOutlineScaled(u, v, size);
            case DIAMOND -> isDiamondOutline(u, v, size);
            case GRID -> isGridOutline(u, v, size);
            case TRIANGLES -> isTrianglesOutline(u, v, size);
            case CIRCLES -> isCirclesOutline(u, v, size);
            case HEART -> isHeartOutline(u, v, size);
            case DIAGONAL -> isDiagonalLinesOutline(u, v, size);
            case WAVES -> isWavesOutline(u, v, size);
            case STARS -> isStarsOutline(u, v, size);
        };
    }

    private boolean isHoneycombOutlineScaled(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int scaledU = Math.floorDiv(u * 3, safeSize);
        int scaledV = Math.floorDiv(v * 3, safeSize);
        return isHoneycombOutlineBase(scaledU, scaledV);
    }

    private boolean isHoneycombOutlineBase(int u, int v) {
        final int rowStep = 5;
        final int colStep = 6;

        int baseRow = Math.floorDiv(v, rowStep);
        for (int row = baseRow - 1; row <= baseRow + 1; row++) {
            int centerY = row * rowStep + 2;
            int rowShift = (row & 1) == 0 ? 0 : 3;
            int baseCol = Math.floorDiv(u - rowShift, colStep);

            for (int col = baseCol - 1; col <= baseCol + 1; col++) {
                int centerX = rowShift + col * colStep + 2;
                if (isHexOutlineOffset(u - centerX, v - centerY)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isDiamondOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int period = safeSize * 2;
        int localX = Math.floorMod(u, period);
        int localY = Math.floorMod(v, period);
        int center = safeSize;
        int manhattan = Math.abs(localX - center) + Math.abs(localY - center);
        return manhattan == safeSize;
    }

    private boolean isGridOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        return Math.floorMod(u, safeSize) == 0 || Math.floorMod(v, safeSize) == 0;
    }

    private boolean isTrianglesOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int width = safeSize * 4;
        int height = safeSize * 4;

        int tileX = Math.floorDiv(u, width);
        int tileY = Math.floorDiv(v, height);
        boolean inverted = ((tileX + tileY) & 1) == 1;

        int localX = Math.floorMod(u, width);
        int localY = Math.floorMod(v, height);
        int y = inverted ? (height - 1 - localY) : localY;

        int leftEdge = y;
        int rightEdge = (width - 1) - y;
        boolean base = y >= height - 2;
        boolean leftSide = Math.abs(localX - leftEdge) <= 1;
        boolean rightSide = Math.abs(localX - rightEdge) <= 1;
        return base || leftSide || rightSide;
    }

    private boolean isCirclesOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int period = safeSize * 4;
        int center = period / 2;
        int localX = Math.floorMod(u, period) - center;
        int localY = Math.floorMod(v, period) - center;

        int radius = safeSize + (safeSize / 2);
        int distSq = localX * localX + localY * localY;
        int radiusSq = radius * radius;
        int tolerance = Math.max(2, safeSize * 2);
        return Math.abs(distSq - radiusSq) <= tolerance;
    }

    private boolean isHeartOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int tileW = safeSize * 7;
        int tileH = safeSize * 6;
        int localX = Math.floorMod(u, tileW);
        int localY = Math.floorMod(v, tileH);

        if (!isInsideHeart(localX, localY, tileW, tileH)) {
            return false;
        }

        return !isInsideHeart(localX - 1, localY, tileW, tileH)
            || !isInsideHeart(localX + 1, localY, tileW, tileH)
            || !isInsideHeart(localX, localY - 1, tileW, tileH)
            || !isInsideHeart(localX, localY + 1, tileW, tileH);
    }

    private boolean isInsideHeart(int px, int py, int w, int h) {
        double cx = (w - 1) / 2.0;
        double x = ((py % 2 == 1) ? -1 : 1) * (px - cx) / (cx / 1.1);
        double y = 1.15 - py * 2.15 / (h - 1.0);

        double x2 = x * x;
        double y2 = y * y;
        double r = x2 + y2 - 1.0;
        return r * r * r - x2 * y2 * y <= 0;
    }

    private boolean isDiagonalLinesOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int period = safeSize * 2;
        int line = Math.floorMod(u + v, period);
        return line == 0 || line == 1;
    }

    private boolean isWavesOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int periodX = safeSize * 4;
        int periodY = safeSize * 3;

        int localX = Math.floorMod(u, periodX);
        int localY = Math.floorMod(v, periodY);

        double angle = (localX / (double) periodX) * (Math.PI * 2.0);
        double waveCenter = (periodY / 2.0) + Math.sin(angle) * (safeSize * 0.9);
        return Math.abs(localY - waveCenter) <= 1.2;
    }

    private boolean isStarsOutline(int u, int v, int size) {
        int safeSize = Math.max(WALL_MIN_SIZE, size);
        int period = safeSize * 5;
        int center = period / 2;

        int dx = Math.floorMod(u, period) - center;
        int dy = Math.floorMod(v, period) - center;
        int adx = Math.abs(dx);
        int ady = Math.abs(dy);
        int radius = safeSize * 2;

        boolean axisRay = (adx <= 1 || ady <= 1) && Math.max(adx, ady) <= radius;
        boolean diagonalRay = Math.abs(adx - ady) <= 1 && (adx + ady) >= Math.max(2, safeSize / 2) && (adx + ady) <= radius * 2;
        boolean core = (adx <= 1 && ady <= 1);
        return axisRay || diagonalRay || core;
    }

    private List<String> resolveWallColorCycle(IslandData island) {
        IslandWallState state = getWallState(island);
        List<String> cycle = new ArrayList<>();
        String primary = normalizeWallColorName(state.getWallColorName());
        if (primary == null) {
            primary = DEFAULT_WALL_COLOR;
        }
        cycle.add(primary);

        for (String alt : state.getWallAlternateColorNames()) {
            String normalized = normalizeWallColorName(alt);
            if (normalized == null || normalized.equals(cycle.get(cycle.size() - 1))) {
                continue;
            }
            cycle.add(normalized);
        }
        return cycle;
    }

    public CompletableFuture<IslandWallState> disableIslandWalls(UUID actorUuid) {
        if (actorUuid == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.hasMemberPermission(actorUuid, IslandMemberPermission.MANAGE_WALLS)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }
        IslandWallState state = getWallState(island);
        state.setWallsDisabled(true);
        wallsRepository.put(state);
        World world = Universe.get().getWorld(island.getWorldName());
        if (world == null) {
            return CompletableFuture.completedFuture(state);
        }
        IslandWallState finalState = state;
        return loadIslandZoneChunks(world, island)
            .thenCompose(v -> clearIslandBorderWalls(world, island))
            .thenApply(v -> finalState);
    }

    public CompletableFuture<IslandWallState> enableIslandWalls(UUID actorUuid) {
        if (actorUuid == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        IslandData island = islandRepository.getForPlayer(actorUuid);
        if (island == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_island"));
        }
        if (!island.hasMemberPermission(actorUuid, IslandMemberPermission.MANAGE_WALLS)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }
        IslandWallState state = getWallState(island);
        state.setWallsDisabled(false);
        wallsRepository.put(state);
        WallPattern pattern = WallPattern.fromInput(state.getWallPatternId());
        int size = state.getWallPatternSize();
        if (pattern == null || pattern == WallPattern.NONE || size < WALL_MIN_SIZE) {
            return CompletableFuture.completedFuture(state);
        }
        World world = Universe.get().getWorld(island.getWorldName());
        CompletableFuture<World> worldFuture = world == null
            ? worldManager.createOrLoadVoidWorld(island.getWorldName())
            : CompletableFuture.completedFuture(world);
        WallPattern finalPattern = pattern;
        int finalSize = size;
        IslandWallState finalState = state;
        return worldFuture.thenCompose(target -> loadIslandZoneChunks(target, island)
            .thenCompose(v -> generateBorderWalls(target, island, finalPattern, finalSize))
            .thenApply(v -> finalState));
    }

    public CompletableFuture<Void> clearWallsUnconditionally(IslandData island) {
        if (island == null) {
            return CompletableFuture.completedFuture(null);
        }
        IslandWallState state = getWallState(island);
        state.setWallsDisabled(true);
        wallsRepository.put(state);
        World world = Universe.get().getWorld(island.getWorldName());
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        return loadIslandZoneChunks(world, island)
            .thenCompose(v -> clearIslandBorderWalls(world, island));
    }

    public boolean isWallsDisabled(UUID islandOwnerUuid) {
        if (islandOwnerUuid == null) {
            return false;
        }
        return wallsRepository.getOrDefault(islandOwnerUuid).isWallsDisabled();
    }

    private IslandWallState getWallState(IslandData island) {
        if (island == null || island.getOwnerUuid() == null) {
            return new IslandWallState();
        }
        IslandWallState state = wallsRepository.getOrDefault(island.getOwnerUuid());
        state.setIslandOwnerUuid(island.getOwnerUuid());
        return state;
    }

    private int resolveWallColorCycleIndex(int u, int v, int size, int cycleSize) {
        if (cycleSize <= 1) {
            return 0;
        }
        int cell = Math.max(1, size * 2);
        int gx = Math.floorDiv(u, cell);
        int gy = Math.floorDiv(v, cell);
        return Math.floorMod(gx + gy, cycleSize);
    }

    private BlockType resolveWallColorType(String wallColorName) {
        String normalized = normalizeWallColorName(wallColorName);
        if (normalized == null) {
            return null;
        }
        String blockId = WALL_COLOR_BLOCK_IDS.get(normalized);
        if (blockId == null) {
            return null;
        }
        return (BlockType) BlockType.getAssetMap().getAsset(blockId);
    }

    private boolean isWallMaterialId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        if ("Soil_Grass".equalsIgnoreCase(blockId)) {
            return true;
        }
        return WALL_COLOR_BLOCK_IDS.containsValue(blockId);
    }

    private BlockType getBlockType(World world, int x, int y, int z) {
        if (world == null || y < 0) {
            return null;
        }
        try {
            WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                return null;
            }
            int index = chunk.getBlock(x, y, z);
            if (index == 0) {
                return null;
            }
            return (BlockType) BlockType.getAssetMap().getAsset(index);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Map<String, String> createWallColorBlockIds() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("black", "Cloth_Block_Wool_Black");
        map.put("blue", "Cloth_Block_Wool_Blue");
        map.put("blue_light", "Cloth_Block_Wool_Blue_Light");
        map.put("cyan", "Cloth_Block_Wool_Cyan");
        map.put("cyan_light", "Cloth_Block_Wool_Cyan_Light");
        map.put("gray", "Cloth_Block_Wool_Gray");
        map.put("gray_light", "Cloth_Block_Wool_Gray_Light");
        map.put("green", "Cloth_Block_Wool_Green");
        map.put("green_light", "Cloth_Block_Wool_Green_Light");
        map.put("orange", "Cloth_Block_Wool_Orange");
        map.put("orange_light", "Cloth_Block_Wool_Orange_Light");
        map.put("pink", "Cloth_Block_Wool_Pink");
        map.put("pink_light", "Cloth_Block_Wool_Pink_Light");
        map.put("purple", "Cloth_Block_Wool_Purple");
        map.put("purple_light", "Cloth_Block_Wool_Purple_Light");
        map.put("red", "Cloth_Block_Wool_Red");
        map.put("red_light", "Cloth_Block_Wool_Red_Light");
        map.put("white", "Cloth_Block_Wool_White");
        map.put("yellow", "Cloth_Block_Wool_Yellow");
        map.put("yellow_light", "Cloth_Block_Wool_Yellow_Light");
        return map;
    }

    private boolean isHexOutlineOffset(int dx, int dy) {
        return switch (dy) {
            case -3, 2 -> dx >= -1 && dx <= 1;
            case -2, 1 -> dx == -2 || dx == 2;
            case -1, 0 -> dx == -3 || dx == 3;
            default -> false;
        };
    }

    private int getDefaultIslandZoneSize() {
        return Math.max(1, pluginConfig.getInit().getIslandZoneSize());
    }

    private int getMaxIslandExtension() {
        int gap = Math.max(0, pluginConfig.getInit().getIslandSpacing());
        return Math.max(0, (gap / 2) - 2);
    }

    private int getMaxAllowedIslandZoneSize() {
        return getDefaultIslandZoneSize() + getMaxIslandExtension();
    }

    private int normalizeIslandZoneSize(int islandZoneSize) {
        int defaultSize = getDefaultIslandZoneSize();
        if (islandZoneSize <= 0) {
            return defaultSize;
        }
        int maxSize = getMaxAllowedIslandZoneSize();
        return Math.max(defaultSize, Math.min(islandZoneSize, maxSize));
    }

    private int getGridStep(int zoneSize) {
        int gap = Math.max(0, pluginConfig.getInit().getIslandSpacing());
        int safeZone = Math.max(1, pluginConfig.getInit().getIslandZoneSize());
        return safeZone + gap;
    }
}
