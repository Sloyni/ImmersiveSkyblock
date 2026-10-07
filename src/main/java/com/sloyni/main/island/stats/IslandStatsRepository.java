package com.sloyni.main.island.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sloyni.main.config.LockFolderStorage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists {@link IslandStats} to disk, keyed by island owner UUID.
 * Uses the same Gson/JSON pattern as the rest of the plugin repositories.
 */
public class IslandStatsRepository {

    private static final Type MAP_TYPE = new TypeToken<Map<String, IslandStats>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, IslandStats> stats;

    public IslandStatsRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "island_stats.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.stats = new ConcurrentHashMap<>();
        load();
    }

    // ─── Load / Save ─────────────────────────────────────────────────────────

    public void load() {
        stats.clear();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, IslandStats> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, IslandStats> entry : raw.entrySet()) {
                    try {
                        UUID id = UUID.fromString(entry.getKey());
                        if (entry.getValue() != null) {
                            stats.put(id, entry.getValue());
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException ignored) {}
    }

    public void save() {
        Map<String, IslandStats> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, IslandStats> entry : stats.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {}
    }

    // ─── Access ──────────────────────────────────────────────────────────────

    /**
     * Returns the stats for the given island owner, creating a blank entry if absent.
     */
    public IslandStats getOrCreate(UUID ownerUuid) {
        return stats.computeIfAbsent(ownerUuid, id -> {
            IslandStats s = new IslandStats(id);
            return s;
        });
    }

    public IslandStats get(UUID ownerUuid) {
        return stats.get(ownerUuid);
    }

    public void put(UUID ownerUuid, IslandStats s) {
        stats.put(ownerUuid, s);
    }

    public void remove(UUID ownerUuid) {
        if (ownerUuid == null) {
            return;
        }
        stats.remove(ownerUuid);
    }
}
