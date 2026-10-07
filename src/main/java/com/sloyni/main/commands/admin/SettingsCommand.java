package com.sloyni.main.commands.admin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.commands.CommandCollectionUtil;
import com.sloyni.main.util.Chat;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public class SettingsCommand extends AbstractCommandCollection {
    private final ISkyblockPlugin plugin;
    private final AbstractCommand templateValueCommand;
    private final AbstractCommand levelValueCommand;
    private final AbstractCommand blockDefaultValueCommand;
    private final AbstractCommand reduceMobsCommand;
    private final AbstractCommand reduceMobsRateCommand;
    private final AbstractCommand torchProtectionCommand;
    private final AbstractCommand torchProtectionRadiusCommand;
    private final AbstractCommand maxHomesCommand;

    public SettingsCommand(ISkyblockPlugin plugin) {
        super("settings", "Manage ISkyblock settings");
        this.plugin = plugin;
        this.templateValueCommand = (AbstractCommand) new TemplateValueSettingCommand(plugin);
        this.levelValueCommand = (AbstractCommand) new LevelValueSettingCommand(plugin);
        this.blockDefaultValueCommand = (AbstractCommand) new BlockDefaultValueSettingCommand(plugin);
        this.reduceMobsCommand = (AbstractCommand) new BooleanSettingCommand(plugin, "reduce_mobs", "Enable/disable reduced mob density spawn control", plugin.getSettingsCommandHandler()::handleReduceMobsSetting, "settings.reduce_mobs");
        this.reduceMobsRateCommand = (AbstractCommand) new ReduceMobsRateSettingCommand(plugin);
        this.torchProtectionCommand = (AbstractCommand) new BooleanSettingCommand(plugin, "torch_protection", "Enable/disable torch nearby spawn suppression", plugin.getSettingsCommandHandler()::handleTorchProtectionSetting, "settings.torch_protection");
        this.torchProtectionRadiusCommand = (AbstractCommand) new TorchProtectionRadiusSettingCommand(plugin);
        this.maxHomesCommand = (AbstractCommand) new MaxHomesSettingCommand(plugin);
        addSubCommand((AbstractCommand) new BooleanSettingCommand(plugin, "spawn_protection", "Enable/disable island spawn mob protection", plugin.getSettingsCommandHandler()::handleSpawnProtectionSetting, "settings.spawn_protection"));
        addSubCommand((AbstractCommand) new ProtectionRadiusSettingCommand(plugin));
        addSubCommand((AbstractCommand) new TemplatesYSettingCommand(plugin));
        addSubCommand((AbstractCommand) new MaxResetsSettingCommand(plugin));
        addSubCommand((AbstractCommand) new DefaultLanguageSettingCommand(plugin));
        addSubCommand((AbstractCommand) new BooleanSettingCommand(plugin, "auto_lang_match", "Enable/disable automatic first-join language matching", plugin.getSettingsCommandHandler()::handleAutoLangMatchSetting, "settings.auto_lang_match"));
        addSubCommand((AbstractCommand) new FallbackLanguageSettingCommand(plugin));
        addSubCommand((AbstractCommand) new LanguagesSettingCommand(plugin));
        addSubCommand((AbstractCommand) new BooleanSettingCommand(plugin, "void_kill_items", "Enable/disable void cleanup for dropped items", plugin.getSettingsCommandHandler()::handleVoidKillItemsSetting, "settings.void_kill_items"));
        addSubCommand((AbstractCommand) new BooleanSettingCommand(plugin, "void_kill_entities", "Enable/disable void cleanup for NPC entities", plugin.getSettingsCommandHandler()::handleVoidKillEntitiesSetting, "settings.void_kill_entities"));
        addSubCommand((AbstractCommand) new BooleanSettingCommand(plugin, "void_teleport", "Enable/disable instant teleport when player falls below Y 0", plugin.getSettingsCommandHandler()::handleVoidTeleportSetting, "settings.void_teleport"));
        addSubCommand((AbstractCommand) new BooleanSettingCommand(plugin, "translate_templates", "Use per-language template translations instead of static values", plugin.getSettingsCommandHandler()::handleTranslateTemplatesSetting, "settings.translate_templates"));
        syncAddonSubCommands();
    }

    public void syncAddonSubCommands() {
        CommandCollectionUtil.removeSubCommand(this, templateValueCommand);
        CommandCollectionUtil.removeSubCommand(this, levelValueCommand);
        CommandCollectionUtil.removeSubCommand(this, blockDefaultValueCommand);
        CommandCollectionUtil.removeSubCommand(this, reduceMobsCommand);
        CommandCollectionUtil.removeSubCommand(this, reduceMobsRateCommand);
        CommandCollectionUtil.removeSubCommand(this, torchProtectionCommand);
        CommandCollectionUtil.removeSubCommand(this, torchProtectionRadiusCommand);
        CommandCollectionUtil.removeSubCommand(this, maxHomesCommand);

        if (plugin.getAddonManager().isLevelsEnabled()) {
            CommandCollectionUtil.ensureSubCommand(this, templateValueCommand);
            CommandCollectionUtil.ensureSubCommand(this, levelValueCommand);
            CommandCollectionUtil.ensureSubCommand(this, blockDefaultValueCommand);
        }

        if (plugin.getAddonManager().isMobsManagerEnabled()) {
            CommandCollectionUtil.ensureSubCommand(this, reduceMobsCommand);
            CommandCollectionUtil.ensureSubCommand(this, reduceMobsRateCommand);
            CommandCollectionUtil.ensureSubCommand(this, torchProtectionCommand);
            CommandCollectionUtil.ensureSubCommand(this, torchProtectionRadiusCommand);
        }

        if (plugin.getAddonManager().isHomesEnabled()) {
            CommandCollectionUtil.ensureSubCommand(this, maxHomesCommand);
        }
    }

    private static class BooleanSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;
        private final Consumer<Boolean> setter;
        private final String messagePrefix;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "true or false",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public BooleanSettingCommand(ISkyblockPlugin plugin,
                                     String commandName,
                                     String description,
                                     Consumer<Boolean> setter,
                                     String messagePrefix) {
            super(commandName, description);
            this.plugin = plugin;
            this.setter = setter;
            this.messagePrefix = messagePrefix;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String rawValue = valueArg.get(context).trim().toLowerCase();
            if (!"true".equals(rawValue) && !"false".equals(rawValue)) {
                Chat.send(context, plugin.tr(messagePrefix + ".invalid", Map.of("value", rawValue)));
                return;
            }

            boolean enabled = Boolean.parseBoolean(rawValue);
            setter.accept(enabled);
            Chat.send(context, plugin.tr(messagePrefix + ".updated", Map.of("value", String.valueOf(enabled))));
        }
    }

    private static class TemplateValueSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "true or false",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public TemplateValueSettingCommand(ISkyblockPlugin plugin) {
            super("template_value", "Enable or disable template base value subtraction");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isLevelsEnabled()) {
                return;
            }
            String rawValue = valueArg.get(context).trim().toLowerCase();
            if (!"true".equals(rawValue) && !"false".equals(rawValue)) {
                Chat.send(context, plugin.tr("settings.template_value.invalid", Map.of("value", rawValue)));
                return;
            }

            boolean enabled = Boolean.parseBoolean(rawValue);
            plugin.getSettingsCommandHandler().handleTemplateValueSetting(enabled);
            Chat.send(context, plugin.tr("settings.template_value.updated", Map.of("value", String.valueOf(enabled))));
        }
    }

    private static class LevelValueSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public LevelValueSettingCommand(ISkyblockPlugin plugin) {
            super("level_value", "Set required island value points for each level step");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isLevelsEnabled()) {
                return;
            }
            String rawValue = valueArg.get(context).trim();

            int levelValue;
            try {
                levelValue = Integer.parseInt(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.level_value.invalid", Map.of("value", rawValue)));
                return;
            }

            if (levelValue < 1) {
                Chat.send(context, plugin.tr("settings.level_value.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleLevelValueSetting(levelValue);
            Chat.send(context, plugin.tr("settings.level_value.updated", Map.of("value", String.valueOf(levelValue))));
        }
    }

    private static class BlockDefaultValueSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "decimal > 0",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public BlockDefaultValueSettingCommand(ISkyblockPlugin plugin) {
            super("block_default_value", "Set default value used for blocks not present in block_values.json");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isLevelsEnabled()) {
                return;
            }
            String rawValue = valueArg.get(context).trim();

            double blockDefaultValue;
            try {
                blockDefaultValue = Double.parseDouble(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.block_default_value.invalid", Map.of("value", rawValue)));
                return;
            }

            if (Double.isNaN(blockDefaultValue) || Double.isInfinite(blockDefaultValue) || blockDefaultValue <= 0D) {
                Chat.send(context, plugin.tr("settings.block_default_value.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleBlockDefaultValueSetting(blockDefaultValue);
            Chat.send(context, plugin.tr("settings.block_default_value.updated", Map.of("value", String.valueOf(blockDefaultValue))));
        }
    }

    private static class ProtectionRadiusSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public ProtectionRadiusSettingCommand(ISkyblockPlugin plugin) {
            super("protection_radius", "Set island spawn protection cube size (NxNxN)");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String rawValue = valueArg.get(context).trim();

            int radius;
            try {
                radius = Integer.parseInt(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.protection_radius.invalid", Map.of("value", rawValue)));
                return;
            }

            if (radius < 1) {
                Chat.send(context, plugin.tr("settings.protection_radius.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleSpawnProtectionRadiusSetting(radius);
            Chat.send(context, plugin.tr("settings.protection_radius.updated", Map.of("value", String.valueOf(radius))));
        }
    }

    private static class TemplatesYSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public TemplatesYSettingCommand(ISkyblockPlugin plugin) {
            super("templates_y", "Set Y position used for newly generated islands");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String rawValue = valueArg.get(context).trim();
            int y;
            try {
                y = Integer.parseInt(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.templates_y.invalid", Map.of("value", rawValue)));
                return;
            }
            if (y < 1) {
                Chat.send(context, plugin.tr("settings.templates_y.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleTemplatesYSetting(y);
            Chat.send(context, plugin.tr("settings.templates_y.updated", Map.of("value", String.valueOf(y))));
        }
    }

    private static class MaxResetsSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "integer >= 0",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public MaxResetsSettingCommand(ISkyblockPlugin plugin) {
            super("max_resets", "Set default island creates per player (global diff applied except custom reset players)");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String rawValue = valueArg.get(context).trim();
            int maxResets;
            try {
                maxResets = Integer.parseInt(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.max_resets.invalid", Map.of("value", rawValue)));
                return;
            }
            if (maxResets < 0) {
                Chat.send(context, plugin.tr("settings.max_resets.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleMaxResetsSetting(maxResets);
            Chat.send(context, plugin.tr("settings.max_resets.updated", Map.of("value", String.valueOf(maxResets))));
        }
    }

    private static class MaxHomesSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "integer >= 0",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public MaxHomesSettingCommand(ISkyblockPlugin plugin) {
            super("max_homes", "Set default max homes per player (global diff applied except custom home-limit players)");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isHomesEnabled()) {
                return;
            }
            String rawValue = valueArg.get(context).trim();
            int maxHomes;
            try {
                maxHomes = Integer.parseInt(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.max_homes.invalid", Map.of("value", rawValue)));
                return;
            }
            if (maxHomes < 0) {
                Chat.send(context, plugin.tr("settings.max_homes.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleMaxHomesSetting(maxHomes);
            Chat.send(context, plugin.tr("settings.max_homes.updated", Map.of("value", String.valueOf(maxHomes))));
        }
    }

    private static class DefaultLanguageSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "locale",
            "language code",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public DefaultLanguageSettingCommand(ISkyblockPlugin plugin) {
            super("default_language", "Set the global default language");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String locale = valueArg.get(context).trim().toLowerCase();
            if (!plugin.getMessageManager().getAvailableLanguages().contains(locale)) {
                Chat.send(context, plugin.tr("settings.language.invalid", Map.of("value", locale)));
                return;
            }
            plugin.getSettingsCommandHandler().handleDefaultLanguageSetting(locale);
            Chat.send(context, plugin.tr("settings.default_language.updated", Map.of("value", locale)));
        }
    }

    private static class LanguagesSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> actionArg = withRequiredArg(
            "action",
            "enable or disable",
            (ArgumentType<String>) ArgTypes.STRING
        );

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> localeArg = withRequiredArg(
            "locale",
            "language code",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public LanguagesSettingCommand(ISkyblockPlugin plugin) {
            super("languages", "Enable or disable player-selectable languages");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String action = actionArg.get(context).trim().toLowerCase();
            String locale = localeArg.get(context).trim().toLowerCase();
            if (!"enable".equals(action) && !"disable".equals(action)) {
                Chat.send(context, plugin.tr("settings.languages.usage"));
                return;
            }
            if (!plugin.getMessageManager().getAvailableLanguages().contains(locale)) {
                Chat.send(context, plugin.tr("settings.language.invalid", Map.of("value", locale)));
                return;
            }
            plugin.getSettingsCommandHandler().handleLanguageEnabledSetting(locale, "enable".equals(action));
            String key = "enable".equals(action) ? "settings.languages.enabled" : "settings.languages.disabled";
            Chat.send(context, plugin.tr(key, Map.of("value", locale)));
        }
    }

    private static class FallbackLanguageSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "locale",
            "language code",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public FallbackLanguageSettingCommand(ISkyblockPlugin plugin) {
            super("fallback_lang", "Set fallback language for auto language matching");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String locale = valueArg.get(context).trim().toLowerCase();
            if (!plugin.getMessageManager().getAvailableLanguages().contains(locale)) {
                Chat.send(context, plugin.tr("settings.fallback_lang.invalid", Map.of("value", locale)));
                return;
            }
            plugin.getSettingsCommandHandler().handleFallbackLanguageSetting(locale);
            Chat.send(context, plugin.tr("settings.fallback_lang.updated", Map.of("value", locale)));
        }
    }

    private static class ReduceMobsRateSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "0 to 100 or 0% to 100%",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public ReduceMobsRateSettingCommand(ISkyblockPlugin plugin) {
            super("reduce_mobs_rate", "Set reduced mobs removal rate percentage");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isMobsManagerEnabled()) {
                return;
            }
            String rawValue = valueArg.get(context).trim();
            String numericValue = rawValue.endsWith("%")
                ? rawValue.substring(0, rawValue.length() - 1).trim()
                : rawValue;

            int rate;
            try {
                rate = Integer.parseInt(numericValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.reduce_mobs_rate.invalid", Map.of("value", rawValue)));
                return;
            }

            if (rate < 0 || rate > 100) {
                Chat.send(context, plugin.tr("settings.reduce_mobs_rate.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleReduceMobsRateSetting(rate);
            Chat.send(context, plugin.tr("settings.reduce_mobs_rate.updated", Map.of("value", rate + "%")));
        }
    }

    private static class TorchProtectionRadiusSettingCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> valueArg = withRequiredArg(
            "value",
            "integer >= 1",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public TorchProtectionRadiusSettingCommand(ISkyblockPlugin plugin) {
            super("torch_protection_radius", "Set torch nearby spawn suppression radius");
            this.plugin = plugin;
            requirePermission("iskyblock.settings");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!plugin.getAddonManager().isMobsManagerEnabled()) {
                return;
            }
            String rawValue = valueArg.get(context).trim();

            int radius;
            try {
                radius = Integer.parseInt(rawValue);
            } catch (Exception ignored) {
                Chat.send(context, plugin.tr("settings.torch_protection_radius.invalid", Map.of("value", rawValue)));
                return;
            }

            if (radius < 1) {
                Chat.send(context, plugin.tr("settings.torch_protection_radius.invalid", Map.of("value", rawValue)));
                return;
            }

            plugin.getSettingsCommandHandler().handleTorchProtectionRadiusSetting(radius);
            Chat.send(context, plugin.tr("settings.torch_protection_radius.updated", Map.of("value", String.valueOf(radius))));
        }
    }
}
