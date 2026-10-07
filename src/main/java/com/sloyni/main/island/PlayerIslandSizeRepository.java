package com.sloyni.main.island;

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

public class PlayerIslandSizeRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerIslandSizeData>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, PlayerIslandSizeData> players;

    public PlayerIslandSizeRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "player_island_sizes.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.players = new ConcurrentHashMap<>();
        load();
    }

    public synchronized void load() {
        players.clear();
        if (!file.exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Map<String, PlayerIslandSizeData> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, PlayerIslandSizeData> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerIslandSizeData data = entry.getValue();
                    if (data != null) {
                        players.put(uuid, data);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void save() {
        Map<String, PlayerIslandSizeData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerIslandSizeData> entry : players.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized PlayerIslandSizeData getOrCreate(UUID uuid, int defaultZoneSize) {
        PlayerIslandSizeData existing = players.get(uuid);
        if (existing != null) {
            return existing;
        }

        PlayerIslandSizeData created = new PlayerIslandSizeData();
        created.setZoneSize(Math.max(1, defaultZoneSize));
        created.setCustomAdjusted(false);
        players.put(uuid, created);
        save();
        return created;
    }

    public synchronized int getZoneSize(UUID uuid, int defaultZoneSize) {
        return getOrCreate(uuid, defaultZoneSize).getZoneSize();
    }

    public synchronized int setZoneSizeMax(UUID uuid, int requestedZoneSize, int defaultZoneSize) {
        PlayerIslandSizeData data = getOrCreate(uuid, defaultZoneSize);
        int current = Math.max(1, data.getZoneSize());
        int updated = Math.max(current, Math.max(1, requestedZoneSize));
        data.setZoneSize(updated);
        data.setCustomAdjusted(true);
        save();
        return updated;
    }
}
