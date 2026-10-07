package com.sloyni.main.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.ui.InitSetupPage;
import com.sloyni.main.ui.ISkyblockAdminDashboardPage;
import com.sloyni.main.util.Chat;
import javax.annotation.Nonnull;

public class ISkyblockCommand extends AbstractPlayerCommand {
    private final ISkyblockPlugin plugin;
    private final TemplateCommand templateCommand;
    private final SetLobbyCommand setLobbyCommand;
    private final SettingsCommand settingsCommand;
    private final ResetsCommand resetsCommand;
    private final IslandsCommand islandsCommand;
    private final IslandSizeCommand islandSizeCommand;
    private final AddonsCommand addonsCommand;

    public ISkyblockCommand(ISkyblockPlugin plugin, SettingsCommand settingsCommand) {
        super("iskyblock", "ISkyblock admin commands");
        this.plugin = plugin;

        this.templateCommand = new TemplateCommand(plugin);
        this.setLobbyCommand = new SetLobbyCommand(plugin);
        this.settingsCommand = settingsCommand;
        this.resetsCommand = new ResetsCommand(plugin);
        this.islandsCommand = new IslandsCommand(plugin);
        this.islandSizeCommand = new IslandSizeCommand(plugin);
        this.addonsCommand = new AddonsCommand(plugin);

        addSubCommand((AbstractCommand) new HelpCommand(plugin));
        addSubCommand((AbstractCommand) templateCommand);
        addSubCommand((AbstractCommand) setLobbyCommand);
        addSubCommand((AbstractCommand) settingsCommand);
        addSubCommand((AbstractCommand) resetsCommand);
        addSubCommand((AbstractCommand) islandsCommand);
        addSubCommand((AbstractCommand) islandSizeCommand);
        addSubCommand((AbstractCommand) addonsCommand);
    }


    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        boolean isAdmin = com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin");
        boolean isModerator = com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator");

        if (!isAdmin && !isModerator) {
            Chat.send(context, "You do not have permission to use this command.");
            return;
        }

        if (!plugin.getConfigManager().getConfig().isInitialized()) {
            if (isAdmin) {
                openInitSetup(context, store, ref, playerRef, world);
            } else {
                Chat.send(context, "ISkyblock is not initialized yet. Contact an administrator.");
            }
            return;
        }
        openDashboard(store, ref, playerRef, world, isAdmin);
    }

    public void syncAddonSubCommands() {
        // Command trees are immutable once fully registered; keep this as a safe no-op.
    }

    private void openDashboard(@Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef,
                               @Nonnull World world,
                               boolean isAdmin) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
            new ISkyblockAdminDashboardPage(playerRef, world, plugin, !isAdmin));
    }

    private void openInitSetup(@Nonnull CommandContext context,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef,
                               @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        Chat.send(context, plugin.tr("init.opening"));
        player.getPageManager().openCustomPage(ref, store, new InitSetupPage(playerRef, world, plugin));
    }

    private static class HelpCommand extends AbstractPlayerCommand {
        private final ISkyblockPlugin plugin;

        public HelpCommand(ISkyblockPlugin plugin) {
            super("help", "Show ISkyblock commands");
            this.plugin = plugin;
        }


        @Override
        protected void execute(@Nonnull CommandContext context,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef,
                               @Nonnull World world) {
            Player player = store.getComponent(ref, Player.getComponentType());
            boolean isAdmin = player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.admin");
            boolean isModerator = player != null && com.sloyni.main.util.PermissionUtil.has(player, "iskyblock.moderator");

            if (!isAdmin && !isModerator) {
                Chat.send(context, "You do not have permission to use this command.");
                return;
            }

            if (!plugin.getConfigManager().getConfig().isInitialized()) {
                sendHelpLine(context, plugin.getMessageManager().get("init.required_use_iskyblock", plugin.resolvePlayerLanguage(playerRef)));
                sendHelpLine(context, "/iskyblock");
                return;
            }

            if (isModerator && !isAdmin) {
                sendHelpLine(context, "ISkyblock moderator commands:");
                sendHelpLine(context, "/iskyblock - open admin panel");
                sendHelpLine(context, "/iskyblock help - show this help");
                return;
            }

            sendHelpLine(context, "ISkyblock admin commands:");
            sendHelpLine(context, "/iskyblock - open admin dashboard");
            sendHelpLine(context, "/iskyblock help - show this help");
            sendHelpLine(context, "/iskyblock template ...");
            sendHelpLine(context, "/iskyblock setlobby");
            sendHelpLine(context, "/iskyblock settings ...");
            sendHelpLine(context, "/iskyblock resets ...");
            sendHelpLine(context, "/iskyblock islands ...");
            sendHelpLine(context, "/iskyblock islandsize ...");
            sendHelpLine(context, "/iskyblock addons ...");
        }

        private void sendHelpLine(@Nonnull CommandContext context, @Nonnull String text) {
            Chat.send(context, Message.raw(text).color(plugin.getMessagesConfig().getMessageColor()));
        }
    }
}