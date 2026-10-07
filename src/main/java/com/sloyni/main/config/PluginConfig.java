package com.sloyni.main.config;

import java.util.ArrayList;
import java.util.List;

public class PluginConfig {
    private String language = "en";
    private GridConfig grid = new GridConfig();
    private LobbyConfig lobby = new LobbyConfig();
    private SettingsConfig settings = new SettingsConfig();
    private String defaultTemplateId = "default";
    private transient DangerousConfig dangerousConfig = new DangerousConfig();

    public void applyDefaults() {
        if (this.language == null || this.language.isBlank()) {
            this.language = "en";
        }
        if (this.grid == null) {
            this.grid = new GridConfig();
        }
        if (this.lobby == null) {
            this.lobby = new LobbyConfig();
        }
        if (this.settings == null) {
            this.settings = new SettingsConfig();
        }
        if (this.defaultTemplateId == null || this.defaultTemplateId.isBlank()) {
            this.defaultTemplateId = "default";
        }
        if (this.dangerousConfig == null) {
            this.dangerousConfig = new DangerousConfig();
        }
        this.dangerousConfig.applyDefaults();
        this.grid.applyDefaults();
        this.lobby.applyDefaults();
        this.settings.applyDefaults();
        this.settings.setMaxResets(this.dangerousConfig.getMaxResets());
    }

    public boolean isInitialized() {
        return dangerousConfig.isInitialized();
    }

