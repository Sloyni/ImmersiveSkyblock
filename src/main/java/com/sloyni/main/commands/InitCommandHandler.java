package com.sloyni.main.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.Map;

public class InitCommandHandler {
    private final ISkyblockPlugin plugin;

    public InitCommandHandler(ISkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void submitInit(PlayerRef playerRef,
                           String lang,
                           String worldName,
                           String spacingRaw,
                           String zoneSizeRaw,
                           String maxResetsRaw,
                           String maxHomesRaw,
                           String defaultBlockValueRaw) {
        String selectedLang = plugin.getLanguageService().resolveInputLanguage(lang);
        String cleanWorld = worldName == null ? "" : worldName.trim();
        if (cleanWorld.isEmpty() || cleanWorld.length() > 48 || !cleanWorld.matches("^[A-Za-z0-9_-]+$")) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_world"));
            return;
        }

        int spacing;
        try {
            spacing = Integer.parseInt(spacingRaw.trim());
        } catch (Exception ignored) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_spacing"));
            return;
        }

        if (spacing < 0) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_spacing"));
            return;
        }

        int zoneSize;
        try {
            zoneSize = Integer.parseInt(zoneSizeRaw.trim());
        } catch (Exception ignored) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_zone_size"));
            return;
        }

        if (zoneSize < 1) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_zone_size"));
            return;
        }

        int maxResets;
        try {
            maxResets = Integer.parseInt(maxResetsRaw.trim());
        } catch (Exception ignored) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_max_resets"));
            return;
        }

        if (maxResets < 0) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_max_resets"));
            return;
        }

        int maxHomes;
        try {
            maxHomes = Integer.parseInt(maxHomesRaw.trim());
        } catch (Exception ignored) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_max_homes"));
            return;
        }

        if (maxHomes < 0) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_max_homes"));
            return;
        }

        double defaultBlockValue;
        try {
            defaultBlockValue = Double.parseDouble(defaultBlockValueRaw.trim());
        } catch (Exception ignored) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_default_block_value"));
            return;
        }

        if (Double.isNaN(defaultBlockValue) || Double.isInfinite(defaultBlockValue) || defaultBlockValue <= 0D) {
            Chat.send(playerRef, tr(selectedLang, "init.invalid_default_block_value"));
            return;
        }

        if (Universe.get().getWorld(cleanWorld) != null || Universe.get().isWorldLoadable(cleanWorld)) {
            Chat.send(playerRef, tr(selectedLang, "init.world_exists"));
            return;
        }

        Chat.send(playerRef, tr(selectedLang, "init.creating_world", Map.of("world", cleanWorld)));

        plugin.getWorldManager().createOrLoadVoidWorld(cleanWorld).thenAccept(world -> {
            plugin.getConfigManager().getConfig().setLanguage(selectedLang);
            plugin.getConfigManager().getConfig().getInit().setWorldName(cleanWorld);
            plugin.getConfigManager().getConfig().getInit().setIslandSpacing(spacing);
            plugin.getConfigManager().getConfig().getInit().setIslandZoneSize(zoneSize);
            plugin.getConfigManager().getConfig().setInitialized(true);
            plugin.getConfigManager().save();

            plugin.getSettingsCommandHandler().handleMaxResetsSetting(maxResets);
            plugin.getSettingsCommandHandler().handleMaxHomesSetting(maxHomes);
            plugin.getSettingsCommandHandler().handleBlockDefaultValueSetting(defaultBlockValue);

            plugin.getMessageManager().loadLanguage(selectedLang);
            plugin.refreshAddonCommandTrees();
            Chat.send(playerRef, tr(selectedLang, "init.success"));
        }).exceptionally(ex -> {
            Chat.send(playerRef, tr(selectedLang, "init.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            return null;
        });
    }

    private Message tr(String language, String key) {
        return plugin.trForLanguage(language, key);
    }

    private Message tr(String language, String key, Map<String, String> vars) {
        return plugin.trForLanguage(language, key, vars);
    }
}