package com.sloyni.main.addons.homes;

public class HomesAddonDangerousConfig {
    private int maxHomes = 3;

    public void applyDefaults() {
        if (maxHomes < 0) {
            maxHomes = 3;
        }
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public void setMaxHomes(int maxHomes) {
        this.maxHomes = maxHomes;
    }
}
