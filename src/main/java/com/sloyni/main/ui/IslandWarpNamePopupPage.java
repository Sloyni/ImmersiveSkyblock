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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;

public class IslandWarpNamePopupPage extends InteractiveCustomUIPage<IslandWarpNamePopupPage.PageData> {

    private static final int MAX_WARP_NAME_LENGTH = 22;

    public enum Mode {
        CREATE,
        RENAME
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .append(new KeyedCodec<>("@RenameValue", Codec.STRING), (o, v) -> o.renameValue = v, o -> o.renameValue).add()
            .build();

        public String action;
        public String renameValue;
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Mode mode;
    private final String sourceWarpName;
    private final String errorMessage;

    IslandWarpNamePopupPage(@Nonnull PlayerRef playerRef,
                            @Nonnull World sourceWorld,
                            @Nonnull ISkyblockPlugin plugin,
                            @Nonnull Mode mode,
                            @Nonnull String sourceWarpName,
                            @Nonnull String errorMessage) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.mode = mode;
        this.sourceWarpName = safe(sourceWarpName);
        this.errorMessage = safe(errorMessage);
    }

    IslandWarpNamePopupPage(@Nonnull PlayerRef playerRef,
                            @Nonnull World sourceWorld,
                            @Nonnull ISkyblockPlugin plugin,
                            @Nonnull Mode mode,
                            @Nonnull String sourceWarpName) {
        this(playerRef, sourceWorld, plugin, mode, sourceWarpName, "");
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandRenamePopup.ui");

        boolean renameMode = mode == Mode.RENAME;
        commandBuilder.set("#RenamePopupTitle.Text", renameMode
            ? tr("ui.menu.warps.popup.title.rename", "Rename warp")
            : tr("ui.menu.warps.popup.title.create", "Set warp"));
        commandBuilder.set("#RenameIslandHint.Text", tr(
            "ui.menu.warps.popup.hint",
            Map.of("max", String.valueOf(MAX_WARP_NAME_LENGTH)),
            "Warp name (max {max} chars):"
        ));
        commandBuilder.set("#BtnRenameCancel.Text", tr("ui.common.back", "Back"));
        commandBuilder.set("#BtnRenameSave.Text", tr("ui.common.apply", "Apply"));
        commandBuilder.set("#RenameIslandInput.Value", renameMode ? sourceWarpName : "");

        boolean hasError = !errorMessage.isBlank();
        commandBuilder.set("#RenameIslandError.Visible", hasError);
        commandBuilder.set("#RenameIslandError.Text", hasError ? errorMessage : "");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnRenameCancel",
            EventData.of("Action", "Cancel"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnRenameSave",
            EventData.of("Action", "Save").append("@RenameValue", "#RenameIslandInput.Value"), false);
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
            IslandMenuPage.openWarps(player, ref, store, playerRef, resolveWorld(player, store), plugin);
            return;
        }

        if (!"Save".equalsIgnoreCase(action)) {
            return;
        }

        String warpName = normalizeWarpName(data.renameValue);
        if (warpName.isBlank()) {
            reopenWithError(player, ref, store,
                tr("ui.menu.warps.popup.error.empty", "Warp name cannot be empty."));
            return;
        }
        if (warpName.codePointCount(0, warpName.length()) > MAX_WARP_NAME_LENGTH) {
            reopenWithError(player, ref, store,
                tr("ui.menu.warps.popup.error.too_long", Map.of("max", String.valueOf(MAX_WARP_NAME_LENGTH)),
                    "Warp name cannot exceed {max} characters."));
            return;
        }

        if (mode == Mode.RENAME) {
            if (warpName.equalsIgnoreCase(sourceWarpName)) {
                IslandMenuPage.openWarps(player, ref, store, playerRef, resolveWorld(player, store), plugin);
                return;
            }
            plugin.getIslandCommandHandler().handleIslandWarpRenameCommand(playerRef, warpName);
            IslandMenuPage.openWarps(player, ref, store, playerRef, resolveWorld(player, store), plugin);
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            reopenWithError(player, ref, store,
                tr("ui.menu.warps.popup.error.position", "Unable to read your position."));
            return;
        }

        int x = (int) Math.floor(transform.getPosition().x());
        int y = (int) Math.floor(transform.getPosition().y());
        int z = (int) Math.floor(transform.getPosition().z());
        float pitch = transform.getRotation().pitch();
        float yaw = transform.getRotation().yaw();

        World world = resolveWorld(player, store);
        plugin.getIslandCommandHandler().handleIslandWarpSetCommand(playerRef, world, x, y, z, pitch, yaw);
        plugin.getIslandCommandHandler().handleIslandWarpRenameCommand(playerRef, warpName);
        IslandMenuPage.openWarps(player, ref, store, playerRef, world, plugin);
    }

    private void reopenWithError(Player player,
                                 Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 String message) {
        player.getPageManager().openCustomPage(ref, store,
            new IslandWarpNamePopupPage(playerRef, resolveWorld(player, store), plugin, mode, sourceWarpName, message));
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

    private String normalizeWarpName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\u00A0', ' ').trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
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
