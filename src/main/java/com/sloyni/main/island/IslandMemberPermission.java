package com.sloyni.main.island;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum IslandMemberPermission {
    RENAME_ISLAND("rename_island", "ui.menu.permissions.item.rename_island", "Rename island"),
    SET_SPAWN("set_spawn", "ui.menu.permissions.item.set_spawn", "Set spawn"),
    LOCK_ISLAND("lock_island", "ui.menu.permissions.item.lock_island", "Lock / unlock island"),
    RESET_ISLAND("reset_island", "ui.menu.permissions.item.reset_island", "Reset island"),
    REFRESH_LEVEL("refresh_level", "ui.menu.permissions.item.refresh_level", "Refresh island level", AddonDependency.LEVELS),
    MANAGE_WARPS("manage_warps", "ui.menu.permissions.item.manage_warps", "Manage warps", AddonDependency.WARPS),
    MANAGE_WALLS("manage_walls", "ui.menu.permissions.item.manage_walls", "Manage walls", AddonDependency.WALLS),
    TEAM_INVITE("team_invite", "ui.menu.permissions.item.team_invite", "Invite members"),
    TEAM_REMOVE_MEMBER("team_remove_member", "ui.menu.permissions.item.team_remove_member", "Remove members"),
    TEAM_TRANSFER_LEADERSHIP("team_transfer_leadership", "ui.menu.permissions.item.team_transfer_leadership", "Transfer leadership"),
    TEAM_MANAGE_COOP("team_manage_coop", "ui.menu.permissions.item.team_manage_coop", "Manage coop"),
    TEAM_MANAGE_BANS("team_manage_bans", "ui.menu.permissions.item.team_manage_bans", "Manage bans"),
    TEAM_EXPEL("team_expel", "ui.menu.permissions.item.team_expel", "Expel visitors"),
    CHANGE_ISLAND_SETTINGS("change_island_settings", "ui.menu.permissions.item.change_island_settings", "Change island settings");

    private enum AddonDependency {
        NONE,
        LEVELS,
        WARPS,
        WALLS
    }

    private static final Map<String, IslandMemberPermission> BY_KEY = new LinkedHashMap<>();

    static {
        for (IslandMemberPermission permission : values()) {
            BY_KEY.put(permission.key, permission);
        }
    }

    private final String key;
    private final String labelKey;
    private final String fallbackLabel;
    private final AddonDependency addonDependency;

    IslandMemberPermission(String key, String labelKey, String fallbackLabel) {
        this(key, labelKey, fallbackLabel, AddonDependency.NONE);
    }

    IslandMemberPermission(String key, String labelKey, String fallbackLabel, AddonDependency addonDependency) {
        this.key = key;
        this.labelKey = labelKey;
        this.fallbackLabel = fallbackLabel;
        this.addonDependency = addonDependency;
    }

    public String getKey() {
        return key;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getFallbackLabel() {
        return fallbackLabel;
    }

    public boolean isLeaderOnly() {
        return this == RESET_ISLAND;
    }

    public boolean isAvailable(boolean levelsEnabled, boolean warpsEnabled, boolean wallsEnabled) {
        return switch (addonDependency) {
            case NONE -> true;
            case LEVELS -> levelsEnabled;
            case WARPS -> warpsEnabled;
            case WALLS -> wallsEnabled;
        };
    }

    public static List<IslandMemberPermission> ordered() {
        return List.of(values());
    }

    /**
     * Permissions configurable for non-leader members.
     * Leader-only permissions are intentionally excluded.
     */
    public static List<IslandMemberPermission> orderedConfigurable() {
        List<IslandMemberPermission> configurable = new ArrayList<>();
        for (IslandMemberPermission permission : values()) {
            if (!permission.isLeaderOnly()) {
                configurable.add(permission);
            }
        }
        return List.copyOf(configurable);
    }

    public static IslandMemberPermission fromKey(String rawKey) {
        String normalized = normalizeKey(rawKey);
        if (normalized.isBlank()) {
            return null;
        }
        return BY_KEY.get(normalized);
    }

    public static String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        return rawKey.trim().toLowerCase(Locale.ROOT);
    }
}
