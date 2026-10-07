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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, IslandData>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, IslandData> islands;

    public IslandRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "islands.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.islands = new ConcurrentHashMap<>();
        load();
    }

    public void load() {
        this.islands.clear();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, IslandData> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, IslandData> entry : raw.entrySet()) {
                    try {
                        UUID id = UUID.fromString(entry.getKey());
                        this.islands.put(id, entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void save() {
        Map<String, IslandData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, IslandData> entry : islands.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public IslandData get(UUID owner) {
        return islands.get(owner);
    }

    public IslandData getByGrid(int gridX, int gridZ) {
        for (IslandData island : islands.values()) {
            if (island != null && island.getGridX() == gridX && island.getGridZ() == gridZ) {
                return island;
            }
        }
        return null;
    }

    /**
     * Finds the island for the given player — whether they are the original owner/creator,
     * the current leader (after a leadership transfer), or a regular team member.
     */
    public IslandData getForPlayer(UUID uuid) {
        if (uuid == null) return null;
        IslandData direct = islands.get(uuid);
        if (direct != null) return direct;
        for (IslandData island : islands.values()) {
            if (island.isLeader(uuid) || island.isMember(uuid)) return island;
        }
        return null;
    }

    public boolean has(UUID owner) {
        return islands.containsKey(owner);
    }

    public Collection<IslandData> values() {
        return islands.values();
    }

    public void put(IslandData islandData) {
        islands.put(islandData.getOwnerUuid(), islandData);
        save();
    }

    public IslandData remove(UUID ownerUuid) {
        IslandData removed = islands.remove(ownerUuid);
        if (removed != null) {
            save();
        }
        return removed;
    }
}
