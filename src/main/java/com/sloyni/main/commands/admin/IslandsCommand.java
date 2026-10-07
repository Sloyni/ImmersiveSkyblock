package com.sloyni.main.commands.admin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.util.Chat;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class IslandsCommand extends AbstractCommandCollection {
    public IslandsCommand(ISkyblockPlugin plugin) {
        super("islands", "Manage player islands as admin");
        addSubCommand((AbstractCommand) new RenameIslandCommand(plugin));
        addSubCommand((AbstractCommand) new ResetIslandNameCommand(plugin));
        addSubCommand((AbstractCommand) new DeleteIslandCommand(plugin));
        addSubCommand((AbstractCommand) new TeamShowIslandCommand(plugin));
        addSubCommand((AbstractCommand) new TeamAddIslandCommand(plugin));
        addSubCommand((AbstractCommand) new TeamRemoveIslandCommand(plugin));
        addSubCommand((AbstractCommand) new MakeLeaderIslandCommand(plugin));
        addSubCommand((AbstractCommand) new CoopShowIslandCommand(plugin));
        addSubCommand((AbstractCommand) new CoopAddIslandCommand(plugin));
        addSubCommand((AbstractCommand) new CoopRemoveIslandCommand(plugin));
        addSubCommand((AbstractCommand) new BanShowIslandCommand(plugin));
        addSubCommand((AbstractCommand) new BanAddIslandCommand(plugin));
        addSubCommand((AbstractCommand) new BanRemoveIslandCommand(plugin));
        addSubCommand((AbstractCommand) new LockIslandCommand(plugin, true));
        addSubCommand((AbstractCommand) new LockIslandCommand(plugin, false));
    }

    private abstract static class BaseIslandAdminCommand extends CommandBase {
        protected final ISkyblockPlugin plugin;

        protected BaseIslandAdminCommand(ISkyblockPlugin plugin, String name, String description) {
            super(name, description);
            this.plugin = plugin;
            requirePermission("iskyblock.islands");
        }

        protected UUID resolveTargetPlayer(String target) {
            return plugin.getIslandService().resolveKnownPlayerUuid(target);
        }

        protected String extractRemainder(String input, int partsToSkip) {
            if (input == null || input.isBlank()) {
                return "";
            }
            String[] parts = input.trim().split("\\s+", partsToSkip + 1);
            if (parts.length <= partsToSkip) {
                return "";
            }
            String value = parts[partsToSkip].trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1).trim();
            }
            return value;
        }

        protected void handleFailure(CommandContext context, String target, Exception ex) {
            String message = ex.getMessage() == null ? "unknown" : ex.getMessage();
            switch (message) {
                case "target_not_found" -> Chat.send(context, plugin.tr("islands.admin.target_not_found", Map.of("player", target)));
                case "no_island" -> Chat.send(context, plugin.tr("islands.admin.no_island", Map.of("player", target)));
                case "member_not_found" -> Chat.send(context, plugin.tr("islands.admin.member_not_found", Map.of("player", target)));
                case "already_has_island" -> Chat.send(context, plugin.tr("islands.admin.target_has_island", Map.of("player", target)));
                case "already_member" -> Chat.send(context, plugin.tr("islands.admin.already_member", Map.of("player", target)));
                case "already_coop" -> Chat.send(context, plugin.tr("islands.admin.already_coop", Map.of("player", target)));
                case "not_coop" -> Chat.send(context, plugin.tr("islands.admin.not_coop", Map.of("player", target)));
                case "already_banned" -> Chat.send(context, plugin.tr("islands.admin.already_banned", Map.of("player", target)));
                case "not_banned" -> Chat.send(context, plugin.tr("islands.admin.not_banned", Map.of("player", target)));
                case "cannot_ban_member" -> Chat.send(context, plugin.tr("islands.admin.cannot_ban_member", Map.of("player", target)));
                default -> Chat.send(context, plugin.tr("island.leave.failed", Map.of("error", message)));
            }
        }
    }

    private static class RenameIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public RenameIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "rename", "Rename a player's island");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            String newName = extractRemainder(context.getInputString(), 4);
            if (newName.isBlank() || newName.length() > 16) {
                Chat.send(context, plugin.tr("island.setname.invalid", Map.of("max", "16")));
                return;
            }
            try {
                plugin.getIslandService().setIslandNameByTarget(resolveTargetPlayer(target), newName);
                Chat.send(context, plugin.tr("islands.admin.rename.success", Map.of("player", target, "name", newName)));
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }

    private static class ResetIslandNameCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public ResetIslandNameCommand(ISkyblockPlugin plugin) {
            super(plugin, "delname", "Reset a player's island name to default");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            try {
                plugin.getIslandService().resetIslandNameByTarget(resolveTargetPlayer(target));
                Chat.send(context, plugin.tr("islands.admin.delname.success", Map.of("player", target)));
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }

    private static class DeleteIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public DeleteIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "delete", "Delete a player's island");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            try {
                plugin.getIslandService().deleteIslandByTarget(resolveTargetPlayer(target));
                Chat.send(context, plugin.tr("islands.admin.delete.success", Map.of("player", target)));
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }

    private static class TeamShowIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public TeamShowIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "team_show", "Show a player's island team");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            try {
                IslandData island = plugin.getIslandService().getIslandForTarget(resolveTargetPlayer(target));
                Chat.send(context, plugin.tr("islands.admin.team.header", Map.of("player", target, "island", plugin.getIslandService().getIslandDisplayName(island))));
                Chat.send(context, plugin.tr("team.show.leader", Map.of("player", island.getOwnerName())));
                if (island.getMemberNames().isEmpty()) {
                    Chat.send(context, plugin.tr("team.show.no_members"));
                    return;
                }
                for (String member : island.getMemberNames().values()) {
                    Chat.send(context, plugin.tr("team.show.member", Map.of("player", member)));
                }
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }

    private static class TeamAddIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public TeamAddIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "team_add", "Add a member to a player's island");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String memberTarget = targetPlayerArg.get(context);
            try {
                UUID islandTargetUuid = resolveTargetPlayer(islandTarget);
                UUID memberUuid = resolveTargetPlayer(memberTarget);
                plugin.getIslandService().addTeamMemberAsAdmin(islandTargetUuid, memberUuid, memberTarget);
                Chat.send(context, plugin.tr("islands.admin.team_add.success", Map.of("player", memberTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, memberTarget, ex);
            }
        }
    }

    private static class TeamRemoveIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public TeamRemoveIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "team_remove", "Remove a member from a player's island");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String memberTarget = targetPlayerArg.get(context);
            try {
                UUID islandTargetUuid = resolveTargetPlayer(islandTarget);
                plugin.getIslandService().removeTeamMemberAsAdmin(islandTargetUuid, memberTarget);
                Chat.send(context, plugin.tr("islands.admin.team_remove.success", Map.of("player", memberTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, memberTarget, ex);
            }
        }
    }

    private static class MakeLeaderIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public MakeLeaderIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "makeleader", "Transfer island leadership as admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String memberTarget = targetPlayerArg.get(context);
            try {
                UUID islandTargetUuid = resolveTargetPlayer(islandTarget);
                plugin.getIslandService().makeLeaderAsAdmin(islandTargetUuid, memberTarget);
                Chat.send(context, plugin.tr("islands.admin.makeleader.success", Map.of("player", memberTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, memberTarget, ex);
            }
        }
    }

    private static class CoopShowIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public CoopShowIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "coop_show", "Show a player's coop list");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            try {
                Map<String, String> coop = plugin.getIslandService().listCoopMembersAsAdmin(resolveTargetPlayer(target));
                Chat.send(context, plugin.tr("islands.admin.coop.header", Map.of("player", target)));
                if (coop.isEmpty()) {
                    Chat.send(context, plugin.tr("coop.list.empty"));
                    return;
                }
                for (String member : coop.values()) {
                    Chat.send(context, plugin.tr("coop.list.member", Map.of("player", member)));
                }
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }

    private static class CoopAddIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public CoopAddIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "coop_add", "Add a coop member as admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String coopTarget = targetPlayerArg.get(context);
            try {
                plugin.getIslandService().addCoopMemberAsAdmin(resolveTargetPlayer(islandTarget), resolveTargetPlayer(coopTarget), coopTarget);
                Chat.send(context, plugin.tr("islands.admin.coop_add.success", Map.of("player", coopTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, coopTarget, ex);
            }
        }
    }

    private static class CoopRemoveIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public CoopRemoveIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "coop_remove", "Remove a coop member as admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String coopTarget = targetPlayerArg.get(context);
            try {
                plugin.getIslandService().removeCoopMemberAsAdmin(resolveTargetPlayer(islandTarget), coopTarget);
                Chat.send(context, plugin.tr("islands.admin.coop_remove.success", Map.of("player", coopTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, coopTarget, ex);
            }
        }
    }

    private static class BanShowIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public BanShowIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "ban_show", "Show a player's island bans");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            try {
                Map<String, String> banned = plugin.getIslandService().listBannedPlayersAsAdmin(resolveTargetPlayer(target));
                Chat.send(context, plugin.tr("islands.admin.ban.header", Map.of("player", target)));
                if (banned.isEmpty()) {
                    Chat.send(context, plugin.tr("island.ban.list.empty"));
                    return;
                }
                for (String member : banned.values()) {
                    Chat.send(context, plugin.tr("island.ban.list.item", Map.of("player", member)));
                }
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }

    private static class BanAddIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public BanAddIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "ban_add", "Ban a player from an island as admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String banTarget = targetPlayerArg.get(context);
            try {
                plugin.getIslandService().banPlayerAsAdmin(resolveTargetPlayer(islandTarget), resolveTargetPlayer(banTarget), banTarget);
                Chat.send(context, plugin.tr("islands.admin.ban_add.success", Map.of("player", banTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, banTarget, ex);
            }
        }
    }

    private static class BanRemoveIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> islandPlayerArg = withRequiredArg("island_player", "player", (ArgumentType<String>) ArgTypes.STRING);
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> targetPlayerArg = withRequiredArg("target_player", "player", (ArgumentType<String>) ArgTypes.STRING);

        public BanRemoveIslandCommand(ISkyblockPlugin plugin) {
            super(plugin, "ban_remove", "Unban a player from an island as admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String islandTarget = islandPlayerArg.get(context);
            String banTarget = targetPlayerArg.get(context);
            try {
                plugin.getIslandService().unbanPlayerAsAdmin(resolveTargetPlayer(islandTarget), banTarget);
                Chat.send(context, plugin.tr("islands.admin.ban_remove.success", Map.of("player", banTarget, "owner", islandTarget)));
            } catch (Exception ex) {
                handleFailure(context, banTarget, ex);
            }
        }
    }

    private static class LockIslandCommand extends BaseIslandAdminCommand {
        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> playerArg = withRequiredArg("player", "player", (ArgumentType<String>) ArgTypes.STRING);
        private final boolean locked;

        public LockIslandCommand(ISkyblockPlugin plugin, boolean locked) {
            super(plugin, locked ? "lock" : "unlock", locked ? "Lock a player's island" : "Unlock a player's island");
            this.locked = locked;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = playerArg.get(context);
            try {
                plugin.getIslandService().setIslandLockedAsAdmin(resolveTargetPlayer(target), locked);
                Chat.send(context, plugin.tr(locked ? "island.lock.success" : "island.unlock.success"));
            } catch (Exception ex) {
                handleFailure(context, target, ex);
            }
        }
    }
}
