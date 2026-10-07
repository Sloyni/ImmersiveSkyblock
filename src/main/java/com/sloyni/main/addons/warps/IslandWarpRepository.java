package com.sloyni.main.addons.warps;

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

public class IslandWarpRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, IslandWarpData>>() {}.getType();

    private final File file;
    private final Gson gson;
    private final Map<UUID, IslandWarpData> warps;

    public IslandWarpRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "island_warps.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.warps = new ConcurrentHashMap<>();
        load();
    }

    public synchronized void load() {
        warps.clear();
        if (!file.exists()) {
            save();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Map<String, IslandWarpData> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, IslandWarpData> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    IslandWarpData data = entry.getValue();
                    if (data != null) {
                        if (data.getOwnerUuid() == null) {
                            data.setOwnerUuid(uuid);
                        }
                        warps.put(uuid, data);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void save() {
        Map<String, IslandWarpData> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, IslandWarpData> entry : warps.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized IslandWarpData get(UUID ownerUuid) {
        return ownerUuid == null ? null : warps.get(ownerUuid);
    }

    public synchronized Collection<IslandWarpData> values() {
        return warps.values();
    }

    public synchronized void put(IslandWarpData data) {
        if (data == null || data.getOwnerUuid() == null) {
            return;
        }
        warps.put(data.getOwnerUuid(), data);
        save();
    }

    public synchronized IslandWarpData remove(UUID ownerUuid) {
        if (ownerUuid == null) {
            return null;
        }
        IslandWarpData removed = warps.remove(ownerUuid);
        if (removed != null) {
            save();
        }
        return removed;
    }
}
