package com.sloyni.main.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.warps.IslandWarpData;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.TeamInviteManager;
import com.sloyni.main.ui.IslandInvitesPage;
import com.sloyni.main.ui.IslandMenuPage;
import com.sloyni.main.ui.IslandTemplateSelectPage;
import com.sloyni.main.util.Chat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IslandCommandHandler {
    private final ISkyblockPlugin plugin;

    public IslandCommandHandler(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void dispatchIslandTokens(Player player,
                                     Ref<EntityStore> ref,
                                     Store<EntityStore> store,
                                     PlayerRef playerRef,
                                     World world,
                                     String[] rawTokens) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        String[] tokens = normalizeIslandTokens(rawTokens);

        if (tokens.length >= 1 && "menu".equalsIgnoreCase(tokens[0])) {
            IslandMenuPage.open(player, ref, store, playerRef, world, plugin);
            return;
        }

        if (tokens.length >= 1 && "value".equalsIgnoreCase(tokens[0])) {
            handleIslandValueCommand(player, playerRef);
            return;
        }

        if (tokens.length >= 1 && "invites".equalsIgnoreCase(tokens[0])) {
            openInvitesMenu(player, ref, store, playerRef, world);
            return;
        }

        if (tokens.length >= 1) {
            handleIslandUnknownCommand(playerRef, tokens[0]);
            return;
        }

        handleIslandCommand(player, ref, store, playerRef, world);
    }

    private void openInvitesMenu(Player player,
                                  Ref<EntityStore> ref,
                                  Store<EntityStore> store,
                                  PlayerRef playerRef,
                                  World world) {
        IslandData existing = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (existing != null) {
            Chat.send(playerRef, plugin.tr(playerRef, "island.invites.has_island"));
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
            new IslandInvitesPage(playerRef, world, plugin));
    }

    public void handleIslandCommand(Player player,
                                    Ref<EntityStore> ref,
                                    Store<EntityStore> store,
                                    PlayerRef playerRef,
                                    World sourceWorld) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island != null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.teleporting"));
            plugin.getIslandService().teleportToIsland(playerRef, sourceWorld).exceptionally(ex -> {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.create_failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                return null;
            });
            return;
        }

        if (player != null) {
            int remaining = plugin.getIslandService().getRemainingResets(playerRef.getUuid());
            if (remaining <= 0) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.create.no_resets"));
                return;
            }
            if (plugin.getIslandTemplateManager().getEnabled().isEmpty()) {
                Chat.send(playerRef, plugin.tr("island.create_failed", Map.of("error", "no enabled island template available")));
                return;
            }
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.open_selector"));
            IslandTemplateSelectPage page = new IslandTemplateSelectPage(playerRef, sourceWorld, plugin);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    public void createIslandFromTemplate(PlayerRef playerRef, World sourceWorld, String templateId) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.creating"));
        plugin.getIslandService().createIsland(playerRef, sourceWorld, templateId).exceptionally(ex -> {
            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            if ("no_resets_left".equals(msg)) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.create.no_resets"));
            } else {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.create_failed", Map.of("error", msg == null ? "unknown" : msg)));
            }
            return null;
        });
    }

    public void handleIslandLeaveCommand(PlayerRef playerRef, World sourceWorld) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.no_island"));
            return;
        }

        var lobby = plugin.getConfigManager().getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || lobby.getWorldName() == null || lobby.getWorldName().isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "lobby.not_set"));
            return;
        }

        if (island.isLeader(playerRef.getUuid()) && island.getMemberCount() > 0) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.leader_has_members"));
            return;
        }

        boolean isMember = island.isMember(playerRef.getUuid());
        if (!isMember) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.deleting"));
        }
        plugin.getIslandService().leaveIslandAndTeleportToLobby(
            playerRef,
            sourceWorld,
            lobby.getWorldName(),
            lobby.getX(),
            lobby.getY(),
                lobby.getZ(),
                lobby.getPitch(),
                lobby.getYaw()
        ).thenRun(() -> {
            String key = isMember ? "island.leave.member_success" : "island.leave.success";
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, key));
            try {
                if (plugin.getIslandMenuHudManager() != null) {
                    plugin.getIslandMenuHudManager().refreshHudForPlayer(playerRef.getUuid());
                }
            } catch (Exception ignore) {
            }
        }).exceptionally(ex -> {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            return null;
        });
    }

    public void handleTeamShowCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }
        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.no_island"));
            return;
        }
        UUID leaderUuid = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
        String leaderName = leaderUuid != null ? island.getMemberNames().getOrDefault(leaderUuid.toString(), island.getOwnerName()) : island.getOwnerName();
        if (island.getLeaderUuid() == null || island.getLeaderUuid().equals(island.getOwnerUuid())) {
            leaderName = island.getOwnerName();
        } else {
            PlayerRef onlineLeader = Universe.get().getPlayer(leaderUuid);
            leaderName = onlineLeader != null ? onlineLeader.getUsername() : leaderUuid.toString();
        }
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.show.header"));
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.show.leader", Map.of("player", leaderName)));
        if (island.getMemberCount() == 0) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.show.no_members"));
        } else {
            for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.show.member", Map.of("player", entry.getValue())));
            }
        }
    }

    public void handleTeamUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.usage"));
    }

    public void handleTeamInviteCommand(PlayerRef playerRef, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }
        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.no_island"));
            return;
        }
        if (!plugin.getIslandService().hasIslandActionPermission(playerRef.getUuid(), com.sloyni.main.island.IslandMemberPermission.TEAM_INVITE)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.not_leader"));
            return;
        }
        if (targetName.equalsIgnoreCase(playerRef.getUsername())) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.self"));
            return;
        }

        PlayerRef targetRef = findOnlinePlayer(targetName);
        if (targetRef == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.not_online", Map.of("player", targetName)));
            return;
        }
        if (island.isMember(targetRef.getUuid())) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.already_member", Map.of("player", targetRef.getUsername())));
            return;
        }
        IslandData targetIsland = plugin.getIslandService().getPlayerIsland(targetRef.getUuid());
        if (targetIsland != null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.already_has_island", Map.of("player", targetRef.getUsername())));
            return;
        }

        plugin.getTeamInviteManager().createInvite(playerRef.getUuid(), playerRef.getUsername(), targetRef.getUuid());
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.invite.sent", Map.of("player", targetRef.getUsername())));
        Chat.send(targetRef, plugin.trNoPrefix(targetRef, "team.invite.received", Map.of("leader", playerRef.getUsername())));
    }

    public void handleTeamAcceptCommand(PlayerRef playerRef, String inviterName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        IslandData existing = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (existing != null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.accept.already_has_island"));
            return;
        }

        TeamInviteManager.Invite invite = plugin.getTeamInviteManager().consumeInvite(playerRef.getUuid(), inviterName);
        if (invite == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.accept.no_invite", Map.of("leader", inviterName)));
            return;
        }
        try {
            plugin.getIslandService().addTeamMember(invite.inviterUuid(), playerRef);
        } catch (IllegalStateException e) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.accept.island_gone"));
            return;
        }
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.accept.success", Map.of("leader", inviterName)));

        PlayerRef inviterRef = Universe.get().getPlayer(invite.inviterUuid());
        if (inviterRef != null) {
            Chat.send(inviterRef, plugin.trNoPrefix(inviterRef, "team.accept.notify_leader", Map.of("player", playerRef.getUsername())));
        }
    }

    public void handleTeamRemoveCommand(PlayerRef playerRef, String memberName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        PlayerRef kickedRef = findOnlinePlayer(memberName);

        // Check if target has moderator/admin protection
        for (PlayerRef online : Universe.get().getPlayers()) {
            if (online.getUsername() != null && online.getUsername().equalsIgnoreCase(memberName)) {
                if (hasIslandAdminPermission(online)) {
                    Chat.send(playerRef, plugin.tr(playerRef, "island.expel.cannot_expel_admin", Map.of("player", online.getUsername())));
                    return;
                }
                break;
            }
        }

        var lobby = plugin.getConfigManager().getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || lobby.getWorldName() == null || lobby.getWorldName().isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "lobby.not_set"));
            return;
        }
        Message kickedMsg = plugin.trNoPrefix(playerRef, "team.remove.kicked_message", Map.of("leader", playerRef.getUsername()));
        Message queuedMsg = plugin.trNoPrefix(playerRef, "team.remove.kicked_queued_message", Map.of("leader", playerRef.getUsername()));
        plugin.getIslandService().kickMember(
            playerRef,
            memberName,
            kickedMsg,
            queuedMsg,
            lobby.getWorldName(),
            lobby.getX(),
            lobby.getY(),
            lobby.getZ(),
            lobby.getPitch(),
            lobby.getYaw()
        ).thenRun(() -> {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.remove.success", Map.of("player", memberName)));
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.remove.success_notice", Map.of("player", memberName)));
            if (kickedRef != null && plugin.getIslandMenuHudManager() != null) {
                try {
                    plugin.getIslandMenuHudManager().refreshHudForPlayer(kickedRef.getUuid());
                } catch (Exception ignore) {
                }
            }
        }).exceptionally(ex -> {
            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            if ("no_island".equals(msg)) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.remove.no_island"));
            } else if ("not_leader".equals(msg)) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.remove.not_leader"));
            } else if ("member_not_found".equals(msg)) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.remove.member_not_found", Map.of("player", memberName)));
            } else {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", msg == null ? "unknown" : msg)));
            }
            return null;
        });
    }

    public void handleMakeLeaderCommand(PlayerRef playerRef, String memberName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }
        Message notifyMsg = plugin.trNoPrefix(playerRef, "makeleader.received");
        try {
            String newLeaderName = plugin.getIslandService().makeLeader(playerRef, memberName, notifyMsg);
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "makeleader.success", Map.of("player", newLeaderName)));
        } catch (IllegalStateException e) {
            switch (e.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "makeleader.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "makeleader.not_leader"));
                case "not_member" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "makeleader.not_member", Map.of("player", memberName)));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", e.getMessage())));
            }
        }
    }

    public void handleCoopEnableCommand(PlayerRef playerRef, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        if (targetName == null || targetName.isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.usage"));
            return;
        }

        PlayerRef targetRef = findOnlinePlayer(targetName);
        if (targetRef == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.not_online", Map.of("player", targetName)));
            return;
        }

        try {
            plugin.getIslandService().addCoopMember(playerRef, targetRef);
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.enable.success", Map.of("player", targetRef.getUsername())));
            Chat.send(targetRef, plugin.trNoPrefix(targetRef, "coop.enable.notify", Map.of("leader", playerRef.getUsername())));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.not_leader"));
                case "self" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.self"));
                case "already_member" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.already_member", Map.of("player", targetRef.getUsername())));
                case "already_coop" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.already_coop", Map.of("player", targetRef.getUsername())));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleCoopDisableCommand(PlayerRef playerRef, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        if (targetName == null || targetName.isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.usage"));
            return;
        }

        try {
            String removedName = plugin.getIslandService().removeCoopMember(playerRef, targetName);
            String displayName = removedName == null || removedName.isBlank() ? targetName : removedName;
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.disable.success", Map.of("player", displayName)));

            PlayerRef targetRef = findOnlinePlayer(displayName);
            if (targetRef != null) {
                Chat.send(targetRef, plugin.trNoPrefix(targetRef, "coop.disable.notify", Map.of("leader", playerRef.getUsername())));
            }
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.not_leader"));
                case "not_coop" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.not_found", Map.of("player", targetName)));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleCoopListCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        try {
            Map<String, String> coopMembers = plugin.getIslandService().listCoopMembers(playerRef);
            if (coopMembers.isEmpty()) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.list.empty"));
                return;
            }
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.list.header"));
            for (String name : coopMembers.values()) {
                if (name != null && !name.isBlank()) {
                    Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.list.member", Map.of("player", name)));
                }
            }
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.not_leader"));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleCoopUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "coop.usage"));
    }

    public void handleIslandHomeUsageCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isHomesEnabled()) {
            return;
        }
        plugin.getAddonManager().getHomesAddon().getIslandCommandHandler().handleIslandHomeUsageCommand(playerRef);
    }

    public void handleIslandHomeListCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isHomesEnabled()) {
            return;
        }
        plugin.getAddonManager().getHomesAddon().getIslandCommandHandler().handleIslandHomeListCommand(playerRef);
    }

    public void handleIslandHomeSetCommand(PlayerRef playerRef, World sourceWorld, String homeId, int x, int y, int z, float pitch, float yaw) {
        if (!plugin.getAddonManager().isHomesEnabled()) {
            return;
        }
        if (!isValidHomeId(homeId)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.invalid_id", Map.of("home", homeId == null ? "" : homeId)));
            return;
        }
        plugin.getAddonManager().getHomesAddon().getIslandCommandHandler().handleIslandHomeSetCommand(playerRef, sourceWorld, homeId, x, y, z, pitch, yaw);
    }

    public void handleIslandHomeGoCommand(PlayerRef playerRef, World sourceWorld, String homeId) {
        if (!plugin.getAddonManager().isHomesEnabled()) {
            return;
        }
        if (!isValidHomeId(homeId)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.invalid_id", Map.of("home", homeId == null ? "" : homeId)));
            return;
        }
        plugin.getAddonManager().getHomesAddon().getIslandCommandHandler().handleIslandHomeGoCommand(playerRef, sourceWorld, homeId);
    }

    public void handleIslandHomeRenameCommand(PlayerRef playerRef, String oldHomeId, String newHomeId) {
        if (!plugin.getAddonManager().isHomesEnabled()) {
            return;
        }
        if (!isValidHomeId(oldHomeId) || !isValidHomeId(newHomeId)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.invalid_id", Map.of("home", (newHomeId == null || newHomeId.isBlank()) ? String.valueOf(oldHomeId) : newHomeId)));
            return;
        }
        plugin.getAddonManager().getHomesAddon().getIslandCommandHandler().handleIslandHomeRenameCommand(playerRef, oldHomeId, newHomeId);
    }

    public void handleIslandHomeDeleteCommand(PlayerRef playerRef, String homeId) {
        if (!plugin.getAddonManager().isHomesEnabled()) {
            return;
        }
        if (!isValidHomeId(homeId)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "home.invalid_id", Map.of("home", homeId == null ? "" : homeId)));
            return;
        }
        plugin.getAddonManager().getHomesAddon().getIslandCommandHandler().handleIslandHomeDeleteCommand(playerRef, homeId);
    }

    public void handleIslandLanguageUsageCommand(PlayerRef playerRef) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.usage"));
    }

    public void handleIslandLanguageListCommand(PlayerRef playerRef) {
        var enabled = plugin.getConfigManager().getConfig().getSettings().getEnabledLanguages();
        String current = plugin.resolvePlayerLanguage(playerRef);
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.list.header", Map.of("current", current)));
        for (String locale : enabled) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.list.item", Map.of("locale", locale)));
        }
    }

    public void handleIslandLanguageSetCommand(PlayerRef playerRef, String locale) {
        if (locale == null || locale.isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.usage"));
            return;
        }

        String normalized = locale.trim().toLowerCase(Locale.ROOT);
        if (!plugin.getMessageManager().getAvailableLanguages().contains(normalized)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.invalid", Map.of("locale", normalized)));
            return;
        }
        if (!plugin.getConfigManager().getConfig().getSettings().getEnabledLanguages().contains(normalized)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.disabled", Map.of("locale", normalized)));
            return;
        }

        plugin.getPlayerPreferenceRepository().setLanguage(playerRef.getUuid(), normalized);
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "language.set.success", Map.of("locale", normalized)));
        try {
            if (plugin.getIslandMenuHudManager() != null) {
                plugin.getIslandMenuHudManager().refreshHudForPlayer(playerRef.getUuid());
            }
        } catch (Exception ignore) {
        }
    }

    public void handleIslandUnknownCommand(PlayerRef playerRef, String command) {
        String normalized = command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
        sendIslandHelpLine(playerRef, plugin.rawText(playerRef, "island.command.unknown").replace("{command}", normalized));
        handleIslandHelpCommand(playerRef);
    }

    public void handleIslandHelpCommand(PlayerRef playerRef) {
        sendIslandHelpLine(playerRef, plugin.rawText(playerRef, "island.help.header"));
        sendIslandHelpLine(playerRef, plugin.rawText(playerRef, "island.help.island"));
        sendIslandHelpLine(playerRef, plugin.rawText(playerRef, "island.help.menu"));
        sendIslandHelpLine(playerRef, plugin.rawText(playerRef, "island.help.invites"));
    }

    private static final Pattern HELP_ARG_PATTERN = Pattern.compile("<[^>]+>");

    private void sendIslandHelpLine(PlayerRef playerRef, String text) {
        String bodyColor = plugin.getMessagesConfig().getMessageColor();
        String attrColor = plugin.getMessagesConfig().getAttributeColor();
        Matcher m = HELP_ARG_PATTERN.matcher(text);
        List<Message> parts = new ArrayList<>();
        int pos = 0;
        while (m.find()) {
            if (m.start() > pos) {
                parts.add(Message.raw(text.substring(pos, m.start())).color(bodyColor));
            }
            parts.add(Message.raw(m.group()).color(attrColor));
            pos = m.end();
        }
        if (pos < text.length()) {
            parts.add(Message.raw(text.substring(pos)).color(bodyColor));
        }
        if (parts.isEmpty()) {
            Chat.send(playerRef, Message.raw(text).color(bodyColor));
            return;
        }
        Chat.send(playerRef, Message.join(parts.toArray(new Message[0])));
    }

    public void handleIslandLevelCommand(PlayerRef playerRef, String targetName) {
        plugin.getAddonManager().getLevelsAddon().getIslandCommandHandler().handleIslandLevelCommand(playerRef, targetName);
    }

    public void handleIslandNearCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }
        try {
            Map<String, IslandData> neighbors = plugin.getIslandService().getNeighboringIslands(playerRef.getUuid());
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.near.header"));
            for (Map.Entry<String, IslandData> entry : neighbors.entrySet()) {
                String islandName = entry.getValue() == null
                    ? plugin.rawText(playerRef, "island.near.empty")
                    : plugin.getIslandService().getIslandDisplayName(entry.getValue());
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.near.item", Map.of(
                    "direction", entry.getKey(),
                    "island", islandName
                )));
            }
        } catch (IllegalStateException ex) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.no_island"));
        }
    }

    public void handleIslandSetNameCommand(PlayerRef playerRef, String islandName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }
        String normalized = islandName == null ? "" : islandName.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isBlank() || normalized.length() > 16) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setname.invalid", Map.of("max", "16")));
            return;
        }
        try {
            plugin.getIslandService().setIslandName(playerRef, normalized);
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setname.success", Map.of("name", normalized)));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setname.not_leader"));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandInfoCommand(PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "team.no_island"));
            return;
        }

        int currentSize = island.getZoneSize() > 0
            ? island.getZoneSize()
            : plugin.getIslandService().getDefaultIslandZoneSize();

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.info.size", Map.of(
            "size", String.valueOf(currentSize),
            "max", String.valueOf(plugin.getIslandService().getMaxAllowedIslandZoneSize())
        )));
    }

    public void handleIslandValueCommand(Player player, PlayerRef playerRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        if (player == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.no_player_entity"));
            return;
        }

        Object held = null;
        try {
            Class<?> itemStackClass = null;
            try {
                itemStackClass = Class.forName("com.hypixel.hytale.server.core.inventory.ItemStack");
            } catch (ClassNotFoundException ignored) {
            }

            String[] directItemMethods = new String[]{
                "getHeldItem", "getHeldItemStack", "getMainHandItem", "getMainHand",
                "getItemInHand", "getEquippedItem", "getSelectedItem", "getActiveItem"
            };
            for (String name : directItemMethods) {
                try {
                    java.lang.reflect.Method m = player.getClass().getMethod(name);
                    if (m.getParameterCount() != 0) continue;
                    Object res = m.invoke(player);
                    if (res != null) {
                        held = res;
                        break;
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (Exception ignored) {
                }
            }

            if (held == null) {
                // Try to resolve via inventory-like objects
                String[] inventoryMethods = new String[]{"getInventory", "getPlayerInventory", "getEquipment", "getContainer"};
                for (String invName : inventoryMethods) {
                    try {
                        java.lang.reflect.Method invMethod = player.getClass().getMethod(invName);
                        Object inv = invMethod.invoke(player);
                        if (inv == null) continue;

                        String[] invItemMethods = new String[]{
                            "getHeldItem", "getMainHand", "getMainHandItem", "getSelectedItem",
                            "getItemInHand", "getActiveItem", "getSelectedHotbarItem"
                        };
                        for (String mName : invItemMethods) {
                            try {
                                java.lang.reflect.Method m = inv.getClass().getMethod(mName);
                                Object res = m.invoke(inv);
                                if (res != null) {
                                    held = res;
                                    break;
                                }
                            } catch (NoSuchMethodException ignored) {
                            } catch (Exception ignored) {
                            }
                        }
                        if (held != null) break;
                    } catch (NoSuchMethodException ignored) {
                    } catch (Exception ignored) {
                    }
                }
            }

            if (held == null) {
                // Fallback: scan methods returning ItemStack-like values
                for (java.lang.reflect.Method m : player.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    Class<?> rt = m.getReturnType();
                    if (rt == null) continue;
                    boolean looksLikeItem = rt.getSimpleName().equals("ItemStack") || rt.getName().endsWith(".ItemStack");
                    if (!looksLikeItem && itemStackClass != null) {
                        looksLikeItem = itemStackClass.isAssignableFrom(rt);
                    }
                    if (!looksLikeItem) continue;
                    try {
                        Object res = m.invoke(player);
                        if (res != null) {
                            held = res;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // Finally, try PlayerRef-side getters (some implementations expose via playerRef)
            if (held == null) {
                try {
                    java.lang.reflect.Method getRef = player.getClass().getMethod("getPlayerRef");
                    Object pref = getRef.invoke(player);
                    if (pref != null) {
                        for (java.lang.reflect.Method m : pref.getClass().getMethods()) {
                            if (m.getParameterCount() != 0) continue;
                            Class<?> rt = m.getReturnType();
                            if (rt == null) continue;
                            boolean looksLike = rt.getSimpleName().equals("ItemStack") || rt.getName().endsWith(".ItemStack");
                            if (!looksLike && itemStackClass != null) looksLike = itemStackClass.isAssignableFrom(rt);
                            if (!looksLike) continue;
                            try {
                                Object res = m.invoke(pref);
                                if (res != null) {
                                    held = res;
                                    break;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        if (held == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.no_item"));
            return;
        }

        // Try to extract a string id from the ItemStack via common methods/fields
        String rawId = null;
        try {
            String[] tryMethods = new String[]{"getId", "getName", "getItemId", "getType", "getTypeName", "getNameId", "getKey", "getItemKey"};
            for (String mName : tryMethods) {
                try {
                    java.lang.reflect.Method m = held.getClass().getMethod(mName);
                    Object res = m.invoke(held);
                    if (res != null) {
                        rawId = String.valueOf(res);
                        break;
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (Exception ignored) {
                }
            }
            if (rawId == null) {
                // try common fields
                String[] fields = new String[]{"id", "name", "type", "itemId", "key"};
                for (String fName : fields) {
                    try {
                        java.lang.reflect.Field f = held.getClass().getField(fName);
                        Object res = f.get(held);
                        if (res != null) {
                            rawId = String.valueOf(res);
                            break;
                        }
                    } catch (NoSuchFieldException ignored) {
                    } catch (Exception ignored) {
                    }
                }
            }
            if (rawId == null) {
                rawId = held.toString();
            }
        } catch (Exception ex) {
            rawId = String.valueOf(held.toString());
        }

        if (rawId == null || rawId.isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.unknown_item"));
            return;
        }

        // Resolve canonical block id via IslandValueService
        try {
            var levels = plugin.getAddonManager().getLevelsAddon();
            var svc = levels.getIslandValueService();
            String canonical = svc.resolveCanonicalBlockId(rawId);
            if (canonical == null) {
                // try with rawId as-is
                canonical = svc.resolveCanonicalBlockId(rawId.trim());
            }
            if (canonical == null) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.unknown_block", Map.of("value", rawId)));
                return;
            }

            Map<String, Double> configured = svc.getConfiguredBlockValuesSnapshot();
            Double configuredValue = configured.get(canonical);
            double value;
            boolean configuredFlag = false;
            if (configuredValue != null) {
                value = configuredValue;
                configuredFlag = true;
            } else {
                value = levels.getBlockDefaultValue();
            }

            if (configuredFlag) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.item_configured", Map.of("item", canonical, "value", String.valueOf(value))));
            } else {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.item_default", Map.of("item", canonical, "value", String.valueOf(value))));
            }
        } catch (Exception ex) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.value.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
        }
    }

    public void handleIslandVisitUsageCommand(PlayerRef playerRef) {
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandVisitUsageCommand(playerRef);
    }

    public void handleIslandVisitCommand(PlayerRef playerRef, World sourceWorld, String targetName) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandVisitCommand(playerRef, sourceWorld, targetName);
    }

    public void handleIslandWarpsCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandWarpsCommand(playerRef);
    }

    public void handleIslandWarpUsageCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandWarpUsageCommand(playerRef);
    }

    public void handleIslandWarpSetCommand(PlayerRef playerRef, World sourceWorld, int x, int y, int z, float pitch, float yaw) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandWarpSetCommand(playerRef, sourceWorld, x, y, z, pitch, yaw);
    }

    public void handleIslandWarpStateCommand(PlayerRef playerRef, boolean enabled) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandWarpStateCommand(playerRef, enabled);
    }

    public void handleIslandWarpDeleteCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandWarpDeleteCommand(playerRef);
    }

    public void handleIslandWarpRenameCommand(PlayerRef playerRef, String warpName) {
        if (!plugin.getAddonManager().isWarpsEnabled()) {
            return;
        }
        plugin.getAddonManager().getWarpsAddon().getIslandCommandHandler().handleIslandWarpRenameCommand(playerRef, warpName);
    }

    public void handleIslandWallsUsageCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsUsageCommand(playerRef);
    }

    public void handleIslandWallsListCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsListCommand(playerRef);
    }

    public void handleIslandWallsCommand(PlayerRef playerRef, String wallType, String sizeInput) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsCommand(playerRef, wallType, sizeInput);
    }

    public void handleIslandWallsColorUsageCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsColorUsageCommand(playerRef);
    }

    public void handleIslandWallsColorAlternateUsageCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsColorAlternateUsageCommand(playerRef);
    }

    public void handleIslandWallsColorListCommand(PlayerRef playerRef) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsColorListCommand(playerRef);
    }

    public void handleIslandWallsColorCommand(PlayerRef playerRef, String colorName) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsColorCommand(playerRef, colorName);
    }

    public void handleIslandWallsColorAlternateCommand(PlayerRef playerRef, String[] colors) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return;
        }
        plugin.getAddonManager().getIslandWallsAddon().getIslandCommandHandler().handleIslandWallsColorAlternateCommand(playerRef, colors);
    }

    public void handleIslandTopCommand(PlayerRef playerRef) {
        plugin.getAddonManager().getLevelsAddon().getIslandCommandHandler().handleIslandTopCommand(playerRef);
    }

    public void sendSimpleMessage(PlayerRef playerRef, String key) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, key));
    }

    public void sendMessage(PlayerRef playerRef, String key, Map<String, String> vars) {
        Chat.send(playerRef, plugin.trNoPrefix(playerRef, key, vars));
    }

    public void handleIslandExpelCommand(PlayerRef leaderRef, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.not_initialized"));
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.usage"));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(leaderRef.getUuid());
        if (island == null) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.no_island"));
            return;
        }
        if (!plugin.getIslandService().hasIslandActionPermission(leaderRef.getUuid(), com.sloyni.main.island.IslandMemberPermission.TEAM_EXPEL)) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.not_leader"));
            return;
        }

        PlayerRef targetRef = findOnlinePlayer(targetName);
        if (targetRef == null || targetRef.getUuid() == null) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.not_online", Map.of("player", targetName)));
            return;
        }
        if (targetRef.getUuid().equals(leaderRef.getUuid())) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.self"));
            return;
        }
        if (island.isOnTeam(targetRef.getUuid())) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.cannot_expel_member", Map.of("player", targetRef.getUsername())));
            return;
        }
        if (hasIslandAdminPermission(targetRef)) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.cannot_expel_admin", Map.of("player", targetRef.getUsername())));
            return;
        }

        if (island.isCoopMember(targetRef.getUuid())) {
            try {
                plugin.getIslandService().removeCoopMember(leaderRef, targetRef.getUuid());
            } catch (IllegalStateException ignored) {
            }
        }

        var transform = targetRef.getTransform();
        World islandWorld = Universe.get().getWorld(plugin.getConfigManager().getConfig().getInit().getWorldName());
        if (transform == null || islandWorld == null) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.not_present", Map.of("player", targetRef.getUsername())));
            return;
        }

        int x = (int) Math.floor(transform.getPosition().x);
        int z = (int) Math.floor(transform.getPosition().z);
        IslandData atLocation = plugin.getProtectionService().getIslandAtBlock(x, z);
        if (atLocation == null || atLocation.getOwnerUuid() == null || !atLocation.getOwnerUuid().equals(island.getOwnerUuid())) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.not_present", Map.of("player", targetRef.getUsername())));
            return;
        }

        plugin.getIslandAccessController().routePlayerToIslandOrLobby(targetRef, islandWorld)
            .thenRun(() -> {
                Chat.send(leaderRef, plugin.tr(leaderRef, "island.expel.success", Map.of("player", targetRef.getUsername())));
                Chat.send(targetRef, plugin.trNoPrefix(targetRef, "island.expel.kicked", Map.of("leader", leaderRef.getUsername())));
            })
            .exceptionally(ex -> {
                Chat.send(leaderRef, plugin.tr(leaderRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                return null;
            });
    }

    public void handleIslandBanUsageCommand(PlayerRef leaderRef) {
        Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.usage"));
    }

    public void handleIslandBanAddCommand(PlayerRef leaderRef, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.not_initialized"));
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.usage"));
            return;
        }

        PlayerRef targetRef = findOnlinePlayer(targetName);
        if (targetRef == null || targetRef.getUuid() == null) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.not_online", Map.of("player", targetName)));
            return;
        }
        if (hasIslandAdminPermission(targetRef)) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.cannot_ban_admin", Map.of("player", targetRef.getUsername())));
            return;
        }

        try {
            plugin.getIslandService().banPlayer(leaderRef, targetRef);
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.success", Map.of("player", targetRef.getUsername())));
            Chat.send(targetRef, plugin.trNoPrefix(targetRef, "island.ban.kicked", Map.of("leader", leaderRef.getUsername())));
            World islandWorld = Universe.get().getWorld(plugin.getConfigManager().getConfig().getInit().getWorldName());
            if (islandWorld != null) {
                plugin.getIslandAccessController().enforceBannedIslandAccess(targetRef, islandWorld);
            }
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.no_island"));
                case "not_leader" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.not_leader"));
                case "self" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.self"));
                case "cannot_ban_member" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.cannot_ban_member", Map.of("player", targetRef.getUsername())));
                case "already_banned" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.already_banned", Map.of("player", targetRef.getUsername())));
                default -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandBanListCommand(PlayerRef leaderRef) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.not_initialized"));
            return;
        }

        try {
            Map<String, String> banned = plugin.getIslandService().listBannedPlayers(leaderRef);
            if (banned.isEmpty()) {
                Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.list.empty"));
                return;
            }
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.list.header"));
            for (String name : banned.values()) {
                if (name != null && !name.isBlank()) {
                    Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.list.item", Map.of("player", name)));
                }
            }
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.no_island"));
                case "not_leader" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.not_leader"));
                default -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandBanRemoveCommand(PlayerRef leaderRef, String targetName) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.not_initialized"));
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.usage"));
            return;
        }

        try {
            String removedName = plugin.getIslandService().unbanPlayer(leaderRef, targetName);
            Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.remove.success", Map.of("player", removedName)));
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.no_island"));
                case "not_leader" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.not_leader"));
                case "not_banned" -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.ban.remove.not_found", Map.of("player", targetName)));
                default -> Chat.send(leaderRef, plugin.tr(leaderRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandLockCommand(PlayerRef playerRef, boolean locked) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        try {
            IslandData island = plugin.getIslandService().setIslandLocked(playerRef.getUuid(), locked);
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, locked ? "island.lock.success" : "island.unlock.success"));
            if (locked) {
                plugin.getIslandAccessController().ejectLockedIslandVisitors(island);
            }
        } catch (IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "no_island" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.lock.no_island"));
                case "not_leader" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.lock.not_leader"));
                case "already_locked" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.lock.already_locked"));
                case "already_unlocked" -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.lock.already_unlocked"));
                default -> Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }
    }

    public void handleIslandSetSpawnCommand(PlayerRef playerRef,
                                            World sourceWorld,
                                            int preferredX,
                                            int preferredY,
                                            int preferredZ,
                                            float preferredPitch,
                                            float preferredYaw) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.not_initialized"));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setspawn.no_island"));
            return;
        }

        if (!plugin.getIslandService().hasIslandActionPermission(playerRef.getUuid(), com.sloyni.main.island.IslandMemberPermission.SET_SPAWN)) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setspawn.not_leader"));
            return;
        }

        Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setspawn.updating"));
        plugin.getIslandService().setIslandSpawnAndTeleportByActor(playerRef, sourceWorld, preferredX, preferredY, preferredZ, preferredPitch, preferredYaw)
            .thenAccept(result -> {
                String key = result.fallbackUsed() ? "island.setspawn.success_fallback" : "island.setspawn.success";
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, key, Map.of(
                    "x", String.valueOf(result.x()),
                    "y", String.valueOf(result.y()),
                    "z", String.valueOf(result.z())
                )));
            })
            .exceptionally(ex -> {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.setspawn.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                return null;
            });
    }

    private PlayerRef findOnlinePlayer(String targetName) {
        for (PlayerRef online : Universe.get().getPlayers()) {
            if (online.getUsername().equalsIgnoreCase(targetName)) {
                return online;
            }
        }
        return null;
    }

    private boolean hasIslandAdminPermission(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return false;
        }
        // Try direct holder component access
        try {
            if (playerRef.getHolder() != null) {
                Player player = playerRef.getHolder().getComponent(Player.getComponentType());
                if (player != null) {
                    return com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin");
                }
            }
        } catch (Exception ignored) {
        }
        // Fallback: iterate online players to find the Player
        try {
            for (PlayerRef online : Universe.get().getPlayers()) {
                if (online.getUuid() != null && online.getUuid().equals(playerRef.getUuid())) {
                    if (online.getHolder() != null) {
                        Player player = online.getHolder().getComponent(Player.getComponentType());
                        if (player != null) {
                            return com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin");
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isValidHomeId(String homeId) {
        return homeId != null && !homeId.isBlank() && homeId.length() <= 32 && homeId.matches("^[A-Za-z0-9_-]+$");
    }

    private String[] normalizeIslandTokens(String[] rawTokens) {
        if (rawTokens == null || rawTokens.length == 0) {
            return new String[0];
        }
        int startIndex = 0;
        if ("island".equalsIgnoreCase(rawTokens[0]) || "is".equalsIgnoreCase(rawTokens[0])) {
            startIndex = 1;
        }
        if (startIndex >= rawTokens.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(rawTokens, startIndex, rawTokens.length);
    }
}