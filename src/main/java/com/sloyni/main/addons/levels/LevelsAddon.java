package com.sloyni.main.addons.levels;

import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.AddonStorage;
import com.sloyni.main.config.LockFolderStorage;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.island.IslandTemplateManager;
import java.io.File;

public class LevelsAddon {
    public static final String KEY = "levels";

    private final LevelsAddonConfigManager configManager;
    private final IslandValueService islandValueService;
    private final LevelsIslandCommandHandler islandCommandHandler;

    public LevelsAddon(ISkyblockPlugin plugin,
                       File pluginDataFolder,
                       PluginConfig mainConfig,
                       IslandRepository islandRepository,
                       IslandTemplateManager islandTemplateManager,
                       IslandService islandService) {
        this.configManager = new LevelsAddonConfigManager(pluginDataFolder);
        migrateLegacyFiles(pluginDataFolder);
        this.islandValueService = new IslandValueService(
            configManager.getAddonDirectory(),
            islandRepository,
            islandTemplateManager,
            mainConfig.getInit(),
            configManager.getConfig()
        );
        this.islandCommandHandler = new LevelsIslandCommandHandler(plugin, this);
        this.islandValueService.migrateIncorrectTemplatePenalties();
    }

    public boolean isEnabled() {
        return configManager.getConfig().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        configManager.getConfig().setEnabled(enabled);
        configManager.save();
    }

    public IslandValueService getIslandValueService() {
        return islandValueService;
    }

    public LevelsIslandCommandHandler getIslandCommandHandler() {
        return islandCommandHandler;
    }

    public void handleTemplateValueSetting(boolean enabled) {
        configManager.getConfig().setTemplateValueEnabled(enabled);
        configManager.save();
        islandValueService.invalidateTemplatePenaltyCache();
        islandValueService.clearValueCache();
    }

    public boolean isTemplateValueEnabled() {
        return configManager.getConfig().isTemplateValueEnabled();
    }

    public void handleLevelValueSetting(int levelValue) {
        configManager.getConfig().setLevelValue(Math.max(1, levelValue));
        configManager.save();
        islandValueService.clearValueCache();
    }

    public void handleBlockDefaultValueSetting(double blockDefaultValue) {
        double safeValue = (Double.isNaN(blockDefaultValue) || Double.isInfinite(blockDefaultValue) || blockDefaultValue <= 0D)
            ? 1D
            : blockDefaultValue;
        configManager.getConfig().setBlockDefaultValue(safeValue);
        configManager.save();
        islandValueService.clearValueCache();
        islandValueService.invalidateTemplatePenaltyCache();
    }

    public int getLevelValue() {
        return Math.max(1, configManager.getConfig().getLevelValue());
    }

    public double getBlockDefaultValue() {
        double value = configManager.getConfig().getBlockDefaultValue();
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0D) {
            return 1D;
        }
        return value;
    }

    private void migrateLegacyFiles(File pluginDataFolder) {
        AddonStorage.migrateLegacyFile(
            new File(pluginDataFolder, "block_values.json"),
            new File(configManager.getAddonDirectory(), "block_values.json")
        );
        AddonStorage.migrateLegacyFile(
            new File(LockFolderStorage.ensureLockDirectory(pluginDataFolder), "island_value_cache.json"),
            AddonStorage.resolveAddonLockedFile(pluginDataFolder, KEY, "island_value_cache.json")
        );
    }
}