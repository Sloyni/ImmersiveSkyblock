package com.sloyni.main.addons.warps;

import java.util.UUID;

public class IslandWarpData {
    private UUID ownerUuid;
    private String ownerName;
    private UUID islandOwnerUuid;
    private String worldName;
    private int x;
    private int y;
    private int z;
    private boolean enabled;
    private String warpName;
    private float pitch = 0f;
    private float yaw = 0f;
    private long visitCount = 0L;

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public UUID getIslandOwnerUuid() {
        return islandOwnerUuid;
    }

    public void setIslandOwnerUuid(UUID islandOwnerUuid) {
        this.islandOwnerUuid = islandOwnerUuid;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWarpName() {
        return warpName;
    }

    public void setWarpName(String warpName) {
        this.warpName = warpName;
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

    public long getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(long visitCount) {
        this.visitCount = Math.max(0L, visitCount);
    }
}
