package com.sloyni.main.addons.islandwalls;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sloyni.main.addons.AddonStorage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class IslandWallsAddonConfigManager {
    private final File pluginDataFolder;
    private final Gson gson;
    private IslandWallsAddonConfig config;
    private IslandWallsAddonDangerousConfig dangerousConfig;

    public IslandWallsAddonConfigManager(File pluginDataFolder) {
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

    public IslandWallsAddonConfig getConfig() {
        return config;
    }

    public IslandWallsAddonDangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    private IslandWallsAddonConfig readConfig() {
        if (!getConfigFile().exists()) {
            return new IslandWallsAddonConfig();
        }
        try (Reader reader = new FileReader(getConfigFile())) {
            IslandWallsAddonConfig loaded = gson.fromJson(reader, IslandWallsAddonConfig.class);
            return loaded == null ? new IslandWallsAddonConfig() : loaded;
        } catch (Exception ignored) {
            return new IslandWallsAddonConfig();
        }
    }

    private IslandWallsAddonDangerousConfig readDangerousConfig() {
        if (!getDangerousConfigFile().exists()) {
            return new IslandWallsAddonDangerousConfig();
        }
        try (Reader reader = new FileReader(getDangerousConfigFile())) {
            IslandWallsAddonDangerousConfig loaded = gson.fromJson(reader, IslandWallsAddonDangerousConfig.class);
            return loaded == null ? new IslandWallsAddonDangerousConfig() : loaded;
        } catch (Exception ignored) {
            return new IslandWallsAddonDangerousConfig();
        }
    }

    private File getConfigFile() {
        return AddonStorage.resolveAddonConfigFile(pluginDataFolder, IslandWallsAddon.KEY);
    }

    private File getDangerousConfigFile() {
        return AddonStorage.resolveAddonDangerousConfigFile(pluginDataFolder, IslandWallsAddon.KEY);
    }
}
