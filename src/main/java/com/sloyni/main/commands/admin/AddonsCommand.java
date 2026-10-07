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

public class AddonsCommand extends AbstractCommandCollection {
    public AddonsCommand(ISkyblockPlugin plugin) {
        super("addons", "Enable, disable and inspect ISkyblock addons");
        addSubCommand((AbstractCommand) new AddonsListCommand(plugin));
        addSubCommand((AbstractCommand) new AddonsToggleCommand(plugin, true));
        addSubCommand((AbstractCommand) new AddonsToggleCommand(plugin, false));
    }

    private static class AddonsListCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        public AddonsListCommand(ISkyblockPlugin plugin) {
            super("list", "List addon states");
            this.plugin = plugin;
            requirePermission("iskyblock.addons");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            Chat.send(context, plugin.tr("addons.list.header"));
            for (Map.Entry<String, Boolean> entry : plugin.getAddonManager().getAddonStates().entrySet()) {
                Chat.send(context, plugin.tr("addons.list.item", Map.of(
                    "addon", entry.getKey(),
                    "state", entry.getValue() ? "enabled" : "disabled"
                )));
            }
        }
    }

    private static class AddonsToggleCommand extends CommandBase {
        private final ISkyblockPlugin plugin;
        private final boolean enable;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> addonArg = withRequiredArg(
            "addon",
            "addon id",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public AddonsToggleCommand(ISkyblockPlugin plugin, boolean enable) {
            super(enable ? "enable" : "disable", enable ? "Enable an addon" : "Disable an addon");
            this.plugin = plugin;
            this.enable = enable;
            requirePermission("iskyblock.addons");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String addonKey = addonArg.get(context).trim().toLowerCase();
            if (!plugin.getAddonManager().setAddonEnabled(addonKey, enable)) {
                Chat.send(context, plugin.tr("addons.unknown", Map.of("addon", addonKey)));
                return;
            }

            Chat.send(context, plugin.tr(enable ? "addons.enabled" : "addons.disabled", Map.of("addon", addonKey)));
        }
    }
}