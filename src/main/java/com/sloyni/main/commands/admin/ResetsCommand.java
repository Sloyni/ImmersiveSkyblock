package com.sloyni.main.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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

public class ResetsCommand extends AbstractCommandCollection {
    public ResetsCommand(ISkyblockPlugin plugin) {
        super("resets", "Manage player island create resets");
        addSubCommand((AbstractCommand) new ResetsAddCommand(plugin));
        addSubCommand((AbstractCommand) new ResetsShowCommand(plugin));
        addSubCommand((AbstractCommand) new ResetsSetCommand(plugin));
        addSubCommand((AbstractCommand) new ResetsRemoveCommand(plugin));
    }

    private static class ResetsAddCommand extends CommandBase {
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
        private final RequiredArg<String> amountArg = withRequiredArg(
            "amount",
            "Integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public ResetsAddCommand(ISkyblockPlugin plugin) {
            super("add", "Add create resets to a player and mark as custom-adjusted");
            this.plugin = plugin;
            requirePermission("iskyblock.resets");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            String rawAmount = amountArg.get(context);

            int amount;
            try {
                amount = Integer.parseInt(rawAmount.trim());
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("resets.add.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            if (amount < 1) {
                Chat.send(context, plugin.tr("resets.add.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            try {
                int remaining = plugin.getResetCommandHandler().handleResetsAdd(target, amount);
                Chat.send(context, plugin.tr("resets.add.success", Map.of(
                    "player", target,
                    "amount", String.valueOf(amount),
                    "remaining", String.valueOf(remaining)
                )));
            } catch (IllegalStateException ex) {
                switch (ex.getMessage()) {
                    case "target_not_found", "target_not_online" -> Chat.send(context, plugin.tr("resets.add.target_not_online", Map.of("player", target)));
                    default -> Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                }
            }
        }
    }

    private static class ResetsShowCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg(
            "player",
            "Player name or UUID",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public ResetsShowCommand(ISkyblockPlugin plugin) {
            super("show", "Show current island create resets for a player");
            this.plugin = plugin;
            requirePermission("iskyblock.resets");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);

            try {
                int remaining = plugin.getResetCommandHandler().handleResetsShow(target);
                Chat.send(context, plugin.tr("resets.show.success", Map.of(
                    "player", target,
                    "remaining", String.valueOf(remaining)
                )));
            } catch (IllegalStateException ex) {
                switch (ex.getMessage()) {
                    case "target_not_found", "target_not_online" -> Chat.send(context, plugin.tr("resets.show.target_not_online", Map.of("player", target)));
                    default -> Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                }
            }
        }
    }

    private static class ResetsSetCommand extends CommandBase {
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
        private final RequiredArg<String> amountArg = withRequiredArg(
            "number",
            "Integer >= 0",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public ResetsSetCommand(ISkyblockPlugin plugin) {
            super("set", "Set player island create resets and mark as custom-adjusted");
            this.plugin = plugin;
            requirePermission("iskyblock.resets");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            String rawAmount = amountArg.get(context);

            int amount;
            try {
                amount = Integer.parseInt(rawAmount.trim());
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("resets.set.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            if (amount < 0) {
                Chat.send(context, plugin.tr("resets.set.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            try {
                int remaining = plugin.getResetCommandHandler().handleResetsSet(target, amount);
                Chat.send(context, plugin.tr("resets.set.success", Map.of(
                    "player", target,
                    "amount", String.valueOf(amount),
                    "remaining", String.valueOf(remaining)
                )));
            } catch (IllegalStateException ex) {
                switch (ex.getMessage()) {
                    case "target_not_found", "target_not_online" -> Chat.send(context, plugin.tr("resets.set.target_not_online", Map.of("player", target)));
                    default -> Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                }
            }
        }
    }

    private static class ResetsRemoveCommand extends CommandBase {
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
        private final RequiredArg<String> amountArg = withRequiredArg(
            "number",
            "Integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public ResetsRemoveCommand(ISkyblockPlugin plugin) {
            super("remove", "Remove player island create resets and mark as custom-adjusted");
            this.plugin = plugin;
            requirePermission("iskyblock.resets");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            String rawAmount = amountArg.get(context);

            int amount;
            try {
                amount = Integer.parseInt(rawAmount.trim());
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("resets.remove.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            if (amount < 1) {
                Chat.send(context, plugin.tr("resets.remove.invalid_amount", Map.of("value", rawAmount)));
                return;
            }

            try {
                int remaining = plugin.getResetCommandHandler().handleResetsRemove(target, amount);
                Chat.send(context, plugin.tr("resets.remove.success", Map.of(
                    "player", target,
                    "amount", String.valueOf(amount),
                    "remaining", String.valueOf(remaining)
                )));
            } catch (IllegalStateException ex) {
                switch (ex.getMessage()) {
                    case "target_not_found", "target_not_online" -> Chat.send(context, plugin.tr("resets.remove.target_not_online", Map.of("player", target)));
                    default -> Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage())));
                }
            }
        }
    }
}
