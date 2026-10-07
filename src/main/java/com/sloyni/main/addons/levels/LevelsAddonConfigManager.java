package com.sloyni.main.addons.levels;

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

public class LevelsAddonConfigManager {
    private final File pluginDataFolder;
    private final Gson gson;
    private LevelsAddonConfig config;
    private LevelsAddonDangerousConfig dangerousConfig;

    public LevelsAddonConfigManager(File pluginDataFolder) {
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

    public File getAddonDirectory() {
        return AddonStorage.resolveAddonDirectory(pluginDataFolder, LevelsAddon.KEY);
    }

    public LevelsAddonConfig getConfig() {
        return config;
    }

    public LevelsAddonDangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    private LevelsAddonConfig readConfig(boolean configExists) {
        if (!configExists) {
            return new LevelsAddonConfig();
        }
        try (Reader reader = new FileReader(getConfigFile())) {
            LevelsAddonConfig loaded = gson.fromJson(reader, LevelsAddonConfig.class);
            return loaded == null ? new LevelsAddonConfig() : loaded;
        } catch (Exception ignored) {
            return new LevelsAddonConfig();
        }
    }

    private LevelsAddonDangerousConfig readDangerousConfig() {
        if (!getDangerousConfigFile().exists()) {
            return new LevelsAddonDangerousConfig();
        }
        try (Reader reader = new FileReader(getDangerousConfigFile())) {
            LevelsAddonDangerousConfig loaded = gson.fromJson(reader, LevelsAddonDangerousConfig.class);
            return loaded == null ? new LevelsAddonDangerousConfig() : loaded;
        } catch (Exception ignored) {
            return new LevelsAddonDangerousConfig();
        }
    }

    private void seedFromMainConfig() {
        config.setEnabled(true);
        JsonObject settings = loadLegacySettingsObject();
        if (settings == null) {
            return;
        }

        Boolean templateValueEnabled = readBoolean(settings, "templateValueEnabled");
        Integer levelValue = readInt(settings, "levelValue");
        Double blockDefaultValue = readDouble(settings, "blockDefaultValue");

        if (templateValueEnabled != null) {
            config.setTemplateValueEnabled(templateValueEnabled);
        }
        if (levelValue != null) {
            config.setLevelValue(levelValue);
        }
        if (blockDefaultValue != null) {
            config.setBlockDefaultValue(blockDefaultValue);
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

    private Double readDouble(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement value = obj.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsDouble();
        } catch (Exception ignored) {
            return null;
        }
    }

    private File getConfigFile() {
        return AddonStorage.resolveAddonConfigFile(pluginDataFolder, LevelsAddon.KEY);
    }

    private File getDangerousConfigFile() {
        return AddonStorage.resolveAddonDangerousConfigFile(pluginDataFolder, LevelsAddon.KEY);
    }
}