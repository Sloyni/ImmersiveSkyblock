package com.sloyni.main.addons.mobsmanager;

public class MobsManagerAddonConfig {
    private boolean enabled = true;
    private boolean reduceMobsEnabled = true;
    private int reduceMobsRatePercent = 20;
    private boolean torchProtectionEnabled = true;
    private int torchProtectionRadius = 5;

    public void applyDefaults() {
        if (reduceMobsRatePercent < 0 || reduceMobsRatePercent > 100) {
            reduceMobsRatePercent = 20;
        }
        if (torchProtectionRadius < 1) {
            torchProtectionRadius = 5;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReduceMobsEnabled() {
        return reduceMobsEnabled;
    }

    public void setReduceMobsEnabled(boolean reduceMobsEnabled) {
        this.reduceMobsEnabled = reduceMobsEnabled;
    }

    public int getReduceMobsRatePercent() {
        return reduceMobsRatePercent;
    }

    public void setReduceMobsRatePercent(int reduceMobsRatePercent) {
        this.reduceMobsRatePercent = reduceMobsRatePercent;
    }

    public boolean isTorchProtectionEnabled() {
        return torchProtectionEnabled;
    }

    public void setTorchProtectionEnabled(boolean torchProtectionEnabled) {
        this.torchProtectionEnabled = torchProtectionEnabled;
    }

    public int getTorchProtectionRadius() {
        return torchProtectionRadius;
    }

    public void setTorchProtectionRadius(int torchProtectionRadius) {
        this.torchProtectionRadius = torchProtectionRadius;
    }
}
