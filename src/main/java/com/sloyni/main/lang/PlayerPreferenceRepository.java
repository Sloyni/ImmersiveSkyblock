package com.sloyni.main.lang;

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

public class PlayerPreferenceRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerPreferenceData>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, PlayerPreferenceData> preferences;

    public PlayerPreferenceRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "player_preferences.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.preferences = new ConcurrentHashMap<>();
        load();
    }

    public synchronized void load() {
        preferences.clear();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, PlayerPreferenceData> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, PlayerPreferenceData> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    if (entry.getValue() != null) {
                        preferences.put(uuid, entry.getValue());
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void save() {
        Map<String, PlayerPreferenceData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerPreferenceData> entry : preferences.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized String getLanguage(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        PlayerPreferenceData data = preferences.get(playerUuid);
        return data == null ? null : data.getLanguage();
    }

    public synchronized void setLanguage(UUID playerUuid, String language) {
        if (playerUuid == null) {
            return;
        }
        PlayerPreferenceData data = preferences.computeIfAbsent(playerUuid, ignored -> new PlayerPreferenceData());
        data.setLanguage(language);
        save();
    }
}
