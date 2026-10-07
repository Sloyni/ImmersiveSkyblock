package com.sloyni.main.addons.homes;

import java.util.LinkedHashMap;
import java.util.Map;

public class HomesAddonConfig {
    private boolean enabled = true;
    private int maxHomes = 3;
    private Map<String, Integer> homeGroups = new LinkedHashMap<>();

    public void applyDefaults() {
        if (maxHomes < 0) {
            maxHomes = 3;
        }
        if (homeGroups == null) {
            homeGroups = new LinkedHashMap<>();
        }
        Map<String, Integer> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : homeGroups.entrySet()) {
            String groupId = normalizeGroupId(entry.getKey());
            if (groupId.isBlank() || "default".equals(groupId)) {
                continue;
            }
            int limit = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            sanitized.put(groupId, limit);
        }
        homeGroups = sanitized;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public void setMaxHomes(int maxHomes) {
        this.maxHomes = maxHomes;
    }

    public Map<String, Integer> getHomeGroups() {
        return homeGroups;
    }

    public void setHomeGroups(Map<String, Integer> homeGroups) {
        this.homeGroups = homeGroups;
    }

    private static String normalizeGroupId(String rawGroupId) {
        if (rawGroupId == null) {
            return "";
        }
        String normalized = rawGroupId.trim().toLowerCase();
        return normalized.matches("^[a-z0-9_-]+$") ? normalized : "";
    }
}