    public void setInitialized(boolean initialized) {
        dangerousConfig.setInitialized(initialized);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public InitConfig getInit() {
        return dangerousConfig.getInit();
    }

    public void setInit(InitConfig init) {
        dangerousConfig.setInit(init);
    }

    public GridConfig getGrid() {
        return grid;
    }

    public void setGrid(GridConfig grid) {
        this.grid = grid;
    }

    public LobbyConfig getLobby() {
        return lobby;
    }

    public void setLobby(LobbyConfig lobby) {
        this.lobby = lobby;
    }

    public SettingsConfig getSettings() {
        return settings;
    }

    public void setSettings(SettingsConfig settings) {
        this.settings = settings;
    }

    public String getDefaultTemplateId() {
        return defaultTemplateId;
    }

    public void setDefaultTemplateId(String defaultTemplateId) {
        this.defaultTemplateId = defaultTemplateId;
    }

    public DangerousConfig getDangerousConfig() {
        return dangerousConfig;
    }

    public void setDangerousConfig(DangerousConfig dangerousConfig) {
        this.dangerousConfig = dangerousConfig;
    }

    public static class DangerousConfig {
        private boolean initialized = false;
        private InitConfig init = new InitConfig();
        private int maxResets = 3;

        public void applyDefaults() {
            if (this.init == null) {
                this.init = new InitConfig();
            }
            this.init.applyDefaults();
            if (this.maxResets < 0) {
                this.maxResets = 3;
            }
        }

        public boolean isInitialized() {
            return initialized;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        public InitConfig getInit() {
            return init;
        }

        public void setInit(InitConfig init) {
            this.init = init;
        }

        public int getMaxResets() {
            return maxResets;
        }

        public void setMaxResets(int maxResets) {
            this.maxResets = maxResets;
        }
    }

    public static class InitConfig {
        private String worldName = "";
        private int islandSpacing = 400;
        private int islandZoneSize = 200;

        public void applyDefaults() {
            if (this.worldName == null) {
                this.worldName = "";
            }
            if (this.islandSpacing < 0) {
                this.islandSpacing = 0;
            }
            if (this.islandZoneSize < 1) {
                this.islandZoneSize = 200;
            }
        }

        public String getWorldName() {
            return worldName;
        }

        public void setWorldName(String worldName) {
            this.worldName = worldName;
        }

        public int getIslandSpacing() {
            return islandSpacing;
        }

        public void setIslandSpacing(int islandSpacing) {
            this.islandSpacing = islandSpacing;
        }

        public int getIslandZoneSize() {
            return islandZoneSize;
        }

        public void setIslandZoneSize(int islandZoneSize) {
            this.islandZoneSize = islandZoneSize;
        }
    }

    public static class GridConfig {
        private int islandY = 64;

        public void applyDefaults() {
            if (this.islandY < 1) {
                this.islandY = 64;
            }
        }

        public int getIslandY() {
            return islandY;
        }

        public void setIslandY(int islandY) {
            this.islandY = islandY;
        }
    }

    public static class LobbyConfig {
        private String worldName = "";
        private int x = 0;
        private int y = 64;
        private int z = 0;
        private float pitch = 0f;
        private float yaw = 0f;
        private boolean configured = false;

        public void applyDefaults() {
            if (this.worldName == null) {
                this.worldName = "";
            }
            if (this.y < 1) {
                this.y = 64;
            }
        }

        public String getWorldName() {
            return worldName;
        }

        public void setWorldName(String worldName) {
            this.worldName = worldName;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public boolean isConfigured() {
            return configured;
        }

        public void setConfigured(boolean configured) {
            this.configured = configured;
        }
    }

    public static class SettingsConfig {
        private boolean spawnProtectionEnabled = true;
        private int spawnProtectionRadius = 20;
        private transient int maxResets = 3;
        private List<String> enabledLanguages = new ArrayList<>();
        private boolean autoLangMatchEnabled = false;
        private String fallbackLanguage = "en";
        private boolean voidKillItemsEnabled = true;
        private boolean voidKillEntitiesEnabled = true;
        private boolean voidTeleportEnabled = false;
        private boolean translateTemplates = true;
        private boolean visitorDropEnabled = false;
        private boolean visitorPickupEnabled = false;

        public void applyDefaults() {
            if (spawnProtectionRadius < 1) {
                spawnProtectionRadius = 20;
            }
            if (enabledLanguages == null) {
                enabledLanguages = new ArrayList<>();
            }
            if (enabledLanguages.isEmpty()) {
                enabledLanguages.add("en");
                enabledLanguages.add("fr");
            }
            if (fallbackLanguage == null || fallbackLanguage.isBlank()) {
                fallbackLanguage = "en";
            }
            fallbackLanguage = fallbackLanguage.trim().toLowerCase();
        }

        public boolean isSpawnProtectionEnabled() {
            return spawnProtectionEnabled;
        }

        public void setSpawnProtectionEnabled(boolean spawnProtectionEnabled) {
            this.spawnProtectionEnabled = spawnProtectionEnabled;
        }

        public int getSpawnProtectionRadius() {
            return spawnProtectionRadius;
        }

        public void setSpawnProtectionRadius(int spawnProtectionRadius) {
            this.spawnProtectionRadius = spawnProtectionRadius;
        }

        public int getMaxResets() {
            return maxResets;
        }

        public void setMaxResets(int maxResets) {
            this.maxResets = maxResets;
        }

        public List<String> getEnabledLanguages() {
            return enabledLanguages == null ? new ArrayList<>() : enabledLanguages;
        }

        public void setEnabledLanguages(List<String> enabledLanguages) {
            this.enabledLanguages = enabledLanguages;
        }

        public boolean isAutoLangMatchEnabled() {
            return autoLangMatchEnabled;
        }

        public void setAutoLangMatchEnabled(boolean autoLangMatchEnabled) {
            this.autoLangMatchEnabled = autoLangMatchEnabled;
        }

        public String getFallbackLanguage() {
            return fallbackLanguage;
        }

        public void setFallbackLanguage(String fallbackLanguage) {
            this.fallbackLanguage = fallbackLanguage;
        }

        public boolean isVoidKillItemsEnabled() {
            return voidKillItemsEnabled;
        }

        public void setVoidKillItemsEnabled(boolean voidKillItemsEnabled) {
            this.voidKillItemsEnabled = voidKillItemsEnabled;
        }

        public boolean isVoidKillEntitiesEnabled() {
            return voidKillEntitiesEnabled;
        }

        public void setVoidKillEntitiesEnabled(boolean voidKillEntitiesEnabled) {
            this.voidKillEntitiesEnabled = voidKillEntitiesEnabled;
        }

        public boolean isVoidTeleportEnabled() {
            return voidTeleportEnabled;
        }

        public void setVoidTeleportEnabled(boolean voidTeleportEnabled) {
            this.voidTeleportEnabled = voidTeleportEnabled;
        }

        public boolean isTranslateTemplates() {
            return translateTemplates;
        }

        public void setTranslateTemplates(boolean translateTemplates) {
            this.translateTemplates = translateTemplates;
        }

        public boolean isVisitorDropEnabled() {
            return visitorDropEnabled;
        }

        public void setVisitorDropEnabled(boolean visitorDropEnabled) {
            this.visitorDropEnabled = visitorDropEnabled;
        }

        public boolean isVisitorPickupEnabled() {
            return visitorPickupEnabled;
        }

        public void setVisitorPickupEnabled(boolean visitorPickupEnabled) {
            this.visitorPickupEnabled = visitorPickupEnabled;
        }
    }
}
