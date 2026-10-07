package com.sloyni.main.addons.homes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sloyni.main.addons.AddonStorage;
import com.sloyni.main.config.LockFolderStorage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class HomesAddonConfigManager {
    private final File pluginDataFolder;
    private final Gson gson;
    private HomesAddonConfig config;
    private HomesAddonDangerousConfig dangerousConfig;

    public HomesAddonConfigManager(File pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void load() {
        boolean configExists = getConfigFile().exists();
        this.config = readConfig(configExists);
        this.dangerousConfig = readDangerousConfig(configExists);
        if (!configExists) {
            seedDefaultsFromLegacyConfigFiles();
        }
        this.config.applyDefaults();
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

    public HomesAddonConfig getConfig() {
        return config;
    }

    public HomesAddonDangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    private HomesAddonConfig readConfig(boolean configExists) {
        if (!configExists) {
            return new HomesAddonConfig();
        }
        try (Reader reader = new FileReader(getConfigFile())) {
            HomesAddonConfig loaded = gson.fromJson(reader, HomesAddonConfig.class);
            return loaded == null ? new HomesAddonConfig() : loaded;
        } catch (Exception ignored) {
            return new HomesAddonConfig();
        }
    }

    private HomesAddonDangerousConfig readDangerousConfig(boolean configExists) {
        if (!configExists || !getDangerousConfigFile().exists()) {
            return new HomesAddonDangerousConfig();
        }
        try (Reader reader = new FileReader(getDangerousConfigFile())) {
            HomesAddonDangerousConfig loaded = gson.fromJson(reader, HomesAddonDangerousConfig.class);
            return loaded == null ? new HomesAddonDangerousConfig() : loaded;
        } catch (Exception ignored) {
            return new HomesAddonDangerousConfig();
        }
    }

    private void seedDefaultsFromLegacyConfigFiles() {
        int seeded = readLegacyMaxHomes();
        config.setEnabled(true);
        config.setMaxHomes(seeded);
        dangerousConfig.setMaxHomes(seeded);
    }

    private int readLegacyMaxHomes() {
        int fallback = 3;

        File dangerousFile = new File(LockFolderStorage.ensureLockDirectory(pluginDataFolder), "config-dangerous.json");
        Integer dangerousValue = readLegacyMaxHomesFromDangerousConfig(dangerousFile);
        if (dangerousValue != null) {
            return dangerousValue;
        }

        File configFile = new File(pluginDataFolder, "config.json");
        Integer settingsValue = readLegacyMaxHomesFromSettings(configFile);
        if (settingsValue != null) {
            return settingsValue;
        }

        return fallback;
    }

    private Integer readLegacyMaxHomesFromDangerousConfig(File file) {
        JsonObject root = readJsonObject(file);
        if (root == null || !root.has("maxHomes") || !root.get("maxHomes").isJsonPrimitive()) {
            return null;
        }
        try {
            return Math.max(0, root.get("maxHomes").getAsInt());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer readLegacyMaxHomesFromSettings(File file) {
        JsonObject root = readJsonObject(file);
        if (root == null || !root.has("settings") || !root.get("settings").isJsonObject()) {
            return null;
        }
        JsonObject settings = root.getAsJsonObject("settings");
        if (!settings.has("maxHomes") || !settings.get("maxHomes").isJsonPrimitive()) {
            return null;
        }
        try {
            return Math.max(0, settings.get("maxHomes").getAsInt());
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject readJsonObject(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private File getConfigFile() {
        return AddonStorage.resolveAddonConfigFile(pluginDataFolder, HomesAddon.KEY);
    }

    private File getDangerousConfigFile() {
        return AddonStorage.resolveAddonDangerousConfigFile(pluginDataFolder, HomesAddon.KEY);
    }
}
