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
import com.sloyni.main.island.IslandTemplate;
import com.sloyni.main.util.Chat;
import com.sloyni.main.config.PluginConfig;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public class IslandTemplateSelectPage extends InteractiveCustomUIPage<IslandTemplateSelectPage.PageData> {
    private static final int RESET_CREATE_MAX_RETRIES = 60;
    private static final long RESET_CREATE_RETRY_DELAY_MS = 150L;
    private static final ConcurrentMap<UUID, Boolean> RESET_TEMPLATE_IN_PROGRESS = new ConcurrentHashMap<>();

    private enum FlowMode {
        CREATE_ONLY,
        RESET_THEN_CREATE
    }

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World world;
    private final int templateIndex;
    private final FlowMode flowMode;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Select", Codec.STRING), (o, v) -> o.select = v, o -> o.select).add()
            .append(new KeyedCodec<>("Cancel", Codec.STRING), (o, v) -> o.cancel = v, o -> o.cancel).add()
            .append(new KeyedCodec<>("Prev", Codec.STRING), (o, v) -> o.prev = v, o -> o.prev).add()
            .append(new KeyedCodec<>("Next", Codec.STRING), (o, v) -> o.next = v, o -> o.next).add()
            .build();

        public String select;
        public String cancel;
        public String prev;
        public String next;
    }

    public IslandTemplateSelectPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull ISkyblockPlugin plugin) {
        this(playerRef, world, plugin, 0, FlowMode.CREATE_ONLY);
    }

    public IslandTemplateSelectPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull ISkyblockPlugin plugin, int templateIndex) {
        this(playerRef, world, plugin, templateIndex, FlowMode.CREATE_ONLY);
    }

    public static IslandTemplateSelectPage forReset(@Nonnull PlayerRef playerRef,
                                                    @Nonnull World world,
                                                    @Nonnull ISkyblockPlugin plugin) {
        return new IslandTemplateSelectPage(playerRef, world, plugin, 0, FlowMode.RESET_THEN_CREATE);
    }

    private IslandTemplateSelectPage(@Nonnull PlayerRef playerRef,
                                     @Nonnull World world,
                                     @Nonnull ISkyblockPlugin plugin,
                                     int templateIndex,
                                     @Nonnull FlowMode flowMode) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.world = world;
        this.plugin = plugin;
        this.templateIndex = templateIndex;
        this.flowMode = flowMode;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        List<IslandTemplate> templates = plugin.getIslandTemplateManager().getEnabled();
        int index = templates.isEmpty() ? 0 : Math.max(0, Math.min(templateIndex, templates.size() - 1));
        IslandTemplate template = templates.isEmpty()
            ? plugin.getIslandTemplateManager().getDefault()
            : templates.get(index);
        String language = plugin.resolvePlayerLanguage(playerRef);
        PluginConfig.SettingsConfig settings = plugin.getConfigManager().getConfig().getSettings();
        boolean translateTemplates = settings.isTranslateTemplates();
        String fallbackLanguage = settings.getFallbackLanguage();

        commandBuilder.append("Pages/ISkyblockTemplateSelect.ui");
        commandBuilder.set("#TemplateTitle.Text", resolveTitle(template, translateTemplates, language, fallbackLanguage));
        commandBuilder.set("#TemplateDescription.Text", resolveDescription(template, translateTemplates, language, fallbackLanguage));
        String previewPath = plugin.getTemplateImageManager().getImagePath(template.getId(), template.getPreviewImage());
        if (previewPath != null && !previewPath.isBlank()) {
            commandBuilder.set("#Preview.Background", previewPath);
        }
        commandBuilder.set("#SelectButton.Text", plugin.getMessageManager().get("ui.island.select", language));
        commandBuilder.set("#CancelButton.Text", plugin.getMessageManager().get("ui.island.cancel", language));
        commandBuilder.set("#Counter.Text", templates.size() > 1 ? (index + 1) + " / " + templates.size() : "");
        commandBuilder.set("#PrevButton.Visible", templates.size() > 1);
        commandBuilder.set("#NextButton.Visible", templates.size() > 1);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SelectButton", EventData.of("Select", template.getId()), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Cancel", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PrevButton", EventData.of("Prev", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#NextButton", EventData.of("Next", "true"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.cancel != null) {
            if (flowMode == FlowMode.RESET_THEN_CREATE) {
                IslandMenuPage.open(player, ref, store, playerRef, resolveCurrentWorld(player, store), plugin);
            } else {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            return;
        }

        List<IslandTemplate> templates = plugin.getIslandTemplateManager().getEnabled();
        int total = templates.size();

        if (data.prev != null && total > 1) {
            int newIndex = templateIndex <= 0 ? total - 1 : templateIndex - 1;
            World currentWorld = resolveCurrentWorld(player, store);
            player.getPageManager().openCustomPage(ref, store,
                new IslandTemplateSelectPage(playerRef, currentWorld, plugin, newIndex, flowMode));
            return;
        }

        if (data.next != null && total > 1) {
            int newIndex = templateIndex >= total - 1 ? 0 : templateIndex + 1;
            World currentWorld = resolveCurrentWorld(player, store);
            player.getPageManager().openCustomPage(ref, store,
                new IslandTemplateSelectPage(playerRef, currentWorld, plugin, newIndex, flowMode));
            return;
        }

        if (data.select != null) {
            if (flowMode == FlowMode.RESET_THEN_CREATE) {
                resetThenCreateFromTemplate(player, ref, store, data.select);
                return;
            }

            player.getPageManager().setPage(ref, store, Page.None);
            plugin.createIslandFromTemplate(playerRef, resolveCurrentWorld(player, store), data.select);
        }
    }

    private void resetThenCreateFromTemplate(Player player,
                                             Ref<EntityStore> ref,
                                             Store<EntityStore> store,
                                             String templateId) {
        String selectedTemplateId = templateId == null ? "" : templateId.trim();
        if (selectedTemplateId.isBlank()) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return;
        }
        if (RESET_TEMPLATE_IN_PROGRESS.putIfAbsent(playerUuid, Boolean.TRUE) != null) {
            return;
        }

        var lobby = plugin.getConfigManager().getConfig().getLobby();
        if (lobby == null || !lobby.isConfigured() || lobby.getWorldName() == null || lobby.getWorldName().isBlank()) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "lobby.not_set"));
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
            return;
        }

        IslandData island = plugin.getIslandService().getPlayerIsland(playerRef.getUuid());
        if (island == null) {
            player.getPageManager().setPage(ref, store, Page.None);
            plugin.createIslandFromTemplate(playerRef, resolveCurrentWorld(player, store), selectedTemplateId);
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
            return;
        }

        if (!island.isLeader(playerRef.getUuid())) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "ui.menu.permissions.error.not_leader"));
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
            return;
        }
        if (island.getMemberCount() > 0) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "ui.menu.reset.error.leader_has_members"));
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
            return;
        }

        int remainingResets = plugin.getIslandService().getRemainingResets(playerRef.getUuid());
        if (remainingResets <= 0) {
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "island.create.no_resets"));
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);

        World currentWorld = resolveCurrentWorld(player, store);
        plugin.getIslandService().resetIslandAndTeleportToLobby(
            playerRef,
            currentWorld,
            lobby.getWorldName(),
            lobby.getX(),
            lobby.getY(),
            lobby.getZ(),
            lobby.getPitch(),
            lobby.getYaw()
        ).thenRun(() -> createSelectedTemplateAfterReset(selectedTemplateId, 0, resolveCurrentWorld(player, store))).exceptionally(ex -> {
            String error = resolveThrowableMessage(ex);
            Chat.send(playerRef, plugin.trNoPrefix(playerRef, "ui.menu.reset.error.failed", Map.of("error", error)));
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
            return null;
        });
    }

    private void createSelectedTemplateAfterReset(String templateId,
                                                  int attempt,
                                                  World preferredWorld) {
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return;
        }

        if (attempt > RESET_CREATE_MAX_RETRIES) {
            // Last chance best-effort create to avoid leaving the player stuck with no island.
            Ref<EntityStore> finalRef = playerRef.getReference();
            Store<EntityStore> finalStore = finalRef == null ? null : finalRef.getStore();
            EntityStore finalEntityStore = finalStore == null ? null : finalStore.getExternalData();
            World finalWorld = finalEntityStore != null && finalEntityStore.getWorld() != null
                ? finalEntityStore.getWorld()
                : preferredWorld;

            if (finalWorld != null) {
                finalWorld.execute(() -> {
                    plugin.createIslandFromTemplate(playerRef, finalWorld, templateId);
                    RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
                });
            } else {
                RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
                Chat.send(playerRef, plugin.trNoPrefix(playerRef, "ui.menu.reset.error.failed", Map.of("error", "post_reset_timeout")));
            }
            return;
        }

        // Wait until reset has fully removed island state before creating the replacement.
        if (plugin.getIslandService().getPlayerIsland(playerUuid) != null) {
            scheduleResetCreateRetry(templateId, attempt + 1, preferredWorld);
            return;
        }

        Ref<EntityStore> liveRef = playerRef.getReference();
        if (liveRef == null || !liveRef.isValid()) {
            scheduleResetCreateRetry(templateId, attempt + 1, preferredWorld);
            return;
        }

        Store<EntityStore> liveStore = liveRef.getStore();
        if (liveStore == null) {
            scheduleResetCreateRetry(templateId, attempt + 1, preferredWorld);
            return;
        }

        EntityStore liveEntityStore = liveStore.getExternalData();
        World sourceWorld = liveEntityStore != null && liveEntityStore.getWorld() != null
            ? liveEntityStore.getWorld()
            : preferredWorld;
        if (sourceWorld == null) {
            scheduleResetCreateRetry(templateId, attempt + 1, world);
            return;
        }

        sourceWorld.execute(() -> {
            if (plugin.getIslandService().getPlayerIsland(playerUuid) != null) {
                scheduleResetCreateRetry(templateId, attempt + 1, sourceWorld);
                return;
            }

            Ref<EntityStore> refNow = playerRef.getReference();
            if (refNow == null || !refNow.isValid() || refNow.getStore() == null) {
                scheduleResetCreateRetry(templateId, attempt + 1, sourceWorld);
                return;
            }

            plugin.createIslandFromTemplate(playerRef, sourceWorld, templateId);
            RESET_TEMPLATE_IN_PROGRESS.remove(playerUuid);
        });
    }

    private void scheduleResetCreateRetry(String templateId, int attempt, World preferredWorld) {
        CompletableFuture.delayedExecutor(RESET_CREATE_RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
            .execute(() -> createSelectedTemplateAfterReset(templateId, attempt, preferredWorld));
    }

    private String resolveThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
            return throwable.getCause().getMessage();
        }
        return throwable.getMessage() == null ? "unknown" : throwable.getMessage();
    }

    private World resolveCurrentWorld(Player player, Store<EntityStore> store) {
        if (player != null && player.getWorld() != null) {
            return player.getWorld();
        }
        EntityStore entityStore = store.getExternalData();
        if (entityStore != null && entityStore.getWorld() != null) {
            return entityStore.getWorld();
        }
        return world;
    }

    private String resolveTitle(IslandTemplate template,
                                boolean translateTemplates,
                                String language,
                                String fallbackLanguage) {
        if (template == null) return "";
        if (!translateTemplates) {
            if (template.getStaticTitle() != null && !template.getStaticTitle().isBlank()) {
                return template.getStaticTitle();
            }
        }

        if (template.getTitleTranslations() != null) {
            String translated = template.getTitleTranslations().get(language);
            if (translated != null && !translated.isBlank()) return translated;
            translated = template.getTitleTranslations().get(fallbackLanguage);
            if (translated != null && !translated.isBlank()) return translated;
        }

        String key = template.getTitleKey();
        return translateTemplates
            ? plugin.getMessageManager().get(key, language)
            : key;
    }

    private String resolveDescription(IslandTemplate template,
                                      boolean translateTemplates,
                                      String language,
                                      String fallbackLanguage) {
        if (template == null) return "";
        if (!translateTemplates) {
            if (template.getStaticDescription() != null && !template.getStaticDescription().isBlank()) {
                return template.getStaticDescription();
            }
        }

        if (template.getDescriptionTranslations() != null) {
            String translated = template.getDescriptionTranslations().get(language);
            if (translated != null && !translated.isBlank()) return translated;
            translated = template.getDescriptionTranslations().get(fallbackLanguage);
            if (translated != null && !translated.isBlank()) return translated;
        }

        String key = template.getDescriptionKey();
        return translateTemplates
            ? plugin.getMessageManager().get(key, language)
            : key;
    }
}
