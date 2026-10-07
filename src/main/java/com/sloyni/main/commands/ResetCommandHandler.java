package com.sloyni.main.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.sloyni.main.config.ConfigManager;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.lang.MessageManager;
import com.sloyni.main.util.Chat;
import java.util.Map;
import java.util.UUID;

public class ResetCommandHandler {
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final IslandService islandService;

    public ResetCommandHandler(ConfigManager configManager,
                               MessageManager messageManager,
                               IslandService islandService) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.islandService = islandService;
    }

    public void handleIslandResetsCommand(PlayerRef playerRef) {
        if (!configManager.getConfig().isInitialized()) {
            Chat.send(playerRef, tr(playerRef, "island.not_initialized"));
            return;
        }

        int remaining = islandService.getRemainingResets(playerRef.getUuid());
        Chat.send(playerRef, tr(playerRef, "island.resets.remaining", Map.of("remaining", String.valueOf(Math.max(0, remaining)))));
    }

    public int handleResetsAdd(String targetName, int amount) {
        UUID targetUuid = resolveTargetUuid(targetName);
        return islandService.addManualResets(targetUuid, Math.max(0, amount));
    }

    public int handleResetsShow(String targetName) {
        UUID targetUuid = resolveTargetUuid(targetName);
        return islandService.getRemainingResets(targetUuid);
    }

    public int handleResetsSet(String targetName, int amount) {
        UUID targetUuid = resolveTargetUuid(targetName);
        return islandService.setManualResets(targetUuid, Math.max(0, amount));
    }

    public int handleResetsRemove(String targetName, int amount) {
        UUID targetUuid = resolveTargetUuid(targetName);
        return islandService.removeManualResets(targetUuid, Math.max(0, amount));
    }

    private UUID resolveTargetUuid(String targetName) {
        return islandService.resolveKnownPlayerUuid(targetName);
    }

    private Message tr(PlayerRef playerRef, String key) {
        return com.sloyni.main.ISkyblockPlugin.getInstance().tr(playerRef, key);
    }

    private Message tr(PlayerRef playerRef, String key, Map<String, String> vars) {
        return com.sloyni.main.ISkyblockPlugin.getInstance().tr(playerRef, key, vars);
    }
}
