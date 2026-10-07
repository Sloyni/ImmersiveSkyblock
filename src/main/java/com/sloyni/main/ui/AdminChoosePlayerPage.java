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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class AdminChoosePlayerPage extends InteractiveCustomUIPage<AdminChoosePlayerPage.PageData> {
    private static final int MAX_DISPLAY_PLAYERS = 20;

    private final ISkyblockPlugin plugin;
    private final ISkyblockAdminDashboardPage parentPage;
    private final Map<Integer, PlayerEntry> indexToPlayer;

    private String searchQuery;
    private List<PlayerEntry> allPlayers;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("SelectPlayer", Codec.STRING), (o, v) -> o.selectPlayer = v, o -> o.selectPlayer).add()
            .append(new KeyedCodec<>("Back", Codec.STRING), (o, v) -> o.back = v, o -> o.back).add()
            .append(new KeyedCodec<>("@SearchQuery", Codec.STRING), (o, v) -> o.searchQuery = v, o -> o.searchQuery).add()
            .build();

        public String selectPlayer;
        public String back;
        public String searchQuery;
    }

    public record PlayerEntry(UUID uuid, String username) {
    }

    public AdminChoosePlayerPage(@Nonnull PlayerRef playerRef,
                                 @Nonnull ISkyblockPlugin plugin,
                                 @Nonnull ISkyblockAdminDashboardPage parentPage) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.plugin = plugin;
        this.parentPage = parentPage;
        this.indexToPlayer = new LinkedHashMap<>();
        this.searchQuery = "";
        this.allPlayers = new ArrayList<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/ISkyblockChoosePlayer.ui");
        commandBuilder.set("#PageTitle.Text", "Choose Player");
        commandBuilder.set("#Subtitle.Text", "All players seen on this server. Search and select.");
        commandBuilder.set("#SearchInput.PlaceholderText", "Search by player name...");
        commandBuilder.set("#BackButton.Text", "Back");

        this.allPlayers = buildAllPlayers();
        populatePlayerRows(commandBuilder, eventBuilder, filterPlayers());

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("@SearchQuery", "#SearchInput.Value"),
            false
        );
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Back", "true"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        if (data.searchQuery != null) {
            String normalized = data.searchQuery.trim();
            if (!normalized.equals(this.searchQuery)) {
                this.searchQuery = normalized;
                sendIncrementalUpdate();
            }
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.selectPlayer != null) {
            try {
                int index = Integer.parseInt(data.selectPlayer);
                PlayerEntry entry = indexToPlayer.get(index);
                if (entry != null) {
                    parentPage.onPlayerChosen(entry.uuid(), entry.username());
                }
            } catch (NumberFormatException ignored) {
            }
            player.getPageManager().openCustomPage(ref, store, parentPage);
            return;
        }

        if (data.back != null) {
            player.getPageManager().openCustomPage(ref, store, parentPage);
        }
    }

    private List<PlayerEntry> buildAllPlayers() {
        List<PlayerEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : plugin.getIslandService().getAllKnownPlayers().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isBlank()) {
                entries.add(new PlayerEntry(entry.getKey(), entry.getValue()));
            }
        }

        entries.sort(Comparator.comparing(playerEntry -> playerEntry.username().toLowerCase(Locale.ROOT)));
        return entries;
    }

    private List<PlayerEntry> filterPlayers() {
        if (searchQuery == null || searchQuery.isBlank()) {
            return allPlayers;
        }

        String normalized = searchQuery.toLowerCase(Locale.ROOT);
        List<PlayerEntry> filtered = new ArrayList<>();
        for (PlayerEntry entry : allPlayers) {
            if (entry.username() != null && entry.username().toLowerCase(Locale.ROOT).contains(normalized)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private void populatePlayerRows(UICommandBuilder commandBuilder,
                                    UIEventBuilder eventBuilder,
                                    List<PlayerEntry> players) {
        indexToPlayer.clear();

        commandBuilder.set("#CountLabel.Text", "Players: " + players.size());
        commandBuilder.set("#EmptyLabel.Visible", players.isEmpty());

        for (int i = 0; i < MAX_DISPLAY_PLAYERS; i++) {
            String rowId = "#PlayerRow" + i;
            if (i < players.size()) {
                PlayerEntry entry = players.get(i);
                indexToPlayer.put(i, entry);
                commandBuilder.set(rowId + ".Visible", true);
                commandBuilder.set("#PlayerName" + i + ".Text", entry.username());
                commandBuilder.set("#SelectButton" + i + ".Text", "Select");
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#SelectButton" + i,
                    EventData.of("SelectPlayer", String.valueOf(i)),
                    false
                );
            } else {
                commandBuilder.set(rowId + ".Visible", false);
            }
        }
    }

    private void sendIncrementalUpdate() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        populatePlayerRows(cmd, evt, filterPlayers());
        evt.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("@SearchQuery", "#SearchInput.Value"),
            false
        );
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Back", "true"), false);
        sendUpdate(cmd, evt, false);
    }
}