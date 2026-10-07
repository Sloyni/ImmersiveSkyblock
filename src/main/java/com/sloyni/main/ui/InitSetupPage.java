package com.sloyni.main.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;

public class InitSetupPage extends InteractiveCustomUIPage<InitSetupPage.PageData> {
    private static final int STEP_WELCOME = 0;
    private static final int STEP_LANGUAGE = 1;
    private static final int STEP_WORLD = 2;
    private static final int STEP_SPACING = 3;
    private static final int STEP_ZONE_SIZE = 4;
    private static final int STEP_LIMITS = 5;
    private static final int STEP_DEFAULT_BLOCK_VALUE = 6;
    private static final int STEP_SUMMARY = 7;
    private static final int LAST_STEP = STEP_SUMMARY;

    private final PlayerRef playerRef;
    private final ISkyblockPlugin plugin;
    private int currentStep = STEP_WELCOME;
    private String selectedLanguage = "en";
    private String worldNameDraft;
    private String spacingDraft;
    private String zoneSizeDraft;
    private String maxResetsDraft;
    private String maxHomesDraft;
    private String defaultBlockValueDraft;
    private String stepErrorKey = null;

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Next", Codec.STRING), (o, v) -> o.next = v, o -> o.next).add()
            .append(new KeyedCodec<>("Back", Codec.STRING), (o, v) -> o.back = v, o -> o.back).add()
            .append(new KeyedCodec<>("Cancel", Codec.STRING), (o, v) -> o.cancel = v, o -> o.cancel).add()
            .append(new KeyedCodec<>("Finish", Codec.STRING), (o, v) -> o.finish = v, o -> o.finish).add()
            .append(new KeyedCodec<>("@Language", Codec.STRING), (o, v) -> o.language = v, o -> o.language).add()
            .append(new KeyedCodec<>("@WorldName", Codec.STRING), (o, v) -> o.worldName = v, o -> o.worldName).add()
            .append(new KeyedCodec<>("@Spacing", Codec.STRING), (o, v) -> o.spacing = v, o -> o.spacing).add()
            .append(new KeyedCodec<>("@ZoneSize", Codec.STRING), (o, v) -> o.zoneSize = v, o -> o.zoneSize).add()
            .append(new KeyedCodec<>("@MaxResets", Codec.STRING), (o, v) -> o.maxResets = v, o -> o.maxResets).add()
            .append(new KeyedCodec<>("@MaxHomes", Codec.STRING), (o, v) -> o.maxHomes = v, o -> o.maxHomes).add()
            .append(new KeyedCodec<>("@DefaultBlockValue", Codec.STRING), (o, v) -> o.defaultBlockValue = v, o -> o.defaultBlockValue).add()
            .build();

