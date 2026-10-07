package com.sloyni.main.addons.oregenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OreGeneratorAddon {
    public static final String KEY = "ore_generator";

    private final OreGeneratorAddonConfigManager configManager;

    public OreGeneratorAddon(File pluginDataFolder) {
        this.configManager = new OreGeneratorAddonConfigManager(pluginDataFolder);
    }

    public boolean isEnabled() {
        return configManager.getConfig().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        configManager.getConfig().setEnabled(enabled);
        configManager.save();
    }

    public OreGeneratorAddonConfig getGeneratorConfig() {
        return configManager.getConfig();
    }

    public double getOreChance() {
        return configManager.getConfig().getOreChance();
    }

    public long getRegenDelayMs() {
        return configManager.getConfig().getRegenDelayMs();
    }

    public boolean isSpawnRubble() {
        return configManager.getConfig().isSpawnRubble();
    }

    public List<OreGeneratorAddonConfig.OreEntry> getOres() {
        return configManager.getConfig().getOres();
    }

    public Map<String, Double> getOresSnapshot() {
        Map<String, Double> snapshot = new LinkedHashMap<>();
        for (OreGeneratorAddonConfig.OreEntry ore : getOres()) {
            if (ore.getBlockId() != null && !ore.getBlockId().isBlank()) {
                snapshot.put(ore.getBlockId(), ore.getWeight());
            }
        }
        return snapshot;
    }

    public void handleOreChanceSetting(double oreChance) {
        configManager.getConfig().setOreChance(Math.max(0.0d, Math.min(1.0d, oreChance)));
        configManager.save();
    }

    public void handleRegenDelaySetting(long regenDelayMs) {
        configManager.getConfig().setRegenDelayMs(Math.max(0, regenDelayMs));
        configManager.save();
    }

    public void handleSpawnRubbleSetting(boolean spawnRubble) {
        configManager.getConfig().setSpawnRubble(spawnRubble);
        configManager.save();
    }

    public void addOre(String blockId, double weight) {
        if (blockId == null || blockId.isBlank()) return;
        List<OreGeneratorAddonConfig.OreEntry> ores = configManager.getConfig().getOres();
        for (OreGeneratorAddonConfig.OreEntry ore : ores) {
            if (blockId.equals(ore.getBlockId())) {
                ore.setWeight(weight);
                configManager.save();
                return;
            }
        }
        ores.add(new OreGeneratorAddonConfig.OreEntry(blockId, weight));
        configManager.save();
    }

    public void updateOre(String oldBlockId, String newBlockId, double newWeight) {
        if (oldBlockId == null || oldBlockId.isBlank()) return;
        List<OreGeneratorAddonConfig.OreEntry> ores = configManager.getConfig().getOres();
        for (OreGeneratorAddonConfig.OreEntry ore : ores) {
            if (oldBlockId.equals(ore.getBlockId())) {
                if (newBlockId != null && !newBlockId.isBlank()) {
                    ore.setBlockId(newBlockId);
                }
                ore.setWeight(newWeight);
                configManager.save();
                return;
            }
        }
    }

    public void removeOre(String blockId) {
        if (blockId == null || blockId.isBlank()) return;
        List<OreGeneratorAddonConfig.OreEntry> ores = configManager.getConfig().getOres();
        ores.removeIf(ore -> blockId.equals(ore.getBlockId()));
        configManager.save();
    }

    // ── Tier management ──

    public List<OreGeneratorAddonConfig.GeneratorTier> getTiersSnapshot() {
        return new ArrayList<>(configManager.getConfig().getTiers());
    }

    public void addTier(String name, String permission, double oreChance) {
        if (name == null || name.isBlank()) return;
        List<OreGeneratorAddonConfig.GeneratorTier> tiers = configManager.getConfig().getTiers();
        for (OreGeneratorAddonConfig.GeneratorTier t : tiers) {
            if (name.equals(t.getName())) {
                t.setPermission(permission);
                t.setOreChance(oreChance);
                configManager.save();
                return;
            }
        }
        tiers.add(new OreGeneratorAddonConfig.GeneratorTier(name, permission, oreChance, new java.util.HashMap<>()));
        configManager.save();
    }

    public void updateTier(String oldName, String newName, String permission, double oreChance) {
        if (oldName == null || oldName.isBlank()) return;
        List<OreGeneratorAddonConfig.GeneratorTier> tiers = configManager.getConfig().getTiers();
        for (OreGeneratorAddonConfig.GeneratorTier t : tiers) {
            if (oldName.equals(t.getName())) {
                if (newName != null && !newName.isBlank()) {
                    t.setName(newName);
                }
                if (permission != null) {
                    t.setPermission(permission);
                }
                t.setOreChance(oreChance);
                configManager.save();
                return;
            }
        }
    }

    public void removeTier(String name) {
        if (name == null || name.isBlank()) return;
        List<OreGeneratorAddonConfig.GeneratorTier> tiers = configManager.getConfig().getTiers();
        tiers.removeIf(t -> name.equals(t.getName()));
        configManager.save();
    }
}
