package com.sloyni.main.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import javax.annotation.Nonnull;

public class IslandWallApplyingPopupPage extends InteractiveCustomUIPage<IslandWallApplyingPopupPage.PageData> {

    private final PlayerRef playerRef;
    private final ISkyblockPlugin plugin;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
            .build();

        public String action;
    }

    IslandWallApplyingPopupPage(@Nonnull PlayerRef playerRef,
                                @Nonnull ISkyblockPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/IslandWallApplyingPopup.ui");
        commandBuilder.set("#WallApplyingTitle.Text", plugin.rawText(playerRef, "ui.menu.walls.applying.title"));
        commandBuilder.set("#WallApplyingMessage.Text", plugin.rawText(playerRef, "ui.menu.walls.applying.message"));
        commandBuilder.set("#WallApplyingHint.Text", plugin.rawText(playerRef, "ui.menu.walls.applying.hint"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        // Intentionally no interactions: this popup is auto-closed by reopening IslandMenuPage.
    }
}
