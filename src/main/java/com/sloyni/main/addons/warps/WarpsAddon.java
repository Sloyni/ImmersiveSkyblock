package com.sloyni.main.addons.warps;

import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.world.WorldManager;
import java.io.File;

public class WarpsAddon {
    public static final String KEY = "warps";

    private final WarpsAddonConfigManager configManager;
    private final WarpsIslandCommandHandler islandCommandHandler;
    private final WarpsService service;

    public WarpsAddon(ISkyblockPlugin plugin,
                      File pluginDataFolder,
                      PluginConfig mainConfig,
                      IslandRepository islandRepository,
                      IslandWarpRepository islandWarpRepository,
                      WorldManager worldManager,
                      IslandService islandService) {
        this.configManager = new WarpsAddonConfigManager(pluginDataFolder);
        this.islandCommandHandler = new WarpsIslandCommandHandler(plugin);
        this.service = new WarpsService(islandRepository, islandWarpRepository, worldManager, mainConfig, islandService);
    }

    public boolean isEnabled() {
        return configManager.getConfig().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        configManager.getConfig().setEnabled(enabled);
        configManager.save();
    }

    public WarpsIslandCommandHandler getIslandCommandHandler() {
        return islandCommandHandler;
    }

    public WarpsService getService() {
        return service;
    }
}
