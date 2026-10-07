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
import java.util.List;
import javax.annotation.Nonnull;

public class ISkyblockAdminDashboardLanguagesPage
        extends InteractiveCustomUIPage<ISkyblockAdminDashboardLanguagesPage.PageData> {

    private static final int MAX_LANG_ROWS = 8;

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final ISkyblockAdminDashboardPage parent;

    private List<String> langSlots;
    private String errorMessage;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
            .builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Back", Codec.STRING), (o, v) -> o.back = v, o -> o.back).add()
            .append(new KeyedCodec<>("ToggleLang0", Codec.STRING), (o, v) -> o.toggleLang0 = v, o -> o.toggleLang0).add()
            .append(new KeyedCodec<>("ToggleLang1", Codec.STRING), (o, v) -> o.toggleLang1 = v, o -> o.toggleLang1).add()
            .append(new KeyedCodec<>("ToggleLang2", Codec.STRING), (o, v) -> o.toggleLang2 = v, o -> o.toggleLang2).add()
            .append(new KeyedCodec<>("ToggleLang3", Codec.STRING), (o, v) -> o.toggleLang3 = v, o -> o.toggleLang3).add()
            .append(new KeyedCodec<>("ToggleLang4", Codec.STRING), (o, v) -> o.toggleLang4 = v, o -> o.toggleLang4).add()
            .append(new KeyedCodec<>("ToggleLang5", Codec.STRING), (o, v) -> o.toggleLang5 = v, o -> o.toggleLang5).add()
            .append(new KeyedCodec<>("ToggleLang6", Codec.STRING), (o, v) -> o.toggleLang6 = v, o -> o.toggleLang6).add()
            .append(new KeyedCodec<>("ToggleLang7", Codec.STRING), (o, v) -> o.toggleLang7 = v, o -> o.toggleLang7).add()
            .build();

        public String back;
        public String toggleLang0;
        public String toggleLang1;
        public String toggleLang2;
        public String toggleLang3;
        public String toggleLang4;
        public String toggleLang5;
        public String toggleLang6;
        public String toggleLang7;
    }

    public ISkyblockAdminDashboardLanguagesPage(@Nonnull PlayerRef playerRef,
                                                @Nonnull World sourceWorld,
                                                @Nonnull ISkyblockPlugin plugin,
                                                @Nonnull ISkyblockAdminDashboardPage parent) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;
        this.sourceWorld = sourceWorld;
        this.parent = parent;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {

        commandBuilder.append("Pages/ISkyblockAdminDashboard.LanguagesManager.ui");

        String language = plugin.resolvePlayerLanguage(playerRef);
        langSlots = plugin.getMessageManager().getAvailableLanguages();
        List<String> enabledLanguages = plugin.getConfigManager().getConfig().getSettings().getEnabledLanguages();

        commandBuilder.set("#LangMgrTitle.Text",       plugin.getMessageManager().get("ui.admin_dashboard.settings.manage_languages", language));
        commandBuilder.set("#LangMgrDescription.Text", plugin.getMessageManager().get("ui.admin_dashboard.settings.lang_section_description", language));
        commandBuilder.set("#LangMgrBackButton.Text",  plugin.getMessageManager().get("ui.common.back", language));
        commandBuilder.set("#LangMgrError.Visible", errorMessage != null && !errorMessage.isBlank());
        commandBuilder.set("#LangMgrError.Text", errorMessage == null ? "" : errorMessage);

        for (int i = 0; i < MAX_LANG_ROWS; i++) {
            if (i < langSlots.size()) {
                String locale = langSlots.get(i);
                boolean enabled = enabledLanguages != null && enabledLanguages.stream()
                    .anyMatch(e -> e != null && e.equalsIgnoreCase(locale));
                commandBuilder.set("#LangRow" + i + ".Visible", true);
                commandBuilder.set("#LangRow" + i + ".Background", enabled ? "#223149" : "#182638");
                commandBuilder.set("#LangEnabled" + i + ".Value", enabled);
                commandBuilder.set("#LangLabel" + i + ".Text", plugin.getLanguageService().langLabel(locale));
            } else {
                commandBuilder.set("#LangRow" + i + ".Visible", false);
            }
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LangMgrBackButton",
            EventData.of("Back", "true"), false);

        for (int i = 0; i < MAX_LANG_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LangEnabled" + i,
                EventData.of("ToggleLang" + i, String.valueOf(i)), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (data.back != null) {
            errorMessage = null;
            player.getPageManager().openCustomPage(ref, store, parent);
            return;
        }

        if (applyLanguageToggle(player, ref, store, 0, data.toggleLang0)) return;
        if (applyLanguageToggle(player, ref, store, 1, data.toggleLang1)) return;
        if (applyLanguageToggle(player, ref, store, 2, data.toggleLang2)) return;
        if (applyLanguageToggle(player, ref, store, 3, data.toggleLang3)) return;
        if (applyLanguageToggle(player, ref, store, 4, data.toggleLang4)) return;
        if (applyLanguageToggle(player, ref, store, 5, data.toggleLang5)) return;
        if (applyLanguageToggle(player, ref, store, 6, data.toggleLang6)) return;
        if (applyLanguageToggle(player, ref, store, 7, data.toggleLang7)) return;
    }

    private boolean applyLanguageToggle(@Nonnull Player player,
                                        @Nonnull Ref<EntityStore> ref,
                                        @Nonnull Store<EntityStore> store,
                                        int index,
                                        String rawValue) {
        if (rawValue == null) {
            return false;
        }
        if (langSlots == null || index < 0 || index >= langSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return true;
        }

        String locale = langSlots.get(index);
        List<String> enabled = plugin.getConfigManager().getConfig().getSettings().getEnabledLanguages();
        boolean currentlyEnabled = enabled != null && enabled.stream()
            .anyMatch(e -> e != null && e.equalsIgnoreCase(locale));
        boolean desiredEnabled = !currentlyEnabled;

        if (currentlyEnabled != desiredEnabled) {
            if (!desiredEnabled) {
                long enabledCount = enabled == null ? 0L : enabled.stream()
                    .filter(e -> e != null && !e.isBlank())
                    .count();
                if (enabledCount <= 1L) {
                    String language = plugin.resolvePlayerLanguage(playerRef);
                    errorMessage = plugin.getMessageManager().get("ui.admin_dashboard.settings.languages_min_one", language);
                    player.getPageManager().openCustomPage(ref, store, this);
                    return true;
                }
            }
            errorMessage = null;
            plugin.getSettingsCommandHandler().handleLanguageEnabledSetting(locale, desiredEnabled);
        }

        player.getPageManager().openCustomPage(ref, store, this);
        return true;
    }
}
