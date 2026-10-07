package com.sloyni.main.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.Chat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class IslandTeamPlayerPickerPage extends InteractiveCustomUIPage<IslandTeamPlayerPickerPage.PageData> {
    private static final int MAX_ROWS = 10;

    public enum Mode {
        INVITE,
        COOP_ADD,
        BAN_ADD,
        EXPEL
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .append(new KeyedCodec<>("SelectIndex", Codec.STRING), (o, v) -> o.selectIndex = v, o -> o.selectIndex).add()
            .append(new KeyedCodec<>("@SearchInput", Codec.STRING), (o, v) -> o.searchInput = v, o -> o.searchInput).add()
            .build();

        public String action;
        public String selectIndex;
        public String searchInput;
    }

    private record PlayerEntry(UUID uuid, String username) {
    }

    private record PickerActionResult(boolean success, String title, String message) {
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Mode mode;
    private String searchInput;
    private String searchQuery;
    private List<PlayerEntry> displayedPlayers;

    IslandTeamPlayerPickerPage(@Nonnull PlayerRef playerRef,
                               @Nonnull World sourceWorld,
                               @Nonnull ISkyblockPlugin plugin,
                               @Nonnull Mode mode,
                               @Nonnull String searchInput,
                               @Nonnull String searchQuery) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.mode = mode;
        this.searchInput = safe(searchInput);
        this.searchQuery = safe(searchQuery);
        this.displayedPlayers = new ArrayList<>();
    }

    IslandTeamPlayerPickerPage(@Nonnull PlayerRef playerRef,
                               @Nonnull World sourceWorld,
                               @Nonnull ISkyblockPlugin plugin,
                               @Nonnull Mode mode) {
        this(playerRef, sourceWorld, plugin, mode, "", "");
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandTeamPlayerPicker.ui");
        commandBuilder.set("#TeamPickerTitle.Text", resolveTitle());
        commandBuilder.set("#TeamPickerSubtitle.Text", resolveSubtitle());
        commandBuilder.set("#PickerSearchField.PlaceholderText",
            tr("ui.menu.team.picker.search.placeholder", "Search player name..."));
        commandBuilder.set("#PickerSearchField.Value", searchInput);
        commandBuilder.set("#PickerSearchButton.Text", tr("ui.menu.team.picker.search.button", "Search"));
        commandBuilder.set("#PickerBackButton.Text", tr("ui.menu.team.picker.back", "Back"));

        displayedPlayers = filterPlayers(buildKnownPlayers());
        commandBuilder.set("#PickerCountLabel.Text",
            tr("ui.menu.team.picker.count", Map.of("count", String.valueOf(displayedPlayers.size())), "Players: {count}"));
        commandBuilder.set("#PickerEmptyLabel.Visible", displayedPlayers.isEmpty());

        String actionButtonLabel = resolveActionButtonLabel();
        for (int i = 0; i < MAX_ROWS; i++) {
            if (i < displayedPlayers.size()) {
                PlayerEntry entry = displayedPlayers.get(i);
                commandBuilder.set("#PickerRow" + i + ".Visible", true);
                commandBuilder.set("#PickerName" + i + ".Text", entry.username());
                commandBuilder.set("#PickerAction" + i + ".Text", actionButtonLabel);
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#PickerAction" + i,
                    EventData.of("Action", "Select").append("SelectIndex", String.valueOf(i)),
                    false
                );
            } else {
                commandBuilder.set("#PickerRow" + i + ".Visible", false);
            }
        }

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#PickerSearchField",
            EventData.of("@SearchInput", "#PickerSearchField.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PickerSearchButton",
            EventData.of("Action", "Search").append("@SearchInput", "#PickerSearchField.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PickerBackButton",
            EventData.of("Action", "Back"),
            false
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.searchInput != null) {
            searchInput = data.searchInput;
        }

        String action = safe(data.action).trim();
        switch (action) {
            case "Search" -> {
                searchQuery = safe(searchInput).trim();
                reopen(player, ref, store);
                return;
            }
            case "Back" -> {
                IslandMenuPage.openTeam(player, ref, store, playerRef, resolveWorld(player, store), plugin);
                return;
            }
            case "Select" -> {
                int index;
                try {
                    index = Integer.parseInt(safe(data.selectIndex));
                } catch (NumberFormatException ignored) {
                    reopen(player, ref, store);
                    return;
                }
                if (index < 0 || index >= displayedPlayers.size()) {
                    reopen(player, ref, store);
                    return;
                }

                PlayerEntry selected = displayedPlayers.get(index);
                PickerActionResult result;
                if (mode == Mode.INVITE) {
                    result = performInvite(selected);
                } else {
                    result = performAction(selected.username());
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandInfoPopupPage(
                        playerRef,
                        resolveWorld(player, store),
                        plugin,
                        result.title(),
                        result.message(),
                        IslandInfoPopupPage.ReturnTab.TEAM
                    )
                );
                return;
            }
            default -> {
            }
        }
    }

    private PickerActionResult performAction(String targetName) {
        return switch (mode) {
            case COOP_ADD -> performCoopAdd(targetName);
            case BAN_ADD -> performBanAdd(targetName);
            case EXPEL -> performExpel(targetName);
            default -> new PickerActionResult(true, "", "");
        };
    }

    private PickerActionResult performCoopAdd(String targetName) {
        String title = tr("ui.menu.team.picker.result.coop", "Add Coop");
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            return new PickerActionResult(false, title, tr("island.not_initialized", "ISkyblock is not initialized yet."));
        }
        if (targetName == null || targetName.isBlank()) {
            return new PickerActionResult(false, title, tr("coop.usage", "Usage: /island coop <add|remove> <player>"));
        }

        PlayerRef targetRef = resolvePlayerByName(targetName);
        if (targetRef == null) {
            return new PickerActionResult(false, title,
                tr("coop.not_online", Map.of("player", targetName), "Player {player} is not online."));
        }

        try {
            plugin.getIslandService().addCoopMember(playerRef, targetRef);
            Chat.send(targetRef, plugin.tr(targetRef, "coop.enable.notify", Map.of("leader", playerRef.getUsername())));
            return new PickerActionResult(true, title,
                tr("coop.enable.success", Map.of("player", targetRef.getUsername()), "{player} has been added as coop."));
        } catch (IllegalStateException ex) {
            String errorKey = switch (ex.getMessage()) {
                case "no_island" -> "coop.no_island";
                case "not_leader" -> "coop.not_leader";
                case "self" -> "coop.self";
                case "already_member" -> "coop.already_member";
                case "already_coop" -> "coop.already_coop";
                default -> null;
            };
            if (errorKey != null) {
                Map<String, String> vars = ("already_member".equals(ex.getMessage()) || "already_coop".equals(ex.getMessage()))
                    ? Map.of("player", targetRef.getUsername())
                    : Map.of();
                return new PickerActionResult(false, title,
                    tr(errorKey, vars, tr(errorKey, "Operation failed: " + ex.getMessage())));
            }
            return new PickerActionResult(false, title,
                tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage()), "Failed: {error}"));
        }
    }

    private PickerActionResult performBanAdd(String targetName) {
        String title = tr("ui.menu.team.picker.result.ban", "Ban Player");
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            return new PickerActionResult(false, title, tr("island.not_initialized", "ISkyblock is not initialized yet."));
        }
        if (targetName == null || targetName.isBlank()) {
            return new PickerActionResult(false, title, tr("island.ban.usage", "Usage: /island ban <player>"));
        }

        PlayerRef targetRef = resolvePlayerByName(targetName);
        if (targetRef == null) {
            return new PickerActionResult(false, title,
                tr("island.ban.not_online", Map.of("player", targetName), "Player {player} is not online."));
        }
        if (targetRef.getUuid().equals(playerRef.getUuid())) {
            return new PickerActionResult(false, title, tr("island.ban.self", "You cannot ban yourself."));
        }

        // Block banning admins and moderators
        try {
            if (targetRef.getHolder() != null) {
                Player targetPlayer = targetRef.getHolder().getComponent(Player.getComponentType());
                if (targetPlayer != null && (com.sloyni.main.util.PermissionUtil.has(targetPlayer, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(targetPlayer, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(targetPlayer, "iskyblock.admin"))) {
                    return new PickerActionResult(false, title,
                        tr("island.ban.cannot_ban_admin", Map.of("player", targetRef.getUsername()), "Cannot ban {player}, they are an administrator."));
                }
            }
        } catch (Exception ignored) {
        }

        try {
            plugin.getIslandService().banPlayer(playerRef, targetRef);
            Chat.send(targetRef, plugin.tr(targetRef, "island.ban.kicked", Map.of("leader", playerRef.getUsername())));
            return new PickerActionResult(true, title,
                tr("island.ban.success", Map.of("player", targetRef.getUsername()), "{player} has been banned."));
        } catch (IllegalStateException ex) {
            String errorKey = switch (ex.getMessage()) {
                case "no_island" -> "island.ban.no_island";
                case "not_leader" -> "island.ban.not_leader";
                case "cannot_ban_member" -> "island.ban.cannot_ban_member";
                case "already_banned" -> "island.ban.already_banned";
                default -> null;
            };
            if (errorKey != null) {
                Map<String, String> vars = ("cannot_ban_member".equals(ex.getMessage()) || "already_banned".equals(ex.getMessage()))
                    ? Map.of("player", targetRef.getUsername())
                    : Map.of();
                return new PickerActionResult(false, title,
                    tr(errorKey, vars, tr(errorKey, "Operation failed: " + ex.getMessage())));
            }
            return new PickerActionResult(false, title,
                tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage()), "Failed: {error}"));
        }
    }

    private PickerActionResult performExpel(String targetName) {
        String title = tr("ui.menu.team.picker.result.expel", "Expel Player");
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            return new PickerActionResult(false, title, tr("island.not_initialized", "ISkyblock is not initialized yet."));
        }
        if (targetName == null || targetName.isBlank()) {
            return new PickerActionResult(false, title, tr("island.expel.usage", "Usage: /island expel <player>"));
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            return new PickerActionResult(false, title, tr("island.expel.no_island", "You do not have an island."));
        }
        if (!plugin.getIslandService().hasIslandActionPermission(playerRef.getUuid(), com.sloyni.main.island.IslandMemberPermission.TEAM_EXPEL)) {
            return new PickerActionResult(false, title, tr("island.expel.not_leader", "Only the island leader can expel players."));
        }

        PlayerRef targetRef = resolvePlayerByName(targetName);
        if (targetRef == null) {
            return new PickerActionResult(false, title,
                tr("island.expel.not_online", Map.of("player", targetName), "Player {player} is not online."));
        }
        if (targetRef.getUuid().equals(playerRef.getUuid())) {
            return new PickerActionResult(false, title, tr("island.expel.self", "You cannot expel yourself."));
        }
        if (island.isOnTeam(targetRef.getUuid())) {
            return new PickerActionResult(false, title,
                tr("island.expel.cannot_expel_member", Map.of("player", targetRef.getUsername()), "Cannot expel {player}, they are a team member."));
        }

        // Block expelling admins and moderators
        try {
            if (targetRef.getHolder() != null) {
                Player targetPlayer = targetRef.getHolder().getComponent(Player.getComponentType());
                if (targetPlayer != null && (com.sloyni.main.util.PermissionUtil.has(targetPlayer, "iskyblock.island.admin") || com.sloyni.main.util.PermissionUtil.has(targetPlayer, "iskyblock.moderator") || com.sloyni.main.util.PermissionUtil.has(targetPlayer, "iskyblock.admin"))) {
                    return new PickerActionResult(false, title,
                        tr("island.expel.cannot_expel_admin", Map.of("player", targetRef.getUsername()), "Cannot expel {player}, they are an administrator."));
                }
            }
        } catch (Exception ignored) {
        }

        // Remove from coop if present
        if (island.isCoopMember(targetRef.getUuid())) {
            try {
                plugin.getIslandService().removeCoopMember(playerRef, targetRef.getUuid());
            } catch (IllegalStateException ignored) {
            }
        }

        // Teleport the expelled player to the island world lobby
        String islandWorldName = plugin.getConfigManager().getConfig().getInit().getWorldName();
        World islandWorld = Universe.get().getWorld(islandWorldName);
        if (islandWorld == null) {
            return new PickerActionResult(false, title, tr("island.expel.not_present",
                Map.of("player", targetRef.getUsername()), "{player} could not be found on the island."));
        }

        plugin.getIslandAccessController().routePlayerToIslandOrLobby(targetRef, islandWorld)
            .thenRun(() -> {
                Chat.send(targetRef, plugin.tr(targetRef, "island.expel.kicked", Map.of("leader", playerRef.getUsername())));
            });
        return new PickerActionResult(true, title,
            tr("island.expel.success", Map.of("player", targetRef.getUsername()), "{player} has been expelled."));
    }

    private PickerActionResult performInvite(PlayerEntry selected) {
        String targetName = safe(selected == null ? null : selected.username());
        UUID targetUuid = selected == null ? null : selected.uuid();
        String title = resolveResultTitle();

        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            return new PickerActionResult(false, title, tr("island.not_initialized", "ISkyblock is not initialized yet."));
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            return new PickerActionResult(false, title, tr("team.invite.no_island", "You need an island to invite players."));
        }
        if (!plugin.getIslandService().hasIslandActionPermission(playerRef.getUuid(), com.sloyni.main.island.IslandMemberPermission.TEAM_INVITE)) {
            return new PickerActionResult(false, title, tr("team.invite.not_leader", "Only the island leader can invite players."));
        }
        if (targetUuid == null || targetName.isBlank()) {
            return new PickerActionResult(false, title,
                tr("team.invite.not_online", Map.of("player", "unknown"), "Player unknown is not online."));
        }
        if (targetUuid.equals(playerRef.getUuid()) || targetName.equalsIgnoreCase(playerRef.getUsername())) {
            return new PickerActionResult(false, title, tr("team.invite.self", "You cannot invite yourself."));
        }

        PlayerRef targetRef = Universe.get().getPlayer(targetUuid);
        if (targetRef == null) {
            return new PickerActionResult(false, title,
                tr("team.invite.not_online", Map.of("player", targetName), "Player {player} is not online."));
        }
        if (island.isMember(targetRef.getUuid())) {
            return new PickerActionResult(false, title,
                tr("team.invite.already_member", Map.of("player", targetRef.getUsername()), "{player} is already a member of your island."));
        }

        IslandData targetIsland = plugin.getIslandService().getPlayerIsland(targetRef.getUuid());
        if (targetIsland != null) {
            return new PickerActionResult(false, title,
                tr("team.invite.already_has_island", Map.of("player", targetRef.getUsername()), "{player} already has an island."));
        }

        plugin.getTeamInviteManager().createInvite(playerRef.getUuid(), playerRef.getUsername(), targetRef.getUuid());
        Chat.send(targetRef, plugin.trNoPrefix(targetRef, "team.invite.received", Map.of("leader", playerRef.getUsername())));

        return new PickerActionResult(
            true,
            title,
            tr("team.invite.sent", Map.of("player", targetRef.getUsername()), "Invitation sent to {player}. It expires in 60 seconds.")
        );
    }

    private String tr(String key, String fallback) {
        String language = normalizeLanguage(plugin.resolvePlayerLanguage(playerRef));
        String value = plugin.getMessageManager().getExact(key, language);
        if (value == null || value.isBlank()) {
            value = plugin.getMessageManager().getExact(key, "en");
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    private String tr(String key, Map<String, String> vars, String fallback) {
        String value = tr(key, fallback);
        if (vars == null || vars.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            value = value.replace("{" + safe(entry.getKey()) + "}", safe(entry.getValue()));
        }
        return value;
    }

    private List<PlayerEntry> buildKnownPlayers() {
        List<PlayerEntry> players = new ArrayList<>();
        List<Map.Entry<UUID, String>> ordered = new ArrayList<>(plugin.getKnownPlayerRepository().all().entrySet());
        Collections.reverse(ordered);

        for (Map.Entry<UUID, String> entry : ordered) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();
            if (uuid == null || name == null || name.isBlank()) {
                continue;
            }
            players.add(new PlayerEntry(uuid, name));
        }
        return players;
    }

    private List<PlayerEntry> filterPlayers(List<PlayerEntry> players) {
        String normalized = safe(searchQuery).trim().toLowerCase(Locale.ROOT);
        List<PlayerEntry> filtered = new ArrayList<>();

        if (normalized.isBlank()) {
            for (PlayerEntry entry : players) {
                if (filtered.size() >= MAX_ROWS) {
                    break;
                }
                filtered.add(entry);
            }
            return filtered;
        }

        for (PlayerEntry entry : players) {
            String username = safe(entry.username()).toLowerCase(Locale.ROOT);
            if (!username.contains(normalized)) {
                continue;
            }
            filtered.add(entry);
            if (filtered.size() >= MAX_ROWS) {
                break;
            }
        }
        return filtered;
    }

    private void reopen(Player player,
                        Ref<EntityStore> ref,
                        Store<EntityStore> store) {
        player.getPageManager().openCustomPage(ref, store,
            new IslandTeamPlayerPickerPage(playerRef, resolveWorld(player, store), plugin, mode, safe(searchInput), safe(searchQuery)));
    }

    private World resolveWorld(Player player, Store<EntityStore> store) {
        if (player != null && player.getWorld() != null) {
            return player.getWorld();
        }
        EntityStore entityStore = store.getExternalData();
        if (entityStore != null && entityStore.getWorld() != null) {
            return entityStore.getWorld();
        }
        return sourceWorld;
    }

    private String resolveTitle() {
        return switch (mode) {
            case INVITE -> tr("ui.menu.team.picker.title.invite", "Invite player");
            case COOP_ADD -> tr("ui.menu.team.picker.title.coop_add", "Add coop player");
            case BAN_ADD -> tr("ui.menu.team.picker.title.ban_add", "Ban player");
            case EXPEL -> tr("ui.menu.team.picker.title.expel", "Expel player");
        };
    }

    private String resolveSubtitle() {
        return tr("ui.menu.team.picker.subtitle", "Latest 10 players shown by default. Use search to find someone.");
    }

    private String resolveActionButtonLabel() {
        return switch (mode) {
            case INVITE -> tr("ui.menu.team.picker.action.invite", "Invite");
            case COOP_ADD -> tr("ui.menu.team.picker.action.coop", "Coop");
            case BAN_ADD -> tr("ui.menu.team.picker.action.ban", "Ban");
            case EXPEL -> tr("ui.menu.team.picker.action.expel", "Expel");
        };
    }

    private String resolveResultTitle() {
        return switch (mode) {
            case INVITE -> tr("ui.menu.team.picker.result.invite", "Invite");
            case COOP_ADD -> tr("ui.menu.team.picker.result.coop", "Add Coop");
            case BAN_ADD -> tr("ui.menu.team.picker.result.ban", "Ban Player");
            case EXPEL -> tr("ui.menu.team.picker.result.expel", "Expel Player");
        };
    }

    private PlayerRef resolvePlayerByName(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }
        for (PlayerRef online : Universe.get().getPlayers()) {
            if (online.getUsername() != null && online.getUsername().equalsIgnoreCase(targetName)) {
                return online;
            }
        }
        return null;
    }

    private String normalizeLanguage(String language) {
        String normalized = safe(language).trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "en";
        }
        int separator = normalized.indexOf('-');
        if (separator > 0) {
            normalized = normalized.substring(0, separator);
        }
        return normalized.isBlank() ? "en" : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}