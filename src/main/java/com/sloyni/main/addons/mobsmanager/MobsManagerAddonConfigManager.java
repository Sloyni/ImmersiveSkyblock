package com.sloyni.main.addons.mobsmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sloyni.main.addons.AddonStorage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class MobsManagerAddonConfigManager {
    private final File pluginDataFolder;
    private final Gson gson;
    private MobsManagerAddonConfig config;
    private MobsManagerAddonDangerousConfig dangerousConfig;

    public MobsManagerAddonConfigManager(File pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void load() {
        boolean configExists = getConfigFile().exists();
        this.config = readConfig(configExists);
        if (!configExists) {
            seedFromMainConfig();
        }
        this.config.applyDefaults();

        this.dangerousConfig = readDangerousConfig();
        this.dangerousConfig.applyDefaults();
        save();
    }

    public void save() {
        try (Writer writer = new FileWriter(getConfigFile())) {
            gson.toJson(this.config, writer);
        } catch (IOException ignored) {
        }

        try (Writer writer = new FileWriter(getDangerousConfigFile())) {
            gson.toJson(this.dangerousConfig, writer);
        } catch (IOException ignored) {
        }
    }

    public MobsManagerAddonConfig getConfig() {
        return config;
    }

    public MobsManagerAddonDangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    private MobsManagerAddonConfig readConfig(boolean configExists) {
        if (!configExists) {
            return new MobsManagerAddonConfig();
        }
        try (Reader reader = new FileReader(getConfigFile())) {
            MobsManagerAddonConfig loaded = gson.fromJson(reader, MobsManagerAddonConfig.class);
            return loaded == null ? new MobsManagerAddonConfig() : loaded;
        } catch (Exception ignored) {
            return new MobsManagerAddonConfig();
        }
    }

    private MobsManagerAddonDangerousConfig readDangerousConfig() {
        if (!getDangerousConfigFile().exists()) {
            return new MobsManagerAddonDangerousConfig();
        }
        try (Reader reader = new FileReader(getDangerousConfigFile())) {
            MobsManagerAddonDangerousConfig loaded = gson.fromJson(reader, MobsManagerAddonDangerousConfig.class);
            return loaded == null ? new MobsManagerAddonDangerousConfig() : loaded;
        } catch (Exception ignored) {
            return new MobsManagerAddonDangerousConfig();
        }
    }

    private void seedFromMainConfig() {
        config.setEnabled(true);
        JsonObject settings = loadLegacySettingsObject();
        if (settings == null) {
            return;
        }

        Boolean reduceMobsEnabled = readBoolean(settings, "reduceMobsEnabled");
        Integer reduceMobsRatePercent = readInt(settings, "reduceMobsRatePercent");
        Boolean torchProtectionEnabled = readBoolean(settings, "torchProtectionEnabled");
        Integer torchProtectionRadius = readInt(settings, "torchProtectionRadius");

        if (reduceMobsEnabled != null) {
            config.setReduceMobsEnabled(reduceMobsEnabled);
        }
        if (reduceMobsRatePercent != null) {
            config.setReduceMobsRatePercent(reduceMobsRatePercent);
        }
        if (torchProtectionEnabled != null) {
            config.setTorchProtectionEnabled(torchProtectionEnabled);
        }
        if (torchProtectionRadius != null) {
            config.setTorchProtectionRadius(torchProtectionRadius);
        }
    }

    private JsonObject loadLegacySettingsObject() {
        File legacyConfigFile = new File(pluginDataFolder, "config.json");
        if (!legacyConfigFile.exists()) {
            return null;
        }
        try (Reader reader = new FileReader(legacyConfigFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("settings") || !root.get("settings").isJsonObject()) {
                return null;
            }
            return root.getAsJsonObject("settings");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean readBoolean(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement value = obj.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsBoolean();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer readInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement value = obj.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private File getConfigFile() {
        return AddonStorage.resolveAddonConfigFile(pluginDataFolder, MobsManagerAddon.KEY);
    }

    private File getDangerousConfigFile() {
        return AddonStorage.resolveAddonDangerousConfigFile(pluginDataFolder, MobsManagerAddon.KEY);
    }
}
