package com.sloyni.main.commands.admin;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class IslandSizeCommand extends CommandBase {
    private final ISkyblockPlugin plugin;

    @Nonnull
    @SuppressWarnings("unchecked")
    private final RequiredArg<String> playerArg = withRequiredArg(
        "player",
        "Player name or UUID",
        (ArgumentType<String>) ArgTypes.STRING
    );

    @Nonnull
    @SuppressWarnings("unchecked")
    private final RequiredArg<String> sizeArg = withRequiredArg(
        "size",
        "Island size",
        (ArgumentType<String>) ArgTypes.STRING
    );

    public IslandSizeCommand(ISkyblockPlugin plugin) {
        super("island_size", "Set irreversible island size upgrade for a player");
        this.plugin = plugin;
        requirePermission("iskyblock.island_size");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String target = playerArg.get(context);
        String rawSize = sizeArg.get(context);

        int requestedSize;
        try {
            requestedSize = Integer.parseInt(rawSize.trim());
        } catch (Exception ignored) {
            Chat.send(context, plugin.tr("island_size.admin.invalid", Map.of(
                "value", rawSize,
                "min", String.valueOf(plugin.getIslandService().getMinUpgradeableIslandZoneSize()),
                "max", String.valueOf(plugin.getIslandService().getMaxAllowedIslandZoneSize())
            )));
            return;
        }

        UUID targetUuid;
        try {
            targetUuid = plugin.getIslandService().resolveKnownPlayerUuid(target);
        } catch (IllegalStateException ex) {
            Chat.send(context, plugin.tr("islands.admin.target_not_found", Map.of("player", target)));
            return;
        }

        plugin.getIslandService().setPlayerIslandSize(targetUuid, requestedSize)
            .thenAccept(result -> {
                if (result.islandFound()) {
                    Chat.send(context, plugin.tr("island_size.admin.success_with_island", Map.of(
                        "player", target,
                        "size", String.valueOf(result.playerSize()),
                        "island_size", String.valueOf(result.islandSize())
                    )));
                    return;
                }

                Chat.send(context, plugin.tr("island_size.admin.success_no_island", Map.of(
                    "player", target,
                    "size", String.valueOf(result.playerSize())
                )));
            })
            .exceptionally(ex -> {
                String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                if ("invalid_island_size_min".equals(message) || "invalid_island_size_max".equals(message)) {
                    Chat.send(context, plugin.tr("island_size.admin.invalid", Map.of(
                        "value", String.valueOf(requestedSize),
                        "min", String.valueOf(plugin.getIslandService().getMinUpgradeableIslandZoneSize()),
                        "max", String.valueOf(plugin.getIslandService().getMaxAllowedIslandZoneSize())
                    )));
                } else if ("target_not_found".equals(message)) {
                    Chat.send(context, plugin.tr("islands.admin.target_not_found", Map.of("player", target)));
                } else {
                    Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", message == null ? "unknown" : message)));
                }
                return null;
            });
    }
}
