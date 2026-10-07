package com.sloyni.main.addons.warps;

public class WarpsAddonConfig {
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
