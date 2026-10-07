package com.sloyni.main.commands.admin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.Map;
import javax.annotation.Nonnull;

public class HomesCommand extends AbstractCommandCollection {
    public HomesCommand(ISkyblockPlugin plugin) {
        super("homes", "Manage player max homes limits");
        addSubCommand((AbstractCommand) new HomesShowCommand(plugin));
        addSubCommand((AbstractCommand) new HomesAddCommand(plugin));
        addSubCommand((AbstractCommand) new HomesSetCommand(plugin));
        addSubCommand((AbstractCommand) new HomesRemoveCommand(plugin));
    }

    private abstract static class BaseHomesCommand extends CommandBase {
        protected final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        protected final RequiredArg<String> playerArg = withRequiredArg(
            "player",
            "Player name or UUID",
            (ArgumentType<String>) ArgTypes.STRING
        );

        protected BaseHomesCommand(ISkyblockPlugin plugin, String name, String description) {
            super(name, description);
            this.plugin = plugin;
            requirePermission("iskyblock.homes");
        }

        protected void handleTargetError(CommandContext context, String target, IllegalStateException ex) {
            switch (ex.getMessage()) {
                case "target_not_found", "target_not_online" -> Chat.send(context, plugin.tr("homes.admin.target_not_online", Map.of("player", target)));
                default -> Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
            }
        }

        protected java.util.UUID resolveTargetUuid(String target) {
            return plugin.getIslandService().resolveKnownPlayerUuid(target);
        }
    }

    private static class HomesShowCommand extends BaseHomesCommand {
        public HomesShowCommand(ISkyblockPlugin plugin) {
            super(plugin, "show", "Show max homes for a player");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isHomesEnabled()) {
                return;
            }
            String target = playerArg.get(context);
            try {
                int maxHomes = plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(resolveTargetUuid(target));
                Chat.send(context, plugin.tr("homes.admin.show.success", Map.of(
                    "player", target,
                    "max", String.valueOf(maxHomes)
                )));
            } catch (IllegalStateException ex) {
                handleTargetError(context, target, ex);
            }
        }
    }

    private static class HomesAddCommand extends BaseHomesCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> amountArg = withRequiredArg(
            "number",
            "Integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public HomesAddCommand(ISkyblockPlugin plugin) {
            super(plugin, "add", "Add max homes to a player");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isHomesEnabled()) {
                return;
            }
            String target = playerArg.get(context);
            String rawAmount = amountArg.get(context);

            int amount;
            try {
                amount = Integer.parseInt(rawAmount.trim());
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("homes.admin.add.invalid_amount", Map.of("value", rawAmount)));
                return;
            }
            if (amount < 1) {
                Chat.send(context, plugin.tr("homes.admin.add.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            try {
                int maxHomes = plugin.getAddonManager().getHomesAddon().getService().addManualMaxHomes(resolveTargetUuid(target), amount);
                Chat.send(context, plugin.tr("homes.admin.add.success", Map.of(
                    "player", target,
                    "amount", String.valueOf(amount),
                    "max", String.valueOf(maxHomes)
                )));
            } catch (IllegalStateException ex) {
                handleTargetError(context, target, ex);
            }
        }
    }

    private static class HomesSetCommand extends BaseHomesCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> amountArg = withRequiredArg(
            "number",
            "Integer >= 0",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public HomesSetCommand(ISkyblockPlugin plugin) {
            super(plugin, "set", "Set max homes for a player");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isHomesEnabled()) {
                return;
            }
            String target = playerArg.get(context);
            String rawAmount = amountArg.get(context);

            int amount;
            try {
                amount = Integer.parseInt(rawAmount.trim());
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("homes.admin.set.invalid_amount", Map.of("value", rawAmount)));
                return;
            }
            if (amount < 0) {
                Chat.send(context, plugin.tr("homes.admin.set.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            try {
                int maxHomes = plugin.getAddonManager().getHomesAddon().getService().setManualMaxHomes(resolveTargetUuid(target), amount);
                Chat.send(context, plugin.tr("homes.admin.set.success", Map.of(
                    "player", target,
                    "amount", String.valueOf(amount),
                    "max", String.valueOf(maxHomes)
                )));
            } catch (IllegalStateException ex) {
                handleTargetError(context, target, ex);
            }
        }
    }

    private static class HomesRemoveCommand extends BaseHomesCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> amountArg = withRequiredArg(
            "number",
            "Integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public HomesRemoveCommand(ISkyblockPlugin plugin) {
            super(plugin, "remove", "Remove max homes from a player");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isHomesEnabled()) {
                return;
            }
            String target = playerArg.get(context);
            String rawAmount = amountArg.get(context);

            int amount;
            try {
                amount = Integer.parseInt(rawAmount.trim());
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("homes.admin.remove.invalid_amount", Map.of("value", rawAmount)));
                return;
            }
            if (amount < 1) {
                Chat.send(context, plugin.tr("homes.admin.remove.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            try {
                int maxHomes = plugin.getAddonManager().getHomesAddon().getService().removeManualMaxHomes(resolveTargetUuid(target), amount);
                Chat.send(context, plugin.tr("homes.admin.remove.success", Map.of(
                    "player", target,
                    "amount", String.valueOf(amount),
                    "max", String.valueOf(maxHomes)
                )));
            } catch (IllegalStateException ex) {
                handleTargetError(context, target, ex);
            }
        }
    }
}