package com.sloyni.main.addons.levels;

public class LevelsAddonConfig {
    private boolean enabled = true;
    private boolean templateValueEnabled = true;
    private int levelValue = 100;
    private double blockDefaultValue = 1.0D;

    public void applyDefaults() {
        if (levelValue < 1) {
            levelValue = 100;
        }
        if (Double.isNaN(blockDefaultValue) || Double.isInfinite(blockDefaultValue) || blockDefaultValue <= 0.0D) {
            blockDefaultValue = 1.0D;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTemplateValueEnabled() {
        return templateValueEnabled;
    }

    public void setTemplateValueEnabled(boolean templateValueEnabled) {
        this.templateValueEnabled = templateValueEnabled;
    }

    public int getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(int levelValue) {
        this.levelValue = levelValue;
    }

    public double getBlockDefaultValue() {
        return blockDefaultValue;
    }

    public void setBlockDefaultValue(double blockDefaultValue) {
        this.blockDefaultValue = blockDefaultValue;
    }
}