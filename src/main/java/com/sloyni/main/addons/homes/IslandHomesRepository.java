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

public class IslandHomesRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, Map<String, Map<String, HomeLocation>>>>() {}.getType();

    private final File file;
    private final Gson gson;
    private Map<String, Map<String, Map<String, HomeLocation>>> data;

    public IslandHomesRepository(File dataFolder) {
        this.file = LockFolderStorage.resolveLockedFile(dataFolder, "island_homes.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.data = new LinkedHashMap<>();
        load();
    }

    public synchronized void load() {
        data = new LinkedHashMap<>();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, Map<String, Map<String, HomeLocation>>> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                data.putAll(raw);
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    public synchronized boolean isEmpty() {
        return data.isEmpty();
    }

    public synchronized Map<String, HomeLocation> getHomes(UUID islandOwnerUuid, UUID playerUuid) {
        if (islandOwnerUuid == null || playerUuid == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Map<String, HomeLocation>> islandHomes = data.get(islandOwnerUuid.toString());
        if (islandHomes == null) {
            return new LinkedHashMap<>();
        }
        Map<String, HomeLocation> homes = islandHomes.get(playerUuid.toString());
        return homes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(homes);
    }

    public synchronized HomeLocation getHome(UUID islandOwnerUuid, UUID playerUuid, String homeId) {
        if (homeId == null || homeId.isBlank()) {
            return null;
        }
        return getHomes(islandOwnerUuid, playerUuid).get(homeId.toLowerCase());
    }

    public synchronized boolean hasHome(UUID islandOwnerUuid, UUID playerUuid, String homeId) {
        return getHome(islandOwnerUuid, playerUuid, homeId) != null;
    }

    public synchronized int getHomeCount(UUID islandOwnerUuid, UUID playerUuid) {
        return getHomes(islandOwnerUuid, playerUuid).size();
    }

    public synchronized boolean setHome(UUID islandOwnerUuid, UUID playerUuid, String homeId, int x, int y, int z, float pitch, float yaw) {
        if (islandOwnerUuid == null || playerUuid == null || homeId == null || homeId.isBlank()) {
            return false;
        }
        String islandKey = islandOwnerUuid.toString();
        String playerKey = playerUuid.toString();
        String homeKey = homeId.toLowerCase();

        Map<String, Map<String, HomeLocation>> islandHomes = data.computeIfAbsent(islandKey, ignored -> new LinkedHashMap<>());
        Map<String, HomeLocation> playerHomes = islandHomes.computeIfAbsent(playerKey, ignored -> new LinkedHashMap<>());
        boolean created = !playerHomes.containsKey(homeKey);
        playerHomes.put(homeKey, new HomeLocation(x, y, z, pitch, yaw));
        save();
        return created;
    }

    public synchronized boolean renameHome(UUID islandOwnerUuid, UUID playerUuid, String oldHomeId, String newHomeId) {
        if (islandOwnerUuid == null || playerUuid == null
            || oldHomeId == null || newHomeId == null
            || oldHomeId.isBlank() || newHomeId.isBlank()) {
            return false;
        }

        String islandKey = islandOwnerUuid.toString();
        String playerKey = playerUuid.toString();
        Map<String, Map<String, HomeLocation>> islandHomes = data.get(islandKey);
        if (islandHomes == null) {
            return false;
        }
        Map<String, HomeLocation> playerHomes = islandHomes.get(playerKey);
        if (playerHomes == null) {
            return false;
        }

        String oldKey = oldHomeId.toLowerCase();
        String newKey = newHomeId.toLowerCase();
        if (!playerHomes.containsKey(oldKey) || playerHomes.containsKey(newKey)) {
            return false;
        }

        HomeLocation home = playerHomes.remove(oldKey);
        playerHomes.put(newKey, home);
        save();
        return true;
    }

    public synchronized boolean deleteHome(UUID islandOwnerUuid, UUID playerUuid, String homeId) {
        if (islandOwnerUuid == null || playerUuid == null || homeId == null || homeId.isBlank()) {
            return false;
        }
        String islandKey = islandOwnerUuid.toString();
        String playerKey = playerUuid.toString();
        Map<String, Map<String, HomeLocation>> islandHomes = data.get(islandKey);
        if (islandHomes == null) {
            return false;
        }
        Map<String, HomeLocation> playerHomes = islandHomes.get(playerKey);
        if (playerHomes == null) {
            return false;
        }

        boolean removed = playerHomes.remove(homeId.toLowerCase()) != null;
        if (removed) {
            if (playerHomes.isEmpty()) {
                islandHomes.remove(playerKey);
            }
            if (islandHomes.isEmpty()) {
                data.remove(islandKey);
            }
            save();
        }
        return removed;
    }

    public synchronized void clearHomes(UUID islandOwnerUuid, UUID playerUuid) {
        if (islandOwnerUuid == null || playerUuid == null) {
            return;
        }
        String islandKey = islandOwnerUuid.toString();
        String playerKey = playerUuid.toString();
        Map<String, Map<String, HomeLocation>> islandHomes = data.get(islandKey);
        if (islandHomes == null) {
            return;
        }
        if (islandHomes.remove(playerKey) != null) {
            if (islandHomes.isEmpty()) {
                data.remove(islandKey);
            }
            save();
        }
    }
}
