package com.sloyni.main.addons.mobsmanager;

import com.sloyni.main.addons.AddonStorage;
import java.io.File;

public class MobsManagerAddon {
    public static final String KEY = "mobs_manager";

    private final MobsManagerAddonConfigManager configManager;

    public MobsManagerAddon(File pluginDataFolder) {
        this.configManager = new MobsManagerAddonConfigManager(pluginDataFolder);
        migrateLegacyFiles(pluginDataFolder);
    }

    public boolean isEnabled() {
        return configManager.getConfig().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        configManager.getConfig().setEnabled(enabled);
        configManager.save();
    }

    public boolean isReduceMobsEnabled() {
        return configManager.getConfig().isReduceMobsEnabled();
    }

    public int getReduceMobsRatePercent() {
        return Math.max(0, Math.min(100, configManager.getConfig().getReduceMobsRatePercent()));
    }

    public boolean isTorchProtectionEnabled() {
        return configManager.getConfig().isTorchProtectionEnabled();
    }

    public int getTorchProtectionRadius() {
        return Math.max(1, configManager.getConfig().getTorchProtectionRadius());
    }

    public void handleReduceMobsSetting(boolean enabled) {
        configManager.getConfig().setReduceMobsEnabled(enabled);
        configManager.save();
    }

    public void handleReduceMobsRateSetting(int ratePercent) {
        configManager.getConfig().setReduceMobsRatePercent(Math.max(0, Math.min(100, ratePercent)));
        configManager.save();
    }

    public void handleTorchProtectionSetting(boolean enabled) {
        configManager.getConfig().setTorchProtectionEnabled(enabled);
        configManager.save();
    }

    public void handleTorchProtectionRadiusSetting(int radius) {
        configManager.getConfig().setTorchProtectionRadius(Math.max(1, radius));
        configManager.save();
    }

    private void migrateLegacyFiles(File pluginDataFolder) {
        AddonStorage.migrateLegacyFile(
            new File(pluginDataFolder, "reduce_mobs.json"),
            new File(AddonStorage.resolveAddonDirectory(pluginDataFolder, KEY), "reduce_mobs.json")
        );
    }
}
