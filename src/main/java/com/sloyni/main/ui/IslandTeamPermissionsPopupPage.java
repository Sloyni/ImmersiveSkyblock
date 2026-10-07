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
import com.sloyni.main.island.IslandMemberPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class IslandTeamPermissionsPopupPage extends InteractiveCustomUIPage<IslandTeamPermissionsPopupPage.PageData> {
    private static final int MAX_PERMISSION_ROWS = 13;

    public enum Mode {
        DEFAULT,
        MEMBER
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
            .build();

        public String action;
        public String param;
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Mode mode;
    private final UUID targetMemberUuid;
    private final String errorMessage;

    IslandTeamPermissionsPopupPage(@Nonnull PlayerRef playerRef,
                                   @Nonnull World sourceWorld,
                                   @Nonnull ISkyblockPlugin plugin,
                                   @Nonnull Mode mode,
                                   UUID targetMemberUuid,
                                   String errorMessage) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.mode = mode;
        this.targetMemberUuid = targetMemberUuid;
        this.errorMessage = safe(errorMessage);
    }

    IslandTeamPermissionsPopupPage(@Nonnull PlayerRef playerRef,
                                   @Nonnull World sourceWorld,
                                   @Nonnull ISkyblockPlugin plugin,
                                   @Nonnull Mode mode,
                                   UUID targetMemberUuid) {
        this(playerRef, sourceWorld, plugin, mode, targetMemberUuid, "");
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandTeamPermissionsPopup.ui");

        String language = safe(plugin.resolvePlayerLanguage(playerRef));
        List<IslandMemberPermission> permissions = resolveVisiblePermissions();

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        boolean canEdit = island != null && island.isLeader(playerRef.getUuid());
        boolean memberMode = mode == Mode.MEMBER;
        boolean memberExists = !memberMode || (targetMemberUuid != null && island != null && island.isMember(targetMemberUuid));

        String resolvedError = safe(errorMessage);
        if (resolvedError.isBlank()) {
            if (island == null) {
                resolvedError = tr(language, "team.no_island", "You do not have an island.");
            } else if (!canEdit) {
                resolvedError = tr(language, "ui.menu.permissions.error.not_leader", "Only the island leader can edit member permissions.");
            } else if (!memberExists) {
                resolvedError = tr(language, "ui.menu.permissions.error.member_not_found", "Member not found on this island.");
            }
        }

        String memberName = memberMode ? resolveMemberName(targetMemberUuid, island) : "";

        String popupTitle = memberMode
            ? tr(language, "ui.menu.permissions.member.title", Map.of("player", memberName), "Permissions: {player}")
            : tr(language, "ui.menu.permissions.default.title", "Default member permissions");

        commandBuilder.set("#PermissionsTopTitle.Text", popupTitle);
        commandBuilder.set("#PermissionsPopupTitle.Text", popupTitle);
        commandBuilder.set(
            "#PermissionsPopupSubtitle.Text",
            memberMode
                ? tr(language, "ui.menu.permissions.member.subtitle", "Configure member overrides or follow defaults.")
                : tr(language, "ui.menu.permissions.default.subtitle", "These permissions apply to members that follow defaults.")
        );
        commandBuilder.set("#BtnPermissionsBack.Text", tr(language, "ui.common.back", "Back"));

        boolean hasError = !resolvedError.isBlank();
        commandBuilder.set("#PermissionsError.Visible", hasError);
        commandBuilder.set("#PermissionsError.Text", hasError ? resolvedError : "");

        boolean showFollowDefault = memberMode && canEdit && memberExists;
        boolean followDefault = showFollowDefault && plugin.getIslandService().isMemberFollowingDefaultPermissions(playerRef.getUuid(), targetMemberUuid);

        commandBuilder.set("#PermissionsFollowDefaultRow.Visible", showFollowDefault);
        commandBuilder.set("#PermissionsFollowDefaultHint.Text", tr(language, "ui.menu.permissions.member.follow_defaults", "Follow default permissions"));
        commandBuilder.set("#PermissionsFollowDefaultCheck.Value", followDefault);
        commandBuilder.set("#PermissionsInheritedHint.Visible", showFollowDefault && followDefault);
        commandBuilder.set("#PermissionsInheritedHint.Text", tr(language, "ui.menu.permissions.member.inherited_hint", "This member currently uses default permissions."));

        List<String> defaultKeys = island == null ? List.of() : island.getDefaultMemberPermissionKeys();
        List<String> memberKeys = memberMode && island != null && targetMemberUuid != null
            ? island.getMemberExplicitPermissionKeys(targetMemberUuid)
            : List.of();

        for (int i = 0; i < MAX_PERMISSION_ROWS; i++) {
            if (i < permissions.size()) {
                IslandMemberPermission permission = permissions.get(i);
                boolean enabled = memberMode
                    ? (followDefault ? defaultKeys.contains(permission.getKey()) : memberKeys.contains(permission.getKey()))
                    : defaultKeys.contains(permission.getKey());

                commandBuilder.set("#PermissionsRow" + i + ".Visible", true);
                commandBuilder.set("#PermissionsLabel" + i + ".Text", tr(language, permission.getLabelKey(), permission.getFallbackLabel()));
                commandBuilder.set("#PermissionsCheck" + i + ".Value", enabled);
                commandBuilder.set("#PermissionsRow" + i + ".Background", enabled ? "#223149" : "#182638");

                if (canEdit && (!memberMode || memberExists)) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#PermissionsCheck" + i,
                        EventData.of("Action", "TogglePermission").append("Param", String.valueOf(i)),
                        false
                    );
                }
            } else {
                commandBuilder.set("#PermissionsRow" + i + ".Visible", false);
            }
        }

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BtnPermissionsBack",
            EventData.of("Action", "Back"),
            false
        );

        if (showFollowDefault) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#PermissionsFollowDefaultCheck",
                EventData.of("Action", "ToggleFollowDefault"),
                false
            );
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        String action = safe(data.action).trim();
        if ("Back".equalsIgnoreCase(action)) {
            IslandMenuPage.openTeam(player, ref, store, playerRef, resolveWorld(player, store), plugin);
            return;
        }

        if ("ToggleFollowDefault".equalsIgnoreCase(action)) {
            handleToggleFollowDefault(player, ref, store);
            return;
        }

        if ("TogglePermission".equalsIgnoreCase(action)) {
            handleTogglePermission(player, ref, store, data.param);
        }
    }

    private void handleToggleFollowDefault(Player player,
                                           Ref<EntityStore> ref,
                                           Store<EntityStore> store) {
        if (mode != Mode.MEMBER || targetMemberUuid == null) {
            reopen(player, ref, store, "");
            return;
        }

        try {
            boolean current = plugin.getIslandService().isMemberFollowingDefaultPermissions(playerRef.getUuid(), targetMemberUuid);
            plugin.getIslandService().setMemberFollowDefaultPermissions(playerRef.getUuid(), targetMemberUuid, !current);
            reopen(player, ref, store, "");
        } catch (IllegalStateException ex) {
            reopen(player, ref, store, resolveError(ex));
        }
    }

    private void handleTogglePermission(Player player,
                                        Ref<EntityStore> ref,
                                        Store<EntityStore> store,
                                        String rawIndex) {
        int index;
        try {
            index = Integer.parseInt(safe(rawIndex));
        } catch (NumberFormatException ignored) {
            reopen(player, ref, store, "");
            return;
        }

        List<IslandMemberPermission> permissions = resolveVisiblePermissions();
        if (index < 0 || index >= permissions.size()) {
            reopen(player, ref, store, "");
            return;
        }

        IslandMemberPermission toggled = permissions.get(index);

        try {
            if (mode == Mode.DEFAULT) {
                List<String> current = new ArrayList<>(plugin.getIslandService().getDefaultMemberPermissionKeys(playerRef.getUuid()));
                togglePermissionKey(current, toggled.getKey());
                plugin.getIslandService().setDefaultMemberPermissionKeys(playerRef.getUuid(), current);
                reopen(player, ref, store, "");
                return;
            }

            if (targetMemberUuid == null) {
                reopen(player, ref, store,
                    tr(plugin.resolvePlayerLanguage(playerRef), "ui.menu.permissions.error.member_not_found", "Member not found on this island."));
                return;
            }

            boolean followDefault = plugin.getIslandService().isMemberFollowingDefaultPermissions(playerRef.getUuid(), targetMemberUuid);
            List<String> working = followDefault
                ? new ArrayList<>(plugin.getIslandService().getDefaultMemberPermissionKeys(playerRef.getUuid()))
                : new ArrayList<>(plugin.getIslandService().getMemberExplicitPermissionKeys(playerRef.getUuid(), targetMemberUuid));

            togglePermissionKey(working, toggled.getKey());
            plugin.getIslandService().setMemberExplicitPermissionKeys(playerRef.getUuid(), targetMemberUuid, working);
            reopen(player, ref, store, "");
        } catch (IllegalStateException ex) {
            reopen(player, ref, store, resolveError(ex));
        }
    }

    private void togglePermissionKey(List<String> keys, String key) {
        if (keys == null || key == null || key.isBlank()) {
            return;
        }
        int foundIndex = -1;
        for (int i = 0; i < keys.size(); i++) {
            if (key.equalsIgnoreCase(safe(keys.get(i)))) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex >= 0) {
            keys.remove(foundIndex);
        } else {
            keys.add(key);
        }
    }

    private List<IslandMemberPermission> resolveVisiblePermissions() {
        boolean levelsEnabled = plugin.getAddonManager().isLevelsEnabled();
        boolean warpsEnabled = plugin.getAddonManager().isWarpsEnabled();
        boolean wallsEnabled = plugin.getAddonManager().isIslandWallsEnabled();

        List<IslandMemberPermission> visible = new ArrayList<>();
        for (IslandMemberPermission permission : IslandMemberPermission.orderedConfigurable()) {
            if (permission.isAvailable(levelsEnabled, warpsEnabled, wallsEnabled)) {
                visible.add(permission);
            }
        }
        return visible;
    }

    private void reopen(Player player,
                        Ref<EntityStore> ref,
                        Store<EntityStore> store,
                        String error) {
        player.getPageManager().openCustomPage(ref, store,
            new IslandTeamPermissionsPopupPage(
                playerRef,
                resolveWorld(player, store),
                plugin,
                mode,
                targetMemberUuid,
                error
            )
        );
    }

    private String resolveError(IllegalStateException ex) {
        String code = safe(ex == null ? null : ex.getMessage());
        String language = safe(plugin.resolvePlayerLanguage(playerRef));
        return switch (code) {
            case "no_island" -> tr(language, "team.no_island", "You do not have an island.");
            case "not_leader" -> tr(language, "ui.menu.permissions.error.not_leader", "Only the island leader can edit member permissions.");
            case "member_not_found" -> tr(language, "ui.menu.permissions.error.member_not_found", "Member not found on this island.");
            default -> tr(language, "ui.menu.permissions.error.generic", Map.of("error", code), "Failed to update permissions: {error}");
        };
    }

    private String resolveMemberName(UUID memberUuid, IslandData island) {
        if (memberUuid == null) {
            return "Unknown";
        }

        if (island != null) {
            String mapped = island.getMemberNames().get(memberUuid.toString());
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }

        PlayerRef online = Universe.get().getPlayer(memberUuid);
        if (online != null && online.getUsername() != null && !online.getUsername().isBlank()) {
            return online.getUsername();
        }

        String known = plugin.getIslandService().getAllKnownPlayers().get(memberUuid);
        if (known != null && !known.isBlank()) {
            return known;
        }

        return memberUuid.toString();
    }

    private String tr(String language, String key, String fallback) {
        String normalizedLanguage = normalizeLanguage(language);
        String value = plugin.getMessageManager().getExact(key, normalizedLanguage);
        if (value == null || value.isBlank()) {
            value = plugin.getMessageManager().getExact(key, "en");
        }
        if (value == null || value.isBlank()) {
            value = fallback;
        }
        return safe(value);
    }

    private String tr(String language, String key, Map<String, String> vars, String fallback) {
        String value = tr(language, key, fallback);
        if (vars == null || vars.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            value = value.replace("{" + safe(entry.getKey()) + "}", safe(entry.getValue()));
        }
        return value;
    }

    private String normalizeLanguage(String language) {
        String normalized = safe(language).trim().replace('_', '-').toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf('-');
        if (separator > 0) {
            normalized = normalized.substring(0, separator);
        }
        return normalized.isBlank() ? "en" : normalized;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
