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
import com.sloyni.main.addons.homes.HomeLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class ISkyblockAdminDashboardHomesViewerPage
        extends InteractiveCustomUIPage<ISkyblockAdminDashboardHomesViewerPage.PageData> {

    private static final int MAX_HOMES_ROWS = 10;

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final ISkyblockAdminDashboardPage parent;

    private UUID viewerPlayerUuid;
    private String viewerPlayerName;
    private List<HomeEntrySlot> homesSlots;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
            .builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Back",      Codec.STRING), (o, v) -> o.back      = v, o -> o.back).add()
            .append(new KeyedCodec<>("Teleport",  Codec.STRING), (o, v) -> o.teleport  = v, o -> o.teleport).add()
            .build();

        public String back;
        public String teleport;
    }

    public static class HomeEntrySlot {
        public final String id;
        public final String name;

        public HomeEntrySlot(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public ISkyblockAdminDashboardHomesViewerPage(@Nonnull PlayerRef playerRef,
                                                  @Nonnull World sourceWorld,
                                                  @Nonnull ISkyblockPlugin plugin,
                                                  @Nonnull ISkyblockAdminDashboardPage parent,
                                                  @Nonnull UUID viewerPlayerUuid,
                                                  @Nonnull String viewerPlayerName) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;
        this.sourceWorld = sourceWorld;
        this.parent = parent;
        this.viewerPlayerUuid = viewerPlayerUuid;
        this.viewerPlayerName = viewerPlayerName;
        this.homesSlots = new ArrayList<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {

        commandBuilder.append("Pages/ISkyblockAdminDashboard.HomesViewerPage.ui");

        String configuredLanguage = plugin.getConfigManager().getConfig().getLanguage();
        String language = configuredLanguage == null || configuredLanguage.isBlank()
            ? "en"
            : configuredLanguage.trim().toLowerCase();

        // Load homes for the target player
        Map<String, HomeLocation> targetPlayerHomes = plugin.getAddonManager().getHomesAddon().getService().listHomesForTarget(viewerPlayerUuid);
        homesSlots = new ArrayList<>();
        if (targetPlayerHomes != null) {
            for (Map.Entry<String, HomeLocation> entry : targetPlayerHomes.entrySet()) {
                if (homesSlots.size() >= MAX_HOMES_ROWS) {
                    break;
                }
                homesSlots.add(new HomeEntrySlot(entry.getKey(), entry.getKey()));
            }
        }

        // Set title
        String titleText = plugin.getMessageManager().get("ui.admin_dashboard.homes.viewer_title_empty", language);
        if (viewerPlayerName != null && !viewerPlayerName.isBlank()) {
            titleText = plugin.getMessageManager().get("ui.admin_dashboard.homes.viewer_title", language)
                .replace("{player}", viewerPlayerName);
        }
        commandBuilder.set("#HomesViewerTitle.Text", titleText);
        commandBuilder.set("#HomesViewerDescription.Text", plugin.getMessageManager().get("ui.admin_dashboard.homes.viewer_description", language));

        commandBuilder.set("#HomesViewerBackButton.Text", plugin.getMessageManager().get("ui.common.back", language));

        // Set home rows
        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            if (i < homesSlots.size()) {
                HomeEntrySlot slot = homesSlots.get(i);
                commandBuilder.set("#HomesViewerRow" + i + ".Visible", true);
                commandBuilder.set("#HomeViewerName" + i + ".Text", slot.name);
                commandBuilder.set("#HomesViewerTeleport" + i + ".Text", plugin.getMessageManager().get("ui.admin_dashboard.homes.teleport", language));
            } else {
                commandBuilder.set("#HomesViewerRow" + i + ".Visible", false);
            }
        }

        // Event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesViewerBackButton",
            EventData.of("Back", "true"), false);

        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesViewerTeleport" + i,
                EventData.of("Teleport", String.valueOf(i)), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (data.back != null) {
            player.getPageManager().openCustomPage(ref, store, parent);
            return;
        }

        if (data.teleport != null) {
            try {
                int index = Integer.parseInt(data.teleport);
                if (index >= 0 && index < homesSlots.size()) {
                    HomeEntrySlot slot = homesSlots.get(index);
                    PlayerRef adminRef = player.getPlayerRef();
                    if (adminRef != null) {
                        // The homes service expects the home ID (name) to teleport to
                        plugin.getAddonManager().getHomesAddon().getService()
                            .teleportViewerToTargetHome(adminRef, sourceWorld, viewerPlayerUuid, slot.id);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
