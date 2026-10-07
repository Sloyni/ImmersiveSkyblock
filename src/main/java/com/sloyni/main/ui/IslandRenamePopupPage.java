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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Popup page that lets the island leader rename their island.
 * Opened from the pen button in the left panel of {@link IslandMenuPage}.
 */
public class IslandRenamePopupPage extends InteractiveCustomUIPage<IslandRenamePopupPage.PageData> {

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
            .append(new KeyedCodec<>("@RenameValue", Codec.STRING), (o, v) -> o.renameValue = v, o -> o.renameValue).add()
            .build();

        public String action;
        public String param;
        public String renameValue;
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final String errorKey;

    IslandRenamePopupPage(@Nonnull PlayerRef playerRef,
                          @Nonnull World sourceWorld,
                          @Nonnull ISkyblockPlugin plugin,
                          @Nullable String errorKey) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.errorKey = errorKey;
    }

    IslandRenamePopupPage(@Nonnull PlayerRef playerRef,
                          @Nonnull World sourceWorld,
                          @Nonnull ISkyblockPlugin plugin) {
        this(playerRef, sourceWorld, plugin, null);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandRenamePopup.ui");

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        String currentName = island != null ? plugin.getIslandService().getIslandDisplayName(island) : "";

        commandBuilder.set("#RenamePopupTitle.Text", plugin.rawText(playerRef, "ui.rename.title"));
        commandBuilder.set("#RenameIslandHint.Text", plugin.rawText(playerRef, "ui.rename.label"));
        commandBuilder.set("#BtnRenameCancel.Text", plugin.rawText(playerRef, "ui.rename.btn.cancel"));
        commandBuilder.set("#BtnRenameSave.Text", plugin.rawText(playerRef, "ui.rename.btn.save"));
        commandBuilder.set("#RenameIslandInput.Value", currentName);

        boolean hasError = errorKey != null && !errorKey.isBlank();
        commandBuilder.set("#RenameIslandError.Visible", hasError);
        commandBuilder.set("#RenameIslandError.Text", hasError ? plugin.rawText(playerRef, errorKey) : "");

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

        String action = data.action == null ? "" : data.action.trim();

        switch (action) {
            case "Cancel" -> {
                IslandMenuPage.open(player, ref, store, playerRef, sourceWorld, plugin);
            }
            case "Save" -> {
                IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
                String currentName = island != null ? plugin.getIslandService().getIslandDisplayName(island) : "";

                String name = normalizeName(data.renameValue);
                if (name.isBlank()) {
                    reopenWithError(player, ref, store, "ui.rename.error.blank");
                    return;
                }

                // If unchanged, close without rewriting the name.
                if (!currentName.isBlank() && name.equals(currentName.trim())) {
                    IslandMenuPage.open(player, ref, store, playerRef, sourceWorld, plugin);
                    return;
                }

                if (name.codePointCount(0, name.length()) > 16) {
                    reopenWithError(player, ref, store, "ui.rename.error.too_long");
                    return;
                }
                try {
                    plugin.getIslandService().setIslandName(playerRef, name);
                    IslandMenuPage.open(player, ref, store, playerRef, sourceWorld, plugin);
                } catch (IllegalStateException ex) {
                    String key = switch (ex.getMessage() == null ? "" : ex.getMessage()) {
                        case "no_island" -> "team.no_island";
                        default -> "ui.rename.error.not_leader";
                    };
                    reopenWithError(player, ref, store, key);
                }
            }
        }
    }

    private void reopenWithError(Player player,
                                 Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 String error) {
        player.getPageManager().openCustomPage(ref, store,
            new IslandRenamePopupPage(playerRef, sourceWorld, plugin, error));
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.replace('\u00A0', ' ').trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
