package com.sloyni.main.island;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks pending team invitations in memory.
 * Invitations no longer expire. Each player can have multiple pending invites
 * from different inviters. Only the 10 most recent are kept per target.
 */
public class TeamInviteManager {

    private static final int MAX_INVITES_PER_TARGET = 10;

    public record Invite(UUID inviterUuid, String inviterName, UUID targetUuid, long createdAt) {
    }

    /** All pending invites. */
    private final List<Invite> allInvites = new CopyOnWriteArrayList<>();

    /** Creates a new pending invite for the target player. */
    public void createInvite(UUID inviterUuid, String inviterName, UUID targetUuid) {
        // Prune old invites for this target if at limit
        List<Invite> existing = getPendingInvites(targetUuid);
        if (existing.size() >= MAX_INVITES_PER_TARGET) {
            // Remove the oldest invite for this target
            Invite oldest = existing.get(existing.size() - 1);
            allInvites.remove(oldest);
        }
        allInvites.add(new Invite(inviterUuid, inviterName, targetUuid, System.currentTimeMillis()));
    }

    /**
     * Returns a valid invite for the target that was sent by the named inviter,
     * or null if no such invite exists.
     */
    public Invite getValidInvite(UUID targetUuid, String inviterName) {
        for (Invite invite : allInvites) {
            if (invite.targetUuid().equals(targetUuid) && invite.inviterName().equalsIgnoreCase(inviterName)) {
                return invite;
            }
        }
        return null;
    }

    /**
     * Validates and consumes (removes) a pending invite.
     * Returns the invite if valid, null otherwise.
     */
    public Invite consumeInvite(UUID targetUuid, String inviterName) {
        Invite invite = getValidInvite(targetUuid, inviterName);
        if (invite == null) return null;
        allInvites.remove(invite);
        return invite;
    }

    /** Returns true if the target has any pending invite. */
    public boolean hasPendingInvite(UUID targetUuid) {
        return allInvites.stream().anyMatch(invite -> invite.targetUuid().equals(targetUuid));
    }

    /** Returns the most recent pending invite for the target, or null when absent. */
    public Invite getPendingInvite(UUID targetUuid) {
        List<Invite> invites = getPendingInvites(targetUuid);
        return invites.isEmpty() ? null : invites.get(0);
    }

    /**
     * Returns up to 10 most recent pending invites for the target,
     * ordered from most recent to oldest.
     */
    public List<Invite> getPendingInvites(UUID targetUuid) {
        List<Invite> result = new ArrayList<>();
        for (Invite invite : allInvites) {
            if (invite.targetUuid().equals(targetUuid)) {
                result.add(invite);
            }
        }
        result.sort(Comparator.comparingLong(Invite::createdAt).reversed());
        return result.size() > MAX_INVITES_PER_TARGET
            ? result.subList(0, MAX_INVITES_PER_TARGET)
            : result;
    }

    /** Cancels any pending invite from the given inviter to the target. */
    public void cancelInvite(UUID targetUuid) {
        allInvites.removeIf(invite -> invite.targetUuid().equals(targetUuid));
    }
}
