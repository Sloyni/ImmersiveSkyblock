package com.sloyni.main.addons.islandwalls;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IslandWallState {
    private UUID islandOwnerUuid;
    private String wallPatternId = "none";
    private int wallPatternSize = 0;
    private String wallColorName = "red";
    private List<String> wallAlternateColorNames = new ArrayList<>();
    private boolean wallsDisabled;

    public UUID getIslandOwnerUuid() {
        return islandOwnerUuid;
    }

    public void setIslandOwnerUuid(UUID islandOwnerUuid) {
        this.islandOwnerUuid = islandOwnerUuid;
    }

    public String getWallPatternId() {
        return wallPatternId == null || wallPatternId.isBlank() ? "none" : wallPatternId;
    }

    public void setWallPatternId(String wallPatternId) {
        this.wallPatternId = wallPatternId;
    }

    public int getWallPatternSize() {
        return Math.max(0, wallPatternSize);
    }

    public void setWallPatternSize(int wallPatternSize) {
        this.wallPatternSize = Math.max(0, wallPatternSize);
    }

    public String getWallColorName() {
        return wallColorName == null || wallColorName.isBlank() ? "red" : wallColorName;
    }

    public void setWallColorName(String wallColorName) {
        this.wallColorName = wallColorName;
    }

    public List<String> getWallAlternateColorNames() {
        return wallAlternateColorNames == null ? new ArrayList<>() : new ArrayList<>(wallAlternateColorNames);
    }

    public void setWallAlternateColorNames(List<String> wallAlternateColorNames) {
        this.wallAlternateColorNames = wallAlternateColorNames == null ? new ArrayList<>() : new ArrayList<>(wallAlternateColorNames);
    }

    public boolean isWallsDisabled() {
        return wallsDisabled;
    }

    public void setWallsDisabled(boolean wallsDisabled) {
        this.wallsDisabled = wallsDisabled;
    }
}
