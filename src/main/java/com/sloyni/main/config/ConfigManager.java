package com.sloyni.main.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.json";
    private static final String DANGEROUS_CONFIG_FILE = "config-dangerous.json";

    private final File dataFolder;
    private final Gson gson;
    private PluginConfig config;

    public ConfigManager(File dataFolder) {
        this.dataFolder = dataFolder;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        LockFolderStorage.ensureLockDirectory(dataFolder);
        load();
    }

    public void load() {
        File file = getConfigFile();
        JsonObject legacyRoot = readJsonObject(file);
        if (!file.exists()) {
            this.config = new PluginConfig();
            this.config.setDangerousConfig(loadDangerousConfig(legacyRoot));
            this.config.applyDefaults();
            save();
            return;
        }

        try {
            this.config = legacyRoot == null ? null : gson.fromJson(legacyRoot, PluginConfig.class);
            if (this.config == null) {
                this.config = new PluginConfig();
            }
        } catch (Exception ex) {
            this.config = new PluginConfig();
        }

        this.config.setDangerousConfig(loadDangerousConfig(legacyRoot));
        this.config.applyDefaults();
        save();
    }

    public void save() {
        File file = getConfigFile();
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(this.config, writer);
        } catch (IOException ignored) {
        }

        File dangerousFile = getDangerousConfigFile();
        try (Writer writer = new FileWriter(dangerousFile)) {
            gson.toJson(this.config.getDangerousConfig(), writer);
        } catch (IOException ignored) {
        }
    }

    public File getConfigFile() {
        return new File(this.dataFolder, CONFIG_FILE);
    }

    public File getDangerousConfigFile() {
        return new File(LockFolderStorage.ensureLockDirectory(this.dataFolder), DANGEROUS_CONFIG_FILE);
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public PluginConfig getConfig() {
        return config;
    }

    private PluginConfig.DangerousConfig loadDangerousConfig(JsonObject legacyRoot) {
        File dangerousFile = getDangerousConfigFile();
        JsonObject dangerousJson = readJsonObject(dangerousFile);
        if (dangerousJson != null) {
            PluginConfig.DangerousConfig dangerousConfig = gson.fromJson(dangerousJson, PluginConfig.DangerousConfig.class);
            if (dangerousConfig == null) {
                dangerousConfig = new PluginConfig.DangerousConfig();
            }
            applyLegacyDangerousSettings(legacyRoot, dangerousConfig);
            return dangerousConfig;
        }
        if (legacyRoot != null && (legacyRoot.has("initialized") || legacyRoot.has("init"))) {
            PluginConfig.DangerousConfig dangerousConfig = gson.fromJson(legacyRoot, PluginConfig.DangerousConfig.class);
            if (dangerousConfig == null) {
                dangerousConfig = new PluginConfig.DangerousConfig();
            }
            applyLegacyDangerousSettings(legacyRoot, dangerousConfig);
            return dangerousConfig;
        }
        PluginConfig.DangerousConfig dangerousConfig = new PluginConfig.DangerousConfig();
        applyLegacyDangerousSettings(legacyRoot, dangerousConfig);
        return dangerousConfig;
    }

    private void applyLegacyDangerousSettings(JsonObject legacyRoot, PluginConfig.DangerousConfig dangerousConfig) {
        if (legacyRoot == null || dangerousConfig == null || !legacyRoot.has("settings") || !legacyRoot.get("settings").isJsonObject()) {
            return;
        }

        JsonObject settings = legacyRoot.getAsJsonObject("settings");
        if (settings.has("maxResets") && settings.get("maxResets").isJsonPrimitive()) {
            try {
                dangerousConfig.setMaxResets(Math.max(0, settings.get("maxResets").getAsInt()));
            } catch (Exception ignored) {
            }
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
}