        public String next;
        public String back;
        public String cancel;
        public String finish;
        public String language;
        public String worldName;
        public String spacing;
        public String zoneSize;
        public String maxResets;
        public String maxHomes;
        public String defaultBlockValue;
    }

    public InitSetupPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull ISkyblockPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;

        String configuredLanguage = plugin.getConfigManager().getConfig().getLanguage();
        if (configuredLanguage != null && !configuredLanguage.isBlank()) {
            this.selectedLanguage = configuredLanguage.trim().toLowerCase(Locale.ROOT);
        }

        this.worldNameDraft = plugin.getConfigManager().getConfig().getInit().getWorldName();
        this.spacingDraft = String.valueOf(plugin.getConfigManager().getConfig().getInit().getIslandSpacing());
        this.zoneSizeDraft = String.valueOf(plugin.getConfigManager().getConfig().getInit().getIslandZoneSize());
        this.maxResetsDraft = String.valueOf(Math.max(0, plugin.getConfigManager().getConfig().getDangerousConfig().getMaxResets()));
        this.maxHomesDraft = String.valueOf(Math.max(0, plugin.getAddonManager().getHomesAddon().getMaxHomes()));
        this.defaultBlockValueDraft = formatDouble(plugin.getAddonManager().getLevelsAddon().getBlockDefaultValue());

        if (this.worldNameDraft == null) {
            this.worldNameDraft = "";
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        String displayLanguage = plugin.getLanguageService().resolveInputLanguage(selectedLanguage);

        commandBuilder.append("Pages/ISkyblockInit.ui");
        commandBuilder.set("#PageTitle.Text", tr(displayLanguage, "ui.init.title"));
        commandBuilder.set("#Subtitle.Text", tr(displayLanguage, "ui.init.subtitle"));
        commandBuilder.set("#WelcomeTitle.Text", tr(displayLanguage, "ui.init.welcome_title"));
        commandBuilder.set("#WelcomeSubtitle.Text", tr(displayLanguage, "ui.init.welcome_subtitle"));
        commandBuilder.set("#StepIndicator.Text", tr(displayLanguage, "ui.init.step_indicator")
            .replace("{current}", String.valueOf(currentStep))
            .replace("{total}", String.valueOf(LAST_STEP)));
        commandBuilder.set("#LangQuestionLabel.Text", tr(displayLanguage, "ui.init.lang"));
        commandBuilder.set("#WorldQuestionLabel.Text", tr(displayLanguage, "ui.init.world"));
        commandBuilder.set("#SpacingQuestionLabel.Text", tr(displayLanguage, "ui.init.spacing"));
        commandBuilder.set("#ZoneSizeQuestionLabel.Text", tr(displayLanguage, "ui.init.zone_size"));
        commandBuilder.set("#MaxResetsQuestionLabel.Text", tr(displayLanguage, "ui.init.max_resets"));
        commandBuilder.set("#MaxHomesQuestionLabel.Text", tr(displayLanguage, "ui.init.max_homes"));
        commandBuilder.set("#DefaultBlockValueQuestionLabel.Text", tr(displayLanguage, "ui.init.default_block_value"));
        commandBuilder.set("#StepError.Visible", stepErrorKey != null && !stepErrorKey.isBlank());
        commandBuilder.set("#StepError.Text", stepErrorKey == null ? "" : tr(displayLanguage, stepErrorKey));
        commandBuilder.set("#SummaryTitle.Text", tr(displayLanguage, "ui.init.summary_title"));
        commandBuilder.set("#SummaryLanguage.Text", tr(displayLanguage, "ui.init.summary_language") + " " + plugin.getLanguageService().langLabel(selectedLanguage));
        commandBuilder.set("#SummaryWorld.Text", tr(displayLanguage, "ui.init.summary_world") + " " + (worldNameDraft == null || worldNameDraft.isBlank() ? "-" : worldNameDraft));
        commandBuilder.set("#SummarySpacing.Text", tr(displayLanguage, "ui.init.summary_spacing") + " " + (spacingDraft == null || spacingDraft.isBlank() ? "0" : spacingDraft));
        commandBuilder.set("#SummaryZoneSize.Text", tr(displayLanguage, "ui.init.summary_zone_size") + " " + (zoneSizeDraft == null || zoneSizeDraft.isBlank() ? "200" : zoneSizeDraft));
        commandBuilder.set("#SummaryMaxResets.Text", tr(displayLanguage, "ui.init.summary_max_resets") + " " + (maxResetsDraft == null || maxResetsDraft.isBlank() ? "3" : maxResetsDraft));
        commandBuilder.set("#SummaryMaxHomes.Text", tr(displayLanguage, "ui.init.summary_max_homes") + " " + (maxHomesDraft == null || maxHomesDraft.isBlank() ? "3" : maxHomesDraft));
        commandBuilder.set("#SummaryBlockValue.Text", tr(displayLanguage, "ui.init.summary_block_value") + " " + (defaultBlockValueDraft == null || defaultBlockValueDraft.isBlank() ? "1" : defaultBlockValueDraft));

        commandBuilder.set("#BackButton.Text", tr(displayLanguage, "ui.init.back"));
        commandBuilder.set("#NextButton.Text", tr(displayLanguage, "ui.init.next"));
        commandBuilder.set("#FinishButton.Text", tr(displayLanguage, "ui.init.finish"));
        commandBuilder.set("#CancelButton.Text", tr(displayLanguage, "ui.init.cancel"));

        commandBuilder.set("#BackButton.Visible", currentStep > STEP_WELCOME);
        commandBuilder.set("#NextButton.Visible", currentStep < LAST_STEP);
        commandBuilder.set("#FinishButton.Visible", currentStep == LAST_STEP);
        commandBuilder.set("#StepIndicator.Visible", currentStep > STEP_WELCOME);

        List<String> languages = plugin.getMessageManager().getAvailableLanguages();
        DropdownEntryInfo[] langEntries = new DropdownEntryInfo[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            String locale = languages.get(i);
            langEntries[i] = new DropdownEntryInfo(LocalizableString.fromString(plugin.getLanguageService().langLabel(locale)), locale);
        }

        commandBuilder.set("#LangDropdown.Entries", langEntries);
        commandBuilder.set("#LangDropdown.Value", selectedLanguage);
        commandBuilder.set("#WorldInput.Value", worldNameDraft == null ? "" : worldNameDraft);
        commandBuilder.set("#SpacingInput.Value", spacingDraft == null ? "0" : spacingDraft);
        commandBuilder.set("#ZoneSizeInput.Value", zoneSizeDraft == null ? "200" : zoneSizeDraft);
        commandBuilder.set("#MaxResetsInput.Value", maxResetsDraft == null ? "3" : maxResetsDraft);
        commandBuilder.set("#MaxHomesInput.Value", maxHomesDraft == null ? "3" : maxHomesDraft);
        commandBuilder.set("#DefaultBlockValueInput.Value", defaultBlockValueDraft == null ? "1" : defaultBlockValueDraft);

        commandBuilder.set("#WelcomeStep.Visible", currentStep == STEP_WELCOME);
        commandBuilder.set("#LanguageStep.Visible", currentStep == STEP_LANGUAGE);
        commandBuilder.set("#WorldStep.Visible", currentStep == STEP_WORLD);
        commandBuilder.set("#SpacingStep.Visible", currentStep == STEP_SPACING);
        commandBuilder.set("#ZoneSizeStep.Visible", currentStep == STEP_ZONE_SIZE);
        commandBuilder.set("#MaxResetsStep.Visible", currentStep == STEP_LIMITS);
        commandBuilder.set("#DefaultBlockValueStep.Visible", currentStep == STEP_DEFAULT_BLOCK_VALUE);
        commandBuilder.set("#SummaryStep.Visible", currentStep == STEP_SUMMARY);

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LangDropdown", EventData.of("@Language", "#LangDropdown.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldInput", EventData.of("@WorldName", "#WorldInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SpacingInput", EventData.of("@Spacing", "#SpacingInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ZoneSizeInput", EventData.of("@ZoneSize", "#ZoneSizeInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MaxResetsInput", EventData.of("@MaxResets", "#MaxResetsInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MaxHomesInput", EventData.of("@MaxHomes", "#MaxHomesInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DefaultBlockValueInput", EventData.of("@DefaultBlockValue", "#DefaultBlockValueInput.Value"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Back", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#NextButton", EventData.of("Next", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Cancel", "true"), false);

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#FinishButton",
            new EventData()
                .append("Finish", "true")
                .append("@Language", "#LangDropdown.Value")
                .append("@WorldName", "#WorldInput.Value")
                .append("@Spacing", "#SpacingInput.Value")
                .append("@ZoneSize", "#ZoneSizeInput.Value")
                .append("@MaxResets", "#MaxResetsInput.Value")
                .append("@MaxHomes", "#MaxHomesInput.Value")
                .append("@DefaultBlockValue", "#DefaultBlockValueInput.Value"),
            false
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (data.language != null && !data.language.isBlank()) {
            selectedLanguage = data.language.trim().toLowerCase(Locale.ROOT);
        }
        if (data.worldName != null) {
            worldNameDraft = data.worldName;
        }
        if (data.spacing != null) {
            spacingDraft = data.spacing;
        }
        if (data.zoneSize != null) {
            zoneSizeDraft = data.zoneSize;
        }
        if (data.maxResets != null) {
            maxResetsDraft = data.maxResets;
        }
        if (data.maxHomes != null) {
            maxHomesDraft = data.maxHomes;
        }
        if (data.defaultBlockValue != null) {
            defaultBlockValueDraft = data.defaultBlockValue;
        }

        if (data.cancel != null) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (data.back != null) {
            currentStep = Math.max(STEP_WELCOME, currentStep - 1);
            stepErrorKey = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.next != null) {
            String error = validateCurrentStep();
            if (error != null) {
                stepErrorKey = error;
                player.getPageManager().openCustomPage(ref, store, this);
                return;
            }
            stepErrorKey = null;
            currentStep = Math.min(LAST_STEP, currentStep + 1);
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.language != null && currentStep == STEP_LANGUAGE) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.finish != null) {
            String error = validateCurrentStep();
            if (error != null) {
                stepErrorKey = error;
                player.getPageManager().openCustomPage(ref, store, this);
                return;
            }
            player.getPageManager().setPage(ref, store, Page.None);
            plugin.getInitCommandHandler().submitInit(
                this.playerRef,
                selectedLanguage,
                worldNameDraft == null ? "" : worldNameDraft,
                spacingDraft == null ? "0" : spacingDraft,
                zoneSizeDraft == null ? "200" : zoneSizeDraft,
                maxResetsDraft == null ? "3" : maxResetsDraft,
                maxHomesDraft == null ? "3" : maxHomesDraft,
                defaultBlockValueDraft == null ? "1" : defaultBlockValueDraft
            );
        }
    }

    private String tr(String language, String key) {
        return plugin.getMessageManager().get(key, language);
    }

    private String validateCurrentStep() {
        return switch (currentStep) {
            case STEP_LANGUAGE -> validateLanguage();
            case STEP_WORLD -> validateWorld();
            case STEP_SPACING -> validateSpacing();
            case STEP_ZONE_SIZE -> validateZoneSize();
            case STEP_LIMITS -> validateLimits();
            case STEP_DEFAULT_BLOCK_VALUE -> validateBlockValue();
            default -> null;
        };
    }

    private String validateLanguage() {
        if (selectedLanguage == null || selectedLanguage.isBlank()) {
            return "ui.init.lang";
        }
        String normalized = selectedLanguage.trim().toLowerCase(Locale.ROOT);
        if (!plugin.getMessageManager().getAvailableLanguages().contains(normalized)) {
            return "ui.init.lang";
        }
        return null;
    }

    private String validateWorld() {
        String value = safeTrim(worldNameDraft);
        if (value.isBlank() || value.length() > 48 || !value.matches("^[A-Za-z0-9_-]+$")) {
            return "init.invalid_world";
        }
        if (Universe.get().getWorld(value) != null || Universe.get().isWorldLoadable(value)) {
            return "init.world_exists";
        }
        return null;
    }

    private String validateSpacing() {
        String value = safeTrim(spacingDraft);
        if (value.isBlank()) {
            return "init.invalid_spacing";
        }
        try {
            if (Integer.parseInt(value) < 0) {
                return "init.invalid_spacing";
            }
            return null;
        } catch (Exception ignored) {
            return "init.invalid_spacing";
        }
    }

    private String validateZoneSize() {
        String value = safeTrim(zoneSizeDraft);
        if (value.isBlank()) {
            return "init.invalid_zone_size";
        }
        try {
            if (Integer.parseInt(value) < 1) {
                return "init.invalid_zone_size";
            }
            return null;
        } catch (Exception ignored) {
            return "init.invalid_zone_size";
        }
    }

    private String validateLimits() {
        String resets = safeTrim(maxResetsDraft);
        String homes = safeTrim(maxHomesDraft);
        if (resets.isBlank()) {
            return "init.invalid_max_resets";
        }
        if (homes.isBlank()) {
            return "init.invalid_max_homes";
        }
        try {
            if (Integer.parseInt(resets) < 0) {
                return "init.invalid_max_resets";
            }
        } catch (Exception ignored) {
            return "init.invalid_max_resets";
        }
        try {
            if (Integer.parseInt(homes) < 0) {
                return "init.invalid_max_homes";
            }
        } catch (Exception ignored) {
            return "init.invalid_max_homes";
        }
        return null;
    }

    private String validateBlockValue() {
        String value = safeTrim(defaultBlockValueDraft);
        if (value.isBlank()) {
            return "init.invalid_default_block_value";
        }
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed <= 0D) {
                return "init.invalid_default_block_value";
            }
            return null;
        } catch (Exception ignored) {
            return "init.invalid_default_block_value";
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
