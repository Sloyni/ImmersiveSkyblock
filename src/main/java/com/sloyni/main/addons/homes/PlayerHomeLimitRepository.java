package com.sloyni.main.addons.homes;

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

public class PlayerHomeLimitRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerHomeLimitData>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, PlayerHomeLimitData> players;

    public PlayerHomeLimitRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "player_home_limits.json");
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
            Map<String, PlayerHomeLimitData> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, PlayerHomeLimitData> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerHomeLimitData data = entry.getValue();
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
        Map<String, PlayerHomeLimitData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerHomeLimitData> entry : players.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized PlayerHomeLimitData getOrCreate(UUID uuid, int defaultMaxHomes) {
        PlayerHomeLimitData existing = players.get(uuid);
        if (existing != null) {
            return existing;
        }

        PlayerHomeLimitData created = new PlayerHomeLimitData();
        created.setAddedHomes(0);
        created.setCustomAdjusted(false);
        players.put(uuid, created);
        save();
        return created;
    }

    public synchronized int getAddedHomes(UUID uuid) {
        return getOrCreate(uuid, 0).getAddedHomes();
    }

    public synchronized int setAddedHomes(UUID uuid, int amount) {
        PlayerHomeLimitData data = getOrCreate(uuid, 0);
        data.setAddedHomes(Math.max(0, amount));
        data.setCustomAdjusted(true);
        save();
        return data.getAddedHomes();
    }

    public synchronized int addAddedHomes(UUID uuid, int amount) {
        PlayerHomeLimitData data = getOrCreate(uuid, 0);
        data.setAddedHomes(Math.max(0, data.getAddedHomes()) + Math.max(0, amount));
        data.setCustomAdjusted(true);
        save();
        return data.getAddedHomes();
    }

    public synchronized int removeAddedHomes(UUID uuid, int amount) {
        PlayerHomeLimitData data = getOrCreate(uuid, 0);
        int updated = Math.max(0, data.getAddedHomes() - Math.max(0, amount));
        data.setAddedHomes(updated);
        data.setCustomAdjusted(true);
        save();
        return data.getAddedHomes();
    }

    public synchronized int getMaxHomes(UUID uuid, int defaultMaxHomes) {
        return getAddedHomes(uuid);
    }

    public synchronized int addMaxHomes(UUID uuid, int amount, int defaultMaxHomes) {
        return addAddedHomes(uuid, amount);
    }

    public synchronized int setMaxHomes(UUID uuid, int amount, int defaultMaxHomes) {
        return setAddedHomes(uuid, amount);
    }

    public synchronized int removeMaxHomes(UUID uuid, int amount, int defaultMaxHomes) {
        return removeAddedHomes(uuid, amount);
    }

    public synchronized void applyGlobalDefaultChange(int oldDefault, int newDefault) {
        int diff = newDefault - oldDefault;
        if (diff == 0) {
            return;
        }

        for (PlayerHomeLimitData data : players.values()) {
            if (data == null || data.isCustomAdjusted()) {
                continue;
            }
            int updated = Math.max(0, data.getMaxHomes() + diff);
            data.setMaxHomes(updated);
        }
        save();
    }
}
