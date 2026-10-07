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
import com.sloyni.main.island.TeamInviteManager;
import com.sloyni.main.util.Chat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;

public class IslandInvitesPage extends InteractiveCustomUIPage<IslandInvitesPage.PageData> {
    private static final int MAX_ROWS = 10;

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

    private record InviteEntry(String inviterName) {
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private String searchInput;
    private String searchQuery;
    private List<InviteEntry> displayedInvites;

    IslandInvitesPage(@Nonnull PlayerRef playerRef,
                      @Nonnull World sourceWorld,
                      @Nonnull ISkyblockPlugin plugin,
                      @Nonnull String searchInput,
                      @Nonnull String searchQuery) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.searchInput = safe(searchInput);
        this.searchQuery = safe(searchQuery);
        this.displayedInvites = new ArrayList<>();
    }

    public IslandInvitesPage(@Nonnull PlayerRef playerRef,
                             @Nonnull World sourceWorld,
                             @Nonnull ISkyblockPlugin plugin) {
        this(playerRef, sourceWorld, plugin, "", "");
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandInvitesPage.ui");

        String language = normalizeLanguage(plugin.resolvePlayerLanguage(playerRef));

        commandBuilder.set("#InvitesTitle.Text", tr(language, "ui.menu.invites.title", "INVITATIONS"));
        commandBuilder.set("#InvitesSubtitle.Text", tr(language, "ui.menu.invites.subtitle", "Accept an invitation to join an island."));
        commandBuilder.set("#InvitesSearchField.Value", searchInput);
        commandBuilder.set("#InvitesSearchField.PlaceholderText",
            tr(language, "ui.menu.invites.search.placeholder", "Search by inviter name..."));
        commandBuilder.set("#BtnInvitesSearch.Text", tr(language, "ui.menu.invites.search.button", "Search"));
        commandBuilder.set("#BtnInvitesBack.Text", tr(language, "ui.menu.invites.back", "Back"));

        displayedInvites = filterInvites(buildPendingInvites());
        commandBuilder.set("#InvitesCountLabel.Text",
            tr(language, "ui.menu.invites.count", Map.of("count", String.valueOf(displayedInvites.size())), "Invitations: {count}"));
        commandBuilder.set("#InvitesEmptyLabel.Visible", displayedInvites.isEmpty());
        commandBuilder.set("#InvitesEmptyLabel.Text", hasSearchQuery()
            ? tr(language, "ui.menu.invites.empty.search", "No invitation found for this search.")
            : tr(language, "ui.menu.invites.empty", "No pending invitations."));

        for (int i = 0; i < MAX_ROWS; i++) {
            if (i < displayedInvites.size()) {
                InviteEntry entry = displayedInvites.get(i);
                commandBuilder.set("#InviteRow" + i + ".Visible", true);
                commandBuilder.set("#InviteName" + i + ".Text", entry.inviterName());
                commandBuilder.set("#BtnInviteAccept" + i + ".Text",
                    tr(language, "ui.menu.invites.btn.accept", "Accept"));
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BtnInviteAccept" + i,
                    EventData.of("Action", "Accept").append("SelectIndex", String.valueOf(i)),
                    false
                );
            } else {
                commandBuilder.set("#InviteRow" + i + ".Visible", false);
            }
        }

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#InvitesSearchField",
            EventData.of("@SearchInput", "#InvitesSearchField.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BtnInvitesSearch",
            EventData.of("Action", "Search").append("@SearchInput", "#InvitesSearchField.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BtnInvitesBack",
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
                player.getPageManager().setPage(ref, store,
                    com.hypixel.hytale.protocol.packets.interface_.Page.None);
                return;
            }
            case "Accept" -> {
                int index;
                try {
                    index = Integer.parseInt(safe(data.selectIndex));
                } catch (NumberFormatException ignored) {
                    reopen(player, ref, store);
                    return;
                }
                if (index < 0 || index >= displayedInvites.size()) {
                    reopen(player, ref, store);
                    return;
                }

                InviteEntry selected = displayedInvites.get(index);
                String inviterName = selected.inviterName();

                TeamInviteManager.Invite invite = plugin.getTeamInviteManager()
                    .consumeInvite(playerRef.getUuid(), inviterName);
                if (invite == null) {
                    // Invite already consumed or removed, refresh
                    reopen(player, ref, store);
                    return;
                }

                try {
                    plugin.getIslandService().addTeamMember(invite.inviterUuid(), playerRef);
                } catch (IllegalStateException e) {
                    // Island no longer exists or error
                    player.getPageManager().openCustomPage(ref, store,
                        new IslandInfoPopupPage(
                            playerRef,
                            resolveWorld(player, store),
                            plugin,
                            tr(normalizeLanguage(plugin.resolvePlayerLanguage(playerRef)),
                                "ui.menu.invites.error.title", "Error"),
                            tr(normalizeLanguage(plugin.resolvePlayerLanguage(playerRef)),
                                "team.accept.island_gone", "This island no longer exists."),
                            IslandInfoPopupPage.ReturnTab.HOMES
                        )
                    );
                    return;
                }

                // Success
                String language = normalizeLanguage(plugin.resolvePlayerLanguage(playerRef));
                player.getPageManager().openCustomPage(ref, store,
                    new IslandInfoPopupPage(
                        playerRef,
                        resolveWorld(player, store),
                        plugin,
                        tr(language, "ui.menu.invites.success.title", "Accepted"),
                        tr(language, "ui.menu.invites.success.message",
                            Map.of("player", inviterName),
                            "You have joined {player}'s island."),
                        IslandInfoPopupPage.ReturnTab.HOMES
                    )
                );

                // Notify the inviter
                PlayerRef inviterRef = Universe.get().getPlayer(invite.inviterUuid());
                if (inviterRef != null) {
                    Chat.send(inviterRef, plugin.tr(inviterRef, "team.accept.notify_leader",
                        Map.of("player", playerRef.getUsername())));
                }
                return;
            }
            default -> {
            }
        }
    }

    private List<InviteEntry> buildPendingInvites() {
        List<TeamInviteManager.Invite> invites = plugin.getTeamInviteManager()
            .getPendingInvites(playerRef.getUuid());
        List<InviteEntry> entries = new ArrayList<>();
        for (TeamInviteManager.Invite invite : invites) {
            String name = safe(invite.inviterName());
            if (!name.isBlank()) {
                entries.add(new InviteEntry(name));
            }
        }
        return entries;
    }

    private List<InviteEntry> filterInvites(List<InviteEntry> invites) {
        String normalized = safe(searchQuery).trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return invites.size() > MAX_ROWS ? invites.subList(0, MAX_ROWS) : invites;
        }

        List<InviteEntry> filtered = new ArrayList<>();
        for (InviteEntry entry : invites) {
            String name = safe(entry.inviterName()).toLowerCase(Locale.ROOT);
            if (name.contains(normalized)) {
                filtered.add(entry);
                if (filtered.size() >= MAX_ROWS) {
                    break;
                }
            }
        }
        return filtered;
    }

    private boolean hasSearchQuery() {
        return !safe(searchQuery).trim().isBlank();
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        player.getPageManager().openCustomPage(ref, store,
            new IslandInvitesPage(playerRef, resolveWorld(player, store), plugin,
                safe(searchInput), safe(searchQuery)));
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

    private String tr(String language, String key, String fallback) {
        String value = plugin.getMessageManager().getExact(key, language);
        if (value == null || value.isBlank()) {
            value = plugin.getMessageManager().getExact(key, "en");
        }
        return value == null || value.isBlank() ? fallback : value;
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
