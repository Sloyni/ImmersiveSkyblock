package com.sloyni.main.addons.warps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sloyni.main.addons.AddonStorage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class WarpsAddonConfigManager {
    private final File pluginDataFolder;
    private final Gson gson;
    private WarpsAddonConfig config;
    private WarpsAddonDangerousConfig dangerousConfig;

    public WarpsAddonConfigManager(File pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void load() {
        this.config = readConfig();
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

    public WarpsAddonConfig getConfig() {
        return config;
    }

    public WarpsAddonDangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    private WarpsAddonConfig readConfig() {
        if (!getConfigFile().exists()) {
            return new WarpsAddonConfig();
        }
        try (Reader reader = new FileReader(getConfigFile())) {
            WarpsAddonConfig loaded = gson.fromJson(reader, WarpsAddonConfig.class);
            return loaded == null ? new WarpsAddonConfig() : loaded;
        } catch (Exception ignored) {
            return new WarpsAddonConfig();
        }
    }

    private WarpsAddonDangerousConfig readDangerousConfig() {
        if (!getDangerousConfigFile().exists()) {
            return new WarpsAddonDangerousConfig();
        }
        try (Reader reader = new FileReader(getDangerousConfigFile())) {
            WarpsAddonDangerousConfig loaded = gson.fromJson(reader, WarpsAddonDangerousConfig.class);
            return loaded == null ? new WarpsAddonDangerousConfig() : loaded;
        } catch (Exception ignored) {
            return new WarpsAddonDangerousConfig();
        }
    }

    private File getConfigFile() {
        return AddonStorage.resolveAddonConfigFile(pluginDataFolder, WarpsAddon.KEY);
    }

    private File getDangerousConfigFile() {
        return AddonStorage.resolveAddonDangerousConfigFile(pluginDataFolder, WarpsAddon.KEY);
    }
}
