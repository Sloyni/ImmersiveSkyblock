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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.TeleportUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class ISkyblockAdminDashboardIslandsViewerPage
        extends InteractiveCustomUIPage<ISkyblockAdminDashboardIslandsViewerPage.PageData> {

    private static final int MAX_TEAM_ROWS = 5;
    private static final int MAX_COOP_ROWS = 5;
    private static final int MAX_BAN_ROWS = 5;
    private static final String ACTIVE_BG = "#223149";

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final ISkyblockAdminDashboardPage parent;
    private final UUID targetUuid;
    private final String targetName;

    private String islandNameInput = "";
    private String islandSizeInput = "";
    private String teamMemberInput = "";
    private String coopMemberInput = "";
    private String bannedMemberInput = "";
    private String successMessage = null;
    private String errorMessage = null;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
            .builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Back", Codec.STRING), (o, v) -> o.back = v, o -> o.back).add()
            .append(new KeyedCodec<>("TeleportToIsland", Codec.STRING), (o, v) -> o.teleportToIsland = v, o -> o.teleportToIsland).add()
            .append(new KeyedCodec<>("ToggleLock", Codec.STRING), (o, v) -> o.toggleLock = v, o -> o.toggleLock).add()
            .append(new KeyedCodec<>("ApplyIslandName", Codec.STRING), (o, v) -> o.applyIslandName = v, o -> o.applyIslandName).add()
            .append(new KeyedCodec<>("ResetIslandName", Codec.STRING), (o, v) -> o.resetIslandName = v, o -> o.resetIslandName).add()
            .append(new KeyedCodec<>("ApplyIslandSize", Codec.STRING), (o, v) -> o.applyIslandSize = v, o -> o.applyIslandSize).add()
            .append(new KeyedCodec<>("AddTeamMember", Codec.STRING), (o, v) -> o.addTeamMember = v, o -> o.addTeamMember).add()
            .append(new KeyedCodec<>("RemoveTeamMember", Codec.STRING), (o, v) -> o.removeTeamMember = v, o -> o.removeTeamMember).add()
            .append(new KeyedCodec<>("MakeLeader", Codec.STRING), (o, v) -> o.makeLeader = v, o -> o.makeLeader).add()
            .append(new KeyedCodec<>("AddCoopMember", Codec.STRING), (o, v) -> o.addCoopMember = v, o -> o.addCoopMember).add()
            .append(new KeyedCodec<>("RemoveCoopMember", Codec.STRING), (o, v) -> o.removeCoopMember = v, o -> o.removeCoopMember).add()
            .append(new KeyedCodec<>("BanPlayer", Codec.STRING), (o, v) -> o.banPlayer = v, o -> o.banPlayer).add()
            .append(new KeyedCodec<>("UnbanPlayer", Codec.STRING), (o, v) -> o.unbanPlayer = v, o -> o.unbanPlayer).add()
            .append(new KeyedCodec<>("RemoveTeamMemberRow", Codec.STRING), (o, v) -> o.removeTeamMemberRow = v, o -> o.removeTeamMemberRow).add()
            .append(new KeyedCodec<>("MakeLeaderRow", Codec.STRING), (o, v) -> o.makeLeaderRow = v, o -> o.makeLeaderRow).add()
            .append(new KeyedCodec<>("RemoveCoopMemberRow", Codec.STRING), (o, v) -> o.removeCoopMemberRow = v, o -> o.removeCoopMemberRow).add()
            .append(new KeyedCodec<>("UnbanPlayerRow", Codec.STRING), (o, v) -> o.unbanPlayerRow = v, o -> o.unbanPlayerRow).add()
            .append(new KeyedCodec<>("OpenDeleteIslandConfirm", Codec.STRING), (o, v) -> o.openDeleteIslandConfirm = v, o -> o.openDeleteIslandConfirm).add()
            .append(new KeyedCodec<>("@IslandName", Codec.STRING), (o, v) -> o.islandName = v, o -> o.islandName).add()
            .append(new KeyedCodec<>("@IslandSize", Codec.STRING), (o, v) -> o.islandSize = v, o -> o.islandSize).add()
            .append(new KeyedCodec<>("@TeamMember", Codec.STRING), (o, v) -> o.teamMember = v, o -> o.teamMember).add()
            .append(new KeyedCodec<>("@CoopMember", Codec.STRING), (o, v) -> o.coopMember = v, o -> o.coopMember).add()
            .append(new KeyedCodec<>("@BannedMember", Codec.STRING), (o, v) -> o.bannedMember = v, o -> o.bannedMember).add()
            .build();

        public String back;
        public String teleportToIsland;
        public String toggleLock;
        public String applyIslandName;
        public String resetIslandName;
        public String applyIslandSize;
        public String addTeamMember;
        public String removeTeamMember;
        public String makeLeader;
        public String addCoopMember;
        public String removeCoopMember;
        public String banPlayer;
        public String unbanPlayer;
        public String removeTeamMemberRow;
        public String makeLeaderRow;
        public String removeCoopMemberRow;
        public String unbanPlayerRow;
        public String openDeleteIslandConfirm;
        public String islandName;
        public String islandSize;
        public String teamMember;
        public String coopMember;
        public String bannedMember;
    }

    public ISkyblockAdminDashboardIslandsViewerPage(@Nonnull PlayerRef playerRef,
                                                    @Nonnull World sourceWorld,
                                                    @Nonnull ISkyblockPlugin plugin,
                                                    @Nonnull ISkyblockAdminDashboardPage parent,
                                                    @Nonnull UUID targetUuid,
                                                    @Nonnull String targetName) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.parent = parent;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/ISkyblockAdminDashboard.IslandsViewerPage.ui");

        String language = getLanguage();
        IslandData island = plugin.getIslandService().getPlayerIsland(targetUuid);

        commandBuilder.set("#IslandsViewerBackButton.Text", text(language, "ui.common.back"));
        commandBuilder.set("#IslandsViewerSuccess.Visible", successMessage != null && !successMessage.isBlank());
        commandBuilder.set("#IslandsViewerSuccess.Text", successMessage == null ? "" : successMessage);
        commandBuilder.set("#IslandsViewerError.Visible", errorMessage != null && !errorMessage.isBlank());
        commandBuilder.set("#IslandsViewerError.Text", errorMessage == null ? "" : errorMessage);

        if (island == null) {
            commandBuilder.set("#IslandsViewerTitle.Text", text(language, "ui.admin_dashboard.islands.viewer_deleted"));
            commandBuilder.set("#IslandsViewerDescription.Text", text(language, "ui.admin_dashboard.islands.viewer_deleted_description"));
            commandBuilder.set("#IslandsViewerSections.Visible", false);
            bindStaticEvents(eventBuilder);
            return;
        }

        String displayName = plugin.getIslandService().getIslandDisplayName(island);
        int currentSize = Math.max(plugin.getIslandService().getDefaultIslandZoneSize(), island.getZoneSize());
        int minSize = plugin.getIslandService().getMinUpgradeableIslandZoneSize();
        int maxSize = plugin.getIslandService().getMaxAllowedIslandZoneSize();
        String nameDisplay = getDisplayValue(islandNameInput, displayName);
        String sizeDisplay = getDisplayValue(islandSizeInput, String.valueOf(currentSize));
        boolean nameDirty = isDirty(islandNameInput, displayName);
        boolean sizeDirty = isDirty(islandSizeInput, String.valueOf(currentSize));

        commandBuilder.set("#IslandsViewerTitle.Text", text(language, "ui.admin_dashboard.islands.viewer_title", Map.of("player", targetName)));
        commandBuilder.set("#IslandsViewerDescription.Text", text(language, "ui.admin_dashboard.islands.viewer_description"));
        commandBuilder.set("#IslandsViewerSections.Visible", true);
        commandBuilder.set("#IslandsOverviewTitle.Text", text(language, "ui.admin_dashboard.islands.overview_title"));
        commandBuilder.set("#IslandsOverviewDescription.Text", text(language, "ui.admin_dashboard.islands.overview_description"));
        commandBuilder.set("#IslandsViewerTeleportButton.Text", text(language, "ui.admin_dashboard.islands.teleport"));
        commandBuilder.set("#IslandsViewerLockButton.Text", text(language, island.isLocked()
            ? "ui.admin_dashboard.islands.unlock"
            : "ui.admin_dashboard.islands.lock"));

        commandBuilder.set("#IslandsInfoPlayerLabel.Text", text(language, "ui.admin_dashboard.islands.overview_player"));
        commandBuilder.set("#IslandsInfoPlayerValue.Text", targetName);
        commandBuilder.set("#IslandsInfoIslandLabel.Text", text(language, "ui.admin_dashboard.islands.overview_island"));
        commandBuilder.set("#IslandsInfoIslandValue.Text", displayName);
        commandBuilder.set("#IslandsInfoLeaderLabel.Text", text(language, "ui.admin_dashboard.islands.overview_leader"));
        commandBuilder.set("#IslandsInfoLeaderValue.Text", resolveLeaderName(island));
        commandBuilder.set("#IslandsInfoTemplateLabel.Text", text(language, "ui.admin_dashboard.islands.overview_template"));
        commandBuilder.set("#IslandsInfoTemplateValue.Text", island.getTemplateId() == null ? "" : island.getTemplateId());
        commandBuilder.set("#IslandsInfoWorldLabel.Text", text(language, "ui.admin_dashboard.islands.overview_world"));
        commandBuilder.set("#IslandsInfoWorldValue.Text", island.getWorldName() == null ? "" : island.getWorldName());
        commandBuilder.set("#IslandsInfoGridLabel.Text", text(language, "ui.admin_dashboard.islands.overview_grid"));
        commandBuilder.set("#IslandsInfoGridValue.Text", island.getGridX() + ", " + island.getGridZ());
        commandBuilder.set("#IslandsInfoCenterLabel.Text", text(language, "ui.admin_dashboard.islands.overview_center"));
        commandBuilder.set("#IslandsInfoCenterValue.Text", island.getCenterX() + ", " + island.getCenterY() + ", " + island.getCenterZ());
        commandBuilder.set("#IslandsInfoStateLabel.Text", text(language, "ui.admin_dashboard.islands.overview_state"));
        commandBuilder.set("#IslandsInfoStateValue.Text", text(language, island.isLocked()
            ? "ui.admin_dashboard.islands.overview_locked"
            : "ui.admin_dashboard.islands.overview_unlocked"));

        commandBuilder.set("#IslandsNameSectionTitle.Text", text(language, "ui.admin_dashboard.islands.name_title"));
        commandBuilder.set("#IslandsNameSectionDescription.Text", text(language, "ui.admin_dashboard.islands.name_description"));
        commandBuilder.set("#IslandsNameLabel.Text", text(language, "ui.admin_dashboard.islands.name_label"));
        commandBuilder.set("#IslandsNameField.Value", nameDisplay);
        commandBuilder.set("#IslandsNameFieldBorder.Background", nameDirty ? "#f2c94c" : ACTIVE_BG);
        commandBuilder.set("#IslandsNameApplyBox.Visible", nameDirty);
        commandBuilder.set("#IslandsNameResetButton.Text", text(language, "ui.admin_dashboard.islands.name_reset"));

        commandBuilder.set("#IslandsSizeSectionTitle.Text", text(language, "ui.admin_dashboard.islands.size_title"));
        commandBuilder.set("#IslandsSizeSectionDescription.Text", text(language, "ui.admin_dashboard.islands.size_description"));
        commandBuilder.set("#IslandsSizeLabel.Text", text(language, "ui.admin_dashboard.islands.size_label"));
        commandBuilder.set("#IslandsSizeHint.Text", text(language, "ui.admin_dashboard.islands.size_hint", Map.of(
            "min", String.valueOf(minSize),
            "max", String.valueOf(maxSize)
        )));
        commandBuilder.set("#IslandsSizeField.Value", sizeDisplay);
        commandBuilder.set("#IslandsSizeFieldBorder.Background", sizeDirty ? "#f2c94c" : ACTIVE_BG);
        commandBuilder.set("#IslandsSizeApplyBox.Visible", sizeDirty);

        commandBuilder.set("#IslandsTeamSectionTitle.Text", text(language, "ui.admin_dashboard.islands.team_title"));
        commandBuilder.set("#IslandsTeamSectionDescription.Text", text(language, "ui.admin_dashboard.islands.team_description"));
        commandBuilder.set("#IslandsTeamField.PlaceholderText", text(language, "ui.admin_dashboard.islands.team_placeholder"));
        commandBuilder.set("#IslandsTeamField.Value", teamMemberInput == null ? "" : teamMemberInput);
        commandBuilder.set("#IslandsTeamAddButton.Text", text(language, "ui.admin_dashboard.islands.team_add"));
        buildTeamRows(commandBuilder, island, language);

        commandBuilder.set("#IslandsCoopSectionTitle.Text", text(language, "ui.admin_dashboard.islands.coop_title"));
        commandBuilder.set("#IslandsCoopSectionDescription.Text", text(language, "ui.admin_dashboard.islands.coop_description"));
        commandBuilder.set("#IslandsCoopField.PlaceholderText", text(language, "ui.admin_dashboard.islands.coop_placeholder"));
        commandBuilder.set("#IslandsCoopField.Value", coopMemberInput == null ? "" : coopMemberInput);
        commandBuilder.set("#IslandsCoopAddButton.Text", text(language, "ui.admin_dashboard.islands.coop_add"));
        buildMemberRows(commandBuilder, "Coop", MAX_COOP_ROWS, buildMemberRows(island.getCoopNames().values()), text(language, "ui.admin_dashboard.islands.empty_coop"), text(language, "ui.admin_dashboard.islands.coop_remove"));

        commandBuilder.set("#IslandsBanSectionTitle.Text", text(language, "ui.admin_dashboard.islands.bans_title"));
        commandBuilder.set("#IslandsBanSectionDescription.Text", text(language, "ui.admin_dashboard.islands.bans_description"));
        commandBuilder.set("#IslandsBanField.PlaceholderText", text(language, "ui.admin_dashboard.islands.bans_placeholder"));
        commandBuilder.set("#IslandsBanField.Value", bannedMemberInput == null ? "" : bannedMemberInput);
        commandBuilder.set("#IslandsBanAddButton.Text", text(language, "ui.admin_dashboard.islands.bans_add"));
        buildMemberRows(commandBuilder, "Ban", MAX_BAN_ROWS, buildMemberRows(island.getBannedNames().values()), text(language, "ui.admin_dashboard.islands.empty_bans"), text(language, "ui.admin_dashboard.islands.bans_remove"));

        commandBuilder.set("#IslandsDangerSectionTitle.Text", text(language, "ui.admin_dashboard.islands.danger_title"));
        commandBuilder.set("#IslandsDangerSectionDescription.Text", text(language, "ui.admin_dashboard.islands.danger_description"));
        commandBuilder.set("#IslandsDeleteButton.Text", text(language, "ui.admin_dashboard.islands.delete"));

        bindStaticEvents(eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        if (data.islandName != null) {
            islandNameInput = data.islandName.trim();
            return;
        }
        if (data.islandSize != null) {
            islandSizeInput = data.islandSize.trim();
            return;
        }
        if (data.teamMember != null) {
            teamMemberInput = data.teamMember.trim();
            return;
        }
        if (data.coopMember != null) {
            coopMemberInput = data.coopMember.trim();
            return;
        }
        if (data.bannedMember != null) {
            bannedMemberInput = data.bannedMember.trim();
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.back != null) {
            player.getPageManager().openCustomPage(ref, store, parent);
            return;
        }

        if (data.teleportToIsland != null) {
            teleportToIsland(ref, store, player);
            return;
        }

        if (data.toggleLock != null) {
            IslandData island = plugin.getIslandService().getPlayerIsland(targetUuid);
            if (island == null) {
                handleFailure(player, ref, store, "no_island", targetName);
                return;
            }
            try {
                plugin.getIslandService().setIslandLockedAsAdmin(targetUuid, !island.isLocked());
                successMessage = text(getLanguage(), !island.isLocked() ? "island.lock.success" : "island.unlock.success");
                errorMessage = null;
            } catch (Exception ex) {
                handleFailure(player, ref, store, ex.getMessage(), targetName);
                return;
            }
            reopen(player, ref, store);
            return;
        }

        if (data.applyIslandName != null) {
            String requestedName = islandNameInput == null ? "" : islandNameInput.trim();
            if (requestedName.isBlank() || requestedName.length() > 16) {
                errorMessage = text(getLanguage(), "ui.admin_dashboard.islands.error.invalid_name", Map.of("max", "16"));
                successMessage = null;
                reopen(player, ref, store);
                return;
            }
            try {
                plugin.getIslandService().setIslandNameByTarget(targetUuid, requestedName);
                successMessage = text(getLanguage(), "islands.admin.rename.success", Map.of("player", targetName, "name", requestedName));
                errorMessage = null;
                islandNameInput = "";
            } catch (Exception ex) {
                handleFailure(player, ref, store, ex.getMessage(), targetName);
                return;
            }
            reopen(player, ref, store);
            return;
        }

        if (data.resetIslandName != null) {
            try {
                plugin.getIslandService().resetIslandNameByTarget(targetUuid);
                successMessage = text(getLanguage(), "islands.admin.delname.success", Map.of("player", targetName));
                errorMessage = null;
                islandNameInput = "";
            } catch (Exception ex) {
                handleFailure(player, ref, store, ex.getMessage(), targetName);
                return;
            }
            reopen(player, ref, store);
            return;
        }

        if (data.applyIslandSize != null) {
            applyIslandSize(ref, store, player);
            return;
        }

        if (data.addTeamMember != null) {
            addTeamMember(player, ref, store);
            return;
        }

        if (data.removeTeamMemberRow != null) {
            removeTeamMemberByRow(player, ref, store, data.removeTeamMemberRow);
            return;
        }

        if (data.makeLeaderRow != null) {
            makeLeaderByRow(player, ref, store, data.makeLeaderRow);
            return;
        }

        if (data.removeTeamMember != null) {
            removeTeamMember(player, ref, store);
            return;
        }

        if (data.makeLeader != null) {
            makeLeader(player, ref, store);
            return;
        }

        if (data.addCoopMember != null) {
            addCoopMember(player, ref, store);
            return;
        }

        if (data.removeCoopMemberRow != null) {
            removeCoopMemberByRow(player, ref, store, data.removeCoopMemberRow);
            return;
        }

        if (data.removeCoopMember != null) {
            removeCoopMember(player, ref, store);
            return;
        }

        if (data.banPlayer != null) {
            banPlayer(player, ref, store);
            return;
        }

        if (data.unbanPlayerRow != null) {
            unbanPlayerByRow(player, ref, store, data.unbanPlayerRow);
            return;
        }

        if (data.unbanPlayer != null) {
            unbanPlayer(player, ref, store);
            return;
        }

        if (data.openDeleteIslandConfirm != null) {
            successMessage = null;
            errorMessage = null;
            player.getPageManager().openCustomPage(ref, store,
                new ISkyblockAdminDashboardDeleteIslandConfirmPage(playerRef, plugin, this, parent, targetUuid, targetName));
            return;
        }
    }

    private void bindStaticEvents(UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsViewerBackButton",
            EventData.of("Back", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsViewerTeleportButton",
            EventData.of("TeleportToIsland", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsViewerLockButton",
            EventData.of("ToggleLock", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IslandsNameField",
            EventData.of("@IslandName", "#IslandsNameField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsNameApply",
            EventData.of("ApplyIslandName", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsNameResetButton",
            EventData.of("ResetIslandName", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IslandsSizeField",
            EventData.of("@IslandSize", "#IslandsSizeField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsSizeApply",
            EventData.of("ApplyIslandSize", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IslandsTeamField",
            EventData.of("@TeamMember", "#IslandsTeamField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsTeamAddButton",
            EventData.of("AddTeamMember", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IslandsCoopField",
            EventData.of("@CoopMember", "#IslandsCoopField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsCoopAddButton",
            EventData.of("AddCoopMember", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IslandsBanField",
            EventData.of("@BannedMember", "#IslandsBanField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsBanAddButton",
            EventData.of("BanPlayer", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsDeleteButton",
            EventData.of("OpenDeleteIslandConfirm", "true"), false);

        for (int i = 0; i < MAX_TEAM_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsTeamRemoveRowButton" + i,
                EventData.of("RemoveTeamMemberRow", String.valueOf(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsTeamLeaderRowButton" + i,
                EventData.of("MakeLeaderRow", String.valueOf(i)), false);
        }

        for (int i = 0; i < MAX_COOP_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsCoopRemoveRowButton" + i,
                EventData.of("RemoveCoopMemberRow", String.valueOf(i)), false);
        }

        for (int i = 0; i < MAX_BAN_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsBanRemoveRowButton" + i,
                EventData.of("UnbanPlayerRow", String.valueOf(i)), false);
        }
    }

    private void applyIslandSize(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        String raw = islandSizeInput == null ? "" : islandSizeInput.trim();
        int requestedSize;
        try {
            requestedSize = Integer.parseInt(raw);
        } catch (Exception ex) {
            errorMessage = invalidIslandSizeMessage(getLanguage(), raw);
            successMessage = null;
            reopen(player, ref, store);
            return;
        }

        plugin.getIslandService().setPlayerIslandSize(targetUuid, requestedSize)
            .thenAccept(result -> {
                String language = getLanguage();
                successMessage = text(language,
                    result.islandFound() ? "island_size.admin.success_with_island" : "island_size.admin.success_no_island",
                    result.islandFound()
                        ? Map.of(
                            "player", targetName,
                            "size", String.valueOf(result.playerSize()),
                            "island_size", String.valueOf(result.islandSize())
                        )
                        : Map.of(
                            "player", targetName,
                            "size", String.valueOf(result.playerSize())
                        ));
                errorMessage = null;
                islandSizeInput = "";
                Player current = store.getComponent(ref, Player.getComponentType());
                if (current != null) {
                    current.getPageManager().openCustomPage(ref, store, this);
                }
            })
            .exceptionally(ex -> {
                String message = ex.getCause() != null && ex.getCause().getMessage() != null
                    ? ex.getCause().getMessage()
                    : (ex.getMessage() == null ? "unknown" : ex.getMessage());
                if ("invalid_island_size_min".equals(message) || "invalid_island_size_max".equals(message)) {
                    errorMessage = invalidIslandSizeMessage(getLanguage(), raw);
                } else {
                    errorMessage = mapError(getLanguage(), message, targetName);
                }
                successMessage = null;
                Player current = store.getComponent(ref, Player.getComponentType());
                if (current != null) {
                    current.getPageManager().openCustomPage(ref, store, this);
                }
                return null;
            });
    }

    private void addTeamMember(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(teamMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        try {
            UUID memberUuid = plugin.getIslandService().resolveKnownPlayerUuid(member);
            plugin.getIslandService().addTeamMemberAsAdmin(targetUuid, memberUuid, member);
            successMessage = text(getLanguage(), "islands.admin.team_add.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            teamMemberInput = "";
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void removeTeamMember(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(teamMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        removeTeamMember(player, ref, store, member, true);
    }

    private void removeTeamMemberByRow(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String rawIndex) {
        TeamRowAction row = getTeamRowAction(rawIndex);
        if (row == null || !row.canRemove || row.actionName == null || row.actionName.isBlank()) {
            return;
        }

        removeTeamMember(player, ref, store, row.actionName, false);
    }

    private void removeTeamMember(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String member, boolean clearInput) {
        try {
            plugin.getIslandService().removeTeamMemberAsAdmin(targetUuid, member);
            successMessage = text(getLanguage(), "islands.admin.team_remove.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            if (clearInput) {
                teamMemberInput = "";
            }
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void makeLeader(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(teamMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        makeLeader(player, ref, store, member, true);
    }

    private void makeLeaderByRow(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String rawIndex) {
        TeamRowAction row = getTeamRowAction(rawIndex);
        if (row == null || !row.canMakeLeader || row.actionName == null || row.actionName.isBlank()) {
            return;
        }

        makeLeader(player, ref, store, row.actionName, false);
    }

    private void makeLeader(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String member, boolean clearInput) {
        try {
            plugin.getIslandService().makeLeaderAsAdmin(targetUuid, member);
            successMessage = text(getLanguage(), "islands.admin.makeleader.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            if (clearInput) {
                teamMemberInput = "";
            }
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void addCoopMember(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(coopMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        try {
            UUID memberUuid = plugin.getIslandService().resolveKnownPlayerUuid(member);
            plugin.getIslandService().addCoopMemberAsAdmin(targetUuid, memberUuid, member);
            successMessage = text(getLanguage(), "islands.admin.coop_add.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            coopMemberInput = "";
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void removeCoopMember(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(coopMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        removeCoopMember(player, ref, store, member, true);
    }

    private void removeCoopMemberByRow(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String rawIndex) {
        MemberRowAction row = getMemberRowAction(buildMemberRows(plugin.getIslandService().getPlayerIsland(targetUuid).getCoopNames().values()), rawIndex);
        if (row == null || row.actionName == null || row.actionName.isBlank()) {
            return;
        }

        removeCoopMember(player, ref, store, row.actionName, false);
    }

    private void removeCoopMember(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String member, boolean clearInput) {
        try {
            plugin.getIslandService().removeCoopMemberAsAdmin(targetUuid, member);
            successMessage = text(getLanguage(), "islands.admin.coop_remove.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            if (clearInput) {
                coopMemberInput = "";
            }
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void banPlayer(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(bannedMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        try {
            UUID memberUuid = plugin.getIslandService().resolveKnownPlayerUuid(member);
            plugin.getIslandService().banPlayerAsAdmin(targetUuid, memberUuid, member);
            successMessage = text(getLanguage(), "islands.admin.ban_add.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            bannedMemberInput = "";
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void unbanPlayer(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        String member = requirePlayerInput(bannedMemberInput, ref, store, player);
        if (member == null) {
            return;
        }

        unbanPlayer(player, ref, store, member, true);
    }

    private void unbanPlayerByRow(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String rawIndex) {
        IslandData island = plugin.getIslandService().getPlayerIsland(targetUuid);
        if (island == null) {
            handleFailure(player, ref, store, "no_island", targetName);
            return;
        }

        MemberRowAction row = getMemberRowAction(buildMemberRows(island.getBannedNames().values()), rawIndex);
        if (row == null || row.actionName == null || row.actionName.isBlank()) {
            return;
        }

        unbanPlayer(player, ref, store, row.actionName, false);
    }

    private void unbanPlayer(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String member, boolean clearInput) {
        try {
            plugin.getIslandService().unbanPlayerAsAdmin(targetUuid, member);
            successMessage = text(getLanguage(), "islands.admin.ban_remove.success", Map.of("player", member, "owner", targetName));
            errorMessage = null;
            if (clearInput) {
                bannedMemberInput = "";
            }
        } catch (Exception ex) {
            handleFailure(player, ref, store, ex.getMessage(), member);
            return;
        }

        reopen(player, ref, store);
    }

    private void teleportToIsland(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        plugin.getIslandService().resolveIslandTeleportTarget(targetUuid)
            .thenCompose(target -> TeleportUtil.teleport(playerRef, sourceWorld, target.world(), target.x(), target.y(), target.z(), target.pitch(), target.yaw()))
            .thenRun(() -> {
                successMessage = text(getLanguage(), "ui.admin_dashboard.islands.teleport_success", Map.of("player", targetName));
                errorMessage = null;
                Player current = store.getComponent(ref, Player.getComponentType());
                if (current != null) {
                    current.getPageManager().openCustomPage(ref, store, this);
                }
            })
            .exceptionally(ex -> {
                errorMessage = text(getLanguage(), "ui.admin_dashboard.islands.teleport_failed", Map.of("player", targetName));
                successMessage = null;
                Player current = store.getComponent(ref, Player.getComponentType());
                if (current != null) {
                    current.getPageManager().openCustomPage(ref, store, this);
                }
                return null;
            });
    }

    private void handleFailure(Player player,
                               Ref<EntityStore> ref,
                               Store<EntityStore> store,
                               String rawMessage,
                               String playerName) {
        errorMessage = mapError(getLanguage(), rawMessage, playerName);
        successMessage = null;
        reopen(player, ref, store);
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private String requirePlayerInput(String value,
                                      Ref<EntityStore> ref,
                                      Store<EntityStore> store,
                                      Player player) {
        String member = value == null ? "" : value.trim();
        if (!member.isBlank()) {
            return member;
        }

        errorMessage = text(getLanguage(), "ui.admin_dashboard.islands.error.player_required");
        successMessage = null;
        reopen(player, ref, store);
        return null;
    }

    private void buildTeamRows(UICommandBuilder commandBuilder, IslandData island, String language) {
        List<TeamRowAction> rows = buildTeamRows(island);
        String emptyMessage = text(language, "ui.admin_dashboard.islands.empty_team");
        for (int i = 0; i < MAX_TEAM_ROWS; i++) {
            boolean visible = i < rows.size() || (i == 0 && rows.isEmpty());
            commandBuilder.set("#IslandsTeamRow" + i + ".Visible", visible);
            if (!visible) {
                continue;
            }

            if (rows.isEmpty()) {
                commandBuilder.set("#IslandsTeamName" + i + ".Text", emptyMessage);
                commandBuilder.set("#IslandsTeamLeaderRowButton" + i + ".Visible", false);
                commandBuilder.set("#IslandsTeamRemoveRowButton" + i + ".Visible", false);
                continue;
            }

            TeamRowAction row = rows.get(i);
            commandBuilder.set("#IslandsTeamName" + i + ".Text", row.displayName);
            commandBuilder.set("#IslandsTeamLeaderRowButton" + i + ".Text", text(language, "ui.admin_dashboard.islands.team_makeleader"));
            commandBuilder.set("#IslandsTeamRemoveRowButton" + i + ".Text", text(language, "ui.admin_dashboard.islands.team_remove"));
            commandBuilder.set("#IslandsTeamLeaderRowButton" + i + ".Visible", row.canMakeLeader);
            commandBuilder.set("#IslandsTeamRemoveRowButton" + i + ".Visible", row.canRemove);
        }
    }

    private void buildMemberRows(UICommandBuilder commandBuilder,
                                 String prefix,
                                 int maxRows,
                                 List<MemberRowAction> values,
                                 String emptyMessage,
                                 String actionText) {
        List<MemberRowAction> displayValues = values == null ? new ArrayList<>() : values;
        for (int i = 0; i < maxRows; i++) {
            boolean visible = i < displayValues.size() || (i == 0 && displayValues.isEmpty());
            commandBuilder.set("#Islands" + prefix + "Row" + i + ".Visible", visible);
            if (!visible) {
                continue;
            }

            if (displayValues.isEmpty()) {
                commandBuilder.set("#Islands" + prefix + "Name" + i + ".Text", emptyMessage);
                commandBuilder.set("#Islands" + prefix + "RemoveRowButton" + i + ".Visible", false);
                continue;
            }

            MemberRowAction row = displayValues.get(i);
            commandBuilder.set("#Islands" + prefix + "Name" + i + ".Text", row.displayName == null ? "" : row.displayName);
            commandBuilder.set("#Islands" + prefix + "RemoveRowButton" + i + ".Text", actionText);
            commandBuilder.set("#Islands" + prefix + "RemoveRowButton" + i + ".Visible", row.canAct);
        }
    }

    private List<TeamRowAction> buildTeamRows(IslandData island) {
        List<TeamRowAction> rows = new ArrayList<>();
        if (island == null) {
            return rows;
        }

        String leader = resolveLeaderName(island);
        Set<String> seenNames = new HashSet<>();
        if (leader != null && !leader.isBlank()) {
            rows.add(new TeamRowAction(leader + " (leader)", leader, false, false));
            seenNames.add(leader.toLowerCase(Locale.ROOT));
        }

        for (String member : island.getMemberNames().values()) {
            if (member == null || member.isBlank()) {
                continue;
            }
            String key = member.toLowerCase(Locale.ROOT);
            if (seenNames.contains(key)) {
                continue;
            }
            rows.add(new TeamRowAction(member, member, true, true));
            seenNames.add(key);
        }

        return rows;
    }

    private List<MemberRowAction> buildMemberRows(Collection<String> values) {
        List<MemberRowAction> rows = new ArrayList<>();
        if (values == null) {
            return rows;
        }

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            rows.add(new MemberRowAction(value, value, true));
        }
        return rows;
    }

    private TeamRowAction getTeamRowAction(String rawIndex) {
        IslandData island = plugin.getIslandService().getPlayerIsland(targetUuid);
        if (island == null) {
            return null;
        }

        int rowIndex = parseRowIndex(rawIndex, MAX_TEAM_ROWS);
        List<TeamRowAction> rows = buildTeamRows(island);
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex);
    }

    private MemberRowAction getMemberRowAction(List<MemberRowAction> rows, String rawIndex) {
        int rowIndex = parseRowIndex(rawIndex, rows.size());
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex);
    }

    private int parseRowIndex(String rawIndex, int maxRows) {
        try {
            int rowIndex = Integer.parseInt(rawIndex);
            return rowIndex >= 0 && rowIndex < maxRows ? rowIndex : -1;
        } catch (Exception ex) {
            return -1;
        }
    }

    private static final class TeamRowAction {
        private final String displayName;
        private final String actionName;
        private final boolean canRemove;
        private final boolean canMakeLeader;

        private TeamRowAction(String displayName, String actionName, boolean canRemove, boolean canMakeLeader) {
            this.displayName = displayName;
            this.actionName = actionName;
            this.canRemove = canRemove;
            this.canMakeLeader = canMakeLeader;
        }
    }

    private static final class MemberRowAction {
        private final String displayName;
        private final String actionName;
        private final boolean canAct;

        private MemberRowAction(String displayName, String actionName, boolean canAct) {
            this.displayName = displayName;
            this.actionName = actionName;
            this.canAct = canAct;
        }
    }

    private String resolveLeaderName(IslandData island) {
        if (island == null) {
            return "";
        }

        UUID leaderUuid = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
        if (leaderUuid == null) {
            return island.getOwnerName() == null ? "" : island.getOwnerName();
        }
        if (leaderUuid.equals(island.getOwnerUuid())) {
            return island.getOwnerName() == null ? "" : island.getOwnerName();
        }

        // Check member names first (leader may still be in members if not yet removed)
        String memberName = island.getMemberNames().get(leaderUuid.toString());
        if (memberName != null && !memberName.isBlank()) {
            return memberName;
        }

        // Fallback: check online players
        com.hypixel.hytale.server.core.universe.PlayerRef online = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(leaderUuid);
        if (online != null && online.getUsername() != null && !online.getUsername().isBlank()) {
            return online.getUsername();
        }

        // Fallback: check known players repository
        String knownName = plugin.getIslandService().getAllKnownPlayers().get(leaderUuid);
        if (knownName != null && !knownName.isBlank()) {
            return knownName;
        }

        // Last resort: UUID string
        return leaderUuid.toString();
    }

    private String invalidIslandSizeMessage(String language, String value) {
        return text(language, "island_size.admin.invalid", Map.of(
            "value", value == null ? "" : value,
            "min", String.valueOf(plugin.getIslandService().getMinUpgradeableIslandZoneSize()),
            "max", String.valueOf(plugin.getIslandService().getMaxAllowedIslandZoneSize())
        ));
    }

    private String mapError(String language, String rawMessage, String playerName) {
        String message = rawMessage == null ? "unknown" : rawMessage;
        return switch (message) {
            case "target_not_found" -> text(language, "islands.admin.target_not_found", Map.of("player", playerName == null ? "" : playerName));
            case "no_island" -> text(language, "islands.admin.no_island", Map.of("player", targetName));
            case "member_not_found" -> text(language, "islands.admin.member_not_found", Map.of("player", playerName == null ? "" : playerName));
            case "already_has_island" -> text(language, "islands.admin.target_has_island", Map.of("player", playerName == null ? "" : playerName));
            case "already_member" -> text(language, "islands.admin.already_member", Map.of("player", playerName == null ? "" : playerName));
            case "already_coop" -> text(language, "islands.admin.already_coop", Map.of("player", playerName == null ? "" : playerName));
            case "not_coop" -> text(language, "islands.admin.not_coop", Map.of("player", playerName == null ? "" : playerName));
            case "already_banned" -> text(language, "islands.admin.already_banned", Map.of("player", playerName == null ? "" : playerName));
            case "not_banned" -> text(language, "islands.admin.not_banned", Map.of("player", playerName == null ? "" : playerName));
            case "cannot_ban_member" -> text(language, "islands.admin.cannot_ban_member", Map.of("player", playerName == null ? "" : playerName));
            case "name_invalid" -> text(language, "ui.admin_dashboard.islands.error.invalid_name", Map.of("max", "16"));
            default -> text(language, "island.leave.failed", Map.of("error", message));
        };
    }

    private String getLanguage() {
        String configuredLanguage = plugin.getConfigManager().getConfig().getLanguage();
        return configuredLanguage == null || configuredLanguage.isBlank()
            ? "en"
            : configuredLanguage.trim().toLowerCase(Locale.ROOT);
    }

    private String text(String language, String key) {
        return plugin.getMessageManager().get(key, language);
    }

    private String text(String language, String key, Map<String, String> vars) {
        return plugin.getMessageManager().get(key, vars, language);
    }

    private static String getDisplayValue(String draft, String persisted) {
        if (draft == null || draft.isBlank()) {
            return persisted;
        }
        return draft.trim();
    }

    private static boolean isDirty(String draft, String persisted) {
        if (draft == null || draft.isBlank()) {
            return false;
        }
        return !draft.trim().equals(persisted);
    }
}