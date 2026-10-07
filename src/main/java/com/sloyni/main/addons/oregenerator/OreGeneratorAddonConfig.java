package com.sloyni.main.addons.oregenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class OreGeneratorAddonConfig {
    private boolean enabled = true;
    private double oreChance = 0.2d;
    private long regenDelayMs = 1000;
    private boolean spawnRubble = true;
    private List<OreEntry> ores = createDefaultOres();
    private List<GeneratorTier> tiers = createDefaultTiers();

    public void applyDefaults() {
        if (oreChance < 0.0d || oreChance > 1.0d) {
            oreChance = 0.2d;
        }
        if (regenDelayMs < 0) {
            regenDelayMs = 1000;
        }
        if (ores == null || ores.isEmpty()) {
            ores = createDefaultOres();
        }
        if (tiers == null) {
            tiers = createDefaultTiers();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getOreChance() {
        return oreChance;
    }

    public void setOreChance(double oreChance) {
        this.oreChance = oreChance;
    }

    public long getRegenDelayMs() {
        return regenDelayMs;
    }

    public void setRegenDelayMs(long regenDelayMs) {
        this.regenDelayMs = regenDelayMs;
    }

    public boolean isSpawnRubble() {
        return spawnRubble;
    }

    public void setSpawnRubble(boolean spawnRubble) {
        this.spawnRubble = spawnRubble;
    }

    public List<OreEntry> getOres() {
        if (ores == null || ores.isEmpty()) {
            ores = createDefaultOres();
        }
        return ores;
    }

    public void setOres(List<OreEntry> ores) {
        this.ores = ores;
    }

    public List<GeneratorTier> getTiers() {
        return tiers != null ? tiers : new ArrayList<>();
    }

    public void setTiers(List<GeneratorTier> tiers) {
        this.tiers = tiers;
    }

    public double getTotalOreWeight() {
        double total = 0.0d;
        for (OreEntry ore : getOres()) {
            total += ore.getWeight();
        }
        return total;
    }

    public double getEffectiveOreChance(Predicate<String> hasPermission) {
        if (tiers != null) {
            for (GeneratorTier tier : tiers) {
                if (tier.getPermission() != null && hasPermission.test(tier.getPermission())) {
                    Double tierChance = tier.getOreChance();
                    if (tierChance != null && tierChance > 0.0d) {
                        return tierChance;
                    }
                }
            }
        }
        return oreChance;
    }

    public List<OreEntry> getEffectiveOres(Predicate<String> hasPermission) {
        List<OreEntry> baseOres = getOres();
        Map<String, Double> overrides = null;
        if (tiers != null) {
            for (GeneratorTier tier : tiers) {
                if (tier.getPermission() != null && hasPermission.test(tier.getPermission())) {
                    overrides = tier.getOverrides();
                    break;
                }
            }
        }
        if (overrides == null || overrides.isEmpty()) {
            return baseOres;
        }
        List<OreEntry> effectiveOres = new ArrayList<>();
        for (OreEntry ore : baseOres) {
            Double override = overrides.get(ore.getBlockId());
            if (override != null) {
                effectiveOres.add(new OreEntry(ore.getBlockId(), override));
            } else {
                effectiveOres.add(ore);
            }
        }
        return effectiveOres;
    }

    public double getEffectiveTotalWeight(Predicate<String> hasPermission) {
        double total = 0.0d;
        for (OreEntry ore : getEffectiveOres(hasPermission)) {
            total += ore.getWeight();
        }
        return total;
    }

    private static List<OreEntry> createDefaultOres() {
        List<OreEntry> defaultOres = new ArrayList<>();
        defaultOres.add(new OreEntry("Ore_Copper_Stone", 35.0d));
        defaultOres.add(new OreEntry("Ore_Iron_Stone", 30.0d));
        defaultOres.add(new OreEntry("Ore_Silver_Stone", 15.0d));
        defaultOres.add(new OreEntry("Ore_Gold_Stone", 10.0d));
        defaultOres.add(new OreEntry("Ore_Thorium_Mud", 3.0d));
        defaultOres.add(new OreEntry("Ore_Mithril_Stone", 1.5d));
        defaultOres.add(new OreEntry("Ore_Adamantite_Magma", 1.0d));
        return defaultOres;
    }

    private static List<GeneratorTier> createDefaultTiers() {
        List<GeneratorTier> defaultTiers = new ArrayList<>();
        Map<String, Double> vipPlusOverrides = new HashMap<>();
        vipPlusOverrides.put("Ore_Iron_Stone", 35.0d);
        vipPlusOverrides.put("Ore_Gold_Stone", 15.0d);
        vipPlusOverrides.put("Ore_Mithril_Stone", 3.0d);
        vipPlusOverrides.put("Ore_Adamantite_Magma", 2.0d);
        defaultTiers.add(new GeneratorTier("vip_plus", "iskyblock.generator.vipplus", 0.3d, vipPlusOverrides));

        Map<String, Double> vipOverrides = new HashMap<>();
        vipOverrides.put("Ore_Iron_Stone", 32.0d);
        vipOverrides.put("Ore_Gold_Stone", 12.0d);
        defaultTiers.add(new GeneratorTier("vip", "iskyblock.generator.vip", 0.25d, vipOverrides));
        return defaultTiers;
    }

    public static class OreEntry {
        private String blockId;
        private double weight;

        public OreEntry() {
        }

        public OreEntry(String blockId, double weight) {
            this.blockId = blockId;
            this.weight = weight;
        }

        public String getBlockId() {
            return blockId;
        }

        public void setBlockId(String blockId) {
            this.blockId = blockId;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    public static class GeneratorTier {
        private String name;
        private String permission;
        private Double oreChance;
        private Map<String, Double> overrides;

        public GeneratorTier() {
        }

        public GeneratorTier(String name, String permission, Double oreChance, Map<String, Double> overrides) {
            this.name = name;
            this.permission = permission;
            this.oreChance = oreChance;
            this.overrides = overrides;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public Double getOreChance() {
            return oreChance;
        }

        public void setOreChance(Double oreChance) {
            this.oreChance = oreChance;
        }

        public Map<String, Double> getOverrides() {
            return overrides != null ? overrides : new HashMap<>();
        }

        public void setOverrides(Map<String, Double> overrides) {
            this.overrides = overrides;
        }
    }
}
