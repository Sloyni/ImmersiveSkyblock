package com.sloyni.main.commands;

import com.sloyni.main.addons.AddonManager;
import com.sloyni.main.config.ConfigManager;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.lang.LanguageService;
import com.sloyni.main.lang.MessageManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsCommandHandler {
    private final ConfigManager configManager;
    private final IslandService islandService;
    private final AddonManager addonManager;
    private final MessageManager messageManager;
    private final LanguageService languageService;

    public SettingsCommandHandler(ConfigManager configManager,
                                  IslandService islandService,
                                  AddonManager addonManager,
                                  MessageManager messageManager,
                                  LanguageService languageService) {
        this.configManager = configManager;
        this.islandService = islandService;
        this.addonManager = addonManager;
        this.messageManager = messageManager;
        this.languageService = languageService;
    }

    public void handleTemplateValueSetting(boolean enabled) {
        addonManager.getLevelsAddon().handleTemplateValueSetting(enabled);
    }

    public void handleLevelValueSetting(int levelValue) {
        addonManager.getLevelsAddon().handleLevelValueSetting(levelValue);
    }

    public void handleBlockDefaultValueSetting(double blockDefaultValue) {
        addonManager.getLevelsAddon().handleBlockDefaultValueSetting(blockDefaultValue);
    }

    public void handleSpawnProtectionSetting(boolean enabled) {
        configManager.getConfig().getSettings().setSpawnProtectionEnabled(enabled);
        configManager.save();
    }

    public void handleSpawnProtectionRadiusSetting(int radius) {
        configManager.getConfig().getSettings().setSpawnProtectionRadius(Math.max(1, radius));
        configManager.save();
    }

    public void handleTemplatesYSetting(int y) {
        configManager.getConfig().getGrid().setIslandY(Math.max(1, y));
        configManager.save();
    }

    public void handleMaxResetsSetting(int maxResets) {
        int oldDefault = Math.max(0, configManager.getConfig().getSettings().getMaxResets());
        int newDefault = Math.max(0, maxResets);
        configManager.getConfig().getDangerousConfig().setMaxResets(newDefault);
        configManager.getConfig().getSettings().setMaxResets(newDefault);
        configManager.save();
        islandService.applyGlobalResetDefaultChange(oldDefault, newDefault);
    }

    public void handleMaxHomesSetting(int maxHomes) {
        int oldDefault = Math.max(0, addonManager.getHomesAddon().getMaxHomes());
        addonManager.getHomesAddon().handleMaxHomesSetting(maxHomes);
        int newDefault = Math.max(0, addonManager.getHomesAddon().getMaxHomes());
        addonManager.getHomesAddon().getService().applyGlobalHomeDefaultChange(oldDefault, newDefault);
    }

    public void handleDefaultLanguageSetting(String language) {
        if (language == null || language.isBlank()) return;
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        List<String> available = messageManager.getAvailableLanguages();
        String resolved = null;
        if (available.contains(normalized)) {
            resolved = normalized;
        } else {
            // Dropdown may send display label instead of locale code — reverse lookup
            for (String locale : available) {
                String label = languageService.langLabel(locale);
                if (label != null && label.trim().toLowerCase(Locale.ROOT).equals(normalized)) {
                    resolved = locale;
                    break;
                }
            }
        }
        if (resolved == null) return;
        configManager.getConfig().setLanguage(resolved);
        configManager.save();
        messageManager.loadLanguage(resolved);
    }

    public void handleLanguageEnabledSetting(String language, boolean enabled) {
        String normalized = normalizeLanguage(language);
        if (!messageManager.getAvailableLanguages().contains(normalized)) {
            return;
        }
        List<String> enabledLanguages = new ArrayList<>(configManager.getConfig().getSettings().getEnabledLanguages());
        enabledLanguages.removeIf(entry -> entry == null || entry.isBlank() || entry.equalsIgnoreCase(normalized));
        if (enabled) {
            enabledLanguages.add(normalized);
        } else if (enabledLanguages.isEmpty()) {
            enabledLanguages.add(normalized);
        }
        configManager.getConfig().getSettings().setEnabledLanguages(enabledLanguages);
        configManager.save();
    }

    public void handleAutoLangMatchSetting(boolean enabled) {
        configManager.getConfig().getSettings().setAutoLangMatchEnabled(enabled);
        configManager.save();
    }

    public void handleFallbackLanguageSetting(String language) {
        configManager.getConfig().getSettings().setFallbackLanguage(normalizeLanguage(language));
        configManager.save();
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    public void handleReduceMobsSetting(boolean enabled) {
        addonManager.getMobsManagerAddon().handleReduceMobsSetting(enabled);
    }

    public void handleReduceMobsRateSetting(int ratePercent) {
        addonManager.getMobsManagerAddon().handleReduceMobsRateSetting(ratePercent);
    }

    public void handleTorchProtectionSetting(boolean enabled) {
        addonManager.getMobsManagerAddon().handleTorchProtectionSetting(enabled);
    }

    public void handleTorchProtectionRadiusSetting(int radius) {
        addonManager.getMobsManagerAddon().handleTorchProtectionRadiusSetting(radius);
    }

    public void handleVoidKillItemsSetting(boolean enabled) {
        configManager.getConfig().getSettings().setVoidKillItemsEnabled(enabled);
        configManager.save();
    }

    public void handleVoidKillEntitiesSetting(boolean enabled) {
        configManager.getConfig().getSettings().setVoidKillEntitiesEnabled(enabled);
        configManager.save();
    }

    public void handleVoidTeleportSetting(boolean enabled) {
        configManager.getConfig().getSettings().setVoidTeleportEnabled(enabled);
        configManager.save();
    }

    public void handleTranslateTemplatesSetting(boolean enabled) {
        configManager.getConfig().getSettings().setTranslateTemplates(enabled);
        configManager.save();
    }

    public void handleOreGeneratorOreChanceSetting(double oreChance) {
        addonManager.getOreGeneratorAddon().handleOreChanceSetting(oreChance);
    }

    public void handleOreGeneratorRegenDelaySetting(long regenDelayMs) {
        addonManager.getOreGeneratorAddon().handleRegenDelaySetting(regenDelayMs);
    }

    public void handleOreGeneratorSpawnRubbleSetting(boolean spawnRubble) {
        addonManager.getOreGeneratorAddon().handleSpawnRubbleSetting(spawnRubble);
    }

    public void handleVisitorDropSetting(boolean enabled) {
        configManager.getConfig().getSettings().setVisitorDropEnabled(enabled);
        configManager.save();
    }

    public void handleVisitorPickupSetting(boolean enabled) {
        configManager.getConfig().getSettings().setVisitorPickupEnabled(enabled);
        configManager.save();
    }
}