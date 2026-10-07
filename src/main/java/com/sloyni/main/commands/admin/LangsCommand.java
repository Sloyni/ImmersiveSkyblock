package com.sloyni.main.commands.admin;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.util.Chat;
import java.util.Locale;
import javax.annotation.Nonnull;

public class LangsCommand extends AbstractCommandCollection {
    private final ISkyblockPlugin plugin;

    public LangsCommand(ISkyblockPlugin plugin) {
        super("langs", "Manage language files");
        this.plugin = plugin;
        addSubCommand(new AddLanguageCommand(plugin));
    }

    private static class AddLanguageCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> idArg = withRequiredArg(
            "id",
            "language id (e.g. en, fr, pt-br)",
            (ArgumentType<String>) ArgTypes.STRING
        );

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> nameArg = withRequiredArg(
            "name",
            "language display name",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public AddLanguageCommand(ISkyblockPlugin plugin) {
            super("add", "Add a new language file");
            this.plugin = plugin;
            requirePermission("iskyblock.admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String rawId = idArg.get(context).trim().toLowerCase(Locale.ROOT);
            String name = nameArg.get(context).trim();
            if (rawId.isBlank() || name.isBlank()) {
                Chat.send(context, plugin.tr("settings.languages.usage"));
                return;
            }

            // normalize id: allow letters, digits, dash and underscore
                String normalized = rawId.replaceAll("[^a-z0-9_-]", "");
            if (normalized.isBlank()) {
                Chat.send(context, plugin.tr("settings.languages.usage"));
                return;
            }

            // Already exists?
            if (plugin.getMessageManager().getAvailableLanguages().contains(normalized)) {
                Chat.send(context, plugin.tr("settings.languages.enabled", java.util.Map.of("value", normalized)));
                return;
            }

            // Create new language file prefilled from English defaults and set display name as "Name (ID)"
            String label = name + " (" + normalized.toUpperCase() + ")";
            plugin.getMessageManager().createLanguageFromEnglish(normalized, label);

            Chat.send(context, plugin.tr("settings.languages.enabled", java.util.Map.of("value", normalized)));
        }
    }
}
