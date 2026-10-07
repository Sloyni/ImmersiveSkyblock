package com.sloyni.main.island.stats;

import java.util.UUID;

/**
 * Persistent statistics for an island, keyed by owner UUID.
 * Tracks mob kills on the island, owner deaths, and owner time played (in seconds).
 */
public class IslandStats {

    private String ownerUuid;
    private long mobsKilled = 0;
    private long deaths = 0;
    private long timePlayedSeconds = 0;

    public IslandStats() {}

    public IslandStats(UUID ownerUuid) {
        this.ownerUuid = ownerUuid.toString();
    }

    // ─── Owner UUID ───────────────────────────────────────────────────────────

    public UUID getOwnerUuid() {
        try {
            return ownerUuid != null ? UUID.fromString(ownerUuid) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid != null ? ownerUuid.toString() : null;
    }

    // ─── Mobs killed ─────────────────────────────────────────────────────────

    public long getMobsKilled() {
        return mobsKilled;
    }

    public void setMobsKilled(long mobsKilled) {
        this.mobsKilled = Math.max(0, mobsKilled);
    }

    public void incrementMobsKilled() {
        this.mobsKilled++;
    }

    // ─── Deaths ──────────────────────────────────────────────────────────────

    public long getDeaths() {
        return deaths;
    }

    public void setDeaths(long deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    // ─── Time played ─────────────────────────────────────────────────────────

    public long getTimePlayedSeconds() {
        return timePlayedSeconds;
    }

    public void setTimePlayedSeconds(long timePlayedSeconds) {
        this.timePlayedSeconds = Math.max(0, timePlayedSeconds);
    }

    public void addTimePlayedSeconds(long seconds) {
        if (seconds > 0) {
            this.timePlayedSeconds += seconds;
        }
    }

    /**
     * Returns a human-readable time played string (e.g. "0h", "1h30", "3d12h").
     */
    public String getTimePlayedFormatted() {
        long totalMinutes = timePlayedSeconds / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        long days = hours / 24;
        long remainingHours = hours % 24;

        if (days > 0) {
            if (remainingHours > 0) {
                return days + "d" + remainingHours + "h";
            }
            return days + "d";
        }
        if (hours > 0) {
            if (minutes > 0) {
                return hours + "h" + minutes + "m";
            }
            return hours + "h";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return "0h";
    }
}
