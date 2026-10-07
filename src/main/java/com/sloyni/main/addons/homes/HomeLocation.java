package com.sloyni.main.addons.homes;

public class HomeLocation {
    private int x;
    private int y;
    private int z;
    private float pitch = 0f;
    private float yaw = 0f;

    public HomeLocation() {
    }

    public HomeLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public HomeLocation(int x, int y, int z, float pitch, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
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
}
