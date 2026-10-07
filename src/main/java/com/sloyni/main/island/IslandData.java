package com.sloyni.main.island;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class IslandData {
    private UUID ownerUuid;
    private String ownerName;
    private String islandName;
    private String worldName;
    /** Current island leader (may differ from ownerUuid after /island makeleader). */
    private UUID leaderUuid;
    /** UUIDs (as strings) of non-leader team members. */
    private List<String> memberUuidStrings;
    /** UUID-string → display name for each member. */
    private Map<String, String> memberNames;
    /** Default permissions granted to all non-leader members that follow defaults. */
    private List<String> defaultMemberPermissionKeys;
    /** UUID-string → per-member permission profile (override or follow defaults). */
    private Map<String, MemberPermissionProfile> memberPermissionProfiles;
    /** UUIDs (as strings) of coop members with interaction/build access. */
    private List<String> coopUuidStrings;
    /** UUID-string → display name for each coop member. */
    private Map<String, String> coopNames;
    /** UUIDs (as strings) banned from entering this island. */
    private List<String> bannedUuidStrings;
    /** UUID-string -> display name of banned players. */
    private Map<String, String> bannedNames;
    /** UUIDs that must be forced to lobby after being removed while offline/unavailable. */
    private List<String> pendingLobbyTeleportUuidStrings;
    private int centerX;
    private int centerY;
    private int centerZ;
    private int gridX;
    private int gridZ;
    private int zoneSize;
    private String templateId;
    private boolean generated;
    private int preferredSpawnX;
    private int preferredSpawnY;
    private int preferredSpawnZ;
    private float preferredSpawnPitch = 0f;
    private float preferredSpawnYaw = 0f;
    private boolean preferredSpawnConfigured;
    private double templateBaseValue;
    private boolean templateBaseValueConfigured;
    private boolean locked;
    private boolean visitorItemDropEnabled;
    private boolean visitorMobDamageEnabled;

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

    public String getIslandName() {
        return islandName;
    }

    public void setIslandName(String islandName) {
        this.islandName = islandName;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public int getCenterX() {
        return centerX;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public void setCenterY(int centerY) {
        this.centerY = centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(int centerZ) {
        this.centerZ = centerZ;
    }

    public int getGridX() {
        return gridX;
    }

    public void setGridX(int gridX) {
        this.gridX = gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public void setGridZ(int gridZ) {
        this.gridZ = gridZ;
    }

    public int getZoneSize() {
        return zoneSize;
    }

    public void setZoneSize(int zoneSize) {
        this.zoneSize = zoneSize;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public int getPreferredSpawnX() {
        return preferredSpawnX;
    }

    public void setPreferredSpawnX(int preferredSpawnX) {
        this.preferredSpawnX = preferredSpawnX;
    }

    public int getPreferredSpawnY() {
        return preferredSpawnY;
    }

    public void setPreferredSpawnY(int preferredSpawnY) {
        this.preferredSpawnY = preferredSpawnY;
    }

    public int getPreferredSpawnZ() {
        return preferredSpawnZ;
    }

    public void setPreferredSpawnZ(int preferredSpawnZ) {
        this.preferredSpawnZ = preferredSpawnZ;
    }

    public float getPreferredSpawnPitch() {
        return preferredSpawnPitch;
    }

    public void setPreferredSpawnPitch(float preferredSpawnPitch) {
        this.preferredSpawnPitch = preferredSpawnPitch;
    }

    public float getPreferredSpawnYaw() {
        return preferredSpawnYaw;
    }

    public void setPreferredSpawnYaw(float preferredSpawnYaw) {
        this.preferredSpawnYaw = preferredSpawnYaw;
    }

    public boolean isPreferredSpawnConfigured() {
        return preferredSpawnConfigured;
    }

    public void setPreferredSpawnConfigured(boolean preferredSpawnConfigured) {
        this.preferredSpawnConfigured = preferredSpawnConfigured;
    }

    public double getTemplateBaseValue() {
        return templateBaseValue;
    }

    public void setTemplateBaseValue(double templateBaseValue) {
        this.templateBaseValue = templateBaseValue;
    }

    public boolean isTemplateBaseValueConfigured() {
        return templateBaseValueConfigured;
    }

    public void setTemplateBaseValueConfigured(boolean templateBaseValueConfigured) {
        this.templateBaseValueConfigured = templateBaseValueConfigured;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isVisitorItemDropEnabled() {
        return visitorItemDropEnabled;
    }

    public void setVisitorItemDropEnabled(boolean visitorItemDropEnabled) {
        this.visitorItemDropEnabled = visitorItemDropEnabled;
    }

    public boolean isVisitorMobDamageEnabled() {
        return visitorMobDamageEnabled;
    }

    public void setVisitorMobDamageEnabled(boolean visitorMobDamageEnabled) {
        this.visitorMobDamageEnabled = visitorMobDamageEnabled;
    }

    // -------------------------------------------------------------------------
    // Team system
    // -------------------------------------------------------------------------

    public static class MemberPermissionProfile {
        private Boolean followDefault;
        private List<String> permissionKeys;

        public boolean isFollowDefault() {
            return followDefault == null || followDefault;
        }

        public void setFollowDefault(Boolean followDefault) {
            this.followDefault = followDefault;
        }

        public List<String> getPermissionKeys() {
            return permissionKeys == null ? new ArrayList<>() : permissionKeys;
        }

        public void setPermissionKeys(List<String> permissionKeys) {
            this.permissionKeys = permissionKeys;
        }
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    /**
     * Returns true if the given UUID is the current island leader.
     * Falls back to ownerUuid for islands that pre-date the team system (leaderUuid == null).
     */
    public boolean isLeader(UUID uuid) {
        if (uuid == null) return false;
        if (leaderUuid != null) return leaderUuid.equals(uuid);
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    /** Returns true if the given UUID is a non-leader team member. */
    public boolean isMember(UUID uuid) {
        if (uuid == null || memberUuidStrings == null) return false;
        return memberUuidStrings.contains(uuid.toString());
    }

    /** Returns true if the given UUID is leader or a team member. */
    public boolean isOnTeam(UUID uuid) {
        return isLeader(uuid) || isMember(uuid);
    }

    public boolean isCoopMember(UUID uuid) {
        if (uuid == null || coopUuidStrings == null) return false;
        return coopUuidStrings.contains(uuid.toString());
    }

    public boolean canModify(UUID uuid) {
        return isOnTeam(uuid) || isCoopMember(uuid);
    }

    public boolean isBanned(UUID uuid) {
        if (uuid == null || bannedUuidStrings == null) return false;
        return bannedUuidStrings.contains(uuid.toString());
    }

    public int getMemberCount() {
        return memberUuidStrings == null ? 0 : memberUuidStrings.size();
    }

    public List<String> getMemberUuidStrings() {
        return memberUuidStrings == null ? new ArrayList<>() : memberUuidStrings;
    }

    /** UUID-string → display name map for all non-leader members. */
    public Map<String, String> getMemberNames() {
        return memberNames == null ? new LinkedHashMap<>() : memberNames;
    }

    /** Default permissions applied to members whose profile follows defaults. */
    public List<String> getDefaultMemberPermissionKeys() {
        if (defaultMemberPermissionKeys == null) {
            defaultMemberPermissionKeys = new ArrayList<>();
        } else {
            defaultMemberPermissionKeys = sanitizePermissionKeys(defaultMemberPermissionKeys);
        }
        return defaultMemberPermissionKeys;
    }

    public void setDefaultMemberPermissionKeys(List<String> permissionKeys) {
        defaultMemberPermissionKeys = sanitizePermissionKeys(permissionKeys);
    }

    public Map<String, MemberPermissionProfile> getMemberPermissionProfiles() {
        if (memberPermissionProfiles == null) {
            memberPermissionProfiles = new LinkedHashMap<>();
        }
        return memberPermissionProfiles;
    }

    public boolean isMemberFollowingDefaultPermissions(UUID memberUuid) {
        if (memberUuid == null || isLeader(memberUuid) || !isMember(memberUuid)) {
            return true;
        }
        MemberPermissionProfile profile = getMemberPermissionProfiles().get(memberUuid.toString());
        return profile == null || profile.isFollowDefault();
    }

    public void setMemberFollowDefaultPermissions(UUID memberUuid, boolean followDefault) {
        MemberPermissionProfile profile = getOrCreateMemberPermissionProfile(memberUuid);
        if (profile == null) {
            return;
        }
        profile.setFollowDefault(followDefault);
    }

    public List<String> getMemberExplicitPermissionKeys(UUID memberUuid) {
        if (memberUuid == null || isLeader(memberUuid) || !isMember(memberUuid)) {
            return new ArrayList<>();
        }
        MemberPermissionProfile profile = getOrCreateMemberPermissionProfile(memberUuid);
        if (profile == null) {
            return new ArrayList<>();
        }
        List<String> sanitized = sanitizePermissionKeys(profile.getPermissionKeys());
        profile.setPermissionKeys(sanitized);
        return sanitized;
    }

    public void setMemberExplicitPermissionKeys(UUID memberUuid, List<String> permissionKeys) {
        MemberPermissionProfile profile = getOrCreateMemberPermissionProfile(memberUuid);
        if (profile == null) {
            return;
        }
        profile.setPermissionKeys(sanitizePermissionKeys(permissionKeys));
        profile.setFollowDefault(false);
    }

    /**
     * Resolves whether a player may use a team-managed permission.
     * Leaders always return true.
     */
    public boolean hasMemberPermission(UUID uuid, IslandMemberPermission permission) {
        if (uuid == null || permission == null) {
            return false;
        }
        if (isLeader(uuid)) {
            return true;
        }
        if (permission.isLeaderOnly()) {
            return false;
        }
        if (!isMember(uuid)) {
            return false;
        }

        if (isMemberFollowingDefaultPermissions(uuid)) {
            return getDefaultMemberPermissionKeys().contains(permission.getKey());
        }

        return getMemberExplicitPermissionKeys(uuid).contains(permission.getKey());
    }

    public List<String> getCoopUuidStrings() {
        return coopUuidStrings == null ? new ArrayList<>() : coopUuidStrings;
    }

    public Map<String, String> getCoopNames() {
        return coopNames == null ? new LinkedHashMap<>() : coopNames;
    }

    public int getCoopCount() {
        return coopUuidStrings == null ? 0 : coopUuidStrings.size();
    }

    public List<String> getBannedUuidStrings() {
        return bannedUuidStrings == null ? new ArrayList<>() : bannedUuidStrings;
    }

    public Map<String, String> getBannedNames() {
        return bannedNames == null ? new LinkedHashMap<>() : bannedNames;
    }

    public void addMember(UUID uuid, String name) {
        if (memberUuidStrings == null) memberUuidStrings = new ArrayList<>();
        if (memberNames == null) memberNames = new LinkedHashMap<>();
        String key = uuid.toString();
        if (!memberUuidStrings.contains(key)) {
            memberUuidStrings.add(key);
        }
        memberNames.put(key, name);
            if (coopUuidStrings != null) {
                coopUuidStrings.remove(key);
            }
            if (coopNames != null) {
                coopNames.remove(key);
            }
            if (bannedUuidStrings != null) {
                bannedUuidStrings.remove(key);
            }
            if (bannedNames != null) {
                bannedNames.remove(key);
            }
        if (pendingLobbyTeleportUuidStrings != null) {
            pendingLobbyTeleportUuidStrings.remove(key);
        }

        if (memberPermissionProfiles == null) {
            memberPermissionProfiles = new LinkedHashMap<>();
        }
        memberPermissionProfiles.computeIfAbsent(key, ignored -> {
            MemberPermissionProfile profile = new MemberPermissionProfile();
            profile.setFollowDefault(true);
            profile.setPermissionKeys(new ArrayList<>());
            return profile;
        });
    }

    public void removeMember(UUID uuid) {
        if (memberUuidStrings != null) memberUuidStrings.remove(uuid.toString());
        if (memberNames != null) memberNames.remove(uuid.toString());
        clearMemberPermissionProfile(uuid);
    }

    public boolean addCoopMember(UUID uuid, String name) {
        if (uuid == null) return false;
        if (coopUuidStrings == null) coopUuidStrings = new ArrayList<>();
        if (coopNames == null) coopNames = new LinkedHashMap<>();
        String key = uuid.toString();
        if (!coopUuidStrings.contains(key)) {
            coopUuidStrings.add(key);
        }
        coopNames.put(key, name);
        return true;
    }

    public boolean removeCoopMember(UUID uuid) {
        if (uuid == null) return false;
        boolean changed = false;
        String key = uuid.toString();
        if (coopUuidStrings != null) {
            changed = coopUuidStrings.remove(key) || changed;
        }
        if (coopNames != null) {
            changed = (coopNames.remove(key) != null) || changed;
        }
        return changed;
    }

    public boolean addBannedPlayer(UUID uuid, String name) {
        if (uuid == null) return false;
        if (bannedUuidStrings == null) bannedUuidStrings = new ArrayList<>();
        if (bannedNames == null) bannedNames = new LinkedHashMap<>();
        String key = uuid.toString();
        boolean added = false;
        if (!bannedUuidStrings.contains(key)) {
            bannedUuidStrings.add(key);
            added = true;
        }
        if (name != null && !name.isBlank()) {
            bannedNames.put(key, name);
        }
        return added;
    }

    public boolean removeBannedPlayer(UUID uuid) {
        if (uuid == null) return false;
        boolean changed = false;
        String key = uuid.toString();
        if (bannedUuidStrings != null) {
            changed = bannedUuidStrings.remove(key) || changed;
        }
        if (bannedNames != null) {
            changed = (bannedNames.remove(key) != null) || changed;
        }
        return changed;
    }

    public boolean hasPendingLobbyTeleport(UUID uuid) {
        if (uuid == null || pendingLobbyTeleportUuidStrings == null) return false;
        return pendingLobbyTeleportUuidStrings.contains(uuid.toString());
    }

    public void markPendingLobbyTeleport(UUID uuid) {
        if (uuid == null) return;
        if (pendingLobbyTeleportUuidStrings == null) pendingLobbyTeleportUuidStrings = new ArrayList<>();
        String key = uuid.toString();
        if (!pendingLobbyTeleportUuidStrings.contains(key)) {
            pendingLobbyTeleportUuidStrings.add(key);
        }
    }

    public boolean clearPendingLobbyTeleport(UUID uuid) {
        if (uuid == null || pendingLobbyTeleportUuidStrings == null) return false;
        return pendingLobbyTeleportUuidStrings.remove(uuid.toString());
    }

    private MemberPermissionProfile getOrCreateMemberPermissionProfile(UUID memberUuid) {
        if (memberUuid == null || isLeader(memberUuid) || !isMember(memberUuid)) {
            return null;
        }

        String key = memberUuid.toString();
        Map<String, MemberPermissionProfile> profiles = getMemberPermissionProfiles();
        MemberPermissionProfile profile = profiles.get(key);
        if (profile == null) {
            profile = new MemberPermissionProfile();
            profile.setFollowDefault(true);
            profile.setPermissionKeys(new ArrayList<>());
            profiles.put(key, profile);
        }
        return profile;
    }

    private void clearMemberPermissionProfile(UUID memberUuid) {
        if (memberUuid == null || memberPermissionProfiles == null) {
            return;
        }
        memberPermissionProfiles.remove(memberUuid.toString());
    }

    private List<String> sanitizePermissionKeys(List<String> rawKeys) {
        List<String> sanitized = new ArrayList<>();
        if (rawKeys == null || rawKeys.isEmpty()) {
            return sanitized;
        }

        Set<String> seen = new LinkedHashSet<>();
        for (String rawKey : rawKeys) {
            IslandMemberPermission permission = IslandMemberPermission.fromKey(rawKey);
            if (permission == null) {
                continue;
            }
            if (permission.isLeaderOnly()) {
                continue;
            }
            String normalizedKey = permission.getKey();
            if (seen.add(normalizedKey)) {
                sanitized.add(normalizedKey);
            }
        }
        return sanitized;
    }

}
