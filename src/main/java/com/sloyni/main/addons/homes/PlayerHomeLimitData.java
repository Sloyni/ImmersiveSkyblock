package com.sloyni.main.addons.homes;

public class PlayerHomeLimitData {
    private int addedHomes;
    private Integer maxHomes;
    private boolean customAdjusted;

    public int getAddedHomes() {
        if (addedHomes > 0) {
            return addedHomes;
        }
        return maxHomes == null ? 0 : Math.max(0, maxHomes);
    }

    public void setAddedHomes(int addedHomes) {
        this.addedHomes = Math.max(0, addedHomes);
        this.maxHomes = null;
    }

    public int getMaxHomes() {
        return getAddedHomes();
    }

    public void setMaxHomes(int maxHomes) {
        setAddedHomes(maxHomes);
    }

    public boolean isCustomAdjusted() {
        return customAdjusted;
    }

    public void setCustomAdjusted(boolean customAdjusted) {
        this.customAdjusted = customAdjusted;
    }
}
