package com.sloyni.main.addons.oregenerator;

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

public class OreGeneratorAddonConfigManager {
    private final File pluginDataFolder;
    private final Gson gson;
    private OreGeneratorAddonConfig config;
    private OreGeneratorAddonDangerousConfig dangerousConfig;

    public OreGeneratorAddonConfigManager(File pluginDataFolder) {
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
        } catch (IOException e) {
            System.err.println("[ISkyblock] Failed to save ore generator config: " + e.getMessage());
        }

        try (Writer writer = new FileWriter(getDangerousConfigFile())) {
            gson.toJson(this.dangerousConfig, writer);
        } catch (IOException e) {
            System.err.println("[ISkyblock] Failed to save ore generator dangerous config: " + e.getMessage());
        }
    }

    public OreGeneratorAddonConfig getConfig() {
        return config;
    }

    public OreGeneratorAddonDangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    private OreGeneratorAddonConfig readConfig(boolean configExists) {
        if (!configExists) {
            return new OreGeneratorAddonConfig();
        }
        try (Reader reader = new FileReader(getConfigFile())) {
            OreGeneratorAddonConfig loaded = gson.fromJson(reader, OreGeneratorAddonConfig.class);
            return loaded == null ? new OreGeneratorAddonConfig() : loaded;
        } catch (Exception ignored) {
            return new OreGeneratorAddonConfig();
        }
    }

    private OreGeneratorAddonDangerousConfig readDangerousConfig() {
        if (!getDangerousConfigFile().exists()) {
            return new OreGeneratorAddonDangerousConfig();
        }
        try (Reader reader = new FileReader(getDangerousConfigFile())) {
            OreGeneratorAddonDangerousConfig loaded = gson.fromJson(reader, OreGeneratorAddonDangerousConfig.class);
            return loaded == null ? new OreGeneratorAddonDangerousConfig() : loaded;
        } catch (Exception ignored) {
            return new OreGeneratorAddonDangerousConfig();
        }
    }

    private void seedFromMainConfig() {
        config.setEnabled(true);
        JsonObject settings = loadLegacySettingsObject();
        if (settings == null) {
            return;
        }

        Double oreChance = readDouble(settings, "oreChance");
        Long regenDelayMs = readLong(settings, "regenDelayMs");
        Boolean spawnRubble = readBoolean(settings, "spawnRubble");

        if (oreChance != null) {
            config.setOreChance(oreChance);
        }
        if (regenDelayMs != null) {
            config.setRegenDelayMs(regenDelayMs);
        }
        if (spawnRubble != null) {
            config.setSpawnRubble(spawnRubble);
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

    private Long readLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement value = obj.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsLong();
        } catch (Exception ignored) {
            return null;
        }
    }

    private File getConfigFile() {
        return AddonStorage.resolveAddonConfigFile(pluginDataFolder, OreGeneratorAddon.KEY);
    }

    private File getDangerousConfigFile() {
        return AddonStorage.resolveAddonDangerousConfigFile(pluginDataFolder, OreGeneratorAddon.KEY);
    }
}
