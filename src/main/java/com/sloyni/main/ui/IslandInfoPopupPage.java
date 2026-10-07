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
import javax.annotation.Nonnull;

public class IslandInfoPopupPage extends InteractiveCustomUIPage<IslandInfoPopupPage.PageData> {

    public enum ReturnTab {
        SETTINGS,
        TEAM,
        HOMES,
        WARPS,
        TOP
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
    private final String title;
    private final String message;
    private final ReturnTab returnTab;

    IslandInfoPopupPage(@Nonnull PlayerRef playerRef,
                        @Nonnull World sourceWorld,
                        @Nonnull ISkyblockPlugin plugin,
                        @Nonnull String title,
                        @Nonnull String message,
                        @Nonnull ReturnTab returnTab) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.title = safe(title);
        this.message = safe(message);
        this.returnTab = returnTab;
    }

    IslandInfoPopupPage(@Nonnull PlayerRef playerRef,
                        @Nonnull World sourceWorld,
                        @Nonnull ISkyblockPlugin plugin,
                        @Nonnull String title,
                        @Nonnull String message) {
        this(playerRef, sourceWorld, plugin, title, message, ReturnTab.HOMES);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandInfoPopup.ui");

        commandBuilder.set("#InfoPopupTitle.Text", title);
        commandBuilder.set("#InfoPopupMessage.Text", message);
        commandBuilder.set("#BtnInfoClose.Text", plugin.rawText(playerRef, "ui.common.close"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnInfoClose",
            EventData.of("Action", "Close"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        switch (returnTab) {
            case SETTINGS -> IslandMenuPage.open(player, ref, store, playerRef, sourceWorld, plugin);
            case TEAM -> IslandMenuPage.openTeam(player, ref, store, playerRef, sourceWorld, plugin);
            case HOMES -> IslandMenuPage.openHomes(player, ref, store, playerRef, sourceWorld, plugin);
            case WARPS -> IslandMenuPage.openWarps(player, ref, store, playerRef, sourceWorld, plugin);
            case TOP -> IslandMenuPage.openTop(player, ref, store, playerRef, sourceWorld, plugin);
            default -> IslandMenuPage.open(player, ref, store, playerRef, sourceWorld, plugin);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
