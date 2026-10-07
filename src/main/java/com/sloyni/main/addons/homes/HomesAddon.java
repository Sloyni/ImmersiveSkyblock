package com.sloyni.main.addons.homes;

import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.config.LockFolderStorage;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.world.WorldManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class HomesAddon {
    public static final String KEY = "homes";

    private final HomesAddonConfigManager configManager;
    private final HomesIslandCommandHandler islandCommandHandler;
    private final HomesService service;
    private final IslandHomesRepository homesRepository;

    public HomesAddon(ISkyblockPlugin plugin,
                      File pluginDataFolder,
                      PluginConfig mainConfig,
                      IslandRepository islandRepository,
                      PlayerHomeLimitRepository playerHomeLimitRepository,
                      WorldManager worldManager) {
        this.configManager = new HomesAddonConfigManager(pluginDataFolder);
        this.islandCommandHandler = new HomesIslandCommandHandler(plugin);
        this.homesRepository = new IslandHomesRepository(pluginDataFolder);
        migrateLegacyHomes(pluginDataFolder);
        this.service = new HomesService(islandRepository, playerHomeLimitRepository, homesRepository, worldManager, mainConfig, this);
    }

    public boolean isEnabled() {
        return configManager.getConfig().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        configManager.getConfig().setEnabled(enabled);
        configManager.save();
    }

    public int getMaxHomes() {
        return Math.max(0, configManager.getDangerousConfig().getMaxHomes());
    }

    public void handleMaxHomesSetting(int maxHomes) {
        int safe = Math.max(0, maxHomes);
        configManager.getConfig().setMaxHomes(safe);
        configManager.getDangerousConfig().setMaxHomes(safe);
        configManager.save();
    }

    public Map<String, Integer> getHomeGroups() {
        Map<String, Integer> groups = configManager.getConfig().getHomeGroups();
        return groups == null ? new LinkedHashMap<>() : new LinkedHashMap<>(groups);
    }

    public void setHomeGroup(String groupId, int maxHomes) {
        String normalized = normalizeGroupId(groupId);
        if (normalized.isBlank() || "default".equals(normalized)) {
            return;
        }
        Map<String, Integer> groups = new LinkedHashMap<>(getHomeGroups());
        groups.put(normalized, Math.max(0, maxHomes));
        configManager.getConfig().setHomeGroups(groups);
        configManager.getConfig().applyDefaults();
        configManager.save();
    }

    public void removeHomeGroup(String groupId) {
        String normalized = normalizeGroupId(groupId);
        if (normalized.isBlank() || "default".equals(normalized)) {
            return;
        }
        Map<String, Integer> groups = new LinkedHashMap<>(getHomeGroups());
        groups.remove(normalized);
        configManager.getConfig().setHomeGroups(groups);
        configManager.save();
    }

    public HomesIslandCommandHandler getIslandCommandHandler() {
        return islandCommandHandler;
    }

    public HomesService getService() {
        return service;
    }

    public IslandHomesRepository getHomesRepository() {
        return homesRepository;
    }

    private String normalizeGroupId(String rawGroupId) {
        if (rawGroupId == null) {
            return "";
        }
        String normalized = rawGroupId.trim().toLowerCase();
        return normalized.matches("^[a-z0-9_-]+$") ? normalized : "";
    }

    private void migrateLegacyHomes(File pluginDataFolder) {
        if (!homesRepository.isEmpty()) {
            return;
        }

        File islandsFile = LockFolderStorage.resolveLockedFile(pluginDataFolder, "islands.json");
        if (!islandsFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(islandsFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (String islandOwnerKey : root.keySet()) {
                JsonObject islandObj = root.getAsJsonObject(islandOwnerKey);
                if (islandObj == null || !islandObj.has("playerHomes") || !islandObj.get("playerHomes").isJsonObject()) {
                    continue;
                }
                JsonObject playersObj = islandObj.getAsJsonObject("playerHomes");
                for (String playerUuid : playersObj.keySet()) {
                    JsonObject homesObj = playersObj.getAsJsonObject(playerUuid);
                    if (homesObj == null) {
                        continue;
                    }
                    for (String homeId : homesObj.keySet()) {
                        JsonObject loc = homesObj.getAsJsonObject(homeId);
                        if (loc == null) {
                            continue;
                        }
                        JsonElement xEl = loc.get("x");
                        JsonElement yEl = loc.get("y");
                        JsonElement zEl = loc.get("z");
                        if (xEl == null || yEl == null || zEl == null) {
                            continue;
                        }
                        try {
                            homesRepository.setHome(
                                java.util.UUID.fromString(islandOwnerKey),
                                java.util.UUID.fromString(playerUuid),
                                homeId,
                                xEl.getAsInt(),
                                yEl.getAsInt(),
                                zEl.getAsInt(),
                                0f,
                                0f
                            );
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
