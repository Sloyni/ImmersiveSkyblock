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

public class PlayerResetRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerResetData>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, PlayerResetData> players;

    public PlayerResetRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "player_resets.json");
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
            Map<String, PlayerResetData> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, PlayerResetData> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerResetData data = entry.getValue();
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
        Map<String, PlayerResetData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerResetData> entry : players.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized PlayerResetData getOrCreate(UUID uuid, int defaultResets) {
        PlayerResetData existing = players.get(uuid);
        if (existing != null) {
            return existing;
        }

        PlayerResetData created = new PlayerResetData();
        created.setResetsRemaining(Math.max(0, defaultResets));
        created.setCustomAdjusted(false);
        players.put(uuid, created);
        save();
        return created;
    }

    public synchronized int consumeOneCreate(UUID uuid, int defaultResets) {
        PlayerResetData data = getOrCreate(uuid, defaultResets);
        int remaining = Math.max(0, data.getResetsRemaining());
        if (remaining <= 0) {
            return 0;
        }
        data.setResetsRemaining(remaining - 1);
        save();
        return data.getResetsRemaining();
    }

    public synchronized int addResets(UUID uuid, int amount, int defaultResets) {
        PlayerResetData data = getOrCreate(uuid, defaultResets);
        data.setResetsRemaining(Math.max(0, data.getResetsRemaining()) + Math.max(0, amount));
        data.setCustomAdjusted(true);
        save();
        return data.getResetsRemaining();
    }

    public synchronized int setResets(UUID uuid, int amount, int defaultResets) {
        PlayerResetData data = getOrCreate(uuid, defaultResets);
        data.setResetsRemaining(Math.max(0, amount));
        data.setCustomAdjusted(true);
        save();
        return data.getResetsRemaining();
    }

    public synchronized int removeResets(UUID uuid, int amount, int defaultResets) {
        PlayerResetData data = getOrCreate(uuid, defaultResets);
        int updated = Math.max(0, data.getResetsRemaining() - Math.max(0, amount));
        data.setResetsRemaining(updated);
        data.setCustomAdjusted(true);
        save();
        return data.getResetsRemaining();
    }

    public synchronized int addSystemResets(UUID uuid, int amount, int defaultResets) {
        PlayerResetData data = getOrCreate(uuid, defaultResets);
        data.setResetsRemaining(Math.max(0, data.getResetsRemaining()) + Math.max(0, amount));
        save();
        return data.getResetsRemaining();
    }

    public synchronized int getRemaining(UUID uuid, int defaultResets) {
        return getOrCreate(uuid, defaultResets).getResetsRemaining();
    }

    public synchronized void applyGlobalDefaultChange(int oldDefault, int newDefault) {
        int diff = newDefault - oldDefault;
        if (diff == 0) {
            return;
        }

        for (PlayerResetData data : players.values()) {
            if (data == null || data.isCustomAdjusted()) {
                continue;
            }
            int updated = Math.max(0, data.getResetsRemaining() + diff);
            data.setResetsRemaining(updated);
        }
        save();
    }
}
