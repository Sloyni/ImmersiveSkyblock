package com.sloyni.main.addons.islandwalls;

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

public class IslandWallsRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, IslandWallState>>() {}.getType();

    private final File file;
    private final Gson gson;
    private Map<String, IslandWallState> states;

    public IslandWallsRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "island_walls_state.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.states = new LinkedHashMap<>();
        load();
    }

    public synchronized void load() {
        states = new LinkedHashMap<>();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, IslandWallState> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                states.putAll(raw);
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(states, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized boolean isEmpty() {
        return states.isEmpty();
    }

    public synchronized IslandWallState getOrDefault(UUID islandOwnerUuid) {
        if (islandOwnerUuid == null) {
            return new IslandWallState();
        }
        IslandWallState state = states.get(islandOwnerUuid.toString());
        if (state == null) {
            state = new IslandWallState();
            state.setIslandOwnerUuid(islandOwnerUuid);
        }
        return state;
    }

    public synchronized void put(IslandWallState state) {
        if (state == null || state.getIslandOwnerUuid() == null) {
            return;
        }
        states.put(state.getIslandOwnerUuid().toString(), state);
        save();
    }
}
