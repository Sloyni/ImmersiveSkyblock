package com.sloyni.main.addons.islandwalls;

import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.config.LockFolderStorage;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.world.WorldManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class IslandWallsAddon {
    public static final String KEY = "island_walls";

    private final IslandWallsAddonConfigManager configManager;
    private final IslandWallsIslandCommandHandler islandCommandHandler;
    private final IslandWallsService service;
    private final IslandWallsRepository wallsRepository;

    public IslandWallsAddon(ISkyblockPlugin plugin,
                            File pluginDataFolder,
                            PluginConfig mainConfig,
                            IslandRepository islandRepository,
                            WorldManager worldManager) {
        this.configManager = new IslandWallsAddonConfigManager(pluginDataFolder);
        this.islandCommandHandler = new IslandWallsIslandCommandHandler(plugin);
        this.wallsRepository = new IslandWallsRepository(pluginDataFolder);
        migrateLegacyWalls(pluginDataFolder);
        this.service = new IslandWallsService(islandRepository, wallsRepository, worldManager, mainConfig);
    }

    public boolean isEnabled() {
        return configManager.getConfig().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        configManager.getConfig().setEnabled(enabled);
        configManager.save();
    }

    public IslandWallsIslandCommandHandler getIslandCommandHandler() {
        return islandCommandHandler;
    }

    public IslandWallsService getService() {
        return service;
    }

    public IslandWallsRepository getWallsRepository() {
        return wallsRepository;
    }

    private void migrateLegacyWalls(File pluginDataFolder) {
        if (!wallsRepository.isEmpty()) {
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
                if (islandObj == null) {
                    continue;
                }

                IslandWallState state = new IslandWallState();
                try {
                    state.setIslandOwnerUuid(java.util.UUID.fromString(islandOwnerKey));
                } catch (Exception ex) {
                    continue;
                }

                if (islandObj.has("wallPatternId") && !islandObj.get("wallPatternId").isJsonNull()) {
                    state.setWallPatternId(islandObj.get("wallPatternId").getAsString());
                }
                if (islandObj.has("wallPatternSize") && !islandObj.get("wallPatternSize").isJsonNull()) {
                    state.setWallPatternSize(islandObj.get("wallPatternSize").getAsInt());
                }
                if (islandObj.has("wallColorName") && !islandObj.get("wallColorName").isJsonNull()) {
                    state.setWallColorName(islandObj.get("wallColorName").getAsString());
                }

                List<String> alternates = new ArrayList<>();
                if (islandObj.has("wallAlternateColorNames") && islandObj.get("wallAlternateColorNames").isJsonArray()) {
                    JsonArray arr = islandObj.getAsJsonArray("wallAlternateColorNames");
                    for (int i = 0; i < arr.size(); i++) {
                        if (!arr.get(i).isJsonNull()) {
                            alternates.add(arr.get(i).getAsString());
                        }
                    }
                }
                state.setWallAlternateColorNames(alternates);
                wallsRepository.put(state);
            }
        } catch (Exception ignored) {
        }
    }
}
