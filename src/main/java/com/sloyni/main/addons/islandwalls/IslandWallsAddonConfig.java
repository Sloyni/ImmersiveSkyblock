package com.sloyni.main.addons.islandwalls;

public class IslandWallsAddonConfig {
    private boolean enabled = true;

    public void applyDefaults() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
