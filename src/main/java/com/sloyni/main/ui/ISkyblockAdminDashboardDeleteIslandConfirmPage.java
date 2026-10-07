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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class ISkyblockAdminDashboardDeleteIslandConfirmPage
        extends InteractiveCustomUIPage<ISkyblockAdminDashboardDeleteIslandConfirmPage.PageData> {

    private final ISkyblockPlugin plugin;
    private final ISkyblockAdminDashboardIslandsViewerPage viewerPage;
    private final ISkyblockAdminDashboardPage parent;
    private final UUID targetUuid;
    private final String targetName;

    private String errorMessage = null;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
            .builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Cancel", Codec.STRING), (o, v) -> o.cancel = v, o -> o.cancel).add()
            .append(new KeyedCodec<>("Confirm", Codec.STRING), (o, v) -> o.confirm = v, o -> o.confirm).add()
            .build();

        public String cancel;
        public String confirm;
    }

    public ISkyblockAdminDashboardDeleteIslandConfirmPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull ISkyblockPlugin plugin,
            @Nonnull ISkyblockAdminDashboardIslandsViewerPage viewerPage,
            @Nonnull ISkyblockAdminDashboardPage parent,
            @Nonnull UUID targetUuid,
            @Nonnull String targetName) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.plugin = plugin;
        this.viewerPage = viewerPage;
        this.parent = parent;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/ISkyblockAdminDashboard.DeleteIslandConfirm.ui");
        String language = getLanguage();

        commandBuilder.set("#DeleteIslandConfirmWarning.Text", text(language, "ui.admin_dashboard.islands.delete_confirm"));
        commandBuilder.set("#DeleteIslandConfirmDescription.Text", text(language, "ui.admin_dashboard.islands.danger_description"));
        commandBuilder.set("#DeleteIslandConfirmIslandText.Text", targetName);
        commandBuilder.set("#DeleteIslandConfirmError.Visible", errorMessage != null && !errorMessage.isBlank());
        commandBuilder.set("#DeleteIslandConfirmError.Text", errorMessage == null ? "" : errorMessage);
        commandBuilder.set("#DeleteIslandConfirmCancel.Text", text(language, "ui.common.cancel"));
        commandBuilder.set("#DeleteIslandConfirmConfirm.Text", text(language, "ui.common.delete"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteIslandConfirmCancel",
            EventData.of("Cancel", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteIslandConfirmConfirm",
            EventData.of("Confirm", "true"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.cancel != null) {
            player.getPageManager().openCustomPage(ref, store, viewerPage);
            return;
        }

        if (data.confirm != null) {
            try {
                plugin.getIslandService().deleteIslandByTarget(targetUuid);
                player.getPageManager().openCustomPage(ref, store, parent);
            } catch (Exception ex) {
                errorMessage = mapError(ex.getMessage());
                player.getPageManager().openCustomPage(ref, store, this);
            }
        }
    }

    private String mapError(String rawMessage) {
        String message = rawMessage == null ? "unknown" : rawMessage;
        String language = getLanguage();
        return switch (message) {
            case "target_not_found" -> text(language, "islands.admin.target_not_found", Map.of("player", targetName));
            case "no_island" -> text(language, "islands.admin.no_island", Map.of("player", targetName));
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
}
