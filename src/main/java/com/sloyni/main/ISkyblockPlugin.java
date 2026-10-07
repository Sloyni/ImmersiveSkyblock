package com.sloyni.main;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.sloyni.main.addons.homes.PlayerHomeLimitRepository;
import com.sloyni.main.addons.levels.IslandValueService;
import com.sloyni.main.addons.warps.IslandWarpRepository;
import com.sloyni.main.asset.TemplateImageManager;
import com.sloyni.main.addons.AddonManager;
import com.sloyni.main.commands.InitCommandHandler;
import com.sloyni.main.commands.IslandCommandHandler;
import com.sloyni.main.commands.ResetCommandHandler;
import com.sloyni.main.commands.SettingsCommandHandler;
import com.sloyni.main.commands.admin.ISkyblockCommand;
import com.sloyni.main.commands.admin.SettingsCommand;
import com.sloyni.main.commands.user.IslandCommand;
import com.sloyni.main.config.ConfigManager;
import com.sloyni.main.config.MessagesConfig;
import com.sloyni.main.config.MessagesConfigManager;
import com.sloyni.main.events.IslandRespawnRoutingHandler;
import com.sloyni.main.events.OreGeneratorHandler;
import com.sloyni.main.island.IslandAccessController;
import com.sloyni.main.island.IslandProtectionService;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.island.IslandTemplateManager;
import com.sloyni.main.island.TeamInviteManager;
import com.sloyni.main.island.PlayerIslandSizeRepository;
import com.sloyni.main.island.PlayerResetRepository;
import com.sloyni.main.lang.MessageManager;
import com.sloyni.main.lang.LanguageService;
import com.sloyni.main.lang.KnownPlayerRepository;
import com.sloyni.main.lang.PlayerPreferenceRepository;
import com.sloyni.main.menu.IslandMenuHudManager;
import com.sloyni.main.menu.IslandMenuKeybindManager;
import com.sloyni.main.protection.IslandBreakProtection;
import com.sloyni.main.protection.IslandDamageBlockProtection;
import com.sloyni.main.protection.IslandDropProtection;
import com.sloyni.main.protection.IslandPlaceProtection;
import com.sloyni.main.protection.IslandPvpProtection;
import com.sloyni.main.protection.VisitorDamageImmunityProtection;
import com.sloyni.main.protection.IslandEntityDamageProtection;
import com.sloyni.main.protection.IslandMobProtectionHandler;
import com.sloyni.main.protection.IslandUseBlockProtection;
import com.sloyni.main.protection.VisitorMobAggroPreventionSystem;
import com.sloyni.main.protection.MobSpawnProtectionSystem;
import com.sloyni.main.protection.VoidKillSystem;
import com.sloyni.main.protection.IslandAreaTitleSystem;
import com.sloyni.main.events.IslandMobKillStatsHandler;
import com.sloyni.main.events.IslandPlayerDeathStatsHandler;
import com.sloyni.main.events.IslandTimePlayedTracker;
import com.sloyni.main.island.stats.IslandStatsRepository;
import com.sloyni.main.island.stats.IslandStatsService;
import com.sloyni.main.interactions.IslandBreakBlockInteraction;
import com.sloyni.main.interactions.IslandDestroyBlockInteraction;
import com.sloyni.main.interactions.IslandHarvestCropInteraction;
import com.sloyni.main.interactions.IslandPlaceFluidInteraction;
import com.sloyni.main.interactions.IslandRefillContainerInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.sloyni.main.world.WorldManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public class ISkyblockPlugin extends JavaPlugin {
    private static ISkyblockPlugin instance;

    private ConfigManager configManager;
    private MessagesConfigManager messagesConfigManager;
    private MessageManager messageManager;
    private LanguageService languageService;
    private PlayerPreferenceRepository playerPreferenceRepository;
    private KnownPlayerRepository knownPlayerRepository;
    private WorldManager worldManager;
    private IslandRepository islandRepository;
    private IslandWarpRepository islandWarpRepository;
    private PlayerResetRepository playerResetRepository;
    private PlayerHomeLimitRepository playerHomeLimitRepository;
    private PlayerIslandSizeRepository playerIslandSizeRepository;
    private IslandTemplateManager islandTemplateManager;
    private TemplateImageManager templateImageManager;
    private IslandService islandService;
    private ResetCommandHandler resetCommandHandler;
    private SettingsCommandHandler settingsCommandHandler;
    private SettingsCommand settingsCommand;
    private ISkyblockCommand iSkyblockCommand;
    private IslandCommandHandler islandCommandHandler;
    private InitCommandHandler initCommandHandler;
    private IslandValueService islandValueService;
    private IslandProtectionService protectionService;
    private TeamInviteManager teamInviteManager;
    private IslandAccessController islandAccessController;
    private AddonManager addonManager;
    private IslandMenuKeybindManager islandMenuKeybindManager;
    private IslandMenuHudManager islandMenuHudManager;
    private IslandAreaTitleSystem islandAreaTitleSystem;
    private IslandStatsRepository islandStatsRepository;
    private IslandStatsService islandStatsService;
    private IslandTimePlayedTracker islandTimePlayedTracker;

    public ISkyblockPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Nonnull
    public static ISkyblockPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        this.configManager = new ConfigManager(getDataDirectory().toFile());
        this.messagesConfigManager = new MessagesConfigManager(getDataDirectory().toFile());
        this.messageManager = new MessageManager(getDataDirectory().toFile());
        this.languageService = new LanguageService(this.configManager, this.messageManager);
        this.playerPreferenceRepository = new PlayerPreferenceRepository(getDataDirectory().toFile());
        this.knownPlayerRepository = new KnownPlayerRepository(getDataDirectory().toFile());
        this.messageManager.loadLanguage(this.configManager.getConfig().getLanguage());

        this.worldManager = new WorldManager();
        this.islandRepository = new IslandRepository(getDataDirectory().toFile());
        this.islandWarpRepository = new IslandWarpRepository(getDataDirectory().toFile());
        this.playerResetRepository = new PlayerResetRepository(getDataDirectory().toFile());
        this.playerHomeLimitRepository = new PlayerHomeLimitRepository(getDataDirectory().toFile());
        this.playerIslandSizeRepository = new PlayerIslandSizeRepository(getDataDirectory().toFile());
        this.islandTemplateManager = new IslandTemplateManager(getDataDirectory().toFile());
        this.templateImageManager = new TemplateImageManager(getDataDirectory().toFile());
        this.templateImageManager.init();
        this.islandService = new IslandService(
            this.islandRepository,
            this.islandTemplateManager,
            this.playerResetRepository,
            this.playerIslandSizeRepository,
            this.worldManager,
            this.configManager.getConfig()
        );
        this.resetCommandHandler = new ResetCommandHandler(this.configManager, this.messageManager, this.islandService);
        this.addonManager = new AddonManager(
            this,
            getDataDirectory().toFile(),
            this.configManager.getConfig(),
            this.islandRepository,
            this.islandWarpRepository,
            this.playerHomeLimitRepository,
            this.islandTemplateManager,
            this.islandService,
            this.worldManager
        );
        this.islandValueService = this.addonManager.getLevelsAddon().getIslandValueService();
        this.settingsCommandHandler = new SettingsCommandHandler(this.configManager, this.islandService, this.addonManager, this.messageManager, this.languageService);
        this.settingsCommand = new SettingsCommand(this);
        this.protectionService = new IslandProtectionService(this.islandRepository, this.configManager.getConfig());
        this.islandAccessController = new IslandAccessController(this.configManager, this.messageManager, this.islandService, this.protectionService);
        this.teamInviteManager = new TeamInviteManager();
        this.islandCommandHandler = new IslandCommandHandler(this);
        this.initCommandHandler = new InitCommandHandler(this);

        try {
            getEntityStoreRegistry().registerSystem(new IslandBreakProtection());
            System.out.println("[ISkyblock] IslandBreakProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandBreakProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandPlaceProtection());
            System.out.println("[ISkyblock] IslandPlaceProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandPlaceProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandDamageBlockProtection());
            System.out.println("[ISkyblock] IslandDamageBlockProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandDamageBlockProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandUseBlockProtection());
            System.out.println("[ISkyblock] IslandUseBlockProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandUseBlockProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandDropProtection());
            System.out.println("[ISkyblock] IslandDropProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandDropProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandPvpProtection());
            System.out.println("[ISkyblock] IslandPvpProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandPvpProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new VisitorDamageImmunityProtection());
            System.out.println("[ISkyblock] VisitorDamageImmunityProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register VisitorDamageImmunityProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandMobProtectionHandler());
            System.out.println("[ISkyblock] IslandMobProtectionHandler registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandMobProtectionHandler: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandEntityDamageProtection());
            System.out.println("[ISkyblock] IslandEntityDamageProtection registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandEntityDamageProtection: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new VisitorMobAggroPreventionSystem());
            System.out.println("[ISkyblock] VisitorMobAggroPreventionSystem registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register VisitorMobAggroPreventionSystem: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new MobSpawnProtectionSystem());
            System.out.println("[ISkyblock] MobSpawnProtectionSystem registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register MobSpawnProtectionSystem: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new VoidKillSystem());
            System.out.println("[ISkyblock] VoidKillSystem registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register VoidKillSystem: " + e.getMessage());
        }
        this.islandAreaTitleSystem = new IslandAreaTitleSystem();
        try {
            getEntityStoreRegistry().registerSystem(islandAreaTitleSystem);
            System.out.println("[ISkyblock] IslandAreaTitleSystem registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandAreaTitleSystem: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandRespawnRoutingHandler());
            System.out.println("[ISkyblock] IslandRespawnRoutingHandler registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandRespawnRoutingHandler: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new OreGeneratorHandler());
            System.out.println("[ISkyblock] OreGeneratorHandler registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register OreGeneratorHandler: " + e.getMessage());
        }

        try {
            getCodecRegistry(Interaction.CODEC).register("PlaceFluid", IslandPlaceFluidInteraction.class, IslandPlaceFluidInteraction.ISLAND_CODEC);
            System.out.println("[ISkyblock] IslandPlaceFluidInteraction registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandPlaceFluidInteraction: " + e.getMessage());
        }
        try {
            getCodecRegistry(Interaction.CODEC).register("RefillContainer", IslandRefillContainerInteraction.class, IslandRefillContainerInteraction.ISLAND_CODEC);
            System.out.println("[ISkyblock] IslandRefillContainerInteraction registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandRefillContainerInteraction: " + e.getMessage());
        }
        try {
            getCodecRegistry(Interaction.CODEC).register("HarvestCrop", IslandHarvestCropInteraction.class, IslandHarvestCropInteraction.ISLAND_CODEC);
            System.out.println("[ISkyblock] IslandHarvestCropInteraction registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandHarvestCropInteraction: " + e.getMessage());
        }
        try {
            getCodecRegistry(Interaction.CODEC).register("BreakBlock", IslandBreakBlockInteraction.class, IslandBreakBlockInteraction.ISLAND_CODEC);
            System.out.println("[ISkyblock] IslandBreakBlockInteraction registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandBreakBlockInteraction: " + e.getMessage());
        }
        try {
            getCodecRegistry(Interaction.CODEC).register("DestroyBlock", IslandDestroyBlockInteraction.class, IslandDestroyBlockInteraction.ISLAND_CODEC);
            System.out.println("[ISkyblock] IslandDestroyBlockInteraction registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandDestroyBlockInteraction: " + e.getMessage());
        }

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, islandAccessController::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::logPlayerJoinLanguage);

        // ─── Island Statistics ────────────────────────────────────────────────
        this.islandStatsRepository = new IslandStatsRepository(getDataDirectory().toFile());
        this.islandStatsService = new IslandStatsService(this.islandStatsRepository, this.islandRepository, this.protectionService);
        this.islandTimePlayedTracker = new IslandTimePlayedTracker(this);
        try {
            getEntityStoreRegistry().registerSystem(new IslandMobKillStatsHandler());
            System.out.println("[ISkyblock] IslandMobKillStatsHandler registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandMobKillStatsHandler: " + e.getMessage());
        }
        try {
            getEntityStoreRegistry().registerSystem(new IslandPlayerDeathStatsHandler());
            System.out.println("[ISkyblock] IslandPlayerDeathStatsHandler registered");
        } catch (Exception e) {
            System.err.println("[ISkyblock] Failed to register IslandPlayerDeathStatsHandler: " + e.getMessage());
        }
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, islandTimePlayedTracker::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, islandTimePlayedTracker::onPlayerDisconnect);

        this.islandMenuHudManager = new IslandMenuHudManager(this);
        this.islandMenuKeybindManager = new IslandMenuKeybindManager(this);
        this.islandMenuKeybindManager.start();
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, islandMenuHudManager::onPlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, islandMenuHudManager::onPlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, islandMenuKeybindManager::onPlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, (PlayerDisconnectEvent e) -> {
            if (e != null && e.getPlayerRef() != null && e.getPlayerRef().getUuid() != null && islandAreaTitleSystem != null) {
                islandAreaTitleSystem.onPlayerDisconnect(e.getPlayerRef().getUuid());
            }
        });
        islandAccessController.startLockedIslandEnforcement();

        this.iSkyblockCommand = new ISkyblockCommand(this, this.settingsCommand);
        getCommandRegistry().registerCommand((AbstractCommand) this.iSkyblockCommand);
        getCommandRegistry().registerCommand((AbstractCommand) new IslandCommand(this));
    }

    @Override
    protected void shutdown() {
        if (islandAreaTitleSystem != null) {
            islandAreaTitleSystem.shutdown();
        }
        if (islandMenuKeybindManager != null) {
            islandMenuKeybindManager.shutdown();
        }
        if (islandMenuHudManager != null) {
            islandMenuHudManager.shutdown();
        }
        if (islandStatsService != null) {
            islandStatsService.shutdown();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public LanguageService getLanguageService() {
        return languageService;
    }

    public IslandStatsService getIslandStatsService() {
        return islandStatsService;
    }

    public PlayerPreferenceRepository getPlayerPreferenceRepository() {
        return playerPreferenceRepository;
    }

    public KnownPlayerRepository getKnownPlayerRepository() {
        return knownPlayerRepository;
    }

    public IslandService getIslandService() {
        return islandService;
    }

    public ResetCommandHandler getResetCommandHandler() {
        return resetCommandHandler;
    }

    public SettingsCommandHandler getSettingsCommandHandler() {
        return settingsCommandHandler;
    }

    public SettingsCommand getSettingsCommand() {
        return settingsCommand;
    }

    public void refreshAddonCommandTrees() {
        if (settingsCommand != null) {
            settingsCommand.syncAddonSubCommands();
        }
        if (iSkyblockCommand != null) {
            iSkyblockCommand.syncAddonSubCommands();
        }
    }

    public IslandCommandHandler getIslandCommandHandler() {
        return islandCommandHandler;
    }

    public InitCommandHandler getInitCommandHandler() {
        return initCommandHandler;
    }

    public IslandTemplateManager getIslandTemplateManager() {
        return islandTemplateManager;
    }

    public TemplateImageManager getTemplateImageManager() {
        return templateImageManager;
    }

    public void reloadTemplateImages() {
        if (templateImageManager != null) {
            templateImageManager.reload();
        }
    }

    public IslandValueService getIslandValueService() {
        return islandValueService;
    }

    public com.sloyni.main.menu.IslandMenuHudManager getIslandMenuHudManager() {
        return islandMenuHudManager;
    }

    public AddonManager getAddonManager() {
        return addonManager;
    }

    public IslandProtectionService getProtectionService() {
        return protectionService;
    }

    public IslandAccessController getIslandAccessController() {
        return islandAccessController;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public TeamInviteManager getTeamInviteManager() {
        return teamInviteManager;
    }

    public void createIslandFromTemplate(PlayerRef playerRef, World sourceWorld, String templateId) {
        islandCommandHandler.createIslandFromTemplate(playerRef, sourceWorld, templateId);
    }

    // Error-key detection: keys containing these segments are colored with errorColor
    private static final Set<String> ERROR_KEY_SEGMENTS = Collections.unmodifiableSet(
        new java.util.HashSet<>(Arrays.asList(
            "invalid", "failed", "not_initialized", "not_found", "no_island",
            "not_leader", "not_online", "no_resets", "denied", "blocked",
            "not_set", "already_", "cannot_", ".self", "unavailable", "required",
            "outside_boundary", "not_present", "not_coop", "not_banned", "not_member",
            "no_permission", "island_gone", "leader_has_members", "usage"
        )));

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern ARG_PATTERN = Pattern.compile("<[^>]+>");

    private boolean isErrorKey(String key) {
        for (String segment : ERROR_KEY_SEGMENTS) {
            if (key.contains(segment)) return true;
        }
        return false;
    }

    private void appendBodyWithArgHighlight(List<Message> parts, String text, String bodyColor, String attrColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher argMatcher = ARG_PATTERN.matcher(text);
        int pos = 0;
        while (argMatcher.find()) {
            if (argMatcher.start() > pos) {
                parts.add(Message.raw(text.substring(pos, argMatcher.start())).color(bodyColor));
            }
            parts.add(Message.raw(argMatcher.group()).color(attrColor));
            pos = argMatcher.end();
        }
        if (pos < text.length()) {
            parts.add(Message.raw(text.substring(pos)).color(bodyColor));
        }
    }

    private Message buildMessage(String key, String template, Map<String, String> vars) {
        MessagesConfig cfg = messagesConfigManager.getConfig();
        String prefixText = cfg.getPrefix();
        String prefixColor = cfg.getPrefixColor();
        String bodyColor = isErrorKey(key) ? cfg.getErrorColor() : cfg.getMessageColor();
        String attrColor = cfg.getAttributeColor();
        String valueColor = cfg.getValueColor();

        Message prefixMsg = Message.raw(prefixText).color(prefixColor);

        List<Message> parts = new ArrayList<>();
        if (vars == null || vars.isEmpty()) {
            appendBodyWithArgHighlight(parts, template, bodyColor, attrColor);
        } else {
        Matcher matcher = VAR_PATTERN.matcher(template);
        int pos = 0;
        while (matcher.find()) {
            if (matcher.start() > pos) {
                    appendBodyWithArgHighlight(parts, template.substring(pos, matcher.start()), bodyColor, attrColor);
            }
            String value = vars.getOrDefault(matcher.group(1), "{" + matcher.group(1) + "}");
                parts.add(Message.raw(value).color(valueColor));
            pos = matcher.end();
        }
        if (pos < template.length()) {
                appendBodyWithArgHighlight(parts, template.substring(pos), bodyColor, attrColor);
            }
        }
        if (parts.isEmpty()) {
            return Message.join(prefixMsg, Message.raw(template).color(bodyColor));
        }
        Message[] segments = new Message[parts.size() + 1];
        segments[0] = prefixMsg;
        for (int i = 0; i < parts.size(); i++) segments[i + 1] = parts.get(i);
        return Message.join(segments);
    }

    private Message buildMessageNoPrefix(String key, String template, Map<String, String> vars) {
        MessagesConfig cfg = messagesConfigManager.getConfig();
        String bodyColor = isErrorKey(key) ? cfg.getErrorColor() : cfg.getMessageColor();
        String attrColor = cfg.getAttributeColor();
        String valueColor = cfg.getValueColor();

        List<Message> parts = new ArrayList<>();
        if (vars == null || vars.isEmpty()) {
            appendBodyWithArgHighlight(parts, template, bodyColor, attrColor);
        } else {
            Matcher matcher = VAR_PATTERN.matcher(template);
            int pos = 0;
            while (matcher.find()) {
                if (matcher.start() > pos) {
                    appendBodyWithArgHighlight(parts, template.substring(pos, matcher.start()), bodyColor, attrColor);
                }
                String value = vars.getOrDefault(matcher.group(1), "{" + matcher.group(1) + "}");
                parts.add(Message.raw(value).color(valueColor));
                pos = matcher.end();
            }
            if (pos < template.length()) {
                appendBodyWithArgHighlight(parts, template.substring(pos), bodyColor, attrColor);
            }
        }
        if (parts.isEmpty()) {
            return Message.raw(template).color(bodyColor);
        }
        return Message.join(parts.toArray(new Message[0]));
    }

    public Message tr(String key) {
        return buildMessage(key, messageManager.get(key), Collections.emptyMap());
    }

    public Message tr(String key, Map<String, String> vars) {
        return buildMessage(key, messageManager.get(key), vars);
    }

    public Message tr(PlayerRef playerRef, String key) {
        String language = resolvePlayerLanguage(playerRef);
        return buildMessage(key, messageManager.get(key, language), Collections.emptyMap());
    }

    public Message tr(PlayerRef playerRef, String key, Map<String, String> vars) {
        String language = resolvePlayerLanguage(playerRef);
        return buildMessage(key, messageManager.get(key, language), vars);
    }

    public Message trNoPrefix(PlayerRef playerRef, String key) {
        String language = resolvePlayerLanguage(playerRef);
        return buildMessageNoPrefix(key, messageManager.get(key, language), Collections.emptyMap());
    }

    public Message trNoPrefix(PlayerRef playerRef, String key, Map<String, String> vars) {
        String language = resolvePlayerLanguage(playerRef);
        return buildMessageNoPrefix(key, messageManager.get(key, language), vars);
    }

    public Message trForLanguage(String language, String key) {
        return buildMessage(key, messageManager.get(key, language), Collections.emptyMap());
    }

    public Message trForLanguage(String language, String key, Map<String, String> vars) {
        return buildMessage(key, messageManager.get(key, language), vars);
    }

    /** Returns the raw translated text only (no prefix, no color). For use as variable values inside other messages. */
    public String rawText(PlayerRef playerRef, String key) {
        return messageManager.get(key, resolvePlayerLanguage(playerRef));
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfigManager.getConfig();
    }

    public String resolvePlayerLanguage(PlayerRef playerRef) {
        String fallback = getConfiguredFallbackLanguage();
        if (playerRef == null || playerRef.getUuid() == null) {
            return fallback;
        }
        String preferred = playerPreferenceRepository.getLanguage(playerRef.getUuid());
        if (preferred == null || preferred.isBlank()) {
            return fallback;
        }
        String normalizedPreferred = normalizeLanguageCode(preferred);
        if (normalizedPreferred.isBlank() || !isLanguageAvailable(normalizedPreferred) || !isLanguageEnabled(normalizedPreferred)) {
            return fallback;
        }
        return normalizedPreferred;
    }

    private void logPlayerJoinLanguage(PlayerConnectEvent event) {
        if (event == null) {
            return;
        }
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        knownPlayerRepository.upsert(playerRef.getUuid(), playerRef.getUsername());
        String language = playerRef.getLanguage();
        if (language == null || language.isBlank()) {
            language = "unknown";
        }
        System.out.println("[ISkyblock] Player joined: " + playerRef.getUsername() + " | client language: " + language);
        applyAutoLanguageOnFirstJoin(playerRef);
    }

    private void applyAutoLanguageOnFirstJoin(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        if (!configManager.getConfig().getSettings().isAutoLangMatchEnabled()) {
            return;
        }
        String existing = playerPreferenceRepository.getLanguage(playerRef.getUuid());
        if (existing != null && !existing.isBlank()) {
            return;
        }

        String matched = matchLanguageCode(playerRef.getLanguage(), getSupportedLanguagesForMatching());
        if (matched == null || matched.isBlank()) {
            matched = getConfiguredFallbackLanguage();
        }

        playerPreferenceRepository.setLanguage(playerRef.getUuid(), matched);
        System.out.println("[ISkyblock] Auto language saved for " + playerRef.getUsername() + ": " + matched);
    }

    private String getConfiguredFallbackLanguage() {
        String fallback = normalizeLanguageCode(configManager.getConfig().getSettings().getFallbackLanguage());
        if (fallback.isBlank() || !isLanguageAvailable(fallback)) {
            fallback = normalizeLanguageCode(configManager.getConfig().getLanguage());
        }
        if (fallback.isBlank() || !isLanguageAvailable(fallback)) {
            fallback = "en";
        }
        return fallback;
    }

    private List<String> getSupportedLanguagesForMatching() {
        List<String> result = new ArrayList<>();
        List<String> available = messageManager.getAvailableLanguages();
        List<String> enabled = configManager.getConfig().getSettings().getEnabledLanguages();

        for (String language : enabled) {
            String normalized = normalizeLanguageCode(language);
            if (!normalized.isBlank() && available.contains(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }

        if (result.isEmpty()) {
            for (String language : available) {
                String normalized = normalizeLanguageCode(language);
                if (!normalized.isBlank() && !result.contains(normalized)) {
                    result.add(normalized);
                }
            }
        }

        return result;
    }

    private String matchLanguageCode(String clientLanguage, List<String> supportedLanguages) {
        if (supportedLanguages == null || supportedLanguages.isEmpty()) {
            return null;
        }
        String normalizedClient = normalizeLanguageCode(clientLanguage);
        if (normalizedClient.isBlank()) {
            return null;
        }
        String baseClient = extractBaseLanguage(normalizedClient);

        for (String supported : supportedLanguages) {
            String normalizedSupported = normalizeLanguageCode(supported);
            if (normalizedSupported.equals(normalizedClient)) {
                return normalizedSupported;
            }
        }
        for (String supported : supportedLanguages) {
            String normalizedSupported = normalizeLanguageCode(supported);
            if (normalizedClient.startsWith(normalizedSupported + "-")) {
                return normalizedSupported;
            }
        }
        for (String supported : supportedLanguages) {
            String normalizedSupported = normalizeLanguageCode(supported);
            if (extractBaseLanguage(normalizedSupported).equals(baseClient)) {
                return normalizedSupported;
            }
        }
        for (String supported : supportedLanguages) {
            String normalizedSupported = normalizeLanguageCode(supported);
            if (normalizedSupported.startsWith(baseClient + "-")) {
                return normalizedSupported;
            }
        }
        return null;
    }

    private boolean isLanguageAvailable(String language) {
        String normalized = normalizeLanguageCode(language);
        return !normalized.isBlank() && messageManager.getAvailableLanguages().contains(normalized);
    }

    private boolean isLanguageEnabled(String language) {
        String normalized = normalizeLanguageCode(language);
        List<String> enabledLanguages = configManager.getConfig().getSettings().getEnabledLanguages();
        for (String enabledLanguage : enabledLanguages) {
            if (normalizeLanguageCode(enabledLanguage).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeLanguageCode(String language) {
        if (language == null) {
            return "";
        }
        return language.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private String extractBaseLanguage(String language) {
        String normalized = normalizeLanguageCode(language);
        int separator = normalized.indexOf('-');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }

}
