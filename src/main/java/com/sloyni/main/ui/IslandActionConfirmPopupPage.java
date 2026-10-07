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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IslandActionConfirmPopupPage extends InteractiveCustomUIPage<IslandActionConfirmPopupPage.PageData> {

    public enum ActionType {
        RESET_ISLAND,
        LEAVE_ISLAND
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .build();

        public String action;
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final ActionType actionType;
    private final String errorMessage;

    IslandActionConfirmPopupPage(@Nonnull PlayerRef playerRef,
                                 @Nonnull World sourceWorld,
                                 @Nonnull ISkyblockPlugin plugin,
                                 @Nonnull ActionType actionType,
                                 @Nullable String errorMessage) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.plugin = plugin;
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.actionType = actionType;
        this.errorMessage = safe(errorMessage);
    }

    IslandActionConfirmPopupPage(@Nonnull PlayerRef playerRef,
                                 @Nonnull World sourceWorld,
                                 @Nonnull ISkyblockPlugin plugin,
                                 @Nonnull ActionType actionType) {
        this(playerRef, sourceWorld, plugin, actionType, null);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandActionConfirmPopup.ui");

        commandBuilder.set("#ActionConfirmTitle.Text", resolveTitle());
        commandBuilder.set("#ActionConfirmMessage.Text", resolveMessage());
        commandBuilder.set("#BtnActionConfirmCancel.Text", plugin.rawText(playerRef, "ui.common.cancel"));
        commandBuilder.set("#BtnActionConfirmConfirm.Text", resolveConfirmLabel());

        boolean hasError = !errorMessage.isBlank();
        commandBuilder.set("#ActionConfirmError.Visible", hasError);
        commandBuilder.set("#ActionConfirmError.Text", hasError ? errorMessage : "");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnActionConfirmCancel",
            EventData.of("Action", "Cancel"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnActionConfirmConfirm",
            EventData.of("Action", "Confirm"), false);
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
        if ("Cancel".equalsIgnoreCase(action)) {
            IslandMenuPage.open(player, ref, store, playerRef, resolveWorld(player, store), plugin);
            return;
        }

        if (!"Confirm".equalsIgnoreCase(action)) {
            return;
        }

        switch (actionType) {
            case RESET_ISLAND -> performResetIsland(player, ref, store);
            case LEAVE_ISLAND -> performLeaveIsland(player, ref, store);
            default -> IslandMenuPage.open(player, ref, store, playerRef, resolveWorld(player, store), plugin);
        }
    }

    private void performLeaveIsland(Player player,
                                    Ref<EntityStore> ref,
                                    Store<EntityStore> store) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            reopenWithError(player, ref, store,
                localText("island.not_initialized", "ISkyblock is not initialized yet."));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            reopenWithError(player, ref, store, localText("island.leave.no_island", "You do not have an island."));
            return;
        }
        if (island.isLeader(playerRef.getUuid()) && island.getMemberCount() > 0) {
            reopenWithError(player, ref, store,
                localText("island.leave.leader_has_members", "You must remove all team members before leaving your island."));
            return;
        }

        var lobby = plugin.getConfigManager().getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || safe(lobby.getWorldName()).isBlank()) {
            reopenWithError(player, ref, store, localText("lobby.not_set", "Lobby is not configured."));
            return;
        }

        World world = resolveWorld(player, store);
        plugin.getIslandService().leaveIslandAndTeleportToLobby(
            playerRef,
            world,
            lobby.getWorldName(),
            lobby.getX(),
            lobby.getY(),
            lobby.getZ(),
            lobby.getPitch(),
            lobby.getYaw()
        ).thenRun(() -> {
            try {
                if (plugin.getIslandMenuHudManager() != null) {
                    plugin.getIslandMenuHudManager().refreshHudForPlayer(playerRef.getUuid());
                }
            } catch (Exception ignore) {
            }
            player.getPageManager().setPage(ref, store, Page.None);
        }).exceptionally(ex -> {
            reopenWithError(player, ref, store, resolveLeaveErrorMessage(ex));
            return null;
        });
    }

    private void performResetIsland(Player player,
                                    Ref<EntityStore> ref,
                                    Store<EntityStore> store) {
        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            reopenWithError(player, ref, store,
                localText("island.not_initialized", "ISkyblock is not initialized yet."));
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            reopenWithError(player, ref, store,
                localText("ui.menu.reset.error.no_island", "You do not have an island to reset."));
            return;
        }
        if (!island.isLeader(playerRef.getUuid())) {
            reopenWithError(player, ref, store,
                localText("ui.menu.permissions.error.not_leader", "Only the island leader can do this action."));
            return;
        }

        var lobby = plugin.getConfigManager().getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || safe(lobby.getWorldName()).isBlank()) {
            reopenWithError(player, ref, store,
                localText("lobby.not_set", "Lobby is not configured."));
            return;
        }

        if (plugin.getIslandService().getRemainingResets(playerRef.getUuid()) <= 0) {
            reopenWithError(player, ref, store,
                localText("island.create.no_resets", "You have no island creates left. Ask an admin to add resets."));
            return;
        }

        player.getPageManager().openCustomPage(ref, store,
            IslandTemplateSelectPage.forReset(playerRef, resolveWorld(player, store), plugin));
    }

    private void reopenWithError(Player player,
                                 Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 String error) {
        player.getPageManager().openCustomPage(ref, store,
            new IslandActionConfirmPopupPage(playerRef, resolveWorld(player, store), plugin, actionType, error));
    }

    private String resolveTitle() {
        return switch (actionType) {
            case RESET_ISLAND -> localText("ui.menu.confirm.title.reset", "Confirm island reset");
            case LEAVE_ISLAND -> localText("ui.menu.confirm.title.leave", "Confirm leave");
        };
    }

    private String resolveMessage() {
        return switch (actionType) {
            case RESET_ISLAND -> localText("ui.menu.confirm.message.reset", "This action can remove your current island context. Continue?");
            case LEAVE_ISLAND -> localText("ui.menu.confirm.message.leave", "You will leave your current island/team and may be teleported to lobby.");
        };
    }

    private String resolveConfirmLabel() {
        return switch (actionType) {
            case RESET_ISLAND -> localText("ui.menu.settings.btn.reset_island", "Reset island");
            case LEAVE_ISLAND -> localText("ui.menu.settings.btn.leave_island", "Leave island");
        };
    }

    private String resolveLeaveErrorMessage(Throwable throwable) {
        String code = resolveThrowableMessage(throwable);
        return switch (code) {
            case "no_island" -> localText("island.leave.no_island", "You do not have an island.");
            case "leader_has_members" -> localText("island.leave.leader_has_members", "You must remove all team members before leaving your island.");
            default -> code.startsWith("Lobby world not loaded")
                ? localText("lobby.not_set", "Lobby is not configured.")
                : localText("island.leave.failed", Map.of("error", safe(code)), "Leave failed: {error}");
        };
    }

    private String resolveResetErrorMessage(Throwable throwable) {
        String code = resolveThrowableMessage(throwable);
        return switch (code) {
            case "no_island" -> localText("ui.menu.reset.error.no_island", "You do not have an island to reset.");
            case "not_leader" -> localText("ui.menu.permissions.error.not_leader", "Only the island leader can do this action.");
            case "leader_has_members" -> localText("ui.menu.reset.error.leader_has_members", "You must remove all team members before resetting your island.");
            default -> code.startsWith("Lobby world not loaded")
                ? localText("lobby.not_set", "Lobby is not configured.")
                : localText("ui.menu.reset.error.failed", Map.of("error", safe(code)), "Reset failed: {error}");
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

    private String localText(String key, String fallback) {
        String language = normalizeLanguageForMessages(plugin.resolvePlayerLanguage(playerRef));
        String value = language.isBlank() ? null : plugin.getMessageManager().getExact(key, language);
        if ((value == null || value.isBlank()) && !language.isBlank()) {
            int separator = Math.max(language.indexOf('-'), language.indexOf('_'));
            if (separator > 0) {
                value = plugin.getMessageManager().getExact(key, language.substring(0, separator));
            }
        }
        if (value == null || value.isBlank()) {
            value = plugin.getMessageManager().getExact(key, "en");
        }
        if (value == null || value.isBlank()) {
            value = fallback;
        }
        return safe(value);
    }

    private String normalizeLanguageForMessages(String language) {
        if (language == null) {
            return "";
        }

        String raw = language.trim().toLowerCase(Locale.ROOT);
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

    private String localText(String key, Map<String, String> vars, String fallback) {
        String text = localText(key, fallback);
        if (vars == null || vars.isEmpty()) {
            return text;
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            text = text.replace("{" + safe(entry.getKey()) + "}", safe(entry.getValue()));
        }
        return text;
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