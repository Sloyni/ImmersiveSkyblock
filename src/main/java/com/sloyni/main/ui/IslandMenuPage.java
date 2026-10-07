package com.sloyni.main.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.sloyni.main.addons.homes.HomeLocation;
import com.sloyni.main.addons.warps.IslandWarpData;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.islandwalls.IslandWallState;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandMemberPermission;
import com.sloyni.main.island.TeamInviteManager;
import com.sloyni.main.util.Chat;
import com.hypixel.hytale.server.core.Message;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class IslandMenuPage extends InteractiveCustomUIPage<IslandMenuPage.PageData> {
    private enum Tab {
        SETTINGS,
        TEAM,
        HOMES,
        WARPS,
        TOP
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .append(new KeyedCodec<>("TargetUuid", Codec.STRING), (o, v) -> o.targetUuid = v, o -> o.targetUuid).add()
            .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
            .append(new KeyedCodec<>("@LeftLanguage", Codec.STRING), (o, v) -> o.leftLanguage = v, o -> o.leftLanguage).add()
            .append(new KeyedCodec<>("@WallType", Codec.STRING), (o, v) -> o.wallType = v, o -> o.wallType).add()
            .append(new KeyedCodec<>("@WallSize", Codec.DOUBLE), (o, v) -> o.wallSize = v, o -> o.wallSize).add()
            .append(new KeyedCodec<>("@WallColor", Codec.STRING), (o, v) -> o.wallColor = v, o -> o.wallColor).add()
            .append(new KeyedCodec<>("@WarpSearchInput", Codec.STRING), (o, v) -> o.warpSearchInput = v, o -> o.warpSearchInput).add()
            .append(new KeyedCodec<>("@TopSearchInput", Codec.STRING), (o, v) -> o.topSearchInput = v, o -> o.topSearchInput).add()
            .build();

        public String action;
        public String targetUuid;
        public String param;
        public String leftLanguage;
        public String wallType;
        public Double wallSize;
        public String wallColor;
        public String warpSearchInput;
        public String topSearchInput;
    }

    private static final int WALL_MIN_SIZE = 2;
    private static final int WALL_MAX_SIZE = 16;
    private static final int MAX_TEAM_MEMBER_ROWS = 12;
    private static final int MAX_COOP_ROWS = 10;
    private static final int MAX_BAN_ROWS = 10;
    private static final int MAX_HOME_ROWS = 10;
    private static final int MAX_WARP_ROWS = 10;
    private static final int MAX_TOP_ROWS = 10;
    private static final int WARP_PAGE_SIZE = MAX_WARP_ROWS;

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;

    private Tab currentTab;
    private String selectedTargetUuid;
    private String pendingAction;
    private String pendingTargetUuid;
    private String draftWallType;
    private int draftWallSize;
    private List<String> draftWallColors;
    private boolean wallDirty;
    private String settingsErrorTitle;
    private String settingsErrorMessage;
    private String warpSearchInput;
    private String warpSearchQuery;
    private int warpPageIndex;
    private String topSearchInput;
    private String topSearchQuery;

    public static void open(Player player,
                            Ref<EntityStore> ref,
                            Store<EntityStore> store,
                            PlayerRef playerRef,
                            World sourceWorld,
                            ISkyblockPlugin plugin) {
        openWithTab(player, ref, store, playerRef, sourceWorld, plugin, Tab.SETTINGS);
    }

    public static void openTeam(Player player,
                                Ref<EntityStore> ref,
                                Store<EntityStore> store,
                                PlayerRef playerRef,
                                World sourceWorld,
                                ISkyblockPlugin plugin) {
        openWithTab(player, ref, store, playerRef, sourceWorld, plugin, Tab.TEAM);
    }

    public static void openHomes(Player player,
                                 Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 PlayerRef playerRef,
                                 World sourceWorld,
                                 ISkyblockPlugin plugin) {
        openWithTab(player, ref, store, playerRef, sourceWorld, plugin, Tab.HOMES);
    }

    public static void openWarps(Player player,
                                 Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 PlayerRef playerRef,
                                 World sourceWorld,
                                 ISkyblockPlugin plugin) {
        openWithTab(player, ref, store, playerRef, sourceWorld, plugin, Tab.WARPS);
    }

    public static void openTop(Player player,
                               Ref<EntityStore> ref,
                               Store<EntityStore> store,
                               PlayerRef playerRef,
                               World sourceWorld,
                               ISkyblockPlugin plugin) {
        openWithTab(player, ref, store, playerRef, sourceWorld, plugin, Tab.TOP);
    }

    private static void openWithTab(Player player,
                                    Ref<EntityStore> ref,
                                    Store<EntityStore> store,
                                    PlayerRef playerRef,
                                    World sourceWorld,
                                    ISkyblockPlugin plugin,
                                    Tab initialTab) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null || plugin == null) {
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            plugin.getIslandCommandHandler().handleIslandCommand(player, ref, store, playerRef, sourceWorld);
            return;
        }

        Tab resolvedTab = normalizeRequestedTab(plugin, initialTab);

        player.getPageManager().openCustomPage(ref, store,
            new IslandMenuPage(playerRef, sourceWorld, plugin, resolvedTab, "", "", "", "", 0, List.of(), false, "", "", "", "", 0, "", ""));
    }

    private IslandMenuPage(@Nonnull PlayerRef playerRef,
                           @Nonnull World sourceWorld,
                           @Nonnull ISkyblockPlugin plugin,
                           @Nonnull Tab tab,
                           @Nonnull String selectedTargetUuid,
                           @Nonnull String pendingAction,
                           @Nonnull String pendingTargetUuid,
                           @Nonnull String draftWallType,
                           int draftWallSize,
                           @Nonnull List<String> draftWallColors,
                           boolean wallDirty,
                           @Nonnull String settingsErrorTitle,
                           @Nonnull String settingsErrorMessage,
                           @Nonnull String warpSearchInput,
                           @Nonnull String warpSearchQuery,
                           int warpPageIndex,
                           @Nonnull String topSearchInput,
                           @Nonnull String topSearchQuery) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.currentTab = tab;
        this.selectedTargetUuid = selectedTargetUuid;
        this.pendingAction = pendingAction;
        this.pendingTargetUuid = pendingTargetUuid;
        this.draftWallType = draftWallType;
        this.draftWallSize = draftWallSize;
        this.draftWallColors = new ArrayList<>(draftWallColors);
        this.wallDirty = wallDirty;
        this.settingsErrorTitle = settingsErrorTitle;
        this.settingsErrorMessage = settingsErrorMessage;
        this.warpSearchInput = warpSearchInput;
        this.warpSearchQuery = warpSearchQuery;
        this.warpPageIndex = Math.max(0, warpPageIndex);
        this.topSearchInput = topSearchInput;
        this.topSearchQuery = topSearchQuery;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandMenu.ui");

        Player player = store.getComponent(ref, Player.getComponentType());
        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        bindStaticEvents(eventBuilder);

        if (island == null) {
            return;
        }

        currentTab = normalizeRequestedTab(plugin, currentTab);
        boolean homesEnabled = plugin.getAddonManager().isHomesEnabled();
        boolean warpsEnabled = plugin.getAddonManager().isWarpsEnabled();
        boolean levelsEnabled = plugin.getAddonManager().isLevelsEnabled();

        String islandDisplayName = plugin.getIslandService().getIslandDisplayName(island);
        int size = island.getZoneSize() > 0 ? island.getZoneSize() : plugin.getIslandService().getDefaultIslandZoneSize();
        int membersCount = 1 + island.getMemberCount();
        int coopCount = island.getCoopCount();
        int bannedCount = island.getBannedUuidStrings().size();
        int resetsLeft = plugin.getIslandService().getRemainingResets(playerRef.getUuid());

        String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
        if (language.isBlank()) {
            language = "en";
        }

        String levelPrefix = uiText(language, "ui.menu.level.prefix", "Lvl.");
        String topRankText = uiText(language, "ui.menu.rank.unranked", "Unranked");
        String levelText = levelPrefix + " 0";
        if (!levelsEnabled) {
            topRankText = uiText(language, "ui.menu.rank.levels_off", "Levels OFF");
            levelText = levelPrefix + " -";
        } else if (island.getOwnerUuid() != null) {
            var cached = plugin.getIslandValueService().getCachedIslandValue(island.getOwnerUuid());
            if (cached != null) {
                levelText = levelPrefix + " " + cached.level();
            }
            List<com.sloyni.main.addons.levels.IslandValueService.CachedIslandTopEntry> topEntries =
                plugin.getIslandValueService().getTopCachedIslands(0);
            int rank = 1;
            for (com.sloyni.main.addons.levels.IslandValueService.CachedIslandTopEntry entry : topEntries) {
                if (island.getOwnerUuid().equals(entry.ownerUuid())) {
                    topRankText = uiText(language, "ui.menu.rank.top", Map.of("rank", String.valueOf(rank)), "Top #{rank}");
                    break;
                }
                rank++;
            }
        }

        commandBuilder.set("#IslandSizeA.Text", String.valueOf(size));
        commandBuilder.set("#IslandSizeB.Text", String.valueOf(size));
        commandBuilder.set("#IslandName.Text", islandDisplayName);
        commandBuilder.set("#TopRank.Text", topRankText);
        commandBuilder.set("#IslandLevel.Text", levelText);
        commandBuilder.set("#MembersValue.Text", String.valueOf(membersCount));
        commandBuilder.set("#TableHeader.Visible", levelsEnabled);

        // Left panel labels
        commandBuilder.set("#WindowTitle.Text", uiText(language, "ui.menu.title", "MY ISLAND"));
        commandBuilder.set("#LabelMembers.Text", uiText(language, "ui.menu.label.members", "Members:"));
        commandBuilder.set("#LabelMobsKilled.Text", uiText(language, "ui.menu.label.mobs_killed", "Mobs killed:"));
        commandBuilder.set("#LabelTimePlayed.Text", uiText(language, "ui.menu.label.time_played", "Time played:"));
        commandBuilder.set("#LabelDeaths.Text", uiText(language, "ui.menu.label.deaths", "Deaths:"));
        commandBuilder.set("#BtnGoToIsland.Text", uiText(language, "ui.menu.btn.go_to_island", "GO TO ISLAND"));

        // Island statistics (from stats service)
        if (plugin.getIslandStatsService() != null && island.getOwnerUuid() != null) {
            com.sloyni.main.island.stats.IslandStats stats =
                plugin.getIslandStatsService().getStats(island.getOwnerUuid());
            commandBuilder.set("#MobsKilledValue.Text", String.valueOf(stats.getMobsKilled()));
            commandBuilder.set("#TimePlayedValue.Text", stats.getTimePlayedFormatted());
            commandBuilder.set("#DeathsValue.Text", String.valueOf(stats.getDeaths()));
        } else {
            commandBuilder.set("#MobsKilledValue.Text", "0");
            commandBuilder.set("#TimePlayedValue.Text", "0h");
            commandBuilder.set("#DeathsValue.Text", "0");
        }
        String lockText = island.isLocked()
            ? uiText(language, "ui.menu.btn.unlock", "UNLOCK")
            : uiText(language, "ui.menu.btn.lock", "LOCK");
        commandBuilder.set("#BtnLock.Text", lockText);

        applyRightPanelTranslations(commandBuilder, language);

        String selectedTab = switch (currentTab) {
            case SETTINGS -> "Settings";
            case TEAM -> "Team";
            case HOMES -> "Homes";
            case WARPS -> "Warps";
            case TOP -> "Top";
        };
        String rightTitle = switch (currentTab) {
            case SETTINGS -> uiText(language, "ui.menu.tab.settings", "SETTINGS");
            case TEAM -> uiText(language, "ui.menu.tab.team", "TEAM ACTIONS");
            case HOMES -> uiText(language, "ui.menu.tab.homes", "HOMES");
            case WARPS -> uiText(language, "ui.menu.tab.warps", "WARPS");
            case TOP -> uiText(language, "ui.menu.tab.top", "TOP ISLANDS");
        };

        commandBuilder.set("#TabNav.SelectedTab", selectedTab);
        commandBuilder.set("#RightTitle.Text", rightTitle);
        commandBuilder.set("#TabHomes.Visible", homesEnabled);
        commandBuilder.set("#TabWarps.Visible", warpsEnabled);
        commandBuilder.set("#TabTop.Visible", levelsEnabled);
        commandBuilder.set("#PanelSettings.Visible", currentTab == Tab.SETTINGS);
        commandBuilder.set("#PanelTeam.Visible", currentTab == Tab.TEAM);
        commandBuilder.set("#PanelHomes.Visible", homesEnabled && currentTab == Tab.HOMES);
        commandBuilder.set("#PanelWarps.Visible", warpsEnabled && currentTab == Tab.WARPS);
        commandBuilder.set("#PanelTop.Visible", levelsEnabled && currentTab == Tab.TOP);
        commandBuilder.set("#SettingsResetCountValue.Text", String.valueOf(Math.max(0, resetsLeft)));

        bindWallSettings(commandBuilder, island, language, player);

        boolean hasSettingsError = settingsErrorTitle != null && !settingsErrorTitle.isBlank();
        commandBuilder.set("#SettingsErrorPopup.Visible", hasSettingsError);
        commandBuilder.set("#SettingsErrorTitle.Text", hasSettingsError ? settingsErrorTitle : "");
        commandBuilder.set("#SettingsErrorMessage.Text", hasSettingsError ? safe(settingsErrorMessage) : "");

        boolean leader = island.isLeader(playerRef.getUuid());
        boolean leaderHasMembers = leader && island.getMemberCount() > 0;
        boolean canInviteMembers = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_INVITE);
        boolean canManageCoop = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_COOP);
        boolean canManageBans = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_BANS);
        boolean canExpel = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_EXPEL);
        boolean canManageWalls = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.MANAGE_WALLS)
            && player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island_walls");

        commandBuilder.set(
            "#BtnSettingsWallApply.Visible",
            wallDirty && plugin.getAddonManager().isIslandWallsEnabled() && canManageWalls
        );
        commandBuilder.set("#SettingsWallToggleRow.Visible", plugin.getAddonManager().isIslandWallsEnabled() && canManageWalls);
        if (plugin.getAddonManager().isIslandWallsEnabled() && island.getOwnerUuid() != null) {
            boolean wallsDisabled = plugin.getIslandService().isIslandWallsDisabled(island.getOwnerUuid());
            commandBuilder.set("#BtnWallsToggle.Text", wallsDisabled
                ? uiText(language, "ui.menu.walls.btn.enable", "Enable walls")
                : uiText(language, "ui.menu.walls.btn.disable", "Disable walls"));
        }

        boolean canChangeSettings = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.CHANGE_ISLAND_SETTINGS);
        commandBuilder.set("#SettingsItemDropRow.Visible", canChangeSettings);
        commandBuilder.set("#SettingsItemDropToggle.Value", canChangeSettings && island.isVisitorItemDropEnabled());

        commandBuilder.set("#SettingsMobDamageRow.Visible", canChangeSettings);
        commandBuilder.set("#SettingsMobDamageToggle.Value", canChangeSettings && island.isVisitorMobDamageEnabled());

        commandBuilder.set("#BtnResetIsland.Visible", leader);
        commandBuilder.set("#BtnLeaveIsland.Visible", !leaderHasMembers);
        if (leaderHasMembers) {
            commandBuilder.set("#SettingsLeaveHint.Text",
                uiText(language, "island.leave.leader_has_members",
                    "You are the island leader. Transfer leadership or remove all members before leaving."));
        }

        commandBuilder.set("#BtnTeamDefaultPermissions.Visible", leader);
        commandBuilder.set("#BtnTeamInviteOpen.Visible", canInviteMembers);
        commandBuilder.set("#BtnCoopOpen.Visible", canManageCoop);
        commandBuilder.set("#BtnBanOpen.Visible", canManageBans);
        commandBuilder.set("#BtnExpelOpen.Visible", canExpel);

        boolean hasConfirmation = hasPendingConfirmation();
        commandBuilder.set("#ConfirmPanel.Visible", hasConfirmation);
        commandBuilder.set("#ConfirmTitle.Text", hasConfirmation ? buildConfirmationTitle(island, language) : "");
        commandBuilder.set("#ConfirmMessage.Text", hasConfirmation ? buildConfirmationMessage(island, language) : "");

        buildLeftLangDropdown(commandBuilder, eventBuilder);
        buildTeamSnapshot(commandBuilder, eventBuilder, island, language);
        buildHomesSnapshot(commandBuilder, eventBuilder, island, language);
        buildWarpsSnapshot(commandBuilder, eventBuilder, island, language);
        buildTopSnapshot(commandBuilder, island, language);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.warpSearchInput != null) {
            warpSearchInput = data.warpSearchInput;
        }
        if (data.topSearchInput != null) {
            topSearchInput = data.topSearchInput;
        }

        String action = safe(data.action).trim();
        if (action.isEmpty()) {
            return;
        }

        World currentWorld = resolveWorld(player, store);
        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());

        switch (action) {
            case "Close" -> {
                closeMenu(ref, store);
                return;
            }
            case "TabSettings" -> {
                currentTab = Tab.SETTINGS;
                clearConfirmation();
                clearSettingsError();
                reopen(player, ref, store, Tab.SETTINGS);
                return;
            }
            case "TabTeam" -> {
                currentTab = Tab.TEAM;
                clearConfirmation();
                clearSettingsError();
                reopen(player, ref, store, Tab.TEAM);
                return;
            }
            case "TabHomes" -> {
                currentTab = Tab.HOMES;
                clearConfirmation();
                clearSettingsError();
                reopen(player, ref, store, Tab.HOMES);
                return;
            }
            case "TabWarps" -> {
                currentTab = Tab.WARPS;
                clearConfirmation();
                clearSettingsError();
                reopen(player, ref, store, Tab.WARPS);
                return;
            }
            case "TabTop" -> {
                currentTab = Tab.TOP;
                clearConfirmation();
                clearSettingsError();
                reopen(player, ref, store, Tab.TOP);
                return;
            }
            case "HomeSetOpen" -> {
                String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
                if (language.isBlank()) {
                    language = "en";
                }

                if (!plugin.getAddonManager().isHomesEnabled()) {
                    player.getPageManager().openCustomPage(ref, store,
                        new IslandInfoPopupPage(playerRef, resolveWorld(player, store), plugin,
                            uiText(language, "ui.menu.homes.section.title", "Homes"),
                            uiText(language, "ui.menu.homes.addon_disabled", "Homes addon is disabled."),
                            toPopupReturnTab(currentTab)));
                    return;
                }

                int homesUsed = plugin.getAddonManager().getHomesAddon().getService().getPlayerHomeCount(playerRef.getUuid());
                int homesMax = plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(playerRef.getUuid());
                if (homesUsed >= homesMax) {
                    player.getPageManager().openCustomPage(ref, store,
                        new IslandInfoPopupPage(playerRef, resolveWorld(player, store), plugin,
                            uiText(language, "ui.menu.homes.section.title", "Homes"),
                            uiText(language, "home.set.limit_reached",
                                Map.of("max", String.valueOf(homesMax)),
                                "You have reached your homes limit ({max})."),
                            toPopupReturnTab(currentTab)));
                    return;
                }

                player.getPageManager().openCustomPage(ref, store,
                    new IslandHomeNamePopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandHomeNamePopupPage.Mode.CREATE, ""));
                return;
            }
            case "HomeRenameOpen" -> {
                String homeId = safe(data.param).trim();
                if (homeId.isBlank()) {
                    reopen(player, ref, store, currentTab);
                    return;
                }

                player.getPageManager().openCustomPage(ref, store,
                    new IslandHomeNamePopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandHomeNamePopupPage.Mode.RENAME, homeId));
                return;
            }
            case "HomeGo", "HomeDelete" -> {
                boolean closed = executeAction(action, safe(data.param), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "WarpSearchInput" -> {
                return;
            }
            case "WarpSearch" -> {
                warpSearchQuery = safe(warpSearchInput).trim();
                warpPageIndex = 0;
                reopen(player, ref, store, Tab.WARPS);
                return;
            }
            case "WarpSearchClear" -> {
                warpSearchInput = "";
                warpSearchQuery = "";
                warpPageIndex = 0;
                reopen(player, ref, store, Tab.WARPS);
                return;
            }
            case "WarpPagePrev" -> {
                warpPageIndex = Math.max(0, warpPageIndex - 1);
                reopen(player, ref, store, Tab.WARPS);
                return;
            }
            case "WarpPageNext" -> {
                warpPageIndex = warpPageIndex + 1;
                reopen(player, ref, store, Tab.WARPS);
                return;
            }
            case "TopSearchInput" -> {
                return;
            }
            case "TopSearch" -> {
                topSearchQuery = safe(topSearchInput).trim();
                reopen(player, ref, store, Tab.TOP);
                return;
            }
            case "TopSearchClear" -> {
                topSearchInput = "";
                topSearchQuery = "";
                reopen(player, ref, store, Tab.TOP);
                return;
            }
            case "WarpSetOpen" -> {
                if (!ensureActionPermission("WarpSetOpen", player, ref, store, island)) {
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandWarpNamePopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandWarpNamePopupPage.Mode.CREATE, ""));
                return;
            }
            case "WarpRenameOpen" -> {
                if (!ensureActionPermission("WarpRenameOpen", player, ref, store, island)) {
                    return;
                }
                String currentWarpName = safe(data.param).trim();
                player.getPageManager().openCustomPage(ref, store,
                    new IslandWarpNamePopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandWarpNamePopupPage.Mode.RENAME, currentWarpName));
                return;
            }
            case "WarpVisit" -> {
                boolean closed = executeAction(action, safe(data.targetUuid), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "WarpToggle" -> {
                boolean closed = executeAction(action, safe(data.param), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "OpenInviteSelector" -> {
                if (!ensureActionPermission("OpenInviteSelector", player, ref, store, island)) {
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandTeamPlayerPickerPage(playerRef, resolveWorld(player, store), plugin,
                        IslandTeamPlayerPickerPage.Mode.INVITE));
                return;
            }
            case "OpenCoopSelector" -> {
                if (!ensureActionPermission("OpenCoopSelector", player, ref, store, island)) {
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandTeamPlayerPickerPage(playerRef, resolveWorld(player, store), plugin,
                        IslandTeamPlayerPickerPage.Mode.COOP_ADD));
                return;
            }
            case "OpenBanSelector" -> {
                if (!ensureActionPermission("OpenBanSelector", player, ref, store, island)) {
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandTeamPlayerPickerPage(playerRef, resolveWorld(player, store), plugin,
                        IslandTeamPlayerPickerPage.Mode.BAN_ADD));
                return;
            }
            case "OpenExpelSelector" -> {
                if (!ensureActionPermission("OpenExpelSelector", player, ref, store, island)) {
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandTeamPlayerPickerPage(playerRef, resolveWorld(player, store), plugin,
                        IslandTeamPlayerPickerPage.Mode.EXPEL));
                return;
            }
            case "OpenTeamDefaultPermissions" -> {
                if (island == null) {
                    openPermissionDeniedPopup(player, ref, store, null);
                    return;
                }
                if (!island.isLeader(playerRef.getUuid())) {
                    openPermissionDeniedPopup(player, ref, store, null);
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandTeamPermissionsPopupPage(
                        playerRef,
                        resolveWorld(player, store),
                        plugin,
                        IslandTeamPermissionsPopupPage.Mode.DEFAULT,
                        null
                    )
                );
                return;
            }
            case "OpenTeamMemberPermissions" -> {
                if (island == null || !island.isLeader(playerRef.getUuid())) {
                    openPermissionDeniedPopup(player, ref, store, null);
                    return;
                }

                UUID targetMember = parseUuid(safe(data.targetUuid));
                if (targetMember == null || !island.isMember(targetMember)) {
                    player.getPageManager().openCustomPage(ref, store,
                        new IslandTeamPermissionsPopupPage(
                            playerRef,
                            resolveWorld(player, store),
                            plugin,
                            IslandTeamPermissionsPopupPage.Mode.MEMBER,
                            targetMember,
                            uiText(sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef)), "ui.menu.permissions.error.member_not_found", "Member not found on this island.")
                        )
                    );
                    return;
                }

                player.getPageManager().openCustomPage(ref, store,
                    new IslandTeamPermissionsPopupPage(
                        playerRef,
                        resolveWorld(player, store),
                        plugin,
                        IslandTeamPermissionsPopupPage.Mode.MEMBER,
                        targetMember
                    )
                );
                return;
            }
            case "TeamKick" -> {
                boolean closed = executeAction("RemoveSelected", safe(data.targetUuid), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "TeamMakeLead" -> {
                boolean closed = executeAction("MakeLeaderSelected", safe(data.targetUuid), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "CoopRemoveRow" -> {
                boolean closed = executeAction("CoopRemoveSelected", safe(data.targetUuid), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "BanRemoveRow" -> {
                boolean closed = executeAction("BanRemoveSelected", safe(data.targetUuid), player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "SelectTarget" -> {
                selectedTargetUuid = safe(data.targetUuid);
                clearConfirmation();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "ClearTarget" -> {
                selectedTargetUuid = "";
                clearConfirmation();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "SetLanguage" -> {
                if (!safe(data.param).isBlank()) {
                    plugin.getIslandCommandHandler().handleIslandLanguageSetCommand(playerRef, safe(data.param));
                }
                reopen(player, ref, store, currentTab);
                return;
            }
            case "SetLeftLang" -> {
                String langValue = sanitizeLocaleToken(safe(data.leftLanguage));
                if (!langValue.isBlank()) {
                    plugin.getIslandCommandHandler().handleIslandLanguageSetCommand(playerRef, langValue);
                }
                reopen(player, ref, store, currentTab);
                return;
            }
            case "WallTypeChanged" -> {
                if (island != null) {
                    ensureWallDraftInitialized(island);
                    String normalizedType = resolveWallType(safe(data.wallType));
                    if (!normalizedType.equals(draftWallType)) {
                        draftWallType = normalizedType;
                        wallDirty = true;
                    }
                    if ("none".equals(draftWallType)) {
                        draftWallSize = 0;
                    }
                }
                clearSettingsError();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "WallSizeChanged" -> {
                if (island != null) {
                    ensureWallDraftInitialized(island);
                    int parsedSize = data.wallSize == null
                        ? draftWallSize
                        : (int) Math.round(data.wallSize);
                    int newSize = "none".equals(resolveWallType(draftWallType))
                        ? 0
                        : clamp(parsedSize, WALL_MIN_SIZE, WALL_MAX_SIZE);
                    if (newSize != draftWallSize) {
                        draftWallSize = newSize;
                        wallDirty = true;
                    }
                }
                clearSettingsError();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "WallColorAdd" -> {
                if (island != null) {
                    ensureWallDraftInitialized(island);
                    String normalizedColor = resolveWallColor(safe(data.wallColor));
                    if (!normalizedColor.isBlank()) {
                        draftWallColors.add(normalizedColor);
                        wallDirty = true;
                    }
                }
                clearSettingsError();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "WallColorClear" -> {
                if (!draftWallColors.isEmpty()) {
                    draftWallColors.clear();
                    wallDirty = true;
                }
                clearSettingsError();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "ApplyWalls" -> {
                if (!ensureActionPermission("ApplyWalls", player, ref, store, island)) {
                    return;
                }
                applyWallDraft(player, ref, store, island);
                return;
            }
            case "ToggleWalls" -> {
                if (!ensureActionPermission("ToggleWalls", player, ref, store, island)) {
                    return;
                }
                if (island != null && plugin.getAddonManager().isIslandWallsEnabled()) {
                    boolean currentlyDisabled = plugin.getIslandService().isIslandWallsDisabled(island.getOwnerUuid());
                    if (currentlyDisabled) {
                        plugin.getIslandService().enableIslandWalls(playerRef.getUuid());
                    } else {
                        plugin.getIslandService().disableIslandWalls(playerRef.getUuid());
                    }
                }
                reopen(player, ref, store, currentTab);
                return;
            }
            case "ToggleItemDrop" -> {
                if (!ensureActionPermission("ToggleItemDrop", player, ref, store, island)) {
                    return;
                }
                if (island != null) {
                    boolean currentlyEnabled = island.isVisitorItemDropEnabled();
                    plugin.getIslandService().setIslandVisitorItemDropEnabled(playerRef.getUuid(), !currentlyEnabled);
                }
                reopen(player, ref, store, currentTab);
                return;
            }
            case "ToggleMobDamage" -> {
                if (!ensureActionPermission("ToggleMobDamage", player, ref, store, island)) {
                    return;
                }
                if (island != null) {
                    boolean currentlyEnabled = island.isVisitorMobDamageEnabled();
                    plugin.getIslandService().setIslandVisitorMobDamageEnabled(playerRef.getUuid(), !currentlyEnabled);
                }
                reopen(player, ref, store, currentTab);
                return;
            }
            case "DismissSettingsError" -> {
                clearSettingsError();
                reopen(player, ref, store, currentTab);
                return;
            }
            case "RenameIsland" -> {
                if (!ensureActionPermission("RenameIsland", player, ref, store, island)) {
                    return;
                }
                World renameWorld = resolveWorld(player, store);
                player.getPageManager().openCustomPage(ref, store,
                    new IslandRenamePopupPage(playerRef, renameWorld, plugin));
                return;
            }
            case "RefreshLevel" -> {
                if (!ensureActionPermission("RefreshLevel", player, ref, store, island)) {
                    return;
                }
                if (island != null && plugin.getAddonManager().isLevelsEnabled()) {
                    Tab reopenTab = currentTab;
                    World reopenWorld = currentWorld;
                    sendMenuChat(playerRef, "ui.menu.level.refresh.calculating", "Refreshing island level...");
                    plugin.getIslandValueService().invalidateCache(playerRef.getUuid());
                    plugin.getIslandService().getIslandLevel(
                            playerRef.getUuid(),
                            plugin.getIslandValueService())
                        .thenAccept(result -> {
                            if (result != null) {
                                long level = result.level();
                                int levelValue = Math.max(1, plugin.getAddonManager().getLevelsAddon().getLevelValue());
                                long nextLevel = level + 1L;
                                double needed = (double) nextLevel * (double) levelValue;
                                long remaining = Math.max(0L, (long) Math.ceil(needed - result.netValue()));
                                sendMenuChat(playerRef, "ui.menu.level.refresh.result",
                                    Map.of("level", String.valueOf(level), "remaining", String.valueOf(remaining)),
                                    "Level: {level} | Remaining for next level: {remaining} pts");
                                String cacheKey = result.fromCache()
                                    ? "ui.menu.level.refresh.cached"
                                    : "ui.menu.level.refresh.fresh";
                                if (result.fromCache()) {
                                    sendMenuChat(playerRef, cacheKey, "Result returned from cache.");
                                } else {
                                    sendMenuChat(playerRef, cacheKey, "Result recalculated.");
                                }
                            }
                            if (reopenWorld != null) {
                                reopenWorld.execute(() -> reopen(player, ref, store, reopenTab));
                            } else {
                                reopen(player, ref, store, reopenTab);
                            }
                        })
                        .exceptionally((Throwable ex) -> {
                            sendMenuChat(playerRef, "ui.menu.level.refresh.failed",
                                Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage()),
                                "Failed to refresh level: {error}");
                            if (reopenWorld != null) {
                                reopenWorld.execute(() -> reopen(player, ref, store, reopenTab));
                            } else {
                                reopen(player, ref, store, reopenTab);
                            }
                            return null;
                        });
                } else {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "ResetIsland" -> {
                if (!ensureCanOpenResetFlow(player, ref, store, island)) {
                    return;
                }
                clearConfirmation();
                clearSettingsError();
                player.getPageManager().openCustomPage(ref, store,
                    new IslandActionConfirmPopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandActionConfirmPopupPage.ActionType.RESET_ISLAND));
                return;
            }
            case "LeaveIslandOpen" -> {
                if (!ensureCanOpenLeaveFlow(player, ref, store, island)) {
                    return;
                }
                clearConfirmation();
                clearSettingsError();
                player.getPageManager().openCustomPage(ref, store,
                    new IslandActionConfirmPopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandActionConfirmPopupPage.ActionType.LEAVE_ISLAND));
                return;
            }
            case "ConfirmPending" -> {
                String actionToExecute = pendingAction;
                String targetToUse = pendingTargetUuid;
                clearConfirmation();
                boolean closed = executeAction(actionToExecute, targetToUse, player, ref, store, currentWorld, island);
                if (!closed) {
                    reopen(player, ref, store, currentTab);
                }
                return;
            }
            case "CancelPending" -> {
                clearConfirmation();
                reopen(player, ref, store, currentTab);
                return;
            }
            default -> {
            }
        }

        if (requiresTargetSelection(action) && parseUuid(selectedTargetUuid) == null) {
            sendNoTargetSelectedUsage(action);
            reopen(player, ref, store, currentTab);
            return;
        }

        if (requiresConfirmation(action)) {
            queueConfirmation(action, selectedTargetUuid);
            reopen(player, ref, store, currentTab);
            return;
        }

        boolean closed = executeAction(action, selectedTargetUuid, player, ref, store, currentWorld, island);
        if (!closed) {
            reopen(player, ref, store, currentTab);
        }
    }

    private boolean executeAction(String action,
                                  String targetUuid,
                                  Player player,
                                  Ref<EntityStore> ref,
                                  Store<EntityStore> store,
                                  World currentWorld,
                                  IslandData island) {
        if (action == null || action.isBlank()) {
            return false;
        }

        if (!ensureActionPermission(action, player, ref, store, island)) {
            return true;
        }

        switch (action) {
            case "Go" -> {
                closeMenu(ref, store);
                plugin.getIslandCommandHandler().handleIslandCommand(player, ref, store, playerRef, currentWorld);
                return true;
            }
            case "Near" -> plugin.getIslandCommandHandler().handleIslandNearCommand(playerRef);
            case "Info" -> plugin.getIslandCommandHandler().handleIslandInfoCommand(playerRef);
            case "Level" -> plugin.getIslandCommandHandler().handleIslandLevelCommand(playerRef, null);
            case "Top" -> plugin.getIslandCommandHandler().handleIslandTopCommand(playerRef);
            case "Warps" -> plugin.getIslandCommandHandler().handleIslandWarpsCommand(playerRef);
            case "Resets" -> plugin.getResetCommandHandler().handleIslandResetsCommand(playerRef);
            case "Help" -> plugin.getIslandCommandHandler().handleIslandHelpCommand(playerRef);
            case "SetSpawn" -> {
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null) {
                    int x = (int) Math.floor(transform.getPosition().x());
                    int y = (int) Math.floor(transform.getPosition().y());
                    int z = (int) Math.floor(transform.getPosition().z());
                    float pitch = transform.getRotation().pitch();
                    float yaw = transform.getRotation().yaw();
                    plugin.getIslandCommandHandler().handleIslandSetSpawnCommand(playerRef, currentWorld, x, y, z, pitch, yaw);
                }
            }
            case "HomeGo" -> {
                String homeId = safe(targetUuid).trim();
                if (homeId.isBlank()) {
                    return false;
                }
                closeMenu(ref, store);
                plugin.getIslandCommandHandler().handleIslandHomeGoCommand(playerRef, currentWorld, homeId);
                return true;
            }
            case "HomeDelete" -> {
                String homeId = safe(targetUuid).trim();
                if (homeId.isBlank()) {
                    return false;
                }
                plugin.getIslandCommandHandler().handleIslandHomeDeleteCommand(playerRef, homeId);
            }
            case "WarpVisit" -> {
                String targetName = safe(targetUuid).trim();
                if (targetName.isBlank()) {
                    return false;
                }
                closeMenu(ref, store);
                plugin.getIslandCommandHandler().handleIslandVisitCommand(playerRef, currentWorld, targetName);
                return true;
            }
            case "WarpToggle" -> {
                boolean enable = "enable".equalsIgnoreCase(safe(targetUuid).trim());
                plugin.getIslandCommandHandler().handleIslandWarpStateCommand(playerRef, enable);
            }
            case "WarpDelete" -> plugin.getIslandCommandHandler().handleIslandWarpDeleteCommand(playerRef);
            case "Lock" -> {
                if (island != null) {
                    plugin.getIslandCommandHandler().handleIslandLockCommand(playerRef, !island.isLocked());
                }
            }
            case "ResetIsland" -> {
                return performResetIsland(player, ref, store, currentWorld, island);
            }
            case "TeamShow" -> plugin.getIslandCommandHandler().handleTeamShowCommand(playerRef);
            case "CoopList" -> plugin.getIslandCommandHandler().handleCoopListCommand(playerRef);
            case "BanList" -> plugin.getIslandCommandHandler().handleIslandBanListCommand(playerRef);
            case "TeamLeave" -> {
                if (!ensureCanOpenLeaveFlow(player, ref, store, island)) {
                    return false;
                }
                player.getPageManager().openCustomPage(ref, store,
                    new IslandActionConfirmPopupPage(playerRef, resolveWorld(player, store), plugin,
                        IslandActionConfirmPopupPage.ActionType.LEAVE_ISLAND));
                return true;
            }
            case "AcceptInvite" -> {
                TeamInviteManager.Invite invite = plugin.getTeamInviteManager().getPendingInvite(playerRef.getUuid());
                if (invite != null && invite.inviterName() != null && !invite.inviterName().isBlank()) {
                    plugin.getIslandCommandHandler().handleTeamAcceptCommand(playerRef, invite.inviterName());
                } else {
                    plugin.getIslandCommandHandler().handleTeamUsageCommand(playerRef);
                }
            }
            case "InviteSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleTeamInviteCommand(playerRef, targetName);
            }
            case "CoopAddSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleCoopEnableCommand(playerRef, targetName);
            }
            case "BanAddSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleIslandBanAddCommand(playerRef, targetName);
            }
            case "RemoveSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleTeamRemoveCommand(playerRef, targetName);
            }
            case "MakeLeaderSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleMakeLeaderCommand(playerRef, targetName);
            }
            case "CoopRemoveSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleCoopDisableCommand(playerRef, targetName);
            }
            case "BanRemoveSelected" -> {
                String targetName = resolveTargetName(parseUuid(targetUuid), island);
                if (targetName == null) {
                    sendNoTargetSelectedUsage(action);
                    return false;
                }
                plugin.getIslandCommandHandler().handleIslandBanRemoveCommand(playerRef, targetName);
            }
            default -> {
                return false;
            }
        }
        return false;
    }

    private void bindStaticEvents(UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RightContainer #CloseButton", EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabSettings", EventData.of("Action", "TabSettings"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabTeam", EventData.of("Action", "TabTeam"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabHomes", EventData.of("Action", "TabHomes"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabWarps", EventData.of("Action", "TabWarps"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabTop", EventData.of("Action", "TabTop"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnGoToIsland", EventData.of("Action", "Go"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSetSpawn", EventData.of("Action", "SetSpawn"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnLock", EventData.of("Action", "Lock"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnResetIsland", EventData.of("Action", "ResetIsland"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnLeaveIsland", EventData.of("Action", "LeaveIslandOpen"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsWallTypeDropdown",
            EventData.of("Action", "WallTypeChanged").append("@WallType", "#SettingsWallTypeDropdown.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsWallSizeField",
            EventData.of("Action", "WallSizeChanged").append("@WallSize", "#SettingsWallSizeField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSettingsWallColorAdd",
            EventData.of("Action", "WallColorAdd").append("@WallColor", "#SettingsWallColorDropdown.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSettingsWallColorClear", EventData.of("Action", "WallColorClear"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSettingsWallApply", EventData.of("Action", "ApplyWalls"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWallsToggle", EventData.of("Action", "ToggleWalls"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsItemDropToggle", EventData.of("Action", "ToggleItemDrop"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsMobDamageToggle", EventData.of("Action", "ToggleMobDamage"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSettingsErrorClose", EventData.of("Action", "DismissSettingsError"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTeamInviteOpen", EventData.of("Action", "OpenInviteSelector"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTeamDefaultPermissions", EventData.of("Action", "OpenTeamDefaultPermissions"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCoopOpen", EventData.of("Action", "OpenCoopSelector"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnBanOpen", EventData.of("Action", "OpenBanSelector"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnExpelOpen", EventData.of("Action", "OpenExpelSelector"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHomesSetOpen", EventData.of("Action", "HomeSetOpen"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpSetOpen", EventData.of("Action", "WarpSetOpen"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpsSearch", EventData.of("Action", "WarpSearch").append("@WarpSearchInput", "#WarpsSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpsSearchClear", EventData.of("Action", "WarpSearchClear"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpsPrevPage", EventData.of("Action", "WarpPagePrev"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpsNextPage", EventData.of("Action", "WarpPageNext"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WarpsSearchField", EventData.of("Action", "WarpSearchInput").append("@WarpSearchInput", "#WarpsSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTopSearch", EventData.of("Action", "TopSearch").append("@TopSearchInput", "#TopSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTopSearchClear", EventData.of("Action", "TopSearchClear"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TopSearchField", EventData.of("Action", "TopSearchInput").append("@TopSearchInput", "#TopSearchField.Value"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnConfirmYes", EventData.of("Action", "ConfirmPending"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnConfirmNo", EventData.of("Action", "CancelPending"), false);

        // Left-panel icon buttons
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RenameIslandIcon", EventData.of("Action", "RenameIsland"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshLevelIcon", EventData.of("Action", "RefreshLevel"), false);

        // Left-panel language dropdown
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LeftLangDropdown",
            EventData.of("Action", "SetLeftLang").append("@LeftLanguage", "#LeftLangDropdown.Value"), false);
    }

    /**
     * Populates the compact language dropdown (#LeftLangDropdown) in the left panel.
     * Uses {@link DropdownEntryInfo} to build entries dynamically (no pre-declared
     * DropdownEntry elements in the .ui file needed).
     */
    private void buildLeftLangDropdown(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        List<String> locales = plugin.getConfigManager().getConfig().getSettings().getEnabledLanguages();
        String currentLanguage = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));

        if (locales == null || locales.isEmpty()) {
            commandBuilder.set("#LeftLangDropdown.Entries", new DropdownEntryInfo[0]);
            return;
        }

        java.util.List<DropdownEntryInfo> entries = new java.util.ArrayList<>();
        Set<String> seenValues = new LinkedHashSet<>();
        String firstValue = "";
        boolean hasCurrent = false;

        for (String locale : locales) {
            if (locale == null || locale.isBlank()) {
                continue;
            }
            String normalized = sanitizeLocaleToken(locale);
            if (normalized.isBlank() || !seenValues.add(normalized)) {
                continue;
            }
            if (firstValue.isBlank()) {
                firstValue = normalized;
            }
            if (normalized.equalsIgnoreCase(currentLanguage)) {
                hasCurrent = true;
            }
            String label = plugin.getLanguageService().langLabel(normalized);
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(label), normalized));
        }

        commandBuilder.set("#LeftLangDropdown.Entries", entries.toArray(new DropdownEntryInfo[0]));
        if (entries.isEmpty()) {
            commandBuilder.set("#LeftLangDropdown.Value", "");
            return;
        }

        String selectedValue = hasCurrent ? currentLanguage : firstValue;
        commandBuilder.set("#LeftLangDropdown.Value", selectedValue);
    }

    private String sanitizeLocaleToken(String locale) {
        if (locale == null) {
            return "";
        }

        String raw = locale.trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '-'
                || c == '_') {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private void applyRightPanelTranslations(UICommandBuilder commandBuilder, String language) {
        commandBuilder.set("#TabSettings.TooltipText", uiText(language, "ui.menu.tab.settings", "SETTINGS"));
        commandBuilder.set("#TabTeam.TooltipText", uiText(language, "ui.menu.tab.team", "TEAM ACTIONS"));
        commandBuilder.set("#TabHomes.TooltipText", uiText(language, "ui.menu.tab.homes", "HOMES"));
        commandBuilder.set("#TabWarps.TooltipText", uiText(language, "ui.menu.tab.warps", "WARPS"));
        commandBuilder.set("#TabTop.TooltipText", uiText(language, "ui.menu.tab.top", "TOP ISLANDS"));

        commandBuilder.set("#ConfirmTitle.Text", uiText(language, "ui.menu.confirm.title.default", "Confirm action"));
        commandBuilder.set("#ConfirmMessage.Text", uiText(language, "ui.menu.confirm.message.default", "Please confirm this action."));
        commandBuilder.set("#BtnConfirmYes.Text", uiText(language, "ui.menu.confirm.btn.yes", "Confirm"));
        commandBuilder.set("#BtnConfirmNo.Text", uiText(language, "ui.menu.confirm.btn.no", "Cancel"));

        commandBuilder.set("#SettingsSectionTitle.Text", uiText(language, "ui.menu.settings.section.title", "Settings"));
        commandBuilder.set("#SettingsSectionDescription.Text", uiText(language, "ui.menu.settings.section.description", "Configure your island settings."));
        commandBuilder.set("#SettingsSetSpawnLabel.Text", uiText(language, "ui.menu.settings.set_spawn.label", "Set Spawn"));
        commandBuilder.set("#BtnSetSpawn.Text", uiText(language, "ui.menu.settings.btn.set_spawn", "Set spawn"));
        commandBuilder.set("#SettingsItemDropLabel.Text", uiText(language, "ui.menu.settings.item_drop.label", "Visitor item drop"));
        commandBuilder.set("#SettingsMobDamageLabel.Text", uiText(language, "ui.menu.settings.mob_damage.label", "Visitor mob damage"));
        commandBuilder.set("#SettingsWallToggleLabel.Text", uiText(language, "ui.menu.settings.wall_toggle.label", "Island walls"));
        commandBuilder.set("#SettingsWallsTitle.Text", uiText(language, "ui.menu.settings.walls.title", "Walls"));
        commandBuilder.set("#SettingsWallsDescription.Text", uiText(language, "ui.menu.settings.walls.description", "Choose type, size and colors."));
        commandBuilder.set("#BtnSettingsWallApply.Text", uiText(language, "ui.menu.settings.walls.btn.apply", "Apply modifications"));
        commandBuilder.set("#SettingsWallTypeLabel.Text", uiText(language, "ui.menu.settings.walls.type_label", "Wall Type"));
        commandBuilder.set("#SettingsWallSizeLabel.Text", uiText(language, "ui.menu.settings.walls.size_label", "Wall Size"));
        commandBuilder.set("#SettingsWallColorsLabel.Text", uiText(language, "ui.menu.settings.walls.colors_label", "Walls Colors"));
        commandBuilder.set("#BtnSettingsWallColorAdd.Text", uiText(language, "ui.menu.settings.walls.colors.btn.add", "Add"));
        commandBuilder.set("#BtnSettingsWallColorClear.Text", uiText(language, "ui.menu.settings.walls.colors.btn.clear", "Clear"));
        commandBuilder.set("#SettingsWallColorsListLabel.Text", uiText(language, "ui.menu.settings.walls.colors.list_label", "Selected colors cycle"));
        commandBuilder.set("#SettingsResetTitle.Text", uiText(language, "ui.menu.settings.reset.title", "Reset"));
        commandBuilder.set("#SettingsResetDescription.Text", uiText(language, "ui.menu.settings.reset.description", "Delete island after all safety checks pass."));
        commandBuilder.set("#SettingsResetCountLabel.Text", uiText(language, "ui.menu.settings.reset.count_label", "Resets remaining"));
        commandBuilder.set("#BtnResetIsland.Text", uiText(language, "ui.menu.settings.btn.reset_island", "Reset island"));
        commandBuilder.set("#SettingsLeaveTitle.Text", uiText(language, "ui.menu.settings.leave.title", "Leave island"));
        commandBuilder.set("#SettingsLeaveDescription.Text", uiText(language, "ui.menu.settings.leave.description", "Leave your current island and return to lobby."));
        commandBuilder.set("#SettingsLeaveHint.Text", uiText(language, "ui.menu.settings.leave.hint", "You can leave only when requirements are met."));
        commandBuilder.set("#BtnLeaveIsland.Text", uiText(language, "ui.menu.settings.btn.leave_island", "Leave island"));

        commandBuilder.set("#TeamSectionTitle.Text", uiText(language, "ui.menu.team.section.title", "Team"));
        commandBuilder.set("#TeamSectionDescription.Text", uiText(language, "ui.menu.team.section.description", "Manage members and roles."));
        commandBuilder.set("#BtnTeamDefaultPermissions.Text", uiText(language, "ui.menu.team.btn.default_permissions", "Default permissions"));
        commandBuilder.set("#BtnTeamDefaultPermissions.TooltipText", uiText(language, "ui.menu.team.btn.default_permissions.tooltip", "Edit default member permissions"));
        commandBuilder.set("#BtnTeamInviteOpen.Text", uiText(language, "ui.menu.team.btn.invite", "Invite"));
        commandBuilder.set("#TeamHeaderName.Text", uiText(language, "ui.menu.team.header.name", "Name"));
        commandBuilder.set("#TeamHeaderRole.Text", uiText(language, "ui.menu.team.header.role", "Role"));
        commandBuilder.set("#TeamHeaderActions.Text", uiText(language, "ui.menu.team.header.actions", "Actions"));
        commandBuilder.set("#TeamEmptyLabel.Text", uiText(language, "ui.menu.team.empty", "No team members."));
        commandBuilder.set("#TeamCoopTitle.Text", uiText(language, "ui.menu.team.coop.title", "Coop"));
        commandBuilder.set("#TeamCoopDescription.Text", uiText(language, "ui.menu.team.coop.description", "Manage players with coop access."));
        commandBuilder.set("#BtnCoopOpen.Text", uiText(language, "ui.menu.team.btn.coop_add", "Add coop"));
        commandBuilder.set("#CoopHeaderName.Text", uiText(language, "ui.menu.team.coop.header.name", "Name"));
        commandBuilder.set("#CoopHeaderAction.Text", uiText(language, "ui.menu.team.coop.header.action", "Action"));
        commandBuilder.set("#CoopEmptyLabel.Text", uiText(language, "ui.menu.team.coop.empty", "No coop players."));
        commandBuilder.set("#TeamBanTitle.Text", uiText(language, "ui.menu.team.ban.title", "Ban"));
        commandBuilder.set("#TeamBanDescription.Text", uiText(language, "ui.menu.team.ban.description", "Expel or ban players from your island."));
        commandBuilder.set("#BtnExpelOpen.Text", uiText(language, "ui.menu.team.btn.expel", "Expel"));
        commandBuilder.set("#BtnBanOpen.Text", uiText(language, "ui.menu.team.btn.ban", "Ban"));
        commandBuilder.set("#BanHeaderName.Text", uiText(language, "ui.menu.team.ban.header.name", "Name"));
        commandBuilder.set("#BanHeaderAction.Text", uiText(language, "ui.menu.team.ban.header.action", "Action"));
        commandBuilder.set("#BanEmptyLabel.Text", uiText(language, "ui.menu.team.ban.empty", "No banned players."));

        commandBuilder.set("#HomesSectionTitle.Text", uiText(language, "ui.menu.homes.section.title", "Homes"));
        commandBuilder.set("#HomesSectionDescription.Text", uiText(language, "ui.menu.homes.section.description", "Manage personal homes on your island."));
        commandBuilder.set("#HomesHeaderHome.Text", uiText(language, "ui.menu.homes.header.home", "Home"));
        commandBuilder.set("#HomesHeaderActions.Text", uiText(language, "ui.menu.homes.header.actions", "Actions"));

        commandBuilder.set("#WarpsSectionTitle.Text", uiText(language, "ui.menu.warps.section.title", "Warps"));
        commandBuilder.set("#WarpsSectionDescription.Text", uiText(language, "ui.menu.warps.section.description", "Manage island warps and discover others."));
        commandBuilder.set("#BtnWarpsSearch.Text", uiText(language, "ui.menu.warps.search.btn", "Search"));
        commandBuilder.set("#BtnWarpsSearchClear.Text", uiText(language, "ui.menu.warps.search.btn.clear", "Clear"));
        commandBuilder.set("#WarpsHeaderWarp.Text", uiText(language, "ui.menu.warps.header.warp", "Warp"));
        commandBuilder.set("#WarpsHeaderVisits.Text", uiText(language, "ui.menu.warps.header.visits", "Visits"));
        commandBuilder.set("#WarpsHeaderAction.Text", uiText(language, "ui.menu.warps.header.action", "Action"));

        commandBuilder.set("#TopSectionTitle.Text", uiText(language, "ui.menu.top.section.title", "Top islands"));
        commandBuilder.set("#TopSectionDescription.Text", uiText(language, "ui.menu.top.section.description", "Browse highest-value islands."));
        commandBuilder.set("#BtnTopSearch.Text", uiText(language, "ui.menu.top.search.btn", "Search"));
        commandBuilder.set("#BtnTopSearchClear.Text", uiText(language, "ui.menu.top.search.btn.clear", "Clear"));
        commandBuilder.set("#TopHeaderRank.Text", uiText(language, "ui.menu.top.header.rank", "Rank"));
        commandBuilder.set("#TopHeaderIsland.Text", uiText(language, "ui.menu.top.header.island", "Island"));
        commandBuilder.set("#TopHeaderLevel.Text", uiText(language, "ui.menu.top.header.level", "Level"));
    }

    private String uiText(String language, String key, String fallback) {
        String normalized = sanitizeLocaleToken(language);
        if (normalized.isBlank()) {
            normalized = "en";
        }

        String value = plugin.getMessageManager().getExact(key, normalized);
        if (value == null || value.isBlank()) {
            value = plugin.getMessageManager().getExact(key, "en");
        }
        if (value == null || value.isBlank()) {
            value = fallback;
        }
        return safe(value);
    }

    private String uiText(String language, String key, Map<String, String> vars, String fallback) {
        String value = uiText(language, key, fallback);
        if (vars == null || vars.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            value = value.replace("{" + safe(entry.getKey()) + "}", safe(entry.getValue()));
        }
        return value;
    }

    private void sendMenuChat(PlayerRef playerRef, String key, String fallback) {
        sendMenuChat(playerRef, key, Map.of(), fallback);
    }

    private void sendMenuChat(PlayerRef playerRef, String key, Map<String, String> vars, String fallback) {
        String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
        if (language.isBlank()) {
            language = "en";
        }

        // If the player's language file has the key, use it. Otherwise fall back to English translation
        String templateForPlayerLang = plugin.getMessageManager().getExact(key, language);
        String templateEnglish = plugin.getMessageManager().getExact(key, "en");

        if (templateForPlayerLang != null && !templateForPlayerLang.isBlank()) {
            if (vars == null || vars.isEmpty()) {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, key));
            } else {
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, key, vars));
            }
            return;
        }

        if (templateEnglish != null && !templateEnglish.isBlank()) {
            // player's language missing key but English has it — send English localized Message
            if (vars == null || vars.isEmpty()) {
                Chat.send(playerRef, plugin.trForLanguage("en", key));
            } else {
                Chat.send(playerRef, plugin.trForLanguage("en", key, vars));
            }
            return;
        }

        // Fallback: use the provided fallback text, apply simple var replacement and color
        String value = fallback == null ? "" : fallback;
        if (vars != null && !vars.isEmpty()) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                value = value.replace("{" + safe(entry.getKey()) + "}", safe(entry.getValue()));
            }
        }
        String color = plugin.getMessagesConfig() == null ? null : plugin.getMessagesConfig().getMessageColor();
        if (color == null) {
            Chat.send(playerRef, Message.raw(safe(value)));
        } else {
            Chat.send(playerRef, Message.raw(safe(value)).color(color));
        }
    }

    private void buildTeamSnapshot(UICommandBuilder commandBuilder,
                                   UIEventBuilder eventBuilder,
                                   IslandData island,
                                   String language) {
        boolean leader = island != null && island.isLeader(playerRef.getUuid());
        boolean canRemoveMembers = island != null && island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_REMOVE_MEMBER);
        boolean canTransferLeadership = island != null && island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_TRANSFER_LEADERSHIP);
        boolean canEditMemberPermissions = leader;
        boolean canManageCoop = island != null && island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_COOP);
        boolean canManageBans = island != null && island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.TEAM_MANAGE_BANS);

        List<UUID> teamUuids = new ArrayList<>();
        List<String> teamNames = new ArrayList<>();
        List<String> teamRoles = new ArrayList<>();

        UUID leaderUuid = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
        if (leaderUuid != null) {
            teamUuids.add(leaderUuid);
            teamNames.add(resolvePlayerName(leaderUuid, island));
            teamRoles.add("Lead");
        }

        for (Map.Entry<String, String> entry : island.getMemberNames().entrySet()) {
            UUID uuid = parseUuid(entry.getKey());
            if (uuid == null || uuid.equals(leaderUuid)) {
                continue;
            }
            teamUuids.add(uuid);
            teamNames.add(safe(entry.getValue()));
            teamRoles.add("Member");
        }

        commandBuilder.set("#TeamTableHeader.Visible", !teamUuids.isEmpty());
        commandBuilder.set("#TeamEmptyLabel.Visible", teamUuids.isEmpty());
        for (int i = 0; i < MAX_TEAM_MEMBER_ROWS; i++) {
            if (i < teamUuids.size()) {
                UUID uuid = teamUuids.get(i);
                String role = teamRoles.get(i);
                boolean manageable = !"Lead".equals(role);
                boolean showPermissionButton = manageable && canEditMemberPermissions;
                boolean showKickButton = manageable && canRemoveMembers;
                boolean showMakeLeadButton = manageable && canTransferLeadership;
                String roleLabel = "Lead".equals(role)
                    ? uiText(language, "ui.menu.team.role.lead", "Lead")
                    : uiText(language, "ui.menu.team.role.member", "Member");

                commandBuilder.set("#TeamMemberRow" + i + ".Visible", true);
                commandBuilder.set("#TeamMemberName" + i + ".Text", teamNames.get(i));
                commandBuilder.set("#TeamMemberRole" + i + ".Text", roleLabel);
                commandBuilder.set("#BtnTeamPermissions" + i + ".Visible", showPermissionButton);
                commandBuilder.set("#BtnTeamKick" + i + ".Visible", showKickButton);
                commandBuilder.set("#BtnTeamMakeLead" + i + ".Visible", showMakeLeadButton);
                commandBuilder.set("#BtnTeamPermissions" + i + ".Text", uiText(language, "ui.menu.team.btn.permissions", "Permissions"));
                commandBuilder.set(
                    "#BtnTeamPermissions" + i + ".TooltipText",
                    uiText(
                        language,
                        "ui.menu.team.btn.permissions.tooltip",
                        Map.of("player", teamNames.get(i)),
                        "Edit permissions for {player}"
                    )
                );
                commandBuilder.set("#BtnTeamKick" + i + ".Text", uiText(language, "ui.menu.team.btn.remove", "Remove"));
                commandBuilder.set("#BtnTeamMakeLead" + i + ".Text", uiText(language, "ui.menu.team.btn.make_lead", "Make lead"));

                if (showPermissionButton) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#BtnTeamPermissions" + i,
                        EventData.of("Action", "OpenTeamMemberPermissions").append("TargetUuid", uuid.toString()),
                        false
                    );
                }
                if (showKickButton) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#BtnTeamKick" + i,
                        EventData.of("Action", "TeamKick").append("TargetUuid", uuid.toString()),
                        false
                    );
                }
                if (showMakeLeadButton) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#BtnTeamMakeLead" + i,
                        EventData.of("Action", "TeamMakeLead").append("TargetUuid", uuid.toString()),
                        false
                    );
                }
            } else {
                commandBuilder.set("#TeamMemberRow" + i + ".Visible", false);
            }
        }

        List<Map.Entry<UUID, String>> coopEntries = new ArrayList<>();
        for (Map.Entry<String, String> entry : island.getCoopNames().entrySet()) {
            UUID uuid = parseUuid(entry.getKey());
            if (uuid != null) {
                coopEntries.add(Map.entry(uuid, safe(entry.getValue())));
            }
        }

        commandBuilder.set("#CoopTableHeader.Visible", !coopEntries.isEmpty());
        commandBuilder.set("#CoopEmptyLabel.Visible", coopEntries.isEmpty());
        for (int i = 0; i < MAX_COOP_ROWS; i++) {
            if (i < coopEntries.size()) {
                Map.Entry<UUID, String> entry = coopEntries.get(i);
                commandBuilder.set("#CoopRow" + i + ".Visible", true);
                commandBuilder.set("#CoopName" + i + ".Text", entry.getValue());
                commandBuilder.set("#BtnCoopRemove" + i + ".Visible", canManageCoop);
                commandBuilder.set("#BtnCoopRemove" + i + ".Text", uiText(language, "ui.menu.team.btn.remove", "Remove"));
                if (canManageCoop) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#BtnCoopRemove" + i,
                        EventData.of("Action", "CoopRemoveRow").append("TargetUuid", entry.getKey().toString()),
                        false
                    );
                }
            } else {
                commandBuilder.set("#CoopRow" + i + ".Visible", false);
            }
        }

        List<Map.Entry<UUID, String>> banEntries = new ArrayList<>();
        for (Map.Entry<String, String> entry : island.getBannedNames().entrySet()) {
            UUID uuid = parseUuid(entry.getKey());
            if (uuid != null) {
                banEntries.add(Map.entry(uuid, safe(entry.getValue())));
            }
        }

        commandBuilder.set("#BanTableHeader.Visible", !banEntries.isEmpty());
        commandBuilder.set("#BanEmptyLabel.Visible", banEntries.isEmpty());
        for (int i = 0; i < MAX_BAN_ROWS; i++) {
            if (i < banEntries.size()) {
                Map.Entry<UUID, String> entry = banEntries.get(i);
                commandBuilder.set("#BanRow" + i + ".Visible", true);
                commandBuilder.set("#BanName" + i + ".Text", entry.getValue());
                commandBuilder.set("#BtnBanRemove" + i + ".Visible", canManageBans);
                commandBuilder.set("#BtnBanRemove" + i + ".Text", uiText(language, "ui.menu.team.btn.remove", "Remove"));
                if (canManageBans) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#BtnBanRemove" + i,
                        EventData.of("Action", "BanRemoveRow").append("TargetUuid", entry.getKey().toString()),
                        false
                    );
                }
            } else {
                commandBuilder.set("#BanRow" + i + ".Visible", false);
            }
        }
    }

    private void buildHomesSnapshot(UICommandBuilder commandBuilder,
                                    UIEventBuilder eventBuilder,
                                    IslandData island,
                                    String language) {
        boolean homesEnabled = plugin.getAddonManager().isHomesEnabled();
        commandBuilder.set("#BtnHomesSetOpen.Visible", homesEnabled);

        if (!homesEnabled || island == null) {
            commandBuilder.set("#BtnHomesSetOpen.Text", uiText(language, "ui.menu.homes.disabled", "Homes disabled"));
            commandBuilder.set("#HomesTableHeader.Visible", false);
            commandBuilder.set("#HomesEmptyLabel.Visible", true);
            commandBuilder.set("#HomesEmptyLabel.Text", homesEnabled
                ? uiText(language, "ui.menu.homes.empty", "No homes created yet.")
                : uiText(language, "ui.menu.homes.addon_disabled", "Homes addon is disabled."));
            for (int i = 0; i < MAX_HOME_ROWS; i++) {
                commandBuilder.set("#HomesRow" + i + ".Visible", false);
            }
            return;
        }

        Map<String, HomeLocation> homes;
        try {
            homes = plugin.getAddonManager().getHomesAddon().getService().listHomes(playerRef);
        } catch (IllegalStateException ignored) {
            homes = Map.of();
        }

        List<Map.Entry<String, HomeLocation>> entries = new ArrayList<>(homes.entrySet());
        int homesCount = entries.size();
        int homesMax = Math.max(0, plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(playerRef.getUuid()));

        commandBuilder.set("#BtnHomesSetOpen.Text", uiText(
            language,
            "ui.menu.homes.btn.set_home_count",
            Map.of("count", String.valueOf(homesCount), "max", String.valueOf(homesMax)),
            "Set home ({count}/{max})"
        ));
        commandBuilder.set("#HomesTableHeader.Visible", !entries.isEmpty());
        commandBuilder.set("#HomesEmptyLabel.Visible", entries.isEmpty());
        commandBuilder.set("#HomesEmptyLabel.Text", uiText(language, "ui.menu.homes.empty", "No homes created yet."));

        for (int i = 0; i < MAX_HOME_ROWS; i++) {
            if (i < entries.size()) {
                String homeId = safe(entries.get(i).getKey());
                commandBuilder.set("#HomesRow" + i + ".Visible", true);
                commandBuilder.set("#HomeName" + i + ".Text", homeId);
                commandBuilder.set("#BtnHomeGo" + i + ".Text", uiText(language, "ui.menu.homes.btn.go", "Go"));

                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BtnHomeRename" + i,
                    EventData.of("Action", "HomeRenameOpen").append("Param", homeId),
                    false
                );
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BtnHomeDelete" + i,
                    EventData.of("Action", "HomeDelete").append("Param", homeId),
                    false
                );
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BtnHomeGo" + i,
                    EventData.of("Action", "HomeGo").append("Param", homeId),
                    false
                );
            } else {
                commandBuilder.set("#HomesRow" + i + ".Visible", false);
            }
        }
    }

    private void buildWarpsSnapshot(UICommandBuilder commandBuilder,
                                    UIEventBuilder eventBuilder,
                                    IslandData island,
                                    String language) {
        commandBuilder.set("#WarpsSearchField.Value", safe(warpSearchInput));

        boolean warpsEnabled = plugin.getAddonManager().isWarpsEnabled();
        boolean hasQuery = !safe(warpSearchQuery).trim().isBlank();
        commandBuilder.set("#WarpsSearchHint.Visible", hasQuery);
        commandBuilder.set("#WarpsSearchHint.Text", hasQuery
            ? uiText(
                language,
                "ui.menu.warps.search.results",
                Map.of("query", safe(warpSearchQuery).trim()),
                "Search results for: {query}"
            )
            : "");

        if (!warpsEnabled || island == null) {
            commandBuilder.set("#WarpsOwnName.Text", uiText(language, "ui.menu.warps.own.default_name", "Your island warp"));
            commandBuilder.set("#WarpsOwnState.Text", warpsEnabled
                ? uiText(language, "ui.menu.warps.no_island", "No island found.")
                : uiText(language, "ui.menu.warps.addon_disabled", "Warps addon is disabled."));
            commandBuilder.set("#WarpsManageHint.Text", "");
            commandBuilder.set("#BtnWarpSetOpen.Visible", false);
            commandBuilder.set("#BtnWarpToggle.Visible", false);
            commandBuilder.set("#WarpRenameBox.Visible", false);
            commandBuilder.set("#WarpDeleteBox.Visible", false);
            commandBuilder.set("#WarpsTableHeader.Visible", false);
            commandBuilder.set("#WarpsPaginationRow.Visible", false);
            commandBuilder.set("#WarpsEmptyLabel.Visible", true);
            commandBuilder.set("#WarpsEmptyLabel.Text", warpsEnabled
                ? uiText(language, "ui.menu.warps.empty", "No public warps available.")
                : uiText(language, "ui.menu.warps.addon_disabled", "Warps addon is disabled."));
            for (int i = 0; i < MAX_WARP_ROWS; i++) {
                commandBuilder.set("#WarpsRow" + i + ".Visible", false);
            }
            return;
        }

        UUID islandLeaderUuid = island.getLeaderUuid() != null ? island.getLeaderUuid() : island.getOwnerUuid();
        boolean canManageOwnWarp = island.hasMemberPermission(playerRef.getUuid(), IslandMemberPermission.MANAGE_WARPS);
        IslandWarpData ownWarp = plugin.getAddonManager().getWarpsAddon().getService().getIslandWarp(islandLeaderUuid);

        boolean hasOwnWarp = ownWarp != null;
        String ownWarpName = hasOwnWarp
            ? (safe(ownWarp.getWarpName()).isBlank() ? uiText(language, "ui.menu.warps.default_name", "warp") : safe(ownWarp.getWarpName()))
            : uiText(language, "ui.menu.warps.own.default_name", "Your island warp");
        String ownWarpState = hasOwnWarp
            ? uiText(
                language,
                "ui.menu.warps.own.state",
                Map.of(
                    "state", ownWarp.isEnabled()
                        ? uiText(language, "ui.menu.warps.state.enabled", "Enabled")
                        : uiText(language, "ui.menu.warps.state.disabled", "Disabled"),
                    "visits", String.valueOf(Math.max(0L, ownWarp.getVisitCount()))
                ),
                "{state} | Visits: {visits}"
            )
            : uiText(language, "ui.menu.warps.no_own_warp", "No warp set yet.");

        commandBuilder.set("#WarpsOwnName.Text", ownWarpName);
        commandBuilder.set("#WarpsOwnState.Text", ownWarpState);
        commandBuilder.set("#WarpsManageHint.Text", canManageOwnWarp
            ? ""
            : uiText(language, "ui.menu.warps.manage.leader_only", "You do not have permission to manage this warp."));

        commandBuilder.set("#BtnWarpSetOpen.Visible", canManageOwnWarp && !hasOwnWarp);
        commandBuilder.set("#BtnWarpToggle.Visible", canManageOwnWarp && hasOwnWarp);
        commandBuilder.set("#WarpRenameBox.Visible", canManageOwnWarp && hasOwnWarp);
        commandBuilder.set("#WarpDeleteBox.Visible", canManageOwnWarp && hasOwnWarp);
        commandBuilder.set("#BtnWarpSetOpen.Text", uiText(language, "ui.menu.warps.btn.set", "Set warp"));

        if (canManageOwnWarp && hasOwnWarp) {
            boolean enableWarp = !ownWarp.isEnabled();
            commandBuilder.set("#BtnWarpToggle.Text", enableWarp
                ? uiText(language, "ui.menu.warps.btn.enable", "Enable")
                : uiText(language, "ui.menu.warps.btn.disable", "Disable"));
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BtnWarpToggle",
                EventData.of("Action", "WarpToggle").append("Param", enableWarp ? "enable" : "disable"),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BtnWarpRenameIcon",
                EventData.of("Action", "WarpRenameOpen").append("Param", ownWarpName),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BtnWarpDeleteIcon",
                EventData.of("Action", "WarpDelete"),
                false
            );
        }

        List<IslandWarpData> listed = plugin.getAddonManager().getWarpsAddon().getService().listDiscoverableWarps(warpSearchQuery, 0);
        List<IslandWarpData> discoverable = new ArrayList<>();
        for (IslandWarpData warp : listed) {
            if (warp == null || warp.getOwnerUuid() == null) {
                continue;
            }
            discoverable.add(warp);
        }

        int totalWarps = discoverable.size();
        int totalPages = Math.max(1, (totalWarps + WARP_PAGE_SIZE - 1) / WARP_PAGE_SIZE);
        warpPageIndex = Math.max(0, Math.min(warpPageIndex, totalPages - 1));

        int startIndex = warpPageIndex * WARP_PAGE_SIZE;
        int endIndex = Math.min(totalWarps, startIndex + WARP_PAGE_SIZE);
        List<IslandWarpData> displayed = startIndex < endIndex
            ? discoverable.subList(startIndex, endIndex)
            : List.of();

        boolean showPagination = totalWarps > WARP_PAGE_SIZE;
        commandBuilder.set("#WarpsPaginationRow.Visible", showPagination);
        commandBuilder.set("#BtnWarpsPrevPage.Visible", showPagination);
        commandBuilder.set("#BtnWarpsNextPage.Visible", showPagination);
        commandBuilder.set(
            "#WarpsPaginationLabel.Text",
            uiText(
                language,
                "ui.menu.warps.pagination",
                Map.of("current", String.valueOf(warpPageIndex + 1), "total", String.valueOf(totalPages)),
                "Page {current}/{total}"
            )
        );

        commandBuilder.set("#WarpsTableHeader.Visible", !displayed.isEmpty());
        commandBuilder.set("#WarpsEmptyLabel.Visible", displayed.isEmpty());
        commandBuilder.set("#WarpsEmptyLabel.Text", hasQuery
            ? uiText(language, "ui.menu.warps.search.empty", "No warp found for this search.")
            : uiText(language, "ui.menu.warps.empty", "No public warps available."));

        for (int i = 0; i < MAX_WARP_ROWS; i++) {
            if (i < displayed.size()) {
                IslandWarpData warp = displayed.get(i);
                String ownerName = safe(warp.getOwnerName());
                if (ownerName.isBlank() && warp.getOwnerUuid() != null) {
                    ownerName = resolvePlayerName(warp.getOwnerUuid(), island);
                }
                String warpName = safe(warp.getWarpName());
                if (warpName.isBlank()) {
                    warpName = ownerName.isBlank() ? uiText(language, "ui.menu.warps.default_name", "warp") : ownerName;
                }
                String visitTarget = ownerName.isBlank() || "Unknown".equalsIgnoreCase(ownerName)
                    ? warp.getOwnerUuid().toString()
                    : ownerName;

                commandBuilder.set("#WarpsRow" + i + ".Visible", true);
                commandBuilder.set("#WarpName" + i + ".Text", warpName + " (" + ownerName + ")");
                commandBuilder.set("#WarpVisits" + i + ".Text", String.valueOf(Math.max(0L, warp.getVisitCount())));
                commandBuilder.set("#BtnWarpVisit" + i + ".Text", uiText(language, "ui.menu.warps.btn.visit", "Visit"));

                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BtnWarpVisit" + i,
                    EventData.of("Action", "WarpVisit").append("TargetUuid", visitTarget),
                    false
                );
            } else {
                commandBuilder.set("#WarpsRow" + i + ".Visible", false);
            }
        }
    }

    private void buildTopSnapshot(UICommandBuilder commandBuilder,
                                  IslandData viewerIsland,
                                  String language) {
        commandBuilder.set("#TopSearchField.Value", safe(topSearchInput));

        String query = safe(topSearchQuery).trim();
        String normalizedQuery = normalizeSearchText(query);
        boolean hasQuery = !normalizedQuery.isBlank();

        commandBuilder.set("#TopSearchHint.Text", hasQuery
            ? uiText(language, "ui.menu.top.search.results", Map.of("query", query), "Search results for: {query}")
            : uiText(language, "ui.menu.top.search.top10", "Top 10 best islands"));

        if (!plugin.getAddonManager().isLevelsEnabled()) {
            commandBuilder.set("#TopTableHeader.Visible", false);
            commandBuilder.set("#TopEmptyLabel.Visible", true);
            commandBuilder.set("#TopEmptyLabel.Text", uiText(language, "ui.menu.top.levels_disabled", "Levels addon is disabled."));
            for (int i = 0; i < MAX_TOP_ROWS; i++) {
                commandBuilder.set("#TopRow" + i + ".Visible", false);
            }
            return;
        }

        List<com.sloyni.main.addons.levels.IslandValueService.CachedIslandTopEntry> topEntries =
            plugin.getIslandValueService().getTopCachedIslands(0);
        List<Integer> displayedIndexes = new ArrayList<>();

        for (int i = 0; i < topEntries.size(); i++) {
            com.sloyni.main.addons.levels.IslandValueService.CachedIslandTopEntry entry = topEntries.get(i);
            if (entry == null || entry.ownerUuid() == null) {
                continue;
            }

            String ownerName = safe(entry.ownerName());
            if (ownerName.isBlank()) {
                ownerName = resolvePlayerName(entry.ownerUuid(), viewerIsland);
            }

            String islandName = safe(entry.islandName());
            if (islandName.isBlank()) {
                IslandData rankedIsland = plugin.getIslandService().getOwnedIsland(entry.ownerUuid());
                if (rankedIsland != null) {
                    islandName = safe(plugin.getIslandService().getIslandDisplayName(rankedIsland));
                }
            }
            if (islandName.isBlank()) {
                islandName = ownerName.isBlank()
                    ? uiText(language, "ui.menu.top.unknown_island", "Unknown island")
                    : uiText(language, "ui.menu.top.owner_island", Map.of("owner", ownerName), "{owner}'s island");
            }

            if (!hasQuery || matchesTopSearch(normalizedQuery, islandName, ownerName, entry.ownerUuid().toString())) {
                displayedIndexes.add(i);
                if (displayedIndexes.size() >= MAX_TOP_ROWS) {
                    break;
                }
            }
        }

        commandBuilder.set("#TopTableHeader.Visible", !displayedIndexes.isEmpty());
        commandBuilder.set("#TopEmptyLabel.Visible", displayedIndexes.isEmpty());
        commandBuilder.set("#TopEmptyLabel.Text", hasQuery
            ? uiText(language, "ui.menu.top.search.empty", "No island found for this search.")
            : uiText(language, "ui.menu.top.empty", "No ranked islands available."));

        for (int row = 0; row < MAX_TOP_ROWS; row++) {
            if (row < displayedIndexes.size()) {
                int index = displayedIndexes.get(row);
                com.sloyni.main.addons.levels.IslandValueService.CachedIslandTopEntry entry = topEntries.get(index);

                String ownerName = safe(entry.ownerName());
                if (ownerName.isBlank()) {
                    ownerName = resolvePlayerName(entry.ownerUuid(), viewerIsland);
                }

                String islandName = safe(entry.islandName());
                if (islandName.isBlank()) {
                    IslandData rankedIsland = plugin.getIslandService().getOwnedIsland(entry.ownerUuid());
                    if (rankedIsland != null) {
                        islandName = safe(plugin.getIslandService().getIslandDisplayName(rankedIsland));
                    }
                }
                if (islandName.isBlank()) {
                    islandName = ownerName.isBlank()
                        ? uiText(language, "ui.menu.top.unknown_island", "Unknown island")
                        : uiText(language, "ui.menu.top.owner_island", Map.of("owner", ownerName), "{owner}'s island");
                }

                commandBuilder.set("#TopRow" + row + ".Visible", true);
                commandBuilder.set("#TopRank" + row + ".Text", "#" + (index + 1));
                commandBuilder.set("#TopIslandName" + row + ".Text", islandName);
                commandBuilder.set("#TopIslandLevel" + row + ".Text", uiText(language, "ui.menu.level.prefix", "Lvl.") + " " + Math.max(0L, entry.level()));
            } else {
                commandBuilder.set("#TopRow" + row + ".Visible", false);
            }
        }
    }

    private boolean matchesTopSearch(String normalizedQuery,
                                     String islandName,
                                     String ownerName,
                                     String ownerUuid) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }

        String haystack = normalizeSearchText(islandName + " " + ownerName + " " + ownerUuid);
        if (haystack.isBlank()) {
            return false;
        }

        String[] terms = normalizedQuery.split("\\\\s+");
        for (String term : terms) {
            if (term.isBlank()) {
                continue;
            }
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeSearchText(String raw) {
        return safe(raw).trim().toLowerCase(Locale.ROOT);
    }

    private void bindWallSettings(UICommandBuilder commandBuilder, IslandData island, String language, Player player) {
        boolean wallsEnabled = plugin.getAddonManager().isIslandWallsEnabled();
        boolean playerHasWallsPermission = player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.island_walls");
        commandBuilder.set("#SettingsWallsSection.Visible", wallsEnabled && playerHasWallsPermission);
        commandBuilder.set("#SettingsWallsGroup.Visible", wallsEnabled && playerHasWallsPermission);
        if (!wallsEnabled || island == null || !playerHasWallsPermission) {
            commandBuilder.set("#SettingsWallTypeDropdown.Entries", new DropdownEntryInfo[0]);
            commandBuilder.set("#SettingsWallColorDropdown.Entries", new DropdownEntryInfo[0]);
            commandBuilder.set("#SettingsWallColorsListValue.Text", "-");
            commandBuilder.set("#BtnSettingsWallApply.Visible", false);
            return;
        }

        ensureWallDraftInitialized(island);

        List<String> typeIds = plugin.getAddonManager().getIslandWallsAddon().getService().getSupportedWallPatternIds();
        List<DropdownEntryInfo> typeEntries = new ArrayList<>();
        String firstType = "none";
        for (String typeId : typeIds) {
            if (typeId == null || typeId.isBlank()) {
                continue;
            }
            if (typeEntries.isEmpty()) {
                firstType = typeId;
            }
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString(formatWallToken(typeId)), typeId));
        }

        String selectedType = resolveWallType(draftWallType);
        boolean selectedTypeExists = false;
        for (String typeId : typeIds) {
            if (typeId != null && typeId.equalsIgnoreCase(selectedType)) {
                selectedTypeExists = true;
                break;
            }
        }
        if (!selectedTypeExists) {
            selectedType = firstType;
            draftWallType = selectedType;
        }

        commandBuilder.set("#SettingsWallTypeDropdown.Entries", typeEntries.toArray(new DropdownEntryInfo[0]));
        commandBuilder.set("#SettingsWallTypeDropdown.Value", selectedType);

        int displaySize = "none".equals(selectedType)
            ? WALL_MIN_SIZE
            : clamp(draftWallSize, WALL_MIN_SIZE, WALL_MAX_SIZE);
        commandBuilder.set("#SettingsWallSizeField.Value", displaySize);

        List<String> colorNames = plugin.getAddonManager().getIslandWallsAddon().getService().getSupportedWallColorNames();
        List<DropdownEntryInfo> colorEntries = new ArrayList<>();
        String firstColor = "";
        for (String colorName : colorNames) {
            if (colorName == null || colorName.isBlank()) {
                continue;
            }
            if (firstColor.isBlank()) {
                firstColor = colorName;
            }
            colorEntries.add(new DropdownEntryInfo(LocalizableString.fromString(uiWallColorText(language, colorName)), colorName));
        }

        commandBuilder.set("#SettingsWallColorDropdown.Entries", colorEntries.toArray(new DropdownEntryInfo[0]));
        commandBuilder.set("#SettingsWallColorDropdown.Value", firstColor);
        commandBuilder.set("#SettingsWallColorsListValue.Text", formatWallColorCycle(language, draftWallColors));
    }

    private void ensureWallDraftInitialized(IslandData island) {
        if (island == null || island.getOwnerUuid() == null) {
            return;
        }
        if (wallDirty) {
            return;
        }
        if (!safe(draftWallType).isBlank() || draftWallSize > 0 || !draftWallColors.isEmpty()) {
            return;
        }

        IslandWallState state = plugin.getAddonManager().getIslandWallsAddon().getWallsRepository().getOrDefault(island.getOwnerUuid());
        draftWallType = resolveWallType(state.getWallPatternId());
        draftWallSize = "none".equals(draftWallType)
            ? 0
            : clamp(state.getWallPatternSize(), WALL_MIN_SIZE, WALL_MAX_SIZE);

        draftWallColors = new ArrayList<>();
        String primary = resolveWallColor(state.getWallColorName());
        if (primary.isBlank()) {
            primary = "red";
        }
        draftWallColors.add(primary);
        for (String alternate : state.getWallAlternateColorNames()) {
            String normalized = resolveWallColor(alternate);
            if (normalized.isBlank()) {
                continue;
            }
            if (!draftWallColors.isEmpty() && draftWallColors.get(draftWallColors.size() - 1).equals(normalized)) {
                continue;
            }
            draftWallColors.add(normalized);
        }
    }

    private void applyWallDraft(Player player,
                                Ref<EntityStore> ref,
                                Store<EntityStore> store,
                                IslandData island) {
        if (island == null) {
            setSettingsError("Walls", "No island found for this player.");
            reopen(player, ref, store, currentTab);
            return;
        }

        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            setSettingsError("Walls", "Walls addon is disabled.");
            reopen(player, ref, store, currentTab);
            return;
        }

        ensureWallDraftInitialized(island);
        String wallType = resolveWallType(draftWallType);
        int wallSize = "none".equals(wallType)
            ? 0
            : clamp(draftWallSize, WALL_MIN_SIZE, WALL_MAX_SIZE);
        if (!"none".equals(wallType) && wallSize < WALL_MIN_SIZE) {
            setSettingsError("Walls", "Wall size must be between 2 and " + WALL_MAX_SIZE + " for active wall types.");
            reopen(player, ref, store, currentTab);
            return;
        }

        List<String> colorCycle = normalizeWallColorCycle(draftWallColors);
        if (colorCycle.isEmpty()) {
            colorCycle = List.of("red");
        }

        clearSettingsError();

        var wallsService = plugin.getAddonManager().getIslandWallsAddon().getService();
        List<String> finalColorCycle = new ArrayList<>(colorCycle);

        player.getPageManager().openCustomPage(ref, store, new IslandWallApplyingPopupPage(playerRef, plugin));

        CompletableFuture<IslandWallState> applyFuture = wallsService.updateIslandWalls(playerRef, wallType, wallSize)
            .thenCompose(unusedState -> {
                if (finalColorCycle.size() <= 1) {
                    return wallsService.updateIslandWallColor(playerRef, finalColorCycle.get(0));
                }
                return wallsService.updateIslandWallAlternateColors(playerRef, finalColorCycle);
            });

        applyFuture.thenAccept(updatedState -> {
            draftWallType = resolveWallType(updatedState.getWallPatternId());
            draftWallSize = "none".equals(draftWallType)
                ? 0
                : clamp(updatedState.getWallPatternSize(), WALL_MIN_SIZE, WALL_MAX_SIZE);
            draftWallColors = new ArrayList<>();
            String primary = resolveWallColor(updatedState.getWallColorName());
            if (primary.isBlank()) {
                primary = "red";
            }
            draftWallColors.add(primary);
            for (String alternate : updatedState.getWallAlternateColorNames()) {
                String normalized = resolveWallColor(alternate);
                if (normalized.isBlank()) {
                    continue;
                }
                if (draftWallColors.get(draftWallColors.size() - 1).equals(normalized)) {
                    continue;
                }
                draftWallColors.add(normalized);
            }
            wallDirty = false;
            clearSettingsError();
            reopen(player, ref, store, currentTab);
        }).exceptionally(ex -> {
            setSettingsError("Walls", resolveWallErrorMessage(ex));
            reopen(player, ref, store, currentTab);
            return null;
        });
    }

    private boolean performResetIsland(Player player,
                                       Ref<EntityStore> ref,
                                       Store<EntityStore> store,
                                       World currentWorld,
                                       IslandData island) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            setSettingsError("Reset", "ISkyblock is not initialized yet.");
            return false;
        }
        if (island == null) {
            setSettingsError("Reset", "You do not have an island to reset.");
            return false;
        }
        if (!island.isLeader(playerRef.getUuid())) {
            setSettingsError("Reset", "Only the island leader can reset the island.");
            return false;
        }

        var lobby = plugin.getConfigManager().getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || safe(lobby.getWorldName()).isBlank()) {
            setSettingsError("Reset", "Lobby is not configured.");
            return false;
        }

        if (island.isLeader(playerRef.getUuid()) && island.getMemberCount() > 0) {
            setSettingsError("Reset", "You must remove all team members before resetting your island.");
            return false;
        }

        String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
        if (language.isBlank()) {
            language = "en";
        }
        if (plugin.getIslandService().getRemainingResets(playerRef.getUuid()) <= 0) {
            setSettingsError("Reset", uiText(language,
                "island.create.no_resets",
                "You have no island creates left. Ask an admin to add resets."));
            return false;
        }

        clearSettingsError();
        player.getPageManager().openCustomPage(ref, store,
            IslandTemplateSelectPage.forReset(playerRef, resolveWorld(player, store), plugin));
        return true;
    }

    private List<String> normalizeWallColorCycle(List<String> colors) {
        List<String> normalized = new ArrayList<>();
        if (colors == null) {
            return normalized;
        }
        for (String color : colors) {
            String normalizedColor = resolveWallColor(color);
            if (normalizedColor.isBlank()) {
                continue;
            }
            if (!normalized.isEmpty() && normalized.get(normalized.size() - 1).equals(normalizedColor)) {
                continue;
            }
            normalized.add(normalizedColor);
        }
        return normalized;
    }

    private String resolveWallType(String rawType) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return "none";
        }
        String normalized = plugin.getAddonManager().getIslandWallsAddon().getService().normalizeWallPatternId(rawType);
        return normalized == null ? "none" : normalized;
    }

    private String resolveWallColor(String rawColor) {
        if (!plugin.getAddonManager().isIslandWallsEnabled()) {
            return "";
        }
        String normalized = plugin.getAddonManager().getIslandWallsAddon().getService().normalizeWallColorName(rawColor);
        return normalized == null ? "" : normalized;
    }

    private String resolveWallErrorMessage(Throwable throwable) {
        String code = resolveThrowableMessage(throwable);
        return switch (code) {
            case "no_island" -> "No island found for this player.";
            case "not_leader" -> "Only the island leader can change walls.";
            case "invalid_wall_type" -> "Invalid wall type selected.";
            case "invalid_wall_size" -> "Wall size is invalid for the selected type.";
            case "invalid_wall_color", "invalid_wall_color_alternate" -> "One or more selected colors are invalid.";
            default -> "Failed to apply walls: " + safe(code);
        };
    }

    private String resolveResetErrorMessage(Throwable throwable) {
        String code = resolveThrowableMessage(throwable);
        return switch (code) {
            case "no_island" -> "No island found for this player.";
            case "not_leader" -> "Only the island leader can reset the island.";
            case "leader_has_members" -> "You must remove all team members before resetting your island.";
            default -> code != null && code.startsWith("Lobby world not loaded")
                ? "Lobby world is not loaded."
                : "Reset failed: " + safe(code);
        };
    }

    private String resolveThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
            return safe(throwable.getCause().getMessage());
        }
        return safe(throwable.getMessage());
    }

    private String formatWallColorCycle(String language, List<String> colors) {
        if (colors == null || colors.isEmpty()) {
            return "-";
        }

        List<String> labels = new ArrayList<>();
        for (String color : colors) {
            String label = uiWallColorText(language, color);
            if (!label.isBlank()) {
                labels.add(label);
            }
        }

        return labels.isEmpty() ? "-" : String.join(" -> ", labels);
    }

    private String uiWallColorText(String language, String rawColor) {
        String normalized = resolveWallColor(rawColor);
        if (normalized.isBlank()) {
            normalized = safe(rawColor).trim().toLowerCase(Locale.ROOT);
        }
        if (normalized.isBlank()) {
            return "";
        }

        return uiText(
            language,
            "ui.menu.settings.walls.colors.value." + normalized,
            formatWallToken(normalized)
        );
    }

    private String formatWallToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String[] parts = token.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private int parseIntSafe(String value, int fallback) {
        String raw = safe(value).trim();
        if (raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            try {
                return (int) Math.round(Double.parseDouble(raw.replace(',', '.')));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void setSettingsError(String title, String message) {
        settingsErrorTitle = safe(title);
        settingsErrorMessage = safe(message);
    }

    private void clearSettingsError() {
        settingsErrorTitle = "";
        settingsErrorMessage = "";
    }

    private IslandMemberPermission resolveRequiredPermission(String action) {
        return switch (safe(action)) {
            case "RenameIsland" -> IslandMemberPermission.RENAME_ISLAND;
            case "SetSpawn" -> IslandMemberPermission.SET_SPAWN;
            case "Lock" -> IslandMemberPermission.LOCK_ISLAND;
            case "RefreshLevel", "Level" -> IslandMemberPermission.REFRESH_LEVEL;
            case "WarpSetOpen", "WarpRenameOpen", "WarpToggle", "WarpDelete" -> IslandMemberPermission.MANAGE_WARPS;
            case "ApplyWalls", "ToggleWalls" -> IslandMemberPermission.MANAGE_WALLS;
            case "ToggleItemDrop" -> IslandMemberPermission.CHANGE_ISLAND_SETTINGS;
            case "ToggleMobDamage" -> IslandMemberPermission.CHANGE_ISLAND_SETTINGS;
            case "OpenInviteSelector", "InviteSelected" -> IslandMemberPermission.TEAM_INVITE;
            case "OpenCoopSelector", "CoopAddSelected", "CoopRemoveSelected", "CoopRemoveRow" -> IslandMemberPermission.TEAM_MANAGE_COOP;
            case "OpenBanSelector", "BanAddSelected", "BanRemoveSelected", "BanRemoveRow" -> IslandMemberPermission.TEAM_MANAGE_BANS;
            case "OpenExpelSelector" -> IslandMemberPermission.TEAM_EXPEL;
            case "RemoveSelected", "TeamKick" -> IslandMemberPermission.TEAM_REMOVE_MEMBER;
            case "MakeLeaderSelected", "TeamMakeLead" -> IslandMemberPermission.TEAM_TRANSFER_LEADERSHIP;
            default -> null;
        };
    }

    private boolean ensureActionPermission(String action,
                                           Player player,
                                           Ref<EntityStore> ref,
                                           Store<EntityStore> store,
                                           IslandData island) {
        IslandMemberPermission requiredPermission = resolveRequiredPermission(action);
        if (requiredPermission == null) {
            return true;
        }
        if (island != null && island.hasMemberPermission(playerRef.getUuid(), requiredPermission)) {
            return true;
        }
        openPermissionDeniedPopup(player, ref, store, requiredPermission);
        return false;
    }

    private boolean ensureCanOpenResetFlow(Player player,
                                           Ref<EntityStore> ref,
                                           Store<EntityStore> store,
                                           IslandData island) {
        String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
        if (language.isBlank()) {
            language = "en";
        }

        if (island == null) {
            player.getPageManager().openCustomPage(ref, store,
                new IslandInfoPopupPage(
                    playerRef,
                    resolveWorld(player, store),
                    plugin,
                    uiText(language, "ui.common.error", "Error"),
                    uiText(language, "ui.menu.reset.error.no_island", "You do not have an island to reset."),
                    toPopupReturnTab(currentTab)
                )
            );
            return false;
        }

        if (!island.isLeader(playerRef.getUuid())) {
            openPermissionDeniedPopup(player, ref, store, null);
            return false;
        }

        if (island.getMemberCount() > 0) {
            player.getPageManager().openCustomPage(ref, store,
                new IslandInfoPopupPage(
                    playerRef,
                    resolveWorld(player, store),
                    plugin,
                    uiText(language, "ui.common.error", "Error"),
                    uiText(language, "ui.menu.reset.error.leader_has_members",
                        "You must remove all team members before resetting your island."),
                    toPopupReturnTab(currentTab)
                )
            );
            return false;
        }

        if (plugin.getIslandService().getRemainingResets(playerRef.getUuid()) <= 0) {
            player.getPageManager().openCustomPage(ref, store,
                new IslandInfoPopupPage(
                    playerRef,
                    resolveWorld(player, store),
                    plugin,
                    uiText(language, "ui.common.error", "Error"),
                    uiText(language, "island.create.no_resets", "You have no island creates left. Ask an admin to add resets."),
                    toPopupReturnTab(currentTab)
                )
            );
            return false;
        }

        return true;
    }

    private boolean ensureCanOpenLeaveFlow(Player player,
                                           Ref<EntityStore> ref,
                                           Store<EntityStore> store,
                                           IslandData island) {
        String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
        if (language.isBlank()) {
            language = "en";
        }

        if (island == null) {
            player.getPageManager().openCustomPage(ref, store,
                new IslandInfoPopupPage(
                    playerRef,
                    resolveWorld(player, store),
                    plugin,
                    uiText(language, "ui.common.error", "Error"),
                    uiText(language, "island.leave.no_island", "You do not have an island to leave."),
                    toPopupReturnTab(currentTab)
                )
            );
            return false;
        }

        if (island.isLeader(playerRef.getUuid()) && island.getMemberCount() > 0) {
            player.getPageManager().openCustomPage(ref, store,
                new IslandInfoPopupPage(
                    playerRef,
                    resolveWorld(player, store),
                    plugin,
                    uiText(language, "ui.common.error", "Error"),
                    uiText(language, "island.leave.leader_has_members",
                        "You are the island leader. Transfer leadership or remove all members before leaving."),
                    toPopupReturnTab(currentTab)
                )
            );
            return false;
        }

        return true;
    }

    private void openPermissionDeniedPopup(Player player,
                                           Ref<EntityStore> ref,
                                           Store<EntityStore> store,
                                           IslandMemberPermission requiredPermission) {
        String language = sanitizeLocaleToken(plugin.resolvePlayerLanguage(playerRef));
        if (language.isBlank()) {
            language = "en";
        }

        String title = uiText(language, "ui.common.error", "Error");
        String message;
        if (requiredPermission == null) {
            message = uiText(language, "ui.menu.permissions.error.not_leader", "Only the island leader can do this action.");
        } else {
            String actionLabel = uiText(language, requiredPermission.getLabelKey(), requiredPermission.getFallbackLabel());
            message = uiText(
                language,
                "ui.menu.permissions.error.denied_action",
                Map.of("action", actionLabel),
                "You do not have permission for: {action}."
            );
        }

        player.getPageManager().openCustomPage(ref, store,
            new IslandInfoPopupPage(
                playerRef,
                resolveWorld(player, store),
                plugin,
                title,
                message,
                toPopupReturnTab(currentTab)
            )
        );
    }

    private IslandInfoPopupPage.ReturnTab toPopupReturnTab(Tab tab) {
        return switch (tab == null ? Tab.SETTINGS : tab) {
            case SETTINGS -> IslandInfoPopupPage.ReturnTab.SETTINGS;
            case TEAM -> IslandInfoPopupPage.ReturnTab.TEAM;
            case HOMES -> IslandInfoPopupPage.ReturnTab.HOMES;
            case WARPS -> IslandInfoPopupPage.ReturnTab.WARPS;
            case TOP -> IslandInfoPopupPage.ReturnTab.TOP;
        };
    }

    private boolean requiresConfirmation(String action) {
        return "TeamLeave".equalsIgnoreCase(action)
            || "RemoveSelected".equalsIgnoreCase(action)
            || "MakeLeaderSelected".equalsIgnoreCase(action)
            || "BanAddSelected".equalsIgnoreCase(action)
            || "WarpDelete".equalsIgnoreCase(action);
    }

    private boolean requiresTargetSelection(String action) {
        return "InviteSelected".equalsIgnoreCase(action)
            || "CoopAddSelected".equalsIgnoreCase(action)
            || "BanAddSelected".equalsIgnoreCase(action)
            || "RemoveSelected".equalsIgnoreCase(action)
            || "MakeLeaderSelected".equalsIgnoreCase(action)
            || "CoopRemoveSelected".equalsIgnoreCase(action)
            || "BanRemoveSelected".equalsIgnoreCase(action);
    }

    private void sendNoTargetSelectedUsage(String action) {
        if ("InviteSelected".equalsIgnoreCase(action)
            || "RemoveSelected".equalsIgnoreCase(action)
            || "MakeLeaderSelected".equalsIgnoreCase(action)) {
            plugin.getIslandCommandHandler().handleTeamUsageCommand(playerRef);
            return;
        }
        if ("CoopAddSelected".equalsIgnoreCase(action)
            || "CoopRemoveSelected".equalsIgnoreCase(action)) {
            plugin.getIslandCommandHandler().handleCoopUsageCommand(playerRef);
            return;
        }
        if ("BanAddSelected".equalsIgnoreCase(action)
            || "BanRemoveSelected".equalsIgnoreCase(action)) {
            plugin.getIslandCommandHandler().handleIslandBanUsageCommand(playerRef);
            return;
        }
        plugin.getIslandCommandHandler().handleIslandHelpCommand(playerRef);
    }

    private void queueConfirmation(String action, String targetUuid) {
        pendingAction = safe(action);
        pendingTargetUuid = safe(targetUuid);
    }

    private boolean hasPendingConfirmation() {
        return pendingAction != null && !pendingAction.isBlank();
    }

    private void clearConfirmation() {
        pendingAction = "";
        pendingTargetUuid = "";
    }

    private String buildConfirmationTitle(IslandData island, String language) {
        String targetName = resolveTargetName(parseUuid(pendingTargetUuid), island);
        return switch (safe(pendingAction)) {
            case "ResetIsland" -> uiText(language, "ui.menu.confirm.title.reset", "Confirm island reset");
            case "TeamLeave" -> uiText(language, "ui.menu.confirm.title.leave", "Confirm leave");
            case "RemoveSelected" -> uiText(
                language,
                "ui.menu.confirm.title.remove",
                Map.of("target", safe(targetName)),
                "Remove {target}?"
            );
            case "MakeLeaderSelected" -> uiText(
                language,
                "ui.menu.confirm.title.make_leader",
                Map.of("target", safe(targetName)),
                "Transfer leadership to {target}?"
            );
            case "BanAddSelected" -> uiText(
                language,
                "ui.menu.confirm.title.ban",
                Map.of("target", safe(targetName)),
                "Ban {target}?"
            );
            case "WarpDelete" -> uiText(language, "ui.menu.confirm.title.warp_delete", "Delete warp?");
            default -> uiText(language, "ui.menu.confirm.title.default", "Confirm action");
        };
    }

    private String buildConfirmationMessage(IslandData island, String language) {
        String targetName = resolveTargetName(parseUuid(pendingTargetUuid), island);
        return switch (safe(pendingAction)) {
            case "ResetIsland" -> uiText(language, "ui.menu.confirm.message.reset", "This action can remove your current island context. Continue?");
            case "TeamLeave" -> uiText(language, "ui.menu.confirm.message.leave", "You will leave your current island/team and may be teleported to lobby.");
            case "RemoveSelected" -> uiText(
                language,
                "ui.menu.confirm.message.remove",
                Map.of("target", safe(targetName)),
                "{target} will be removed and sent to lobby if online."
            );
            case "MakeLeaderSelected" -> uiText(
                language,
                "ui.menu.confirm.message.make_leader",
                Map.of("target", safe(targetName)),
                "Leadership will move to {target}. This cannot be undone quickly."
            );
            case "BanAddSelected" -> uiText(
                language,
                "ui.menu.confirm.message.ban",
                Map.of("target", safe(targetName)),
                "{target} will be banned and removed from the island area."
            );
            case "WarpDelete" -> uiText(language, "ui.menu.confirm.message.warp_delete", "Your island public warp will be removed.");
            default -> uiText(language, "ui.menu.confirm.message.default", "Please confirm this action.");
        };
    }

    private void reopen(Player player,
                        Ref<EntityStore> ref,
                        Store<EntityStore> store,
                        Tab tab) {
        Tab safeTab = normalizeRequestedTab(plugin, tab);
        player.getPageManager().openCustomPage(ref, store,
            new IslandMenuPage(
                playerRef,
                resolveWorld(player, store),
                plugin,
                safeTab,
                safe(selectedTargetUuid),
                safe(pendingAction),
                safe(pendingTargetUuid),
                safe(draftWallType),
                draftWallSize,
                new ArrayList<>(draftWallColors),
                wallDirty,
                safe(settingsErrorTitle),
                safe(settingsErrorMessage),
                safe(warpSearchInput),
                safe(warpSearchQuery),
                warpPageIndex,
                safe(topSearchInput),
                safe(topSearchQuery)
            )
        );
    }

    private void closeMenu(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
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

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveTargetName(UUID uuid, IslandData island) {
        if (uuid == null) {
            return null;
        }
        String resolved = resolvePlayerName(uuid, island);
        return resolved == null || resolved.isBlank() ? null : resolved;
    }

    private String resolvePlayerName(UUID uuid, IslandData island) {
        if (uuid == null) {
            return "Unknown";
        }
        if (island != null) {
            if (uuid.equals(island.getOwnerUuid()) && island.getOwnerName() != null && !island.getOwnerName().isBlank()) {
                return island.getOwnerName();
            }
            String memberName = island.getMemberNames().get(uuid.toString());
            if (memberName != null && !memberName.isBlank()) {
                return memberName;
            }
            String coopName = island.getCoopNames().get(uuid.toString());
            if (coopName != null && !coopName.isBlank()) {
                return coopName;
            }
            String bannedName = island.getBannedNames().get(uuid.toString());
            if (bannedName != null && !bannedName.isBlank()) {
                return bannedName;
            }
        }

        PlayerRef online = Universe.get().getPlayer(uuid);
        if (online != null && online.getUsername() != null && !online.getUsername().isBlank()) {
            return online.getUsername();
        }

        Map<UUID, String> knownPlayers = plugin.getIslandService().getAllKnownPlayers();
        String knownName = knownPlayers.get(uuid);
        if (knownName != null && !knownName.isBlank()) {
            return knownName;
        }

        return uuid.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static Tab normalizeRequestedTab(ISkyblockPlugin plugin, Tab requestedTab) {
        if (requestedTab == null) {
            return Tab.SETTINGS;
        }
        if (plugin == null || plugin.getAddonManager() == null) {
            return requestedTab;
        }

        return switch (requestedTab) {
            case HOMES -> plugin.getAddonManager().isHomesEnabled() ? Tab.HOMES : Tab.SETTINGS;
            case WARPS -> plugin.getAddonManager().isWarpsEnabled() ? Tab.WARPS : Tab.SETTINGS;
            case TOP -> plugin.getAddonManager().isLevelsEnabled() ? Tab.TOP : Tab.SETTINGS;
            default -> requestedTab;
        };
    }

    private String escape(String value) {
        return safe(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
