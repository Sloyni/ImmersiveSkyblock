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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class KnownPlayerRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, String> players;

    public KnownPlayerRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "known_players.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.players = Collections.synchronizedMap(new LinkedHashMap<>());
        load();
    }

    public void load() {
        this.players.clear();
        if (!file.exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Map<String, String> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return;
            }

            for (Map.Entry<String, String> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    String username = entry.getValue();
                    if (username != null && !username.isBlank()) {
                        this.players.put(uuid, username);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void save() {
        Map<String, String> raw = new LinkedHashMap<>();
        synchronized (players) {
            for (Map.Entry<UUID, String> entry : players.entrySet()) {
                raw.put(entry.getKey().toString(), entry.getValue());
            }
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public void upsert(UUID uuid, String username) {
        if (uuid == null || username == null || username.isBlank()) {
            return;
        }

        String previous;
        synchronized (players) {
            previous = players.put(uuid, username);
        }
        if (previous == null || !previous.equals(username)) {
            save();
        }
    }

    public Map<UUID, String> all() {
        synchronized (players) {
            return new LinkedHashMap<>(players);
        }
    }
}