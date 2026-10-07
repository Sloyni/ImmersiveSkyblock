package com.sloyni.main.island;

public class IslandTemplate {
    public static final String DEFAULT_PREVIEW_IMAGE = "default.png";

    private String id = "default";
    private String titleKey = "template.default.title";
    private String descriptionKey = "template.default.description";
    private String staticTitle;
    private String staticDescription;
    private java.util.Map<String, String> titleTranslations = new java.util.LinkedHashMap<>();
    private java.util.Map<String, String> descriptionTranslations = new java.util.LinkedHashMap<>();
    private String previewImage = DEFAULT_PREVIEW_IMAGE;
    private String prefabName = "island.prefab.json";
    private int displayOrder = 0;
    private int spawnOffsetX = 1;
    private int spawnOffsetY = 5;
    private int spawnOffsetZ = -4;
    private float spawnOffsetPitch = 0f;
    private float spawnOffsetYaw = 0f;
    private boolean enabled = true;

    public void applyDefaults() {
        if (id == null || id.isBlank()) {
            id = "default";
        }
        if (titleKey == null || titleKey.isBlank()) {
            titleKey = "template.default.title";
        }
        if (descriptionKey == null || descriptionKey.isBlank()) {
            descriptionKey = "template.default.description";
        }
        if (titleTranslations == null) {
            titleTranslations = new java.util.LinkedHashMap<>();
        }
        if (descriptionTranslations == null) {
            descriptionTranslations = new java.util.LinkedHashMap<>();
        }
        previewImage = normalizePreviewImage(previewImage);
        if (prefabName == null || prefabName.isBlank()) {
            prefabName = "island.prefab.json";
        }
        if (displayOrder < 0) {
            displayOrder = 0;
        }
    }

    public static String normalizePreviewImage(String previewImage) {
        if (previewImage == null || previewImage.isBlank()) {
            return DEFAULT_PREVIEW_IMAGE;
        }

        String normalized = previewImage.trim().replace('\\', '/');
        if (normalized.equalsIgnoreCase("Common/UI/Custom/Textures/Default.png")) {
            return DEFAULT_PREVIEW_IMAGE;
        }
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getStaticTitle() {
        return staticTitle;
    }

    public void setStaticTitle(String staticTitle) {
        this.staticTitle = staticTitle;
    }

    public String getStaticDescription() {
        return staticDescription;
    }

    public void setStaticDescription(String staticDescription) {
        this.staticDescription = staticDescription;
    }

    public java.util.Map<String, String> getTitleTranslations() {
        return titleTranslations;
    }

    public void setTitleTranslations(java.util.Map<String, String> titleTranslations) {
        this.titleTranslations = titleTranslations;
    }

    public java.util.Map<String, String> getDescriptionTranslations() {
        return descriptionTranslations;
    }

    public void setDescriptionTranslations(java.util.Map<String, String> descriptionTranslations) {
        this.descriptionTranslations = descriptionTranslations;
    }

    public String getPreviewImage() {
        return previewImage;
    }

    public void setPreviewImage(String previewImage) {
        this.previewImage = previewImage;
    }

    public String getPrefabName() {
        return prefabName;
    }

    public void setPrefabName(String prefabName) {
        this.prefabName = prefabName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int getSpawnOffsetX() {
        return spawnOffsetX;
    }

    public void setSpawnOffsetX(int spawnOffsetX) {
        this.spawnOffsetX = spawnOffsetX;
    }

    public int getSpawnOffsetY() {
        return spawnOffsetY;
    }

    public void setSpawnOffsetY(int spawnOffsetY) {
        this.spawnOffsetY = spawnOffsetY;
    }

    public int getSpawnOffsetZ() {
        return spawnOffsetZ;
    }

    public void setSpawnOffsetZ(int spawnOffsetZ) {
        this.spawnOffsetZ = spawnOffsetZ;
    }

    public float getSpawnOffsetPitch() {
        return spawnOffsetPitch;
    }

    public void setSpawnOffsetPitch(float spawnOffsetPitch) {
        this.spawnOffsetPitch = spawnOffsetPitch;
    }

    public float getSpawnOffsetYaw() {
        return spawnOffsetYaw;
    }

    public void setSpawnOffsetYaw(float spawnOffsetYaw) {
        this.spawnOffsetYaw = spawnOffsetYaw;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
