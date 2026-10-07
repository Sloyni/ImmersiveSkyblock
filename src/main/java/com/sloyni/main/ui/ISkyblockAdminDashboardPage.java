package com.sloyni.main.ui;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.AddonManager;
import com.sloyni.main.addons.oregenerator.OreGeneratorAddonConfig;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.island.IslandSchematic;
import com.sloyni.main.island.IslandTemplate;
import com.sloyni.main.util.Chat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public class ISkyblockAdminDashboardPage extends InteractiveCustomUIPage<ISkyblockAdminDashboardPage.PageData> {

    private static final int MAX_TEMPLATE_ROWS = 20;
    private static final int MAX_LANGUAGE_BUTTONS = 8;
    private static final int MAX_ADDON_ROWS = 10;
    private static final int MAX_RESETS_ROWS = 10;
    private static final int MAX_HOMES_ROWS = 10;
    private static final int MAX_ISLAND_ROWS = 10;
    private static final int MAX_LEVELS_ROWS = 5;
    private static final String TEMPLATE_BG_ENABLED = "#223149";
    private static final String TEMPLATE_BG_DISABLED = "#182638";
    private static final String TEMPLATE_BG_ERROR = "#5b2424";

    private final ISkyblockPlugin plugin;
    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final boolean restricted;

    private boolean showCreateForm = false;
    private String formName = "";
    private String formDescription = "";
    private String formId = "";
    private String formError = null;
    private boolean createSuccess = false;

    private boolean showEditTemplateForm = false;
    private String editTemplateId = "";
    private String editLanguage = "";
    private String editName = "";
    private String editDescription = "";
    private String editError = null;

    private boolean showDeleteConfirm = false;
    private String deleteTemplateId = "";
    private String deleteError = null;

    private List<TemplateSlot> templateSlots = new ArrayList<>();
    private List<AddonSlot> addonSlots = new ArrayList<>();
    private DashboardTab activeTab = DashboardTab.TEMPLATES;
    private String settingsProtectionRadiusInput = null;
    private String settingsTemplatesYInput = null;
    private String settingsError = null;
    private boolean settingsProtectionRadiusDirtyShown = false;
    private boolean settingsTemplatesYDirtyShown = false;
    private String mobsReduceRateInput = null;
    private String mobsTorchRadiusInput = null;
    private String mobsError = null;
    private boolean mobsTorchRadiusDirtyShown = false;
    private String resetsMaxResetsInput = null;
    private String resetsSearchInput = "";
    private String resetsSearchQuery = "";
    private String resetsError = null;
    private boolean resetsMaxResetsDirtyShown = false;
    private List<ResetPlayerSlot> resetPlayerSlots = new ArrayList<>();
    private final Map<UUID, String> resetPlayerDraftInputs = new LinkedHashMap<>();
    private String homesMaxHomesInput = null;
    private String homesSearchInput = "";
    private String homesSearchQuery = "";
    private String homesError = null;
    private boolean homesMaxHomesDirtyShown = false;
    private List<HomePlayerSlot> homePlayerSlots = new ArrayList<>();
    private final Map<UUID, String> homePlayerDraftInputs = new LinkedHashMap<>();
    private String islandsSearchInput = "";
    private String islandsSearchQuery = "";
    private String islandsError = null;
    private List<IslandPlayerSlot> islandPlayerSlots = new ArrayList<>();
    private String levelsLevelValueInput = null;
    private String levelsDefaultBlockValueInput = null;
    private String levelsCreateBlockIdInput = "";
    private String levelsCreateBlockValueInput = "";
    private String levelsSearchInput = "";
    private String levelsSearchQuery = "";
    private String levelsError = null;
    private boolean levelsLevelValueDirtyShown = false;
    private boolean levelsDefaultBlockValueDirtyShown = false;
    private List<LevelBlockSlot> levelBlockSlots = new ArrayList<>();
    private final Map<String, String> levelBlockIdDraftInputs = new LinkedHashMap<>();
    private final Map<String, String> levelBlockValueDraftInputs = new LinkedHashMap<>();

    private String oreGenOreChanceInput = null;
    private String oreGenRegenDelayInput = null;
    private String oreGenError = null;
    private boolean oreGenOreChanceDirtyShown = false;
    private boolean oreGenRegenDelayDirtyShown = false;
    private String oreGenCreateBlockIdInput = "";
    private String oreGenCreateWeightInput = "";
    private String oreGenSearchInput = "";
    private String oreGenSearchQuery = "";
    private List<OreSlot> oreSlots = new ArrayList<>();
    private final Map<String, String> oreBlockIdDraftInputs = new LinkedHashMap<>();
    private final Map<String, String> oreWeightDraftInputs = new LinkedHashMap<>();

    private static final int MAX_ORE_GEN_ROWS = 10;
    private static final int MAX_TIER_ROWS = 5;
    private String oreGenTierCreateNameInput = "";
    private String oreGenTierCreateChanceInput = "";
    private String oreGenTierError = null;
    private List<TierSlot> tierSlots = new ArrayList<>();
    private final Map<String, String> tierNameDraftInputs = new LinkedHashMap<>();
    private final Map<String, String> tierChanceDraftInputs = new LinkedHashMap<>();

    private enum ZoneStatus { NO_ZONE, NO_BEDROCK, VALID }

    private enum DashboardTab { TEMPLATES, ADDONS, SETTINGS, MOBS_MANAGER, RESETS, HOMES, ISLANDS, LEVELS, ORE_GENERATOR }

    private static final class TemplateSlot {
        final String id;
        final String displayName;
        final boolean enabled;
        final boolean hasError;
        final boolean deletable;
        final boolean canMoveUp;
        final boolean canMoveDown;

        TemplateSlot(String id,
                     String displayName,
                     boolean enabled,
                     boolean hasError,
                     boolean deletable,
                     boolean canMoveUp,
                     boolean canMoveDown) {
            this.id = id;
            this.displayName = displayName;
            this.enabled = enabled;
            this.hasError = hasError;
            this.deletable = deletable;
            this.canMoveUp = canMoveUp;
            this.canMoveDown = canMoveDown;
        }
    }

    private static final class AddonSlot {
        final String id;
        final String displayName;
        final boolean enabled;

        AddonSlot(String id, String displayName, boolean enabled) {
            this.id = id;
            this.displayName = displayName;
            this.enabled = enabled;
        }
    }

    private static final class ResetPlayerSlot {
        final UUID uuid;
        final String playerName;
        final int currentResets;

        ResetPlayerSlot(UUID uuid, String playerName, int currentResets) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.currentResets = currentResets;
        }
    }

    private static final class HomePlayerSlot {
        final UUID uuid;
        final String playerName;
        final int currentMaxHomes;

        HomePlayerSlot(UUID uuid, String playerName, int currentMaxHomes) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.currentMaxHomes = currentMaxHomes;
        }
    }

    private static final class IslandPlayerSlot {
        final UUID uuid;
        final String playerName;
        final String islandDisplayName;

        IslandPlayerSlot(UUID uuid, String playerName, String islandDisplayName) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.islandDisplayName = islandDisplayName;
        }
    }

    private static final class LevelBlockSlot {
        final String blockId;
        final double blockValue;

        LevelBlockSlot(String blockId, double blockValue) {
            this.blockId = blockId;
            this.blockValue = blockValue;
        }
    }

    private static final class OreSlot {
        final String blockId;
        final double weight;

        OreSlot(String blockId, double weight) {
            this.blockId = blockId;
            this.weight = weight;
        }
    }

    private static final class TierSlot {
        final String name;
        final String permission;
        final double oreChance;

        TierSlot(String name, String permission, double oreChance) {
            this.name = name;
            this.permission = permission;
            this.oreChance = oreChance;
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
            .builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Back",               Codec.STRING), (o, v) -> o.back               = v, o -> o.back).add()
            .append(new KeyedCodec<>("OpenCreateForm",     Codec.STRING), (o, v) -> o.openCreateForm     = v, o -> o.openCreateForm).add()
            .append(new KeyedCodec<>("Finish",             Codec.STRING), (o, v) -> o.finish             = v, o -> o.finish).add()
            .append(new KeyedCodec<>("CancelForm",         Codec.STRING), (o, v) -> o.cancelForm         = v, o -> o.cancelForm).add()
            .append(new KeyedCodec<>("RefreshTemplates",   Codec.STRING), (o, v) -> o.refreshTemplates   = v, o -> o.refreshTemplates).add()
            .append(new KeyedCodec<>("SwitchToTemplatesTab", Codec.STRING), (o, v) -> o.switchToTemplatesTab = v, o -> o.switchToTemplatesTab).add()
            .append(new KeyedCodec<>("SwitchToAddonsTab",  Codec.STRING), (o, v) -> o.switchToAddonsTab  = v, o -> o.switchToAddonsTab).add()
            .append(new KeyedCodec<>("SwitchToSettingsTab", Codec.STRING), (o, v) -> o.switchToSettingsTab = v, o -> o.switchToSettingsTab).add()
            .append(new KeyedCodec<>("SwitchToMobsManagerTab", Codec.STRING), (o, v) -> o.switchToMobsManagerTab = v, o -> o.switchToMobsManagerTab).add()
            .append(new KeyedCodec<>("SwitchToResetsTab", Codec.STRING), (o, v) -> o.switchToResetsTab = v, o -> o.switchToResetsTab).add()
            .append(new KeyedCodec<>("SwitchToHomesTab", Codec.STRING), (o, v) -> o.switchToHomesTab = v, o -> o.switchToHomesTab).add()
            .append(new KeyedCodec<>("SwitchToIslandsTab", Codec.STRING), (o, v) -> o.switchToIslandsTab = v, o -> o.switchToIslandsTab).add()
            .append(new KeyedCodec<>("SwitchToLevelsTab", Codec.STRING), (o, v) -> o.switchToLevelsTab = v, o -> o.switchToLevelsTab).add()
            .append(new KeyedCodec<>("ToggleTemplate",     Codec.STRING), (o, v) -> o.toggleTemplate     = v, o -> o.toggleTemplate).add()
            .append(new KeyedCodec<>("ToggleAddon",       Codec.STRING), (o, v) -> o.toggleAddon        = v, o -> o.toggleAddon).add()
            .append(new KeyedCodec<>("ToggleSpawnProtection", Codec.STRING), (o, v) -> o.toggleSpawnProtection = v, o -> o.toggleSpawnProtection).add()
            .append(new KeyedCodec<>("ToggleVoidKillItems", Codec.STRING), (o, v) -> o.toggleVoidKillItems = v, o -> o.toggleVoidKillItems).add()
            .append(new KeyedCodec<>("ToggleVoidKillEntities", Codec.STRING), (o, v) -> o.toggleVoidKillEntities = v, o -> o.toggleVoidKillEntities).add()
            .append(new KeyedCodec<>("ToggleVoidTeleport", Codec.STRING), (o, v) -> o.toggleVoidTeleport = v, o -> o.toggleVoidTeleport).add()
            .append(new KeyedCodec<>("PlaceSpawn", Codec.STRING), (o, v) -> o.placeSpawn = v, o -> o.placeSpawn).add()
            .append(new KeyedCodec<>("ApplyProtectionRadius", Codec.STRING), (o, v) -> o.applyProtectionRadius = v, o -> o.applyProtectionRadius).add()
            .append(new KeyedCodec<>("ApplyTemplatesY",   Codec.STRING), (o, v) -> o.applyTemplatesY     = v, o -> o.applyTemplatesY).add()
            .append(new KeyedCodec<>("EditTemplate",       Codec.STRING), (o, v) -> o.editTemplate       = v, o -> o.editTemplate).add()
            .append(new KeyedCodec<>("DeleteTemplate",     Codec.STRING), (o, v) -> o.deleteTemplate     = v, o -> o.deleteTemplate).add()
            .append(new KeyedCodec<>("MoveTemplateUp",     Codec.STRING), (o, v) -> o.moveTemplateUp     = v, o -> o.moveTemplateUp).add()
            .append(new KeyedCodec<>("MoveTemplateDown",   Codec.STRING), (o, v) -> o.moveTemplateDown   = v, o -> o.moveTemplateDown).add()
            .append(new KeyedCodec<>("SaveEditTemplateForm",   Codec.STRING), (o, v) -> o.saveEditTemplateForm   = v, o -> o.saveEditTemplateForm).add()
            .append(new KeyedCodec<>("CancelEditTemplateForm", Codec.STRING), (o, v) -> o.cancelEditTemplateForm = v, o -> o.cancelEditTemplateForm).add()
            .append(new KeyedCodec<>("ConfirmDelete",      Codec.STRING), (o, v) -> o.confirmDelete      = v, o -> o.confirmDelete).add()
            .append(new KeyedCodec<>("CancelDelete",       Codec.STRING), (o, v) -> o.cancelDelete       = v, o -> o.cancelDelete).add()
            .append(new KeyedCodec<>("SelectEditLanguage", Codec.STRING), (o, v) -> o.selectEditLanguage = v, o -> o.selectEditLanguage).add()
            .append(new KeyedCodec<>("@FormName",          Codec.STRING), (o, v) -> o.formName           = v, o -> o.formName).add()
            .append(new KeyedCodec<>("@FormDescription",   Codec.STRING), (o, v) -> o.formDescription    = v, o -> o.formDescription).add()
            .append(new KeyedCodec<>("@FormId",            Codec.STRING), (o, v) -> o.formId             = v, o -> o.formId).add()
            .append(new KeyedCodec<>("@EditName",          Codec.STRING), (o, v) -> o.editName           = v, o -> o.editName).add()
            .append(new KeyedCodec<>("@EditDescription",   Codec.STRING), (o, v) -> o.editDescription    = v, o -> o.editDescription).add()
            .append(new KeyedCodec<>("@SettingsProtectionRadius", Codec.STRING), (o, v) -> o.settingsProtectionRadius = v, o -> o.settingsProtectionRadius).add()
            .append(new KeyedCodec<>("@SettingsTemplatesY", Codec.STRING), (o, v) -> o.settingsTemplatesY = v, o -> o.settingsTemplatesY).add()
            .append(new KeyedCodec<>("@SelectDefaultLanguage", Codec.STRING), (o, v) -> o.selectDefaultLanguage = v, o -> o.selectDefaultLanguage).add()
            .append(new KeyedCodec<>("@SelectFallbackLanguage", Codec.STRING), (o, v) -> o.selectFallbackLanguage = v, o -> o.selectFallbackLanguage).add()
            .append(new KeyedCodec<>("ToggleAutoLangMatch", Codec.STRING), (o, v) -> o.toggleAutoLangMatch = v, o -> o.toggleAutoLangMatch).add()
            .append(new KeyedCodec<>("ToggleTranslateTemplates", Codec.STRING), (o, v) -> o.toggleTranslateTemplates = v, o -> o.toggleTranslateTemplates).add()
            .append(new KeyedCodec<>("OpenLanguagesManager", Codec.STRING), (o, v) -> o.openLanguagesManager = v, o -> o.openLanguagesManager).add()
            .append(new KeyedCodec<>("OpenHomesGroupsManager", Codec.STRING), (o, v) -> o.openHomesGroupsManager = v, o -> o.openHomesGroupsManager).add()
            .append(new KeyedCodec<>("ToggleMobsReduceMobs", Codec.STRING), (o, v) -> o.toggleMobsReduceMobs = v, o -> o.toggleMobsReduceMobs).add()
            .append(new KeyedCodec<>("ToggleMobsTorchProtection", Codec.STRING), (o, v) -> o.toggleMobsTorchProtection = v, o -> o.toggleMobsTorchProtection).add()
            .append(new KeyedCodec<>("ApplyMobsTorchRadius", Codec.STRING), (o, v) -> o.applyMobsTorchRadius = v, o -> o.applyMobsTorchRadius).add()
            .append(new KeyedCodec<>("@MobsReduceRate", Codec.DOUBLE), (o, v) -> o.mobsReduceRate = v, o -> o.mobsReduceRate).add()
            .append(new KeyedCodec<>("@MobsTorchRadius", Codec.STRING), (o, v) -> o.mobsTorchRadius = v, o -> o.mobsTorchRadius).add()
            .append(new KeyedCodec<>("ApplyMaxResets", Codec.STRING), (o, v) -> o.applyMaxResets = v, o -> o.applyMaxResets).add()
            .append(new KeyedCodec<>("@ResetsMaxResets", Codec.STRING), (o, v) -> o.resetsMaxResets = v, o -> o.resetsMaxResets).add()
            .append(new KeyedCodec<>("@ResetsSearchInput", Codec.STRING), (o, v) -> o.resetsSearchInput = v, o -> o.resetsSearchInput).add()
            .append(new KeyedCodec<>("ApplyResetsSearch", Codec.STRING), (o, v) -> o.applyResetsSearch = v, o -> o.applyResetsSearch).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue0", Codec.STRING), (o, v) -> o.resetsPlayerValue0 = v, o -> o.resetsPlayerValue0).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue1", Codec.STRING), (o, v) -> o.resetsPlayerValue1 = v, o -> o.resetsPlayerValue1).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue2", Codec.STRING), (o, v) -> o.resetsPlayerValue2 = v, o -> o.resetsPlayerValue2).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue3", Codec.STRING), (o, v) -> o.resetsPlayerValue3 = v, o -> o.resetsPlayerValue3).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue4", Codec.STRING), (o, v) -> o.resetsPlayerValue4 = v, o -> o.resetsPlayerValue4).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue5", Codec.STRING), (o, v) -> o.resetsPlayerValue5 = v, o -> o.resetsPlayerValue5).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue6", Codec.STRING), (o, v) -> o.resetsPlayerValue6 = v, o -> o.resetsPlayerValue6).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue7", Codec.STRING), (o, v) -> o.resetsPlayerValue7 = v, o -> o.resetsPlayerValue7).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue8", Codec.STRING), (o, v) -> o.resetsPlayerValue8 = v, o -> o.resetsPlayerValue8).add()
            .append(new KeyedCodec<>("@ResetsPlayerValue9", Codec.STRING), (o, v) -> o.resetsPlayerValue9 = v, o -> o.resetsPlayerValue9).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue0", Codec.STRING), (o, v) -> o.applyResetsPlayerValue0 = v, o -> o.applyResetsPlayerValue0).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue1", Codec.STRING), (o, v) -> o.applyResetsPlayerValue1 = v, o -> o.applyResetsPlayerValue1).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue2", Codec.STRING), (o, v) -> o.applyResetsPlayerValue2 = v, o -> o.applyResetsPlayerValue2).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue3", Codec.STRING), (o, v) -> o.applyResetsPlayerValue3 = v, o -> o.applyResetsPlayerValue3).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue4", Codec.STRING), (o, v) -> o.applyResetsPlayerValue4 = v, o -> o.applyResetsPlayerValue4).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue5", Codec.STRING), (o, v) -> o.applyResetsPlayerValue5 = v, o -> o.applyResetsPlayerValue5).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue6", Codec.STRING), (o, v) -> o.applyResetsPlayerValue6 = v, o -> o.applyResetsPlayerValue6).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue7", Codec.STRING), (o, v) -> o.applyResetsPlayerValue7 = v, o -> o.applyResetsPlayerValue7).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue8", Codec.STRING), (o, v) -> o.applyResetsPlayerValue8 = v, o -> o.applyResetsPlayerValue8).add()
            .append(new KeyedCodec<>("ApplyResetsPlayerValue9", Codec.STRING), (o, v) -> o.applyResetsPlayerValue9 = v, o -> o.applyResetsPlayerValue9).add()
            .append(new KeyedCodec<>("ApplyMaxHomes", Codec.STRING), (o, v) -> o.applyMaxHomes = v, o -> o.applyMaxHomes).add()
            .append(new KeyedCodec<>("@HomesMaxHomes", Codec.STRING), (o, v) -> o.homesMaxHomes = v, o -> o.homesMaxHomes).add()
            .append(new KeyedCodec<>("@HomesSearchInput", Codec.STRING), (o, v) -> o.homesSearchInput = v, o -> o.homesSearchInput).add()
            .append(new KeyedCodec<>("ApplyHomesSearch", Codec.STRING), (o, v) -> o.applyHomesSearch = v, o -> o.applyHomesSearch).add()
            .append(new KeyedCodec<>("@IslandsSearchInput", Codec.STRING), (o, v) -> o.islandsSearchInput = v, o -> o.islandsSearchInput).add()
            .append(new KeyedCodec<>("ApplyIslandsSearch", Codec.STRING), (o, v) -> o.applyIslandsSearch = v, o -> o.applyIslandsSearch).add()
            .append(new KeyedCodec<>("@HomesPlayerValue0", Codec.STRING), (o, v) -> o.homesPlayerValue0 = v, o -> o.homesPlayerValue0).add()
            .append(new KeyedCodec<>("@HomesPlayerValue1", Codec.STRING), (o, v) -> o.homesPlayerValue1 = v, o -> o.homesPlayerValue1).add()
            .append(new KeyedCodec<>("@HomesPlayerValue2", Codec.STRING), (o, v) -> o.homesPlayerValue2 = v, o -> o.homesPlayerValue2).add()
            .append(new KeyedCodec<>("@HomesPlayerValue3", Codec.STRING), (o, v) -> o.homesPlayerValue3 = v, o -> o.homesPlayerValue3).add()
            .append(new KeyedCodec<>("@HomesPlayerValue4", Codec.STRING), (o, v) -> o.homesPlayerValue4 = v, o -> o.homesPlayerValue4).add()
            .append(new KeyedCodec<>("@HomesPlayerValue5", Codec.STRING), (o, v) -> o.homesPlayerValue5 = v, o -> o.homesPlayerValue5).add()
            .append(new KeyedCodec<>("@HomesPlayerValue6", Codec.STRING), (o, v) -> o.homesPlayerValue6 = v, o -> o.homesPlayerValue6).add()
            .append(new KeyedCodec<>("@HomesPlayerValue7", Codec.STRING), (o, v) -> o.homesPlayerValue7 = v, o -> o.homesPlayerValue7).add()
            .append(new KeyedCodec<>("@HomesPlayerValue8", Codec.STRING), (o, v) -> o.homesPlayerValue8 = v, o -> o.homesPlayerValue8).add()
            .append(new KeyedCodec<>("@HomesPlayerValue9", Codec.STRING), (o, v) -> o.homesPlayerValue9 = v, o -> o.homesPlayerValue9).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue0", Codec.STRING), (o, v) -> o.applyHomesPlayerValue0 = v, o -> o.applyHomesPlayerValue0).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue1", Codec.STRING), (o, v) -> o.applyHomesPlayerValue1 = v, o -> o.applyHomesPlayerValue1).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue2", Codec.STRING), (o, v) -> o.applyHomesPlayerValue2 = v, o -> o.applyHomesPlayerValue2).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue3", Codec.STRING), (o, v) -> o.applyHomesPlayerValue3 = v, o -> o.applyHomesPlayerValue3).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue4", Codec.STRING), (o, v) -> o.applyHomesPlayerValue4 = v, o -> o.applyHomesPlayerValue4).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue5", Codec.STRING), (o, v) -> o.applyHomesPlayerValue5 = v, o -> o.applyHomesPlayerValue5).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue6", Codec.STRING), (o, v) -> o.applyHomesPlayerValue6 = v, o -> o.applyHomesPlayerValue6).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue7", Codec.STRING), (o, v) -> o.applyHomesPlayerValue7 = v, o -> o.applyHomesPlayerValue7).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue8", Codec.STRING), (o, v) -> o.applyHomesPlayerValue8 = v, o -> o.applyHomesPlayerValue8).add()
            .append(new KeyedCodec<>("ApplyHomesPlayerValue9", Codec.STRING), (o, v) -> o.applyHomesPlayerValue9 = v, o -> o.applyHomesPlayerValue9).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer0", Codec.STRING), (o, v) -> o.viewHomesForPlayer0 = v, o -> o.viewHomesForPlayer0).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer1", Codec.STRING), (o, v) -> o.viewHomesForPlayer1 = v, o -> o.viewHomesForPlayer1).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer2", Codec.STRING), (o, v) -> o.viewHomesForPlayer2 = v, o -> o.viewHomesForPlayer2).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer3", Codec.STRING), (o, v) -> o.viewHomesForPlayer3 = v, o -> o.viewHomesForPlayer3).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer4", Codec.STRING), (o, v) -> o.viewHomesForPlayer4 = v, o -> o.viewHomesForPlayer4).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer5", Codec.STRING), (o, v) -> o.viewHomesForPlayer5 = v, o -> o.viewHomesForPlayer5).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer6", Codec.STRING), (o, v) -> o.viewHomesForPlayer6 = v, o -> o.viewHomesForPlayer6).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer7", Codec.STRING), (o, v) -> o.viewHomesForPlayer7 = v, o -> o.viewHomesForPlayer7).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer8", Codec.STRING), (o, v) -> o.viewHomesForPlayer8 = v, o -> o.viewHomesForPlayer8).add()
            .append(new KeyedCodec<>("ViewHomesForPlayer9", Codec.STRING), (o, v) -> o.viewHomesForPlayer9 = v, o -> o.viewHomesForPlayer9).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer0", Codec.STRING), (o, v) -> o.viewIslandForPlayer0 = v, o -> o.viewIslandForPlayer0).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer1", Codec.STRING), (o, v) -> o.viewIslandForPlayer1 = v, o -> o.viewIslandForPlayer1).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer2", Codec.STRING), (o, v) -> o.viewIslandForPlayer2 = v, o -> o.viewIslandForPlayer2).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer3", Codec.STRING), (o, v) -> o.viewIslandForPlayer3 = v, o -> o.viewIslandForPlayer3).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer4", Codec.STRING), (o, v) -> o.viewIslandForPlayer4 = v, o -> o.viewIslandForPlayer4).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer5", Codec.STRING), (o, v) -> o.viewIslandForPlayer5 = v, o -> o.viewIslandForPlayer5).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer6", Codec.STRING), (o, v) -> o.viewIslandForPlayer6 = v, o -> o.viewIslandForPlayer6).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer7", Codec.STRING), (o, v) -> o.viewIslandForPlayer7 = v, o -> o.viewIslandForPlayer7).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer8", Codec.STRING), (o, v) -> o.viewIslandForPlayer8 = v, o -> o.viewIslandForPlayer8).add()
            .append(new KeyedCodec<>("ViewIslandForPlayer9", Codec.STRING), (o, v) -> o.viewIslandForPlayer9 = v, o -> o.viewIslandForPlayer9).add()
            .append(new KeyedCodec<>("ApplyLevelsLevelValue", Codec.STRING), (o, v) -> o.applyLevelsLevelValue = v, o -> o.applyLevelsLevelValue).add()
            .append(new KeyedCodec<>("ApplyLevelsDefaultBlockValue", Codec.STRING), (o, v) -> o.applyLevelsDefaultBlockValue = v, o -> o.applyLevelsDefaultBlockValue).add()
            .append(new KeyedCodec<>("ToggleLevelsTemplateValue", Codec.STRING), (o, v) -> o.toggleLevelsTemplateValue = v, o -> o.toggleLevelsTemplateValue).add()
            .append(new KeyedCodec<>("CreateLevelsBlockValue", Codec.STRING), (o, v) -> o.createLevelsBlockValue = v, o -> o.createLevelsBlockValue).add()
            .append(new KeyedCodec<>("ApplyLevelsSearch", Codec.STRING), (o, v) -> o.applyLevelsSearch = v, o -> o.applyLevelsSearch).add()
            .append(new KeyedCodec<>("@LevelsLevelValue", Codec.STRING), (o, v) -> o.levelsLevelValue = v, o -> o.levelsLevelValue).add()
            .append(new KeyedCodec<>("@LevelsDefaultBlockValue", Codec.STRING), (o, v) -> o.levelsDefaultBlockValue = v, o -> o.levelsDefaultBlockValue).add()
            .append(new KeyedCodec<>("@LevelsCreateBlockId", Codec.STRING), (o, v) -> o.levelsCreateBlockId = v, o -> o.levelsCreateBlockId).add()
            .append(new KeyedCodec<>("@LevelsCreateBlockValue", Codec.STRING), (o, v) -> o.levelsCreateBlockValue = v, o -> o.levelsCreateBlockValue).add()
            .append(new KeyedCodec<>("@LevelsSearchInput", Codec.STRING), (o, v) -> o.levelsSearchInput = v, o -> o.levelsSearchInput).add()
            .append(new KeyedCodec<>("@LevelsBlockId0", Codec.STRING), (o, v) -> o.levelsBlockId0 = v, o -> o.levelsBlockId0).add()
            .append(new KeyedCodec<>("@LevelsBlockId1", Codec.STRING), (o, v) -> o.levelsBlockId1 = v, o -> o.levelsBlockId1).add()
            .append(new KeyedCodec<>("@LevelsBlockId2", Codec.STRING), (o, v) -> o.levelsBlockId2 = v, o -> o.levelsBlockId2).add()
            .append(new KeyedCodec<>("@LevelsBlockId3", Codec.STRING), (o, v) -> o.levelsBlockId3 = v, o -> o.levelsBlockId3).add()
            .append(new KeyedCodec<>("@LevelsBlockId4", Codec.STRING), (o, v) -> o.levelsBlockId4 = v, o -> o.levelsBlockId4).add()
            .append(new KeyedCodec<>("@LevelsBlockValue0", Codec.STRING), (o, v) -> o.levelsBlockValue0 = v, o -> o.levelsBlockValue0).add()
            .append(new KeyedCodec<>("@LevelsBlockValue1", Codec.STRING), (o, v) -> o.levelsBlockValue1 = v, o -> o.levelsBlockValue1).add()
            .append(new KeyedCodec<>("@LevelsBlockValue2", Codec.STRING), (o, v) -> o.levelsBlockValue2 = v, o -> o.levelsBlockValue2).add()
            .append(new KeyedCodec<>("@LevelsBlockValue3", Codec.STRING), (o, v) -> o.levelsBlockValue3 = v, o -> o.levelsBlockValue3).add()
            .append(new KeyedCodec<>("@LevelsBlockValue4", Codec.STRING), (o, v) -> o.levelsBlockValue4 = v, o -> o.levelsBlockValue4).add()
            .append(new KeyedCodec<>("ApplyLevelsBlock0", Codec.STRING), (o, v) -> o.applyLevelsBlock0 = v, o -> o.applyLevelsBlock0).add()
            .append(new KeyedCodec<>("ApplyLevelsBlock1", Codec.STRING), (o, v) -> o.applyLevelsBlock1 = v, o -> o.applyLevelsBlock1).add()
            .append(new KeyedCodec<>("ApplyLevelsBlock2", Codec.STRING), (o, v) -> o.applyLevelsBlock2 = v, o -> o.applyLevelsBlock2).add()
            .append(new KeyedCodec<>("ApplyLevelsBlock3", Codec.STRING), (o, v) -> o.applyLevelsBlock3 = v, o -> o.applyLevelsBlock3).add()
            .append(new KeyedCodec<>("ApplyLevelsBlock4", Codec.STRING), (o, v) -> o.applyLevelsBlock4 = v, o -> o.applyLevelsBlock4).add()
            .append(new KeyedCodec<>("DeleteLevelsBlock0", Codec.STRING), (o, v) -> o.deleteLevelsBlock0 = v, o -> o.deleteLevelsBlock0).add()
            .append(new KeyedCodec<>("DeleteLevelsBlock1", Codec.STRING), (o, v) -> o.deleteLevelsBlock1 = v, o -> o.deleteLevelsBlock1).add()
            .append(new KeyedCodec<>("DeleteLevelsBlock2", Codec.STRING), (o, v) -> o.deleteLevelsBlock2 = v, o -> o.deleteLevelsBlock2).add()
            .append(new KeyedCodec<>("DeleteLevelsBlock3", Codec.STRING), (o, v) -> o.deleteLevelsBlock3 = v, o -> o.deleteLevelsBlock3).add()
            .append(new KeyedCodec<>("DeleteLevelsBlock4", Codec.STRING), (o, v) -> o.deleteLevelsBlock4 = v, o -> o.deleteLevelsBlock4).add()
            .append(new KeyedCodec<>("SwitchToOreGeneratorTab", Codec.STRING), (o, v) -> o.switchToOreGeneratorTab = v, o -> o.switchToOreGeneratorTab).add()
            .append(new KeyedCodec<>("ToggleOreGenSpawnRubble", Codec.STRING), (o, v) -> o.toggleOreGenSpawnRubble = v, o -> o.toggleOreGenSpawnRubble).add()
            .append(new KeyedCodec<>("@OreGenOreChance", Codec.STRING), (o, v) -> o.oreGenOreChance = v, o -> o.oreGenOreChance).add()
            .append(new KeyedCodec<>("ApplyOreGenOreChance", Codec.STRING), (o, v) -> o.applyOreGenOreChance = v, o -> o.applyOreGenOreChance).add()
            .append(new KeyedCodec<>("@OreGenRegenDelay", Codec.STRING), (o, v) -> o.oreGenRegenDelay = v, o -> o.oreGenRegenDelay).add()
            .append(new KeyedCodec<>("ApplyOreGenRegenDelay", Codec.STRING), (o, v) -> o.applyOreGenRegenDelay = v, o -> o.applyOreGenRegenDelay).add()
            .append(new KeyedCodec<>("@OreGenCreateBlockId", Codec.STRING), (o, v) -> o.oreGenCreateBlockId = v, o -> o.oreGenCreateBlockId).add()
            .append(new KeyedCodec<>("@OreGenCreateWeight", Codec.STRING), (o, v) -> o.oreGenCreateWeight = v, o -> o.oreGenCreateWeight).add()
            .append(new KeyedCodec<>("CreateOreGenOre", Codec.STRING), (o, v) -> o.createOreGenOre = v, o -> o.createOreGenOre).add()
            .append(new KeyedCodec<>("@OreGenSearchInput", Codec.STRING), (o, v) -> o.oreGenSearchInput = v, o -> o.oreGenSearchInput).add()
            .append(new KeyedCodec<>("ApplyOreGenSearch", Codec.STRING), (o, v) -> o.applyOreGenSearch = v, o -> o.applyOreGenSearch).add()
            .append(new KeyedCodec<>("@OreGenBlockId0", Codec.STRING), (o, v) -> o.oreGenBlockId0 = v, o -> o.oreGenBlockId0).add()
            .append(new KeyedCodec<>("@OreGenBlockId1", Codec.STRING), (o, v) -> o.oreGenBlockId1 = v, o -> o.oreGenBlockId1).add()
            .append(new KeyedCodec<>("@OreGenBlockId2", Codec.STRING), (o, v) -> o.oreGenBlockId2 = v, o -> o.oreGenBlockId2).add()
            .append(new KeyedCodec<>("@OreGenBlockId3", Codec.STRING), (o, v) -> o.oreGenBlockId3 = v, o -> o.oreGenBlockId3).add()
            .append(new KeyedCodec<>("@OreGenBlockId4", Codec.STRING), (o, v) -> o.oreGenBlockId4 = v, o -> o.oreGenBlockId4).add()
            .append(new KeyedCodec<>("@OreGenBlockId5", Codec.STRING), (o, v) -> o.oreGenBlockId5 = v, o -> o.oreGenBlockId5).add()
            .append(new KeyedCodec<>("@OreGenBlockId6", Codec.STRING), (o, v) -> o.oreGenBlockId6 = v, o -> o.oreGenBlockId6).add()
            .append(new KeyedCodec<>("@OreGenBlockId7", Codec.STRING), (o, v) -> o.oreGenBlockId7 = v, o -> o.oreGenBlockId7).add()
            .append(new KeyedCodec<>("@OreGenBlockId8", Codec.STRING), (o, v) -> o.oreGenBlockId8 = v, o -> o.oreGenBlockId8).add()
            .append(new KeyedCodec<>("@OreGenBlockId9", Codec.STRING), (o, v) -> o.oreGenBlockId9 = v, o -> o.oreGenBlockId9).add()
            .append(new KeyedCodec<>("@OreGenWeight0", Codec.STRING), (o, v) -> o.oreGenWeight0 = v, o -> o.oreGenWeight0).add()
            .append(new KeyedCodec<>("@OreGenWeight1", Codec.STRING), (o, v) -> o.oreGenWeight1 = v, o -> o.oreGenWeight1).add()
            .append(new KeyedCodec<>("@OreGenWeight2", Codec.STRING), (o, v) -> o.oreGenWeight2 = v, o -> o.oreGenWeight2).add()
            .append(new KeyedCodec<>("@OreGenWeight3", Codec.STRING), (o, v) -> o.oreGenWeight3 = v, o -> o.oreGenWeight3).add()
            .append(new KeyedCodec<>("@OreGenWeight4", Codec.STRING), (o, v) -> o.oreGenWeight4 = v, o -> o.oreGenWeight4).add()
            .append(new KeyedCodec<>("@OreGenWeight5", Codec.STRING), (o, v) -> o.oreGenWeight5 = v, o -> o.oreGenWeight5).add()
            .append(new KeyedCodec<>("@OreGenWeight6", Codec.STRING), (o, v) -> o.oreGenWeight6 = v, o -> o.oreGenWeight6).add()
            .append(new KeyedCodec<>("@OreGenWeight7", Codec.STRING), (o, v) -> o.oreGenWeight7 = v, o -> o.oreGenWeight7).add()
            .append(new KeyedCodec<>("@OreGenWeight8", Codec.STRING), (o, v) -> o.oreGenWeight8 = v, o -> o.oreGenWeight8).add()
            .append(new KeyedCodec<>("@OreGenWeight9", Codec.STRING), (o, v) -> o.oreGenWeight9 = v, o -> o.oreGenWeight9).add()
            .append(new KeyedCodec<>("ApplyOreGenOre0", Codec.STRING), (o, v) -> o.applyOreGenOre0 = v, o -> o.applyOreGenOre0).add()
            .append(new KeyedCodec<>("ApplyOreGenOre1", Codec.STRING), (o, v) -> o.applyOreGenOre1 = v, o -> o.applyOreGenOre1).add()
            .append(new KeyedCodec<>("ApplyOreGenOre2", Codec.STRING), (o, v) -> o.applyOreGenOre2 = v, o -> o.applyOreGenOre2).add()
            .append(new KeyedCodec<>("ApplyOreGenOre3", Codec.STRING), (o, v) -> o.applyOreGenOre3 = v, o -> o.applyOreGenOre3).add()
            .append(new KeyedCodec<>("ApplyOreGenOre4", Codec.STRING), (o, v) -> o.applyOreGenOre4 = v, o -> o.applyOreGenOre4).add()
            .append(new KeyedCodec<>("ApplyOreGenOre5", Codec.STRING), (o, v) -> o.applyOreGenOre5 = v, o -> o.applyOreGenOre5).add()
            .append(new KeyedCodec<>("ApplyOreGenOre6", Codec.STRING), (o, v) -> o.applyOreGenOre6 = v, o -> o.applyOreGenOre6).add()
            .append(new KeyedCodec<>("ApplyOreGenOre7", Codec.STRING), (o, v) -> o.applyOreGenOre7 = v, o -> o.applyOreGenOre7).add()
            .append(new KeyedCodec<>("ApplyOreGenOre8", Codec.STRING), (o, v) -> o.applyOreGenOre8 = v, o -> o.applyOreGenOre8).add()
            .append(new KeyedCodec<>("ApplyOreGenOre9", Codec.STRING), (o, v) -> o.applyOreGenOre9 = v, o -> o.applyOreGenOre9).add()
            .append(new KeyedCodec<>("DeleteOreGenOre0", Codec.STRING), (o, v) -> o.deleteOreGenOre0 = v, o -> o.deleteOreGenOre0).add()
            .append(new KeyedCodec<>("DeleteOreGenOre1", Codec.STRING), (o, v) -> o.deleteOreGenOre1 = v, o -> o.deleteOreGenOre1).add()
            .append(new KeyedCodec<>("DeleteOreGenOre2", Codec.STRING), (o, v) -> o.deleteOreGenOre2 = v, o -> o.deleteOreGenOre2).add()
            .append(new KeyedCodec<>("DeleteOreGenOre3", Codec.STRING), (o, v) -> o.deleteOreGenOre3 = v, o -> o.deleteOreGenOre3).add()
            .append(new KeyedCodec<>("DeleteOreGenOre4", Codec.STRING), (o, v) -> o.deleteOreGenOre4 = v, o -> o.deleteOreGenOre4).add()
            .append(new KeyedCodec<>("DeleteOreGenOre5", Codec.STRING), (o, v) -> o.deleteOreGenOre5 = v, o -> o.deleteOreGenOre5).add()
            .append(new KeyedCodec<>("DeleteOreGenOre6", Codec.STRING), (o, v) -> o.deleteOreGenOre6 = v, o -> o.deleteOreGenOre6).add()
            .append(new KeyedCodec<>("DeleteOreGenOre7", Codec.STRING), (o, v) -> o.deleteOreGenOre7 = v, o -> o.deleteOreGenOre7).add()
            .append(new KeyedCodec<>("DeleteOreGenOre8", Codec.STRING), (o, v) -> o.deleteOreGenOre8 = v, o -> o.deleteOreGenOre8).add()
            .append(new KeyedCodec<>("DeleteOreGenOre9", Codec.STRING), (o, v) -> o.deleteOreGenOre9 = v, o -> o.deleteOreGenOre9).add()
            .append(new KeyedCodec<>("@OreGenTierCreateName", Codec.STRING), (o, v) -> o.oreGenTierCreateName = v, o -> o.oreGenTierCreateName).add()
            .append(new KeyedCodec<>("@OreGenTierCreatePerm", Codec.STRING), (o, v) -> o.oreGenTierCreatePerm = v, o -> o.oreGenTierCreatePerm).add()
            .append(new KeyedCodec<>("@OreGenTierCreateChance", Codec.STRING), (o, v) -> o.oreGenTierCreateChance = v, o -> o.oreGenTierCreateChance).add()
            .append(new KeyedCodec<>("CreateOreGenTier", Codec.STRING), (o, v) -> o.createOreGenTier = v, o -> o.createOreGenTier).add()
            .append(new KeyedCodec<>("@OreGenTierName0", Codec.STRING), (o, v) -> o.oreGenTierName0 = v, o -> o.oreGenTierName0).add()
            .append(new KeyedCodec<>("@OreGenTierName1", Codec.STRING), (o, v) -> o.oreGenTierName1 = v, o -> o.oreGenTierName1).add()
            .append(new KeyedCodec<>("@OreGenTierName2", Codec.STRING), (o, v) -> o.oreGenTierName2 = v, o -> o.oreGenTierName2).add()
            .append(new KeyedCodec<>("@OreGenTierName3", Codec.STRING), (o, v) -> o.oreGenTierName3 = v, o -> o.oreGenTierName3).add()
            .append(new KeyedCodec<>("@OreGenTierName4", Codec.STRING), (o, v) -> o.oreGenTierName4 = v, o -> o.oreGenTierName4).add()
            .append(new KeyedCodec<>("@OreGenTierPerm0", Codec.STRING), (o, v) -> o.oreGenTierPerm0 = v, o -> o.oreGenTierPerm0).add()
            .append(new KeyedCodec<>("@OreGenTierPerm1", Codec.STRING), (o, v) -> o.oreGenTierPerm1 = v, o -> o.oreGenTierPerm1).add()
            .append(new KeyedCodec<>("@OreGenTierPerm2", Codec.STRING), (o, v) -> o.oreGenTierPerm2 = v, o -> o.oreGenTierPerm2).add()
            .append(new KeyedCodec<>("@OreGenTierPerm3", Codec.STRING), (o, v) -> o.oreGenTierPerm3 = v, o -> o.oreGenTierPerm3).add()
            .append(new KeyedCodec<>("@OreGenTierPerm4", Codec.STRING), (o, v) -> o.oreGenTierPerm4 = v, o -> o.oreGenTierPerm4).add()
            .append(new KeyedCodec<>("@OreGenTierChance0", Codec.STRING), (o, v) -> o.oreGenTierChance0 = v, o -> o.oreGenTierChance0).add()
            .append(new KeyedCodec<>("@OreGenTierChance1", Codec.STRING), (o, v) -> o.oreGenTierChance1 = v, o -> o.oreGenTierChance1).add()
            .append(new KeyedCodec<>("@OreGenTierChance2", Codec.STRING), (o, v) -> o.oreGenTierChance2 = v, o -> o.oreGenTierChance2).add()
            .append(new KeyedCodec<>("@OreGenTierChance3", Codec.STRING), (o, v) -> o.oreGenTierChance3 = v, o -> o.oreGenTierChance3).add()
            .append(new KeyedCodec<>("@OreGenTierChance4", Codec.STRING), (o, v) -> o.oreGenTierChance4 = v, o -> o.oreGenTierChance4).add()
            .append(new KeyedCodec<>("ApplyOreGenTier0", Codec.STRING), (o, v) -> o.applyOreGenTier0 = v, o -> o.applyOreGenTier0).add()
            .append(new KeyedCodec<>("ApplyOreGenTier1", Codec.STRING), (o, v) -> o.applyOreGenTier1 = v, o -> o.applyOreGenTier1).add()
            .append(new KeyedCodec<>("ApplyOreGenTier2", Codec.STRING), (o, v) -> o.applyOreGenTier2 = v, o -> o.applyOreGenTier2).add()
            .append(new KeyedCodec<>("ApplyOreGenTier3", Codec.STRING), (o, v) -> o.applyOreGenTier3 = v, o -> o.applyOreGenTier3).add()
            .append(new KeyedCodec<>("ApplyOreGenTier4", Codec.STRING), (o, v) -> o.applyOreGenTier4 = v, o -> o.applyOreGenTier4).add()
            .append(new KeyedCodec<>("DeleteOreGenTier0", Codec.STRING), (o, v) -> o.deleteOreGenTier0 = v, o -> o.deleteOreGenTier0).add()
            .append(new KeyedCodec<>("DeleteOreGenTier1", Codec.STRING), (o, v) -> o.deleteOreGenTier1 = v, o -> o.deleteOreGenTier1).add()
            .append(new KeyedCodec<>("DeleteOreGenTier2", Codec.STRING), (o, v) -> o.deleteOreGenTier2 = v, o -> o.deleteOreGenTier2).add()
            .append(new KeyedCodec<>("DeleteOreGenTier3", Codec.STRING), (o, v) -> o.deleteOreGenTier3 = v, o -> o.deleteOreGenTier3).add()
            .append(new KeyedCodec<>("DeleteOreGenTier4", Codec.STRING), (o, v) -> o.deleteOreGenTier4 = v, o -> o.deleteOreGenTier4).add()
            .build();

        public String back;
        public String openCreateForm;
        public String finish;
        public String cancelForm;
        public String refreshTemplates;
        public String switchToTemplatesTab;
        public String switchToAddonsTab;
        public String switchToSettingsTab;
        public String switchToMobsManagerTab;
        public String switchToResetsTab;
        public String switchToHomesTab;
        public String switchToIslandsTab;
        public String switchToLevelsTab;
        public String toggleTemplate;
        public String toggleAddon;
        public String toggleSpawnProtection;
        public String toggleVoidKillItems;
        public String toggleVoidKillEntities;
        public String toggleVoidTeleport;
        public String placeSpawn;
        public String applyProtectionRadius;
        public String applyTemplatesY;
        public String editTemplate;
        public String deleteTemplate;
        public String moveTemplateUp;
        public String moveTemplateDown;
        public String saveEditTemplateForm;
        public String cancelEditTemplateForm;
        public String confirmDelete;
        public String cancelDelete;
        public String selectEditLanguage;
        public String formName;
        public String formDescription;
        public String formId;
        public String editName;
        public String editDescription;
        public String settingsProtectionRadius;
        public String settingsTemplatesY;
        public String selectDefaultLanguage;
        public String selectFallbackLanguage;
        public String toggleAutoLangMatch;
        public String toggleTranslateTemplates;
        public String openLanguagesManager;
        public String openHomesGroupsManager;
        public String toggleMobsReduceMobs;
        public String toggleMobsTorchProtection;
        public String applyMobsTorchRadius;
        public Double mobsReduceRate;
        public String mobsTorchRadius;
        public String applyMaxResets;
        public String resetsMaxResets;
        public String resetsSearchInput;
        public String applyResetsSearch;
        public String resetsPlayerValue0;
        public String resetsPlayerValue1;
        public String resetsPlayerValue2;
        public String resetsPlayerValue3;
        public String resetsPlayerValue4;
        public String resetsPlayerValue5;
        public String resetsPlayerValue6;
        public String resetsPlayerValue7;
        public String resetsPlayerValue8;
        public String resetsPlayerValue9;
        public String applyResetsPlayerValue0;
        public String applyResetsPlayerValue1;
        public String applyResetsPlayerValue2;
        public String applyResetsPlayerValue3;
        public String applyResetsPlayerValue4;
        public String applyResetsPlayerValue5;
        public String applyResetsPlayerValue6;
        public String applyResetsPlayerValue7;
        public String applyResetsPlayerValue8;
        public String applyResetsPlayerValue9;
        public String applyMaxHomes;
        public String homesMaxHomes;
        public String homesSearchInput;
        public String applyHomesSearch;
        public String islandsSearchInput;
        public String applyIslandsSearch;
        public String homesPlayerValue0;
        public String homesPlayerValue1;
        public String homesPlayerValue2;
        public String homesPlayerValue3;
        public String homesPlayerValue4;
        public String homesPlayerValue5;
        public String homesPlayerValue6;
        public String homesPlayerValue7;
        public String homesPlayerValue8;
        public String homesPlayerValue9;
        public String applyHomesPlayerValue0;
        public String applyHomesPlayerValue1;
        public String applyHomesPlayerValue2;
        public String applyHomesPlayerValue3;
        public String applyHomesPlayerValue4;
        public String applyHomesPlayerValue5;
        public String applyHomesPlayerValue6;
        public String applyHomesPlayerValue7;
        public String applyHomesPlayerValue8;
        public String applyHomesPlayerValue9;
        public String viewHomesForPlayer0;
        public String viewHomesForPlayer1;
        public String viewHomesForPlayer2;
        public String viewHomesForPlayer3;
        public String viewHomesForPlayer4;
        public String viewHomesForPlayer5;
        public String viewHomesForPlayer6;
        public String viewHomesForPlayer7;
        public String viewHomesForPlayer8;
        public String viewHomesForPlayer9;
        public String viewIslandForPlayer0;
        public String viewIslandForPlayer1;
        public String viewIslandForPlayer2;
        public String viewIslandForPlayer3;
        public String viewIslandForPlayer4;
        public String viewIslandForPlayer5;
        public String viewIslandForPlayer6;
        public String viewIslandForPlayer7;
        public String viewIslandForPlayer8;
        public String viewIslandForPlayer9;
        public String applyLevelsLevelValue;
        public String applyLevelsDefaultBlockValue;
        public String toggleLevelsTemplateValue;
        public String createLevelsBlockValue;
        public String applyLevelsSearch;
        public String levelsLevelValue;
        public String levelsDefaultBlockValue;
        public String levelsCreateBlockId;
        public String levelsCreateBlockValue;
        public String levelsSearchInput;
        public String levelsBlockId0;
        public String levelsBlockId1;
        public String levelsBlockId2;
        public String levelsBlockId3;
        public String levelsBlockId4;
        public String levelsBlockValue0;
        public String levelsBlockValue1;
        public String levelsBlockValue2;
        public String levelsBlockValue3;
        public String levelsBlockValue4;
        public String applyLevelsBlock0;
        public String applyLevelsBlock1;
        public String applyLevelsBlock2;
        public String applyLevelsBlock3;
        public String applyLevelsBlock4;
        public String deleteLevelsBlock0;
        public String deleteLevelsBlock1;
        public String deleteLevelsBlock2;
        public String deleteLevelsBlock3;
        public String deleteLevelsBlock4;
        public String switchToOreGeneratorTab;
        public String toggleOreGenSpawnRubble;
        public String oreGenOreChance;
        public String applyOreGenOreChance;
        public String oreGenRegenDelay;
        public String applyOreGenRegenDelay;
        public String oreGenCreateBlockId;
        public String oreGenCreateWeight;
        public String createOreGenOre;
        public String oreGenSearchInput;
        public String applyOreGenSearch;
        public String oreGenBlockId0;
        public String oreGenBlockId1;
        public String oreGenBlockId2;
        public String oreGenBlockId3;
        public String oreGenBlockId4;
        public String oreGenBlockId5;
        public String oreGenBlockId6;
        public String oreGenBlockId7;
        public String oreGenBlockId8;
        public String oreGenBlockId9;
        public String oreGenWeight0;
        public String oreGenWeight1;
        public String oreGenWeight2;
        public String oreGenWeight3;
        public String oreGenWeight4;
        public String oreGenWeight5;
        public String oreGenWeight6;
        public String oreGenWeight7;
        public String oreGenWeight8;
        public String oreGenWeight9;
        public String applyOreGenOre0;
        public String applyOreGenOre1;
        public String applyOreGenOre2;
        public String applyOreGenOre3;
        public String applyOreGenOre4;
        public String applyOreGenOre5;
        public String applyOreGenOre6;
        public String applyOreGenOre7;
        public String applyOreGenOre8;
        public String applyOreGenOre9;
        public String deleteOreGenOre0;
        public String deleteOreGenOre1;
        public String deleteOreGenOre2;
        public String deleteOreGenOre3;
        public String deleteOreGenOre4;
        public String deleteOreGenOre5;
        public String deleteOreGenOre6;
        public String deleteOreGenOre7;
        public String deleteOreGenOre8;
        public String deleteOreGenOre9;
        public String oreGenTierCreateName;
        public String oreGenTierCreatePerm;
        public String oreGenTierCreateChance;
        public String createOreGenTier;
        public String oreGenTierName0;
        public String oreGenTierName1;
        public String oreGenTierName2;
        public String oreGenTierName3;
        public String oreGenTierName4;
        public String oreGenTierPerm0;
        public String oreGenTierPerm1;
        public String oreGenTierPerm2;
        public String oreGenTierPerm3;
        public String oreGenTierPerm4;
        public String oreGenTierChance0;
        public String oreGenTierChance1;
        public String oreGenTierChance2;
        public String oreGenTierChance3;
        public String oreGenTierChance4;
        public String applyOreGenTier0;
        public String applyOreGenTier1;
        public String applyOreGenTier2;
        public String applyOreGenTier3;
        public String applyOreGenTier4;
        public String deleteOreGenTier0;
        public String deleteOreGenTier1;
        public String deleteOreGenTier2;
        public String deleteOreGenTier3;
        public String deleteOreGenTier4;
    }

    public ISkyblockAdminDashboardPage(@Nonnull PlayerRef playerRef,
                                       @Nonnull World sourceWorld,
                                       @Nonnull ISkyblockPlugin plugin) {
        this(playerRef, sourceWorld, plugin, false);
    }

    public ISkyblockAdminDashboardPage(@Nonnull PlayerRef playerRef,
                                       @Nonnull World sourceWorld,
                                       @Nonnull ISkyblockPlugin plugin,
                                       boolean restricted) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;
        this.sourceWorld = sourceWorld;
        this.restricted = restricted;
        if (restricted && activeTab != DashboardTab.ISLANDS && activeTab != DashboardTab.HOMES && activeTab != DashboardTab.RESETS) {
            activeTab = DashboardTab.ISLANDS;
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {

        commandBuilder.append("Pages/ISkyblockAdminDashboard.ui");
        boolean inFormMenu = showCreateForm || showEditTemplateForm || showDeleteConfirm;
        ZoneStatus zoneStatus = inFormMenu ? ZoneStatus.NO_ZONE : computeZoneStatus(store, ref);
        String language = getAdminDashboardLanguage();
        String fallbackLanguage = plugin.getConfigManager().getConfig().getSettings().getFallbackLanguage();
        boolean translateTemplates = plugin.getConfigManager().getConfig().getSettings().isTranslateTemplates();
        boolean mobsManagerEnabled = plugin.getAddonManager().isMobsManagerEnabled();
        boolean homesEnabled = plugin.getAddonManager().isHomesEnabled();
        boolean levelsEnabled = plugin.getAddonManager().isLevelsEnabled();
        boolean oreGeneratorEnabled = plugin.getAddonManager().isOreGeneratorEnabled();
        if (activeTab == DashboardTab.MOBS_MANAGER && !mobsManagerEnabled) {
            activeTab = DashboardTab.ADDONS;
        }
        if (activeTab == DashboardTab.HOMES && !homesEnabled) {
            activeTab = DashboardTab.ADDONS;
        }
        if (activeTab == DashboardTab.LEVELS && !levelsEnabled) {
            activeTab = DashboardTab.ADDONS;
        }
        if (activeTab == DashboardTab.ORE_GENERATOR && !oreGeneratorEnabled) {
            activeTab = DashboardTab.ADDONS;
        }
        List<String> editLanguages = getEditLanguages(translateTemplates, fallbackLanguage);
        boolean showTemplatesPanel = !restricted && !inFormMenu && activeTab == DashboardTab.TEMPLATES;
        boolean showAddonsPanel = !restricted && !inFormMenu && activeTab == DashboardTab.ADDONS;
        boolean showSettingsPanel = !restricted && !inFormMenu && activeTab == DashboardTab.SETTINGS;
        boolean showMobsManagerPanel = !restricted && !inFormMenu && activeTab == DashboardTab.MOBS_MANAGER && mobsManagerEnabled;
        boolean showResetsPanel = !inFormMenu && activeTab == DashboardTab.RESETS;
        boolean showHomesPanel = !inFormMenu && activeTab == DashboardTab.HOMES && homesEnabled;
        boolean showIslandsPanel = !inFormMenu && activeTab == DashboardTab.ISLANDS;
        boolean showLevelsPanel = !restricted && !inFormMenu && activeTab == DashboardTab.LEVELS && levelsEnabled;
        boolean showOreGeneratorPanel = !restricted && !inFormMenu && activeTab == DashboardTab.ORE_GENERATOR && oreGeneratorEnabled;

        commandBuilder.set("#SidebarNavTemplates.Visible", !restricted && activeTab != DashboardTab.TEMPLATES);
        commandBuilder.set("#SidebarNavTemplatesActive.Visible", !restricted && activeTab == DashboardTab.TEMPLATES);
        commandBuilder.set("#SidebarNavAddons.Visible", !restricted && activeTab != DashboardTab.ADDONS);
        commandBuilder.set("#SidebarNavAddonsActive.Visible", !restricted && activeTab == DashboardTab.ADDONS);
        commandBuilder.set("#SidebarNavSettings.Visible", !restricted && activeTab != DashboardTab.SETTINGS);
        commandBuilder.set("#SidebarNavSettingsActive.Visible", !restricted && activeTab == DashboardTab.SETTINGS);
        commandBuilder.set("#SidebarNavMobsManager.Visible", !restricted && mobsManagerEnabled && activeTab != DashboardTab.MOBS_MANAGER);
        commandBuilder.set("#SidebarNavMobsManagerActive.Visible", !restricted && mobsManagerEnabled && activeTab == DashboardTab.MOBS_MANAGER);
        commandBuilder.set("#SidebarNavResets.Visible", activeTab != DashboardTab.RESETS);
        commandBuilder.set("#SidebarNavResetsActive.Visible", activeTab == DashboardTab.RESETS);
        commandBuilder.set("#SidebarNavHomes.Visible", homesEnabled && activeTab != DashboardTab.HOMES);
        commandBuilder.set("#SidebarNavHomesActive.Visible", homesEnabled && activeTab == DashboardTab.HOMES);
        commandBuilder.set("#SidebarNavIslands.Visible", activeTab != DashboardTab.ISLANDS);
        commandBuilder.set("#SidebarNavIslandsActive.Visible", activeTab == DashboardTab.ISLANDS);
        commandBuilder.set("#SidebarNavLevels.Visible", !restricted && levelsEnabled && activeTab != DashboardTab.LEVELS);
        commandBuilder.set("#SidebarNavLevelsActive.Visible", !restricted && levelsEnabled && activeTab == DashboardTab.LEVELS);
        commandBuilder.set("#SidebarNavOreGenerator.Visible", !restricted && oreGeneratorEnabled && activeTab != DashboardTab.ORE_GENERATOR);
        commandBuilder.set("#SidebarNavOreGeneratorActive.Visible", !restricted && oreGeneratorEnabled && activeTab == DashboardTab.ORE_GENERATOR);
        commandBuilder.set("#SidebarNavTemplates.Text", uiText(language, "ui.admin_dashboard.sidebar.templates"));
        commandBuilder.set("#SidebarNavTemplatesActive.Text", uiText(language, "ui.admin_dashboard.sidebar.templates"));
        commandBuilder.set("#SidebarNavAddons.Text", uiText(language, "ui.admin_dashboard.sidebar.addons"));
        commandBuilder.set("#SidebarNavAddonsActive.Text", uiText(language, "ui.admin_dashboard.sidebar.addons"));
        commandBuilder.set("#SidebarNavSettings.Text", uiText(language, "ui.admin_dashboard.sidebar.settings"));
        commandBuilder.set("#SidebarNavSettingsActive.Text", uiText(language, "ui.admin_dashboard.sidebar.settings"));
        commandBuilder.set("#SidebarNavMobsManager.Text", uiText(language, "ui.admin_dashboard.sidebar.mobs_manager"));
        commandBuilder.set("#SidebarNavMobsManagerActive.Text", uiText(language, "ui.admin_dashboard.sidebar.mobs_manager"));
        commandBuilder.set("#SidebarNavResets.Text", uiText(language, "ui.admin_dashboard.sidebar.resets"));
        commandBuilder.set("#SidebarNavResetsActive.Text", uiText(language, "ui.admin_dashboard.sidebar.resets"));
        commandBuilder.set("#SidebarNavHomes.Text", uiText(language, "ui.admin_dashboard.sidebar.homes"));
        commandBuilder.set("#SidebarNavHomesActive.Text", uiText(language, "ui.admin_dashboard.sidebar.homes"));
        commandBuilder.set("#SidebarNavIslands.Text", uiText(language, "ui.admin_dashboard.sidebar.islands"));
        commandBuilder.set("#SidebarNavIslandsActive.Text", uiText(language, "ui.admin_dashboard.sidebar.islands"));
        commandBuilder.set("#SidebarNavLevels.Text", uiText(language, "ui.admin_dashboard.sidebar.levels"));
        commandBuilder.set("#SidebarNavLevelsActive.Text", uiText(language, "ui.admin_dashboard.sidebar.levels"));
        commandBuilder.set("#SidebarNavOreGenerator.Text", uiText(language, "ui.admin_dashboard.sidebar.ore_generator"));
        commandBuilder.set("#SidebarNavOreGeneratorActive.Text", uiText(language, "ui.admin_dashboard.sidebar.ore_generator"));
        commandBuilder.set("#CloseButton.Text", uiText(language, "ui.common.close"));

        commandBuilder.set("#CreateSection.Visible", showTemplatesPanel);
        commandBuilder.set("#TemplatesSection.Visible", showTemplatesPanel);
        commandBuilder.set("#AddonsSection.Visible", showAddonsPanel);
        commandBuilder.set("#SettingsSection.Visible", showSettingsPanel);
        commandBuilder.set("#MobsManagerSection.Visible", showMobsManagerPanel);
        commandBuilder.set("#ResetsSection.Visible", showResetsPanel);
        commandBuilder.set("#HomesSection.Visible", showHomesPanel);
        commandBuilder.set("#IslandsSection.Visible", showIslandsPanel);
        commandBuilder.set("#LevelsSection.Visible", showLevelsPanel);
        commandBuilder.set("#OreGeneratorSection.Visible", showOreGeneratorPanel);
        commandBuilder.set("#CreateSectionTitle.Text", uiText(language, "ui.admin_dashboard.create_section_title"));
        commandBuilder.set("#CreateSectionSubtitle.Text", uiText(language, "ui.admin_dashboard.create_section_subtitle"));
        commandBuilder.set("#TemplatesSectionTitle.Text", uiText(language, "ui.admin_dashboard.templates_section_title"));
        commandBuilder.set("#TemplatesSectionDescription.Text", uiText(language, "ui.admin_dashboard.templates_section_description"));
        commandBuilder.set("#AddonsSectionTitle.Text", uiText(language, "ui.admin_dashboard.addons_section_title"));
        commandBuilder.set("#AddonsSectionDescription.Text", uiText(language, "ui.admin_dashboard.addons_section_description"));
        commandBuilder.set("#SettingsSectionTitle.Text", uiText(language, "ui.admin_dashboard.settings_section_title"));
        commandBuilder.set("#SettingsSectionDescription.Text", uiText(language, "ui.admin_dashboard.settings_section_description"));

        boolean spawnProtectionEnabled = plugin.getConfigManager().getConfig().getSettings().isSpawnProtectionEnabled();
        int protectionRadius = plugin.getConfigManager().getConfig().getSettings().getSpawnProtectionRadius();
        int templatesY = plugin.getConfigManager().getConfig().getGrid().getIslandY();
        boolean voidKillItemsEnabled = plugin.getConfigManager().getConfig().getSettings().isVoidKillItemsEnabled();
        boolean voidKillEntitiesEnabled = plugin.getConfigManager().getConfig().getSettings().isVoidKillEntitiesEnabled();
        boolean voidTeleportEnabled = plugin.getConfigManager().getConfig().getSettings().isVoidTeleportEnabled();
        String protectionRadiusDisplay = getSettingsInputDisplayValue(settingsProtectionRadiusInput, protectionRadius);
        String templatesYDisplay = getSettingsInputDisplayValue(settingsTemplatesYInput, templatesY);
        boolean protectionRadiusDirty = isSettingsInputDirty(settingsProtectionRadiusInput, protectionRadius);
        boolean templatesYDirty = isSettingsInputDirty(settingsTemplatesYInput, templatesY);

        commandBuilder.set("#SettingsSpawnProtectionToggle.Value", spawnProtectionEnabled);
        commandBuilder.set("#SettingsProtectionRadiusRow.Visible", spawnProtectionEnabled);
        commandBuilder.set("#SettingsVoidKillItemsToggle.Value", voidKillItemsEnabled);
        commandBuilder.set("#SettingsVoidKillEntitiesToggle.Value", voidKillEntitiesEnabled);
        commandBuilder.set("#SettingsVoidTeleportToggle.Value", voidTeleportEnabled);
        commandBuilder.set("#SettingsSpawnProtectionRow.Background", spawnProtectionEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#SettingsVoidKillItemsRow.Background", voidKillItemsEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#SettingsVoidKillEntitiesRow.Background", voidKillEntitiesEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#SettingsVoidTeleportRow.Background", voidTeleportEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#SettingsProtectionRadiusField.Value", protectionRadiusDisplay);
        commandBuilder.set("#SettingsTemplatesYField.Value", templatesYDisplay);
        commandBuilder.set("#SettingsProtectionRadiusFieldBorder.Background", protectionRadiusDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#SettingsTemplatesYFieldBorder.Background", templatesYDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#SettingsProtectionRadiusApplyBox.Visible", settingsProtectionRadiusDirtyShown);
        commandBuilder.set("#SettingsTemplatesYApplyBox.Visible", settingsTemplatesYDirtyShown);
        commandBuilder.set("#SettingsSectionError.Visible", settingsError != null && !settingsError.isBlank());
        commandBuilder.set("#SettingsSectionError.Text", settingsError == null ? "" : settingsError);

        commandBuilder.set("#SettingsSpawnProtectionLabel.Text", uiText(language, "ui.admin_dashboard.settings.spawn_protection"));
        commandBuilder.set("#SettingsGeneralSectionTitle.Text", uiText(language, "ui.admin_dashboard.settings.general_section_title"));
        commandBuilder.set("#SettingsGeneralSectionDescription.Text", uiText(language, "ui.admin_dashboard.settings.general_section_description"));
        commandBuilder.set("#SettingsPlaceSpawnLabel.Text", uiText(language, "ui.admin_dashboard.settings.spawn_coordinates"));
        commandBuilder.set("#SettingsPlaceSpawnButton.Text", uiText(language, "ui.admin_dashboard.settings.place_spawn"));
        commandBuilder.set("#SettingsProtectionRadiusLabel.Text", uiText(language, "ui.admin_dashboard.settings.protection_radius"));
        commandBuilder.set("#SettingsTemplatesYLabel.Text", uiText(language, "ui.admin_dashboard.settings.templates_y"));
        commandBuilder.set("#SettingsVoidKillItemsLabel.Text", uiText(language, "ui.admin_dashboard.settings.void_kill_items"));
        commandBuilder.set("#SettingsVoidKillEntitiesLabel.Text", uiText(language, "ui.admin_dashboard.settings.void_kill_entities"));
        commandBuilder.set("#SettingsVoidTeleportLabel.Text", uiText(language, "ui.admin_dashboard.settings.void_teleport"));
        String configLang = plugin.getConfigManager().getConfig().getLanguage();
        String defaultLanguage = (configLang != null && !configLang.isBlank()) ? configLang.trim().toLowerCase(java.util.Locale.ROOT) : "en";
        boolean autoLangMatchEnabled = plugin.getConfigManager().getConfig().getSettings().isAutoLangMatchEnabled();
        List<String> availableLanguages = plugin.getMessageManager().getAvailableLanguages();
        DropdownEntryInfo[] langEntries = buildLanguageEntries(availableLanguages);
        commandBuilder.set("#SettingsLangSectionTitle.Text", uiText(language, "ui.admin_dashboard.settings.lang_section_title"));
        commandBuilder.set("#SettingsLangSectionDescription.Text", uiText(language, "ui.admin_dashboard.settings.lang_section_description"));
        commandBuilder.set("#SettingsDefaultLangLabel.Text", uiText(language, "ui.admin_dashboard.settings.default_language"));
        commandBuilder.set("#SettingsDefaultLangDropdown.Entries", langEntries);
        commandBuilder.set("#SettingsDefaultLangDropdown.Value", defaultLanguage);
        commandBuilder.set("#SettingsAutoLangMatchToggle.Value", autoLangMatchEnabled);
        commandBuilder.set("#SettingsAutoLangMatchRow.Background", autoLangMatchEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#SettingsAutoLangMatchLabel.Text", uiText(language, "ui.admin_dashboard.settings.auto_lang_match"));
        commandBuilder.set("#SettingsManageLangsLabel.Text", uiText(language, "ui.admin_dashboard.settings.manage_languages"));
        commandBuilder.set("#SettingsManageLangs.Text", uiText(language, "ui.admin_dashboard.settings.manage_btn"));
        commandBuilder.set("#SettingsFallbackLangLabel.Text", uiText(language, "ui.admin_dashboard.settings.fallback_language"));
        commandBuilder.set("#SettingsFallbackLangDropdown.Entries", langEntries);
        commandBuilder.set("#SettingsFallbackLangDropdown.Value", plugin.getLanguageService().resolveInputLanguage(fallbackLanguage));
        boolean translateTemplatesEnabled = plugin.getConfigManager().getConfig().getSettings().isTranslateTemplates();
        commandBuilder.set("#SettingsTranslateTemplatesToggle.Value", translateTemplatesEnabled);
        commandBuilder.set("#SettingsTranslateTemplatesRow.Background", translateTemplatesEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#SettingsTranslateTemplatesLabel.Text", uiText(language, "ui.admin_dashboard.settings.translate_templates"));

        boolean reduceMobsEnabled = plugin.getAddonManager().getMobsManagerAddon().isReduceMobsEnabled();
        int reduceMobsRate = plugin.getAddonManager().getMobsManagerAddon().getReduceMobsRatePercent();
        boolean torchProtectionEnabled = plugin.getAddonManager().getMobsManagerAddon().isTorchProtectionEnabled();
        int torchProtectionRadius = plugin.getAddonManager().getMobsManagerAddon().getTorchProtectionRadius();
        String torchRadiusDisplay = getSettingsInputDisplayValue(mobsTorchRadiusInput, torchProtectionRadius);
        boolean torchRadiusDirty = isSettingsInputDirty(mobsTorchRadiusInput, torchProtectionRadius);

        commandBuilder.set("#MobsManagerSectionTitle.Text", uiText(language, "ui.admin_dashboard.mobs.title"));
        commandBuilder.set("#MobsManagerSectionDescription.Text", uiText(language, "ui.admin_dashboard.mobs.description"));
        commandBuilder.set("#MobsReduceMobsToggle.Value", reduceMobsEnabled);
        commandBuilder.set("#MobsReduceMobsLabel.Text", uiText(language, "ui.admin_dashboard.mobs.reduce_mobs"));
        commandBuilder.set("#MobsReduceMobsRow.Background", reduceMobsEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#MobsReduceRateRow.Visible", reduceMobsEnabled);
        commandBuilder.set("#MobsReduceRateLabel.Text", uiText(language, "ui.admin_dashboard.mobs.reduce_rate"));
        commandBuilder.set("#MobsReduceRateField.Value", reduceMobsRate);
        commandBuilder.set("#MobsTorchProtectionToggle.Value", torchProtectionEnabled);
        commandBuilder.set("#MobsTorchProtectionLabel.Text", uiText(language, "ui.admin_dashboard.mobs.torch_protection"));
        commandBuilder.set("#MobsTorchProtectionRow.Background", torchProtectionEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#MobsTorchRadiusRow.Visible", torchProtectionEnabled);
        commandBuilder.set("#MobsTorchRadiusLabel.Text", uiText(language, "ui.admin_dashboard.mobs.torch_radius"));
        commandBuilder.set("#MobsTorchRadiusField.Value", torchRadiusDisplay);
        commandBuilder.set("#MobsTorchRadiusFieldBorder.Background", torchRadiusDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#MobsTorchRadiusApplyBox.Visible", mobsTorchRadiusDirtyShown);
        commandBuilder.set("#MobsManagerError.Visible", mobsError != null && !mobsError.isBlank());
        commandBuilder.set("#MobsManagerError.Text", mobsError == null ? "" : mobsError);

        int maxResets = Math.max(0, plugin.getConfigManager().getConfig().getSettings().getMaxResets());
        String maxResetsDisplay = getSettingsInputDisplayValue(resetsMaxResetsInput, maxResets);
        boolean maxResetsDirty = isSettingsInputDirty(resetsMaxResetsInput, maxResets);

        commandBuilder.set("#ResetsPlayersSectionTitle.Text", uiText(language, "ui.admin_dashboard.resets.players_title"));
        commandBuilder.set("#ResetsMaxResetsLabel.Text", uiText(language, "ui.admin_dashboard.resets.max_resets"));
        commandBuilder.set("#ResetsMaxResetsField.Value", maxResetsDisplay);
        commandBuilder.set("#ResetsMaxResetsFieldBorder.Background", maxResetsDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#ResetsMaxResetsApplyBox.Visible", resetsMaxResetsDirtyShown);
        commandBuilder.set("#ResetsSearchField.PlaceholderText", uiText(language, "ui.admin_dashboard.resets.search_placeholder"));
        commandBuilder.set("#ResetsSearchField.Value", resetsSearchInput == null ? "" : resetsSearchInput);
        commandBuilder.set("#ResetsSearchButton.Text", uiText(language, "ui.admin_dashboard.resets.search_button"));
        commandBuilder.set("#ResetsSectionError.Visible", resetsError != null && !resetsError.isBlank());
        commandBuilder.set("#ResetsSectionError.Text", resetsError == null ? "" : resetsError);
        if (restricted) {
            commandBuilder.set("#ResetsTopDivider.Visible", false);
            commandBuilder.set("#ResetsSectionTitle.Visible", false);
            commandBuilder.set("#ResetsSectionDescription.Visible", false);
            commandBuilder.set("#ResetsMaxResetsRow.Visible", false);
        }
        buildResetRows(commandBuilder, resetsSearchQuery);

        int maxHomes = Math.max(0, plugin.getAddonManager().getHomesAddon().getMaxHomes());
        String maxHomesDisplay = getSettingsInputDisplayValue(homesMaxHomesInput, maxHomes);
        boolean maxHomesDirty = isSettingsInputDirty(homesMaxHomesInput, maxHomes);

        commandBuilder.set("#HomesPlayersSectionTitle.Text", uiText(language, "ui.admin_dashboard.homes.players_added_title"));
        commandBuilder.set("#HomesMaxHomesLabel.Text", uiText(language, "ui.admin_dashboard.homes.max_homes_default"));
        commandBuilder.set("#HomesMaxHomesField.Value", maxHomesDisplay);
        commandBuilder.set("#HomesMaxHomesFieldBorder.Background", maxHomesDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#HomesMaxHomesApplyBox.Visible", homesMaxHomesDirtyShown);
        commandBuilder.set("#HomesGroupsManageLabel.Text", uiText(language, "ui.admin_dashboard.homes.groups_title"));
        commandBuilder.set("#HomesGroupsManageDescription.Text", uiText(language, "ui.admin_dashboard.homes.groups_manage_description"));
        commandBuilder.set("#HomesManageGroupsButton.Text", uiText(language, "ui.admin_dashboard.homes.groups_manage_button"));
        commandBuilder.set("#HomesSearchField.PlaceholderText", uiText(language, "ui.admin_dashboard.homes.search_placeholder"));
        commandBuilder.set("#HomesSearchField.Value", homesSearchInput == null ? "" : homesSearchInput);
        commandBuilder.set("#HomesSearchButton.Text", uiText(language, "ui.admin_dashboard.homes.search_button"));
        commandBuilder.set("#HomesSectionError.Visible", homesError != null && !homesError.isBlank());
        commandBuilder.set("#HomesSectionError.Text", homesError == null ? "" : homesError);
        if (restricted) {
            commandBuilder.set("#HomesTopDivider.Visible", false);
            commandBuilder.set("#HomesSectionTitle.Visible", false);
            commandBuilder.set("#HomesSectionDescription.Visible", false);
            commandBuilder.set("#HomesMaxHomesRow.Visible", false);
            commandBuilder.set("#HomesGroupsManageRow.Visible", false);
        }
        buildHomesRows(commandBuilder, homesSearchQuery);
        if (restricted) {
            for (int i = 0; i < MAX_HOMES_ROWS; i++) {
                commandBuilder.set("#HomesPlayerValueFieldBorder" + i + ".Visible", false);
                commandBuilder.set("#HomesPlayerValueApplyBox" + i + ".Visible", false);
            }
        }

        commandBuilder.set("#IslandsPlayersSectionTitle.Text", uiText(language, "ui.admin_dashboard.islands.players_title"));
        commandBuilder.set("#IslandsSectionDescription.Text", uiText(language, "ui.admin_dashboard.islands.description"));
        commandBuilder.set("#IslandsSearchField.PlaceholderText", uiText(language, "ui.admin_dashboard.islands.search_placeholder"));
        commandBuilder.set("#IslandsSearchField.Value", islandsSearchInput == null ? "" : islandsSearchInput);
        commandBuilder.set("#IslandsSearchButton.Text", uiText(language, "ui.admin_dashboard.islands.search_button"));
        commandBuilder.set("#IslandsSectionError.Visible", islandsError != null && !islandsError.isBlank());
        commandBuilder.set("#IslandsSectionError.Text", islandsError == null ? "" : islandsError);
        buildIslandsRows(commandBuilder, islandsSearchQuery);

        int configuredLevelValue = plugin.getAddonManager().getLevelsAddon().getLevelValue();
        double configuredDefaultBlockValue = plugin.getAddonManager().getLevelsAddon().getBlockDefaultValue();
        boolean templateValueEnabled = plugin.getAddonManager().getLevelsAddon().isTemplateValueEnabled();
        String levelValueDisplay = getSettingsInputDisplayValue(levelsLevelValueInput, configuredLevelValue);
        String defaultBlockValueDisplay = getSettingsInputDisplayValue(levelsDefaultBlockValueInput, formatDouble(configuredDefaultBlockValue));
        boolean levelValueDirty = isSettingsInputDirty(levelsLevelValueInput, configuredLevelValue);
        boolean defaultBlockValueDirty = isSettingsInputDirty(levelsDefaultBlockValueInput, formatDouble(configuredDefaultBlockValue));

        commandBuilder.set("#LevelsSectionTitle.Text", uiText(language, "ui.admin_dashboard.levels.title"));
        commandBuilder.set("#LevelsSectionDescription.Text", uiText(language, "ui.admin_dashboard.levels.description"));
        commandBuilder.set("#LevelsLevelValueLabel.Text", uiText(language, "ui.admin_dashboard.levels.level_value"));
        commandBuilder.set("#LevelsLevelValueField.Value", levelValueDisplay);
        commandBuilder.set("#LevelsLevelValueFieldBorder.Background", levelValueDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#LevelsLevelValueApplyBox.Visible", levelsLevelValueDirtyShown);
        commandBuilder.set("#LevelsDefaultBlockValueLabel.Text", uiText(language, "ui.admin_dashboard.levels.default_block_value"));
        commandBuilder.set("#LevelsDefaultBlockValueField.Value", defaultBlockValueDisplay);
        commandBuilder.set("#LevelsDefaultBlockValueFieldBorder.Background", defaultBlockValueDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#LevelsDefaultBlockValueApplyBox.Visible", levelsDefaultBlockValueDirtyShown);
        commandBuilder.set("#LevelsTemplateValueToggle.Value", templateValueEnabled);
        commandBuilder.set("#LevelsTemplateValueRow.Background", templateValueEnabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#LevelsTemplateValueLabel.Text", uiText(language, "ui.admin_dashboard.levels.template_value_enabled"));
        commandBuilder.set("#LevelsBlocksSectionTitle.Text", uiText(language, "ui.admin_dashboard.levels.blocks_title"));
        commandBuilder.set("#LevelsCreateBlockIdField.PlaceholderText", uiText(language, "ui.admin_dashboard.levels.create_block_id_placeholder"));
        commandBuilder.set("#LevelsCreateBlockValueField.PlaceholderText", uiText(language, "ui.admin_dashboard.levels.create_block_value_placeholder"));
        commandBuilder.set("#LevelsCreateBlockIdField.Value", levelsCreateBlockIdInput == null ? "" : levelsCreateBlockIdInput);
        commandBuilder.set("#LevelsCreateBlockValueField.Value", levelsCreateBlockValueInput == null ? "" : levelsCreateBlockValueInput);
        commandBuilder.set("#LevelsCreateBlockButton.Text", uiText(language, "ui.common.create"));
        commandBuilder.set("#LevelsSearchField.PlaceholderText", uiText(language, "ui.admin_dashboard.levels.search_placeholder"));
        commandBuilder.set("#LevelsSearchField.Value", levelsSearchInput == null ? "" : levelsSearchInput);
        commandBuilder.set("#LevelsSearchButton.Text", uiText(language, "ui.admin_dashboard.levels.search_button"));
        commandBuilder.set("#LevelsSectionError.Visible", levelsError != null && !levelsError.isBlank());
        commandBuilder.set("#LevelsSectionError.Text", levelsError == null ? "" : levelsError);
        buildLevelsRows(commandBuilder, levelsSearchQuery);

        double configuredOreChance = plugin.getAddonManager().getOreGeneratorAddon().getOreChance();
        long configuredRegenDelay = plugin.getAddonManager().getOreGeneratorAddon().getRegenDelayMs();
        boolean oreGenSpawnRubble = plugin.getAddonManager().getOreGeneratorAddon().isSpawnRubble();
        String oreChanceDisplay = getSettingsInputDisplayValue(oreGenOreChanceInput, formatDouble(configuredOreChance));
        String regenDelayDisplay = getSettingsInputDisplayValue(oreGenRegenDelayInput, String.valueOf(configuredRegenDelay));
        boolean oreChanceDirty = isSettingsInputDirty(oreGenOreChanceInput, formatDouble(configuredOreChance));
        boolean regenDelayDirty = isSettingsInputDirty(oreGenRegenDelayInput, String.valueOf(configuredRegenDelay));

        commandBuilder.set("#OreGenSectionTitle.Text", uiText(language, "ui.admin_dashboard.ore_generator.title"));
        commandBuilder.set("#OreGenSectionDescription.Text", uiText(language, "ui.admin_dashboard.ore_generator.description"));
        commandBuilder.set("#OreGenOreChanceLabel.Text", uiText(language, "ui.admin_dashboard.ore_generator.ore_chance"));
        commandBuilder.set("#OreGenOreChanceField.Value", oreChanceDisplay);
        commandBuilder.set("#OreGenOreChanceFieldBorder.Background", oreChanceDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#OreGenOreChanceApplyBox.Visible", oreGenOreChanceDirtyShown);
        commandBuilder.set("#OreGenRegenDelayLabel.Text", uiText(language, "ui.admin_dashboard.ore_generator.regen_delay"));
        commandBuilder.set("#OreGenRegenDelayField.Value", regenDelayDisplay);
        commandBuilder.set("#OreGenRegenDelayFieldBorder.Background", regenDelayDirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
        commandBuilder.set("#OreGenRegenDelayApplyBox.Visible", oreGenRegenDelayDirtyShown);
        commandBuilder.set("#OreGenSpawnRubbleToggle.Value", oreGenSpawnRubble);
        commandBuilder.set("#OreGenSpawnRubbleRow.Background", oreGenSpawnRubble ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
        commandBuilder.set("#OreGenSpawnRubbleLabel.Text", uiText(language, "ui.admin_dashboard.ore_generator.spawn_rubble"));
        commandBuilder.set("#OreGenOresSectionTitle.Text", uiText(language, "ui.admin_dashboard.ore_generator.ores_title"));
        commandBuilder.set("#OreGenCreateBlockIdField.PlaceholderText", uiText(language, "ui.admin_dashboard.ore_generator.create_block_id_placeholder"));
        commandBuilder.set("#OreGenCreateWeightField.PlaceholderText", uiText(language, "ui.admin_dashboard.ore_generator.create_weight_placeholder"));
        commandBuilder.set("#OreGenCreateBlockIdField.Value", oreGenCreateBlockIdInput == null ? "" : oreGenCreateBlockIdInput);
        commandBuilder.set("#OreGenCreateWeightField.Value", oreGenCreateWeightInput == null ? "" : oreGenCreateWeightInput);
        commandBuilder.set("#OreGenCreateButton.Text", uiText(language, "ui.common.create"));
        commandBuilder.set("#OreGenSearchField.PlaceholderText", uiText(language, "ui.admin_dashboard.ore_generator.search_placeholder"));
        commandBuilder.set("#OreGenSearchField.Value", oreGenSearchInput == null ? "" : oreGenSearchInput);
        commandBuilder.set("#OreGenSearchButton.Text", uiText(language, "ui.admin_dashboard.ore_generator.search_button"));
        commandBuilder.set("#OreGenSectionError.Visible", oreGenError != null && !oreGenError.isBlank());
        commandBuilder.set("#OreGenSectionError.Text", oreGenError == null ? "" : oreGenError);
        buildOreGenRows(commandBuilder, oreGenSearchQuery);

        commandBuilder.set("#OreGenTiersSectionTitle.Text", uiText(language, "ui.admin_dashboard.ore_generator.tiers_title"));
        commandBuilder.set("#OreGenTiersSectionDescription.Text", uiText(language, "ui.admin_dashboard.ore_generator.tiers_description"));
        commandBuilder.set("#OreGenTierCreateNameField.PlaceholderText", uiText(language, "ui.admin_dashboard.ore_generator.tier_name_placeholder"));
        commandBuilder.set("#OreGenTierCreateChanceField.PlaceholderText", uiText(language, "ui.admin_dashboard.ore_generator.tier_ore_chance_placeholder"));
        commandBuilder.set("#OreGenTierCreateNameField.Value", oreGenTierCreateNameInput == null ? "" : oreGenTierCreateNameInput);
        String generatedCreateTierPerm = buildOreGenTierPermission(oreGenTierCreateNameInput);
        commandBuilder.set(
            "#OreGenTierCreatePermText.Text",
            generatedCreateTierPerm.isBlank()
                ? uiText(language, "ui.admin_dashboard.ore_generator.tier_permission_placeholder")
                : generatedCreateTierPerm
        );
        commandBuilder.set("#OreGenTierCreateChanceField.Value", oreGenTierCreateChanceInput == null ? "" : oreGenTierCreateChanceInput);
        commandBuilder.set("#OreGenTierCreateButton.Text", uiText(language, "ui.common.create"));
        commandBuilder.set("#OreGenTierSectionError.Visible", oreGenTierError != null && !oreGenTierError.isBlank());
        commandBuilder.set("#OreGenTierSectionError.Text", oreGenTierError == null ? "" : oreGenTierError);
        buildOreGenTierRows(commandBuilder);

        commandBuilder.set("#ZoneBox.Visible", showTemplatesPanel && !createSuccess);
        commandBuilder.set("#ZoneStatusGreen.Visible", zoneStatus == ZoneStatus.VALID);
        commandBuilder.set("#ZoneStatusGreen.Text", zoneStatus == ZoneStatus.VALID ? zoneStatusText(zoneStatus, language) : "");
        commandBuilder.set("#ZoneStatusDefault.Visible", zoneStatus != ZoneStatus.VALID);
        commandBuilder.set("#ZoneStatusDefaultLabel.Text",
            zoneStatus != ZoneStatus.VALID ? zoneStatusText(zoneStatus, language) : "");
        commandBuilder.set("#CreateIslandButton.Visible", zoneStatus == ZoneStatus.VALID);
        commandBuilder.set("#CreateIslandButton.Text", uiText(language, "ui.admin_dashboard.create_button"));

        commandBuilder.set("#CreateTemplateForm.Visible", showCreateForm);
        commandBuilder.set("#CreateSuccess.Text", uiText(language, "ui.admin_dashboard.create_success"));
        commandBuilder.set("#CreateTemplateFormTitle.Text", uiText(language, "ui.admin_dashboard.create_title"));
        commandBuilder.set("#CreateTemplateFormIntro.Text", uiText(language, "ui.admin_dashboard.create_intro"));
        commandBuilder.set("#CreateTemplateFormNameLabel.Text", uiText(language, "ui.admin_dashboard.field_island_name"));
        commandBuilder.set("#CreateTemplateFormDescriptionLabel.Text", uiText(language, "ui.admin_dashboard.field_description"));
        commandBuilder.set("#CreateTemplateFormIdLabel.Text", uiText(language, "ui.admin_dashboard.field_template_id"));
        commandBuilder.set("#CancelCreateTemplateFormButton.Text", uiText(language, "ui.common.cancel"));
        commandBuilder.set("#FinishCreateTemplateFormButton.Text", uiText(language, "ui.common.finish"));
        commandBuilder.set("#CreateTemplateNameField.Value", formName);
        commandBuilder.set("#CreateTemplateDescriptionField.Value", formDescription);
        commandBuilder.set("#CreateTemplateIdField.Value", formId);

        boolean hasError = formError != null && !formError.isBlank();
        commandBuilder.set("#CreateTemplateFormError.Visible", hasError);
        commandBuilder.set("#CreateTemplateFormError.Text", hasError ? formError : "");

        String hintId = (formId != null && !formId.isBlank()) ? formId : "{id}";
        commandBuilder.set("#CreateTemplateImageHint.Text",
            uiText(language, "ui.admin_dashboard.image_hint", Map.of(
                "path", "/island_templates/" + hintId + "/" + hintId + ".png"
            )));

        commandBuilder.set("#EditTemplateForm.Visible", showEditTemplateForm);
        commandBuilder.set("#EditTemplateFormTitle.Text", translateTemplates
            ? uiText(language, "ui.admin_dashboard.edit_translations_title")
            : uiText(language, "ui.admin_dashboard.edit_title"));
        commandBuilder.set("#EditTemplateFormIntro.Text", uiText(language, "ui.admin_dashboard.edit_intro"));
        commandBuilder.set("#EditTemplateFormNameLabel.Text", uiText(language, "ui.admin_dashboard.field_name"));
        commandBuilder.set("#EditTemplateFormDescriptionLabel.Text", uiText(language, "ui.admin_dashboard.field_description"));
        commandBuilder.set("#CancelEditTemplateFormButton.Text", uiText(language, "ui.common.cancel"));
        commandBuilder.set("#SaveEditTemplateFormButton.Text", uiText(language, "ui.common.save"));
        String editHintId = (editTemplateId != null && !editTemplateId.isBlank()) ? editTemplateId : "{id}";
        commandBuilder.set("#EditTemplateIdInline.Visible", translateTemplates && editTemplateId != null && !editTemplateId.isBlank());
        commandBuilder.set("#EditTemplateIdInline.Text", (editTemplateId == null || editTemplateId.isBlank()) ? "" : "(ID: " + editTemplateId + ")");
        commandBuilder.set("#EditTemplateNameField.Value", editName);
        commandBuilder.set("#EditTemplateDescriptionField.Value", editDescription);
        commandBuilder.set("#EditTemplateLanguageTabs.Visible", translateTemplates && !editLanguages.isEmpty());
        commandBuilder.set("#EditTemplateLanguageHint.Visible", translateTemplates);
        commandBuilder.set("#EditTemplateLanguageHint.Text",
            uiText(language, "ui.admin_dashboard.template_image_hint", Map.of(
                "path", "/island_templates/" + editHintId + "/" + editHintId + ".png"
            )));

        boolean hasEditError = editError != null && !editError.isBlank();
        commandBuilder.set("#EditTemplateFormError.Visible", hasEditError);
        commandBuilder.set("#EditTemplateFormError.Text", hasEditError ? editError : "");

        commandBuilder.set("#DeleteTemplateConfirmForm.Visible", showDeleteConfirm);
        commandBuilder.set("#DeleteTemplateConfirmTitle.Text", uiText(language, "ui.admin_dashboard.delete_title"));
        commandBuilder.set("#DeleteTemplateConfirmDescription.Text", uiText(language, "ui.admin_dashboard.delete_intro"));
        commandBuilder.set("#CancelDeleteTemplateButton.Text", uiText(language, "ui.common.cancel"));
        commandBuilder.set("#ConfirmDeleteTemplateButton.Text", uiText(language, "ui.common.delete"));
        commandBuilder.set("#DeleteTemplateConfirmTemplateText.Text",
            deleteTemplateId == null || deleteTemplateId.isBlank() ? "" : uiText(language, "ui.admin_dashboard.delete_template", Map.of("id", deleteTemplateId)));
        boolean hasDeleteError = deleteError != null && !deleteError.isBlank();
        commandBuilder.set("#DeleteTemplateConfirmError.Visible", hasDeleteError);
        commandBuilder.set("#DeleteTemplateConfirmError.Text", hasDeleteError ? deleteError : "");

        for (int i = 0; i < MAX_LANGUAGE_BUTTONS; i++) {
            if (i < editLanguages.size()) {
                String locale = editLanguages.get(i);
                boolean selected = locale.equalsIgnoreCase(editLanguage);
                String label = plugin.getLanguageService().langLabel(locale);
                commandBuilder.set("#EditLang" + i + ".Visible", !selected);
                commandBuilder.set("#EditLang" + i + ".Text", label);
                commandBuilder.set("#EditLangActive" + i + ".Visible", selected);
                commandBuilder.set("#EditLangActive" + i + ".Text", label);
            } else {
                commandBuilder.set("#EditLang" + i + ".Visible", false);
                commandBuilder.set("#EditLangActive" + i + ".Visible", false);
            }
        }

        commandBuilder.set("#CreateSuccess.Visible", showTemplatesPanel && createSuccess);

        buildTemplateRows(commandBuilder, language, fallbackLanguage, translateTemplates);
        buildAddonRows(commandBuilder);
        bindEvents(eventBuilder, editLanguages);
    }

    private void buildTemplateRows(UICommandBuilder commandBuilder,
                                   String language,
                                   String fallbackLanguage,
                                   boolean translateTemplates) {
        templateSlots = new ArrayList<>();

        List<IslandTemplate> loaded = plugin.getIslandTemplateManager().getAll();
        Map<String, String> failedMap = plugin.getIslandTemplateManager()
            .getLastLoadReport().getFailedTemplates();

        for (int i = 0; i < loaded.size(); i++) {
            IslandTemplate t = loaded.get(i);
            String name = resolveTitle(t, translateTemplates, language, fallbackLanguage);
            templateSlots.add(new TemplateSlot(
                t.getId(),
                name,
                t.isEnabled(),
                false,
                !"default".equals(t.getId()),
                i > 0,
                i < loaded.size() - 1
            ));
        }
        for (String failedId : failedMap.keySet()) {
            boolean alreadyShown = loaded.stream().anyMatch(t -> t.getId().equals(failedId));
            if (!alreadyShown) {
                templateSlots.add(new TemplateSlot(failedId, failedId, false, true, false, false, false));
            }
        }

        for (int i = 0; i < MAX_TEMPLATE_ROWS; i++) {
            if (i < templateSlots.size()) {
                TemplateSlot slot = templateSlots.get(i);
                commandBuilder.set("#TplRow" + i + ".Visible", true);
                commandBuilder.set("#TplRow" + i + ".Background",
                    slot.hasError ? TEMPLATE_BG_ERROR : (slot.enabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED));
                commandBuilder.set("#TplName" + i + ".Text", slot.displayName);
                commandBuilder.set("#TplSubId" + i + ".Text", slot.id);
                commandBuilder.set("#TplEnabled" + i + ".Visible", !slot.hasError);
                commandBuilder.set("#TplEnabled" + i + ".Value", slot.enabled);
                commandBuilder.set("#TplUpBox" + i + ".Visible", !slot.hasError && slot.canMoveUp);
                commandBuilder.set("#TplDownBox" + i + ".Visible", !slot.hasError && slot.canMoveDown);
                commandBuilder.set("#TplEditBox" + i + ".Visible", !slot.hasError);
                commandBuilder.set("#TplDeleteBox" + i + ".Visible", !slot.hasError && slot.deletable);
                commandBuilder.set("#TplUp" + i + ".Visible", !slot.hasError && slot.canMoveUp);
                commandBuilder.set("#TplDown" + i + ".Visible", !slot.hasError && slot.canMoveDown);
                commandBuilder.set("#TplEdit" + i + ".Visible", !slot.hasError);
                commandBuilder.set("#TplDelete" + i + ".Visible", !slot.hasError && slot.deletable);
                commandBuilder.set("#TplError" + i + ".Visible", slot.hasError);
                commandBuilder.set("#TplError" + i + ".Text", slot.hasError ? uiText(language, "ui.common.error") : "");
            } else {
                commandBuilder.set("#TplRow" + i + ".Visible", false);
            }
        }
    }

    private void buildAddonRows(UICommandBuilder commandBuilder) {
        addonSlots = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : plugin.getAddonManager().getAddonStates().entrySet()) {
            addonSlots.add(new AddonSlot(
                entry.getKey(),
                resolveAddonDisplayName(entry.getKey()),
                Boolean.TRUE.equals(entry.getValue())
            ));
        }

        for (int i = 0; i < MAX_ADDON_ROWS; i++) {
            if (i < addonSlots.size()) {
                AddonSlot slot = addonSlots.get(i);
                commandBuilder.set("#AddonRow" + i + ".Visible", true);
                commandBuilder.set("#AddonRow" + i + ".Background", slot.enabled ? TEMPLATE_BG_ENABLED : TEMPLATE_BG_DISABLED);
                commandBuilder.set("#AddonName" + i + ".Text", slot.displayName);
                commandBuilder.set("#AddonSubId" + i + ".Text", slot.id);
                commandBuilder.set("#AddonEnabled" + i + ".Visible", true);
                commandBuilder.set("#AddonEnabled" + i + ".Value", slot.enabled);
            } else {
                commandBuilder.set("#AddonRow" + i + ".Visible", false);
            }
        }
    }

    private void buildResetRows(UICommandBuilder commandBuilder, String searchQuery) {
        List<ResetPlayerSlot> allKnownPlayers = buildAllResetPlayers();
        String normalizedSearch = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);

        if (normalizedSearch.isBlank()) {
            resetPlayerSlots = allKnownPlayers.subList(0, Math.min(MAX_RESETS_ROWS, allKnownPlayers.size()));
        } else {
            List<ResetPlayerSlot> filtered = new ArrayList<>();
            for (ResetPlayerSlot slot : allKnownPlayers) {
                if (slot.playerName != null && slot.playerName.toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
                    filtered.add(slot);
                }
            }
            resetPlayerSlots = filtered.subList(0, Math.min(MAX_RESETS_ROWS, filtered.size()));
        }

        for (int i = 0; i < MAX_RESETS_ROWS; i++) {
            if (i < resetPlayerSlots.size()) {
                ResetPlayerSlot slot = resetPlayerSlots.get(i);
                String draft = resetPlayerDraftInputs.get(slot.uuid);
                String displayValue = getSettingsInputDisplayValue(draft, slot.currentResets);
                boolean dirty = isSettingsInputDirty(draft, slot.currentResets);
                commandBuilder.set("#ResetsPlayerRow" + i + ".Visible", true);
                commandBuilder.set("#ResetsPlayerName" + i + ".Text", slot.playerName);
                commandBuilder.set("#ResetsPlayerValueField" + i + ".Value", displayValue);
                commandBuilder.set("#ResetsPlayerValueFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#ResetsPlayerValueApplyBox" + i + ".Visible", dirty);
            } else {
                commandBuilder.set("#ResetsPlayerRow" + i + ".Visible", false);
            }
        }
    }

    private List<ResetPlayerSlot> buildAllResetPlayers() {
        List<ResetPlayerSlot> players = new ArrayList<>();
        List<Map.Entry<UUID, String>> ordered = new ArrayList<>(plugin.getKnownPlayerRepository().all().entrySet());
        java.util.Collections.reverse(ordered);

        for (Map.Entry<UUID, String> entry : ordered) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();
            if (uuid == null || name == null || name.isBlank()) {
                continue;
            }
            players.add(new ResetPlayerSlot(uuid, name, Math.max(0, plugin.getIslandService().getRemainingResets(uuid))));
        }
        return players;
    }

    private void buildHomesRows(UICommandBuilder commandBuilder, String searchQuery) {
        List<HomePlayerSlot> allKnownPlayers = buildAllHomePlayers();
        String normalizedSearch = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);

        if (normalizedSearch.isBlank()) {
            homePlayerSlots = allKnownPlayers.subList(0, Math.min(MAX_HOMES_ROWS, allKnownPlayers.size()));
        } else {
            List<HomePlayerSlot> filtered = new ArrayList<>();
            for (HomePlayerSlot slot : allKnownPlayers) {
                if (slot.playerName != null && slot.playerName.toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
                    filtered.add(slot);
                }
            }
            homePlayerSlots = filtered.subList(0, Math.min(MAX_HOMES_ROWS, filtered.size()));
        }

        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            if (i < homePlayerSlots.size()) {
                HomePlayerSlot slot = homePlayerSlots.get(i);
                String draft = homePlayerDraftInputs.get(slot.uuid);
                String displayValue = getSettingsInputDisplayValue(draft, slot.currentMaxHomes);
                boolean dirty = isSettingsInputDirty(draft, slot.currentMaxHomes);
                commandBuilder.set("#HomesPlayerRow" + i + ".Visible", true);
                commandBuilder.set("#HomesPlayerName" + i + ".Text", slot.playerName);
                commandBuilder.set("#HomesPlayerValueField" + i + ".Value", displayValue);
                commandBuilder.set("#HomesPlayerValueFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#HomesPlayerValueApplyBox" + i + ".Visible", dirty);
                commandBuilder.set("#HomesPlayerView" + i + ".Text", uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.homes.view_homes"));
            } else {
                commandBuilder.set("#HomesPlayerRow" + i + ".Visible", false);
            }
        }
    }

    private void buildIslandsRows(UICommandBuilder commandBuilder, String searchQuery) {
        List<IslandPlayerSlot> allKnownPlayers = buildAllIslandPlayers();
        String normalizedSearch = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);

        if (normalizedSearch.isBlank()) {
            islandPlayerSlots = allKnownPlayers.subList(0, Math.min(MAX_ISLAND_ROWS, allKnownPlayers.size()));
        } else {
            List<IslandPlayerSlot> filtered = new ArrayList<>();
            for (IslandPlayerSlot slot : allKnownPlayers) {
                String playerName = slot.playerName == null ? "" : slot.playerName.toLowerCase(Locale.ROOT);
                String islandName = slot.islandDisplayName == null ? "" : slot.islandDisplayName.toLowerCase(Locale.ROOT);
                if (playerName.contains(normalizedSearch) || islandName.contains(normalizedSearch)) {
                    filtered.add(slot);
                }
            }
            islandPlayerSlots = filtered.subList(0, Math.min(MAX_ISLAND_ROWS, filtered.size()));
        }

        for (int i = 0; i < MAX_ISLAND_ROWS; i++) {
            if (i < islandPlayerSlots.size()) {
                IslandPlayerSlot slot = islandPlayerSlots.get(i);
                commandBuilder.set("#IslandsPlayerRow" + i + ".Visible", true);
                commandBuilder.set("#IslandsPlayerName" + i + ".Text", slot.playerName);
                commandBuilder.set("#IslandsPlayerIsland" + i + ".Text", slot.islandDisplayName);
                commandBuilder.set("#IslandsPlayerView" + i + ".Text", uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.islands.view_island"));
            } else {
                commandBuilder.set("#IslandsPlayerRow" + i + ".Visible", false);
            }
        }
    }

    private void buildLevelsRows(UICommandBuilder commandBuilder, String searchQuery) {
        Map<String, Double> configured = plugin.getAddonManager().getLevelsAddon().getIslandValueService().getConfiguredBlockValuesSnapshot();
        levelBlockSlots = new ArrayList<>();
        String normalizedSearch = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Double> entry : configured.entrySet()) {
            if (levelBlockSlots.size() >= MAX_LEVELS_ROWS) {
                break;
            }
            String blockId = entry.getKey();
            Double value = entry.getValue();
            if (blockId == null || blockId.isBlank() || value == null) {
                continue;
            }
            if (!normalizedSearch.isBlank() && !blockId.toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
                continue;
            }
            levelBlockSlots.add(new LevelBlockSlot(blockId, value));
        }

        for (int i = 0; i < MAX_LEVELS_ROWS; i++) {
            if (i < levelBlockSlots.size()) {
                LevelBlockSlot slot = levelBlockSlots.get(i);
                String idDraft = levelBlockIdDraftInputs.get(slot.blockId);
                String valueDraft = levelBlockValueDraftInputs.get(slot.blockId);
                String idDisplay = (idDraft == null || idDraft.isBlank()) ? slot.blockId : idDraft;
                String valueDisplay = (valueDraft == null || valueDraft.isBlank()) ? formatDouble(slot.blockValue) : valueDraft;
                boolean dirty = (idDraft != null && !idDraft.isBlank() && !idDraft.trim().equals(slot.blockId))
                    || (valueDraft != null && !valueDraft.isBlank() && !valueDraft.trim().equals(formatDouble(slot.blockValue)));

                commandBuilder.set("#LevelsBlockRow" + i + ".Visible", true);
                commandBuilder.set("#LevelsBlockIdField" + i + ".Value", idDisplay);
                commandBuilder.set("#LevelsBlockValueField" + i + ".Value", valueDisplay);
                commandBuilder.set("#LevelsBlockIdFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#LevelsBlockValueFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#LevelsBlockApplyBox" + i + ".Visible", dirty);
                commandBuilder.set("#LevelsBlockDelete" + i + ".Text", uiText(getAdminDashboardLanguage(), "ui.common.delete"));
            } else {
                commandBuilder.set("#LevelsBlockRow" + i + ".Visible", false);
            }
        }
    }

    private List<HomePlayerSlot> buildAllHomePlayers() {
        List<HomePlayerSlot> players = new ArrayList<>();
        List<Map.Entry<UUID, String>> ordered = new ArrayList<>(plugin.getKnownPlayerRepository().all().entrySet());
        java.util.Collections.reverse(ordered);

        for (Map.Entry<UUID, String> entry : ordered) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();
            if (uuid == null || name == null || name.isBlank()) {
                continue;
            }
            players.add(new HomePlayerSlot(uuid, name, Math.max(0, plugin.getAddonManager().getHomesAddon().getService().getPlayerMaxHomes(uuid))));
        }
        return players;
    }

    private List<IslandPlayerSlot> buildAllIslandPlayers() {
        List<IslandPlayerSlot> players = new ArrayList<>();
        List<Map.Entry<UUID, String>> ordered = new ArrayList<>(plugin.getKnownPlayerRepository().all().entrySet());
        java.util.Collections.reverse(ordered);

        for (Map.Entry<UUID, String> entry : ordered) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();
            if (uuid == null || name == null || name.isBlank()) {
                continue;
            }

            IslandData island = plugin.getIslandService().getPlayerIsland(uuid);
            if (island == null) {
                continue;
            }

            players.add(new IslandPlayerSlot(uuid, name, plugin.getIslandService().getIslandDisplayName(island)));
        }
        return players;
    }

    private String resolveAddonDisplayName(String addonKey) {
        return switch (addonKey) {
            case AddonManager.LEVELS_ADDON_KEY -> "Levels";
            case AddonManager.MOBS_MANAGER_ADDON_KEY -> "Mobs Manager";
            case AddonManager.WARPS_ADDON_KEY -> "Warps";
            case AddonManager.HOMES_ADDON_KEY -> "Homes";
            case AddonManager.ISLAND_WALLS_ADDON_KEY -> "Island Walls";
            case AddonManager.ORE_GENERATOR_ADDON_KEY -> "Ore Generator";
            default -> addonKey == null ? "" : addonKey;
        };
    }

    private String resolveTitle(IslandTemplate template,
                                boolean translateTemplates,
                                String language,
                                String fallbackLanguage) {
        if (template == null) return "";
        if (!translateTemplates && template.getStaticTitle() != null && !template.getStaticTitle().isBlank()) {
            return template.getStaticTitle();
        }
        if (template.getTitleTranslations() != null) {
            String translated = template.getTitleTranslations().get(language);
            if (translated != null && !translated.isBlank()) return translated;
            translated = template.getTitleTranslations().get(fallbackLanguage);
            if (translated != null && !translated.isBlank()) return translated;
        }
        String key = template.getTitleKey();
        return translateTemplates
            ? plugin.getMessageManager().get(key, language)
            : key;
    }

    private void bindEvents(UIEventBuilder eventBuilder, List<String> editLanguages) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Back", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavTemplates",
            EventData.of("SwitchToTemplatesTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavTemplatesActive",
            EventData.of("SwitchToTemplatesTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavAddons",
            EventData.of("SwitchToAddonsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavAddonsActive",
            EventData.of("SwitchToAddonsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavSettings",
            EventData.of("SwitchToSettingsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavSettingsActive",
            EventData.of("SwitchToSettingsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavMobsManager",
            EventData.of("SwitchToMobsManagerTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavMobsManagerActive",
            EventData.of("SwitchToMobsManagerTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavResets",
            EventData.of("SwitchToResetsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavResetsActive",
            EventData.of("SwitchToResetsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavHomes",
            EventData.of("SwitchToHomesTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavHomesActive",
            EventData.of("SwitchToHomesTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavIslands",
            EventData.of("SwitchToIslandsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavIslandsActive",
            EventData.of("SwitchToIslandsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavLevels",
            EventData.of("SwitchToLevelsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavLevelsActive",
            EventData.of("SwitchToLevelsTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavOreGenerator",
            EventData.of("SwitchToOreGeneratorTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SidebarNavOreGeneratorActive",
            EventData.of("SwitchToOreGeneratorTab", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateIslandButton",
            EventData.of("OpenCreateForm", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#FinishCreateTemplateFormButton",
            EventData.of("Finish", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelCreateTemplateFormButton",
            EventData.of("CancelForm", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveEditTemplateFormButton",
            EventData.of("SaveEditTemplateForm", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelEditTemplateFormButton",
            EventData.of("CancelEditTemplateForm", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmDeleteTemplateButton",
            EventData.of("ConfirmDelete", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelDeleteTemplateButton",
            EventData.of("CancelDelete", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshTemplates",
            EventData.of("RefreshTemplates", "true"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CreateTemplateNameField",
            EventData.of("@FormName", "#CreateTemplateNameField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CreateTemplateDescriptionField",
            EventData.of("@FormDescription", "#CreateTemplateDescriptionField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CreateTemplateIdField",
            EventData.of("@FormId", "#CreateTemplateIdField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditTemplateNameField",
            EventData.of("@EditName", "#EditTemplateNameField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditTemplateDescriptionField",
            EventData.of("@EditDescription", "#EditTemplateDescriptionField.Value"), false);

        for (int i = 0; i < MAX_TEMPLATE_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TplEnabled" + i,
                EventData.of("ToggleTemplate", String.valueOf(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TplUp" + i,
                EventData.of("MoveTemplateUp", String.valueOf(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TplDown" + i,
                EventData.of("MoveTemplateDown", String.valueOf(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TplEdit" + i,
                EventData.of("EditTemplate", String.valueOf(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TplDelete" + i,
                EventData.of("DeleteTemplate", String.valueOf(i)), false);
        }

        for (int i = 0; i < MAX_ADDON_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AddonEnabled" + i,
                EventData.of("ToggleAddon", String.valueOf(i)), false);
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsSpawnProtectionToggle",
            EventData.of("ToggleSpawnProtection", "#SettingsSpawnProtectionToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsVoidKillItemsToggle",
            EventData.of("ToggleVoidKillItems", "#SettingsVoidKillItemsToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsVoidKillEntitiesToggle",
            EventData.of("ToggleVoidKillEntities", "#SettingsVoidKillEntitiesToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsVoidTeleportToggle",
            EventData.of("ToggleVoidTeleport", "#SettingsVoidTeleportToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsPlaceSpawnButton",
            EventData.of("PlaceSpawn", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsProtectionRadiusField",
            EventData.of("@SettingsProtectionRadius", "#SettingsProtectionRadiusField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsTemplatesYField",
            EventData.of("@SettingsTemplatesY", "#SettingsTemplatesYField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsProtectionRadiusApply",
            EventData.of("ApplyProtectionRadius", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsTemplatesYApply",
            EventData.of("ApplyTemplatesY", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsDefaultLangDropdown",
            EventData.of("@SelectDefaultLanguage", "#SettingsDefaultLangDropdown.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsFallbackLangDropdown",
            EventData.of("@SelectFallbackLanguage", "#SettingsFallbackLangDropdown.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsAutoLangMatchToggle",
            EventData.of("ToggleAutoLangMatch", "#SettingsAutoLangMatchToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SettingsTranslateTemplatesToggle",
            EventData.of("ToggleTranslateTemplates", "#SettingsTranslateTemplatesToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsManageLangs",
            EventData.of("OpenLanguagesManager", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MobsReduceMobsToggle",
            EventData.of("ToggleMobsReduceMobs", "#MobsReduceMobsToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MobsReduceRateField",
            EventData.of("@MobsReduceRate", "#MobsReduceRateField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MobsTorchProtectionToggle",
            EventData.of("ToggleMobsTorchProtection", "#MobsTorchProtectionToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MobsTorchRadiusField",
            EventData.of("@MobsTorchRadius", "#MobsTorchRadiusField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MobsTorchRadiusApply",
            EventData.of("ApplyMobsTorchRadius", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ResetsMaxResetsField",
            EventData.of("@ResetsMaxResets", "#ResetsMaxResetsField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ResetsMaxResetsApply",
            EventData.of("ApplyMaxResets", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ResetsSearchField",
            EventData.of("@ResetsSearchInput", "#ResetsSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ResetsSearchButton",
            EventData.of("ApplyResetsSearch", "true"), false);
        for (int i = 0; i < MAX_RESETS_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ResetsPlayerValueField" + i,
                EventData.of("@ResetsPlayerValue" + i, "#ResetsPlayerValueField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ResetsPlayerValueApply" + i,
                EventData.of("ApplyResetsPlayerValue" + i, "true"), false);
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HomesMaxHomesField",
            EventData.of("@HomesMaxHomes", "#HomesMaxHomesField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesMaxHomesApply",
            EventData.of("ApplyMaxHomes", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HomesSearchField",
            EventData.of("@HomesSearchInput", "#HomesSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesSearchButton",
            EventData.of("ApplyHomesSearch", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IslandsSearchField",
            EventData.of("@IslandsSearchInput", "#IslandsSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsSearchButton",
            EventData.of("ApplyIslandsSearch", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesManageGroupsButton",
            EventData.of("OpenHomesGroupsManager", "true"), false);
        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#HomesPlayerValueField" + i,
                EventData.of("@HomesPlayerValue" + i, "#HomesPlayerValueField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesPlayerValueApply" + i,
                EventData.of("ApplyHomesPlayerValue" + i, "true"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomesPlayerView" + i,
                EventData.of("ViewHomesForPlayer" + i, "true"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IslandsPlayerView" + i,
                EventData.of("ViewIslandForPlayer" + i, "true"), false);
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsLevelValueField",
            EventData.of("@LevelsLevelValue", "#LevelsLevelValueField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelsLevelValueApply",
            EventData.of("ApplyLevelsLevelValue", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsDefaultBlockValueField",
            EventData.of("@LevelsDefaultBlockValue", "#LevelsDefaultBlockValueField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelsDefaultBlockValueApply",
            EventData.of("ApplyLevelsDefaultBlockValue", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsTemplateValueToggle",
            EventData.of("ToggleLevelsTemplateValue", "#LevelsTemplateValueToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsCreateBlockIdField",
            EventData.of("@LevelsCreateBlockId", "#LevelsCreateBlockIdField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsCreateBlockValueField",
            EventData.of("@LevelsCreateBlockValue", "#LevelsCreateBlockValueField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelsCreateBlockButton",
            EventData.of("CreateLevelsBlockValue", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsSearchField",
            EventData.of("@LevelsSearchInput", "#LevelsSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelsSearchButton",
            EventData.of("ApplyLevelsSearch", "true"), false);

        for (int i = 0; i < MAX_LEVELS_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsBlockIdField" + i,
                EventData.of("@LevelsBlockId" + i, "#LevelsBlockIdField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LevelsBlockValueField" + i,
                EventData.of("@LevelsBlockValue" + i, "#LevelsBlockValueField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelsBlockApply" + i,
                EventData.of("ApplyLevelsBlock" + i, "true"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelsBlockDelete" + i,
                EventData.of("DeleteLevelsBlock" + i, "true"), false);
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenOreChanceField",
            EventData.of("@OreGenOreChance", "#OreGenOreChanceField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenOreChanceApply",
            EventData.of("ApplyOreGenOreChance", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenRegenDelayField",
            EventData.of("@OreGenRegenDelay", "#OreGenRegenDelayField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenRegenDelayApply",
            EventData.of("ApplyOreGenRegenDelay", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenSpawnRubbleToggle",
            EventData.of("ToggleOreGenSpawnRubble", "#OreGenSpawnRubbleToggle.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenCreateBlockIdField",
            EventData.of("@OreGenCreateBlockId", "#OreGenCreateBlockIdField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenCreateWeightField",
            EventData.of("@OreGenCreateWeight", "#OreGenCreateWeightField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenCreateButton",
            EventData.of("CreateOreGenOre", "true"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenSearchField",
            EventData.of("@OreGenSearchInput", "#OreGenSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenSearchButton",
            EventData.of("ApplyOreGenSearch", "true"), false);

        for (int i = 0; i < MAX_ORE_GEN_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenBlockIdField" + i,
                EventData.of("@OreGenBlockId" + i, "#OreGenBlockIdField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenWeightField" + i,
                EventData.of("@OreGenWeight" + i, "#OreGenWeightField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenOreApply" + i,
                EventData.of("ApplyOreGenOre" + i, "true"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenOreDelete" + i,
                EventData.of("DeleteOreGenOre" + i, "true"), false);
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenTierCreateNameField",
            EventData.of("@OreGenTierCreateName", "#OreGenTierCreateNameField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenTierCreateChanceField",
            EventData.of("@OreGenTierCreateChance", "#OreGenTierCreateChanceField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenTierCreateButton",
            EventData.of("CreateOreGenTier", "true"), false);

        for (int i = 0; i < MAX_TIER_ROWS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenTierNameField" + i,
                EventData.of("@OreGenTierName" + i, "#OreGenTierNameField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#OreGenTierChanceField" + i,
                EventData.of("@OreGenTierChance" + i, "#OreGenTierChanceField" + i + ".Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenTierApply" + i,
                EventData.of("ApplyOreGenTier" + i, "true"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OreGenTierDelete" + i,
                EventData.of("DeleteOreGenTier" + i, "true"), false);
        }

        for (int i = 0; i < MAX_LANGUAGE_BUTTONS && i < editLanguages.size(); i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EditLang" + i,
                EventData.of("SelectEditLanguage", editLanguages.get(i)), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EditLangActive" + i,
                EventData.of("SelectEditLanguage", editLanguages.get(i)), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull PageData data) {

        if (data.formName != null) {
            this.formName = data.formName.trim();
            return;
        }
        if (data.formDescription != null) {
            this.formDescription = data.formDescription.trim();
            return;
        }
        if (data.formId != null) {
            this.formId = data.formId.trim();
            return;
        }
        if (data.editName != null) {
            this.editName = data.editName.trim();
            return;
        }
        if (data.editDescription != null) {
            this.editDescription = data.editDescription.trim();
            return;
        }
        if (data.settingsProtectionRadius != null) {
            this.settingsProtectionRadiusInput = data.settingsProtectionRadius.trim();
            int persisted = plugin.getConfigManager().getConfig().getSettings().getSpawnProtectionRadius();
            boolean nowDirty = isSettingsInputDirty(this.settingsProtectionRadiusInput, persisted);
            if (nowDirty != settingsProtectionRadiusDirtyShown) {
                settingsProtectionRadiusDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.settingsTemplatesY != null) {
            this.settingsTemplatesYInput = data.settingsTemplatesY.trim();
            int persisted = plugin.getConfigManager().getConfig().getGrid().getIslandY();
            boolean nowDirty = isSettingsInputDirty(this.settingsTemplatesYInput, persisted);
            if (nowDirty != settingsTemplatesYDirtyShown) {
                settingsTemplatesYDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.mobsReduceRate != null) {
            this.mobsReduceRateInput = String.valueOf((int) Math.round(data.mobsReduceRate));
            Player p = store.getComponent(ref, Player.getComponentType());
            if (p != null) handleApplyMobsReduceRate(ref, store, p);
            return;
        }
        if (data.mobsTorchRadius != null) {
            this.mobsTorchRadiusInput = data.mobsTorchRadius.trim();
            int persisted = plugin.getAddonManager().getMobsManagerAddon().getTorchProtectionRadius();
            boolean nowDirty = isSettingsInputDirty(this.mobsTorchRadiusInput, persisted);
            if (nowDirty != mobsTorchRadiusDirtyShown) {
                mobsTorchRadiusDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.resetsMaxResets != null) {
            this.resetsMaxResetsInput = data.resetsMaxResets.trim();
            int persisted = Math.max(0, plugin.getConfigManager().getConfig().getSettings().getMaxResets());
            boolean nowDirty = isSettingsInputDirty(this.resetsMaxResetsInput, persisted);
            if (nowDirty != resetsMaxResetsDirtyShown) {
                resetsMaxResetsDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.resetsSearchInput != null) {
            resetsSearchInput = data.resetsSearchInput.trim();
            return;
        }
        for (int i = 0; i < MAX_RESETS_ROWS; i++) {
            String rawDraft = getResetsPlayerDraftValue(data, i);
            if (rawDraft != null) {
                if (i >= 0 && i < resetPlayerSlots.size()) {
                    ResetPlayerSlot slot = resetPlayerSlots.get(i);
                    resetPlayerDraftInputs.put(slot.uuid, rawDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
        }
        if (data.homesMaxHomes != null) {
            this.homesMaxHomesInput = data.homesMaxHomes.trim();
            int persisted = Math.max(0, plugin.getAddonManager().getHomesAddon().getMaxHomes());
            boolean nowDirty = isSettingsInputDirty(this.homesMaxHomesInput, persisted);
            if (nowDirty != homesMaxHomesDirtyShown) {
                homesMaxHomesDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.homesSearchInput != null) {
            homesSearchInput = data.homesSearchInput.trim();
            return;
        }
        if (data.islandsSearchInput != null) {
            islandsSearchInput = data.islandsSearchInput.trim();
            return;
        }
        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            String rawDraft = getHomesPlayerDraftValue(data, i);
            if (rawDraft != null) {
                if (i >= 0 && i < homePlayerSlots.size()) {
                    HomePlayerSlot slot = homePlayerSlots.get(i);
                    homePlayerDraftInputs.put(slot.uuid, rawDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
        }
        if (data.levelsLevelValue != null) {
            levelsLevelValueInput = data.levelsLevelValue.trim();
            int persisted = plugin.getAddonManager().getLevelsAddon().getLevelValue();
            boolean nowDirty = isSettingsInputDirty(levelsLevelValueInput, persisted);
            if (nowDirty != levelsLevelValueDirtyShown) {
                levelsLevelValueDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.levelsDefaultBlockValue != null) {
            levelsDefaultBlockValueInput = data.levelsDefaultBlockValue.trim();
            String persisted = formatDouble(plugin.getAddonManager().getLevelsAddon().getBlockDefaultValue());
            boolean nowDirty = isSettingsInputDirty(levelsDefaultBlockValueInput, persisted);
            if (nowDirty != levelsDefaultBlockValueDirtyShown) {
                levelsDefaultBlockValueDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.levelsCreateBlockId != null) {
            levelsCreateBlockIdInput = data.levelsCreateBlockId.trim();
            return;
        }
        if (data.levelsCreateBlockValue != null) {
            levelsCreateBlockValueInput = data.levelsCreateBlockValue.trim();
            return;
        }
        if (data.levelsSearchInput != null) {
            levelsSearchInput = data.levelsSearchInput.trim();
            return;
        }
        for (int i = 0; i < MAX_LEVELS_ROWS; i++) {
            String idDraft = getLevelsBlockIdDraftValue(data, i);
            if (idDraft != null) {
                if (i >= 0 && i < levelBlockSlots.size()) {
                    LevelBlockSlot slot = levelBlockSlots.get(i);
                    levelBlockIdDraftInputs.put(slot.blockId, idDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
            String valueDraft = getLevelsBlockValueDraftValue(data, i);
            if (valueDraft != null) {
                if (i >= 0 && i < levelBlockSlots.size()) {
                    LevelBlockSlot slot = levelBlockSlots.get(i);
                    levelBlockValueDraftInputs.put(slot.blockId, valueDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
        }
        if (data.oreGenOreChance != null) {
            this.oreGenOreChanceInput = data.oreGenOreChance.trim();
            String persisted = formatDouble(plugin.getAddonManager().getOreGeneratorAddon().getOreChance());
            boolean nowDirty = isSettingsInputDirty(this.oreGenOreChanceInput, persisted);
            if (nowDirty != oreGenOreChanceDirtyShown) {
                oreGenOreChanceDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.oreGenRegenDelay != null) {
            this.oreGenRegenDelayInput = data.oreGenRegenDelay.trim();
            String persisted = String.valueOf(plugin.getAddonManager().getOreGeneratorAddon().getRegenDelayMs());
            boolean nowDirty = isSettingsInputDirty(this.oreGenRegenDelayInput, persisted);
            if (nowDirty != oreGenRegenDelayDirtyShown) {
                oreGenRegenDelayDirtyShown = nowDirty;
                Player p = store.getComponent(ref, Player.getComponentType());
                if (p != null) p.getPageManager().openCustomPage(ref, store, this);
            }
            return;
        }
        if (data.oreGenCreateBlockId != null) {
            oreGenCreateBlockIdInput = data.oreGenCreateBlockId.trim();
            return;
        }
        if (data.oreGenCreateWeight != null) {
            oreGenCreateWeightInput = data.oreGenCreateWeight.trim();
            return;
        }
        if (data.oreGenSearchInput != null) {
            oreGenSearchInput = data.oreGenSearchInput.trim();
            return;
        }
        for (int i = 0; i < MAX_ORE_GEN_ROWS; i++) {
            String idDraft = getOreGenBlockIdDraftValue(data, i);
            if (idDraft != null) {
                if (i >= 0 && i < oreSlots.size()) {
                    OreSlot slot = oreSlots.get(i);
                    oreBlockIdDraftInputs.put(slot.blockId, idDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
            String weightDraft = getOreGenWeightDraftValue(data, i);
            if (weightDraft != null) {
                if (i >= 0 && i < oreSlots.size()) {
                    OreSlot slot = oreSlots.get(i);
                    oreWeightDraftInputs.put(slot.blockId, weightDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
        }
        if (data.oreGenTierCreateName != null) {
            oreGenTierCreateNameInput = data.oreGenTierCreateName.trim();
            return;
        }
        if (data.oreGenTierCreateChance != null) {
            oreGenTierCreateChanceInput = data.oreGenTierCreateChance.trim();
            return;
        }
        // Process tier create/apply/delete BEFORE draft storage so that
        // when the UI bundles ValueChanged + Activating events together
        // the apply action is handled rather than short-circuited.
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            if (data.createOreGenTier != null) {
                handleCreateOreGenTier(ref, store, player);
                return;
            }
            for (int i = 0; i < MAX_TIER_ROWS; i++) {
                if (isApplyOreGenTierRequested(data, i)) {
                    handleApplyOreGenTier(i, ref, store, player);
                    return;
                }
                if (isDeleteOreGenTierRequested(data, i)) {
                    handleDeleteOreGenTier(i, ref, store, player);
                    return;
                }
            }
        }

        for (int i = 0; i < MAX_TIER_ROWS; i++) {
            String tierNameDraft = getOreGenTierNameDraftValue(data, i);
            if (tierNameDraft != null) {
                if (i >= 0 && i < tierSlots.size()) {
                    TierSlot slot = tierSlots.get(i);
                    tierNameDraftInputs.put(slot.name, tierNameDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
            String tierChanceDraft = getOreGenTierChanceDraftValue(data, i);
            if (tierChanceDraft != null) {
                if (i >= 0 && i < tierSlots.size()) {
                    TierSlot slot = tierSlots.get(i);
                    tierChanceDraftInputs.put(slot.name, tierChanceDraft.trim());
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                }
                return;
            }
        }

        if (player == null) return;
        if (player == null) return;

        String fallbackLanguage = plugin.getConfigManager().getConfig().getSettings().getFallbackLanguage();
        boolean translateTemplates = plugin.getConfigManager().getConfig().getSettings().isTranslateTemplates();

        if (data.back != null) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (data.switchToTemplatesTab != null && !restricted) {
            activeTab = DashboardTab.TEMPLATES;
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToAddonsTab != null && !restricted) {
            activeTab = DashboardTab.ADDONS;
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToSettingsTab != null && !restricted) {
            activeTab = DashboardTab.SETTINGS;
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToMobsManagerTab != null && !restricted) {
            if (!plugin.getAddonManager().isMobsManagerEnabled()) {
                activeTab = DashboardTab.ADDONS;
            } else {
                activeTab = DashboardTab.MOBS_MANAGER;
            }
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            mobsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToResetsTab != null) {
            activeTab = DashboardTab.RESETS;
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            mobsError = null;
            resetsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToHomesTab != null) {
            if (!plugin.getAddonManager().isHomesEnabled()) {
                activeTab = restricted ? DashboardTab.ISLANDS : DashboardTab.ADDONS;
            } else {
                activeTab = DashboardTab.HOMES;
            }
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            mobsError = null;
            resetsError = null;
            homesError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToIslandsTab != null) {
            activeTab = DashboardTab.ISLANDS;
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            mobsError = null;
            resetsError = null;
            homesError = null;
            islandsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToLevelsTab != null && !restricted) {
            if (!plugin.getAddonManager().isLevelsEnabled()) {
                activeTab = DashboardTab.ADDONS;
            } else {
                activeTab = DashboardTab.LEVELS;
            }
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            mobsError = null;
            resetsError = null;
            homesError = null;
            levelsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.switchToOreGeneratorTab != null && !restricted) {
            if (!plugin.getAddonManager().isOreGeneratorEnabled()) {
                activeTab = DashboardTab.ADDONS;
            } else {
                activeTab = DashboardTab.ORE_GENERATOR;
            }
            showCreateForm = false;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            formError = null;
            editError = null;
            deleteError = null;
            settingsError = null;
            mobsError = null;
            resetsError = null;
            homesError = null;
            levelsError = null;
            oreGenError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.openCreateForm != null) {
            activeTab = DashboardTab.TEMPLATES;
            ZoneStatus current = computeZoneStatus(store, ref);
            if (current != ZoneStatus.VALID) {
                player.getPageManager().openCustomPage(ref, store, this);
                return;
            }
            showCreateForm = true;
            showEditTemplateForm = false;
            showDeleteConfirm = false;
            createSuccess = false;
            formError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.cancelForm != null) {
            showCreateForm = false;
            formError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.cancelEditTemplateForm != null) {
            showEditTemplateForm = false;
            editError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.cancelDelete != null) {
            showDeleteConfirm = false;
            deleteTemplateId = "";
            deleteError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.refreshTemplates != null) {
            plugin.getIslandTemplateManager().reloadTemplates();
            plugin.reloadTemplateImages();
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.finish != null) {
            handleFinish(ref, store, player);
            return;
        }

        if (data.selectEditLanguage != null) {
            editLanguage = normalizeLanguageCode(data.selectEditLanguage);
            loadEditTemplateFormValues(translateTemplates, fallbackLanguage);
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.toggleTemplate != null) {
            handleToggle(data.toggleTemplate, ref, store, player);
            return;
        }

        if (data.toggleAddon != null) {
            handleToggleAddon(data.toggleAddon, ref, store, player);
            return;
        }

        if (data.toggleSpawnProtection != null) {
            handleSpawnProtectionToggle(data.toggleSpawnProtection, ref, store, player);
            return;
        }

        if (data.toggleVoidKillItems != null) {
            handleVoidKillItemsToggle(data.toggleVoidKillItems, ref, store, player);
            return;
        }

        if (data.toggleVoidKillEntities != null) {
            handleVoidKillEntitiesToggle(data.toggleVoidKillEntities, ref, store, player);
            return;
        }

        if (data.toggleVoidTeleport != null) {
            handleVoidTeleportToggle(data.toggleVoidTeleport, ref, store, player);
            return;
        }

        if (data.placeSpawn != null) {
            handlePlaceSpawn(ref, store, player);
            return;
        }

        if (data.applyProtectionRadius != null) {
            handleApplyProtectionRadius(ref, store, player);
            return;
        }

        if (data.applyTemplatesY != null) {
            handleApplyTemplatesY(ref, store, player);
            return;
        }

        if (data.selectDefaultLanguage != null) {
            if (!data.selectDefaultLanguage.isBlank()) {
                plugin.getSettingsCommandHandler().handleDefaultLanguageSetting(data.selectDefaultLanguage);
            }
            return;
        }

        if (data.selectFallbackLanguage != null) {
            if (!data.selectFallbackLanguage.isBlank()) {
                plugin.getSettingsCommandHandler().handleFallbackLanguageSetting(data.selectFallbackLanguage);
            }
            return;
        }

        if (data.toggleAutoLangMatch != null) {
            boolean current = plugin.getConfigManager().getConfig().getSettings().isAutoLangMatchEnabled();
            plugin.getSettingsCommandHandler().handleAutoLangMatchSetting(!current);
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.toggleTranslateTemplates != null) {
            boolean current = plugin.getConfigManager().getConfig().getSettings().isTranslateTemplates();
            plugin.getSettingsCommandHandler().handleTranslateTemplatesSetting(!current);
            settingsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.openLanguagesManager != null) {
            ISkyblockAdminDashboardLanguagesPage langPage = new ISkyblockAdminDashboardLanguagesPage(playerRef, sourceWorld, plugin, this);
            player.getPageManager().openCustomPage(ref, store, langPage);
            return;
        }

        if (data.openHomesGroupsManager != null) {
            ISkyblockAdminDashboardHomesGroupsPage homesGroupsPage = new ISkyblockAdminDashboardHomesGroupsPage(playerRef, sourceWorld, plugin, this);
            player.getPageManager().openCustomPage(ref, store, homesGroupsPage);
            return;
        }

        if (data.toggleMobsReduceMobs != null) {
            handleMobsReduceMobsToggle(ref, store, player);
            return;
        }



        if (data.toggleMobsTorchProtection != null) {
            handleMobsTorchProtectionToggle(ref, store, player);
            return;
        }

        if (data.applyMobsTorchRadius != null) {
            handleApplyMobsTorchRadius(ref, store, player);
            return;
        }

        if (data.applyMaxResets != null) {
            handleApplyMaxResets(ref, store, player);
            return;
        }

        if (data.applyResetsSearch != null) {
            resetsSearchQuery = resetsSearchInput == null ? "" : resetsSearchInput.trim();
            resetsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.applyMaxHomes != null) {
            handleApplyMaxHomes(ref, store, player);
            return;
        }

        if (data.applyHomesSearch != null) {
            homesSearchQuery = homesSearchInput == null ? "" : homesSearchInput.trim();
            homesError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.applyIslandsSearch != null) {
            islandsSearchQuery = islandsSearchInput == null ? "" : islandsSearchInput.trim();
            islandsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.applyLevelsLevelValue != null) {
            handleApplyLevelsLevelValue(ref, store, player);
            return;
        }

        if (data.applyLevelsDefaultBlockValue != null) {
            handleApplyLevelsDefaultBlockValue(ref, store, player);
            return;
        }

        if (data.toggleLevelsTemplateValue != null) {
            boolean current = plugin.getAddonManager().getLevelsAddon().isTemplateValueEnabled();
            plugin.getAddonManager().getLevelsAddon().handleTemplateValueSetting(!current);
            levelsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.createLevelsBlockValue != null) {
            handleCreateLevelsBlockValue(ref, store, player);
            return;
        }

        if (data.applyLevelsSearch != null) {
            levelsSearchQuery = levelsSearchInput == null ? "" : levelsSearchInput.trim();
            levelsError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.applyOreGenOreChance != null) {
            handleApplyOreGenOreChance(ref, store, player);
            return;
        }

        if (data.applyOreGenRegenDelay != null) {
            handleApplyOreGenRegenDelay(ref, store, player);
            return;
        }

        if (data.toggleOreGenSpawnRubble != null) {
            boolean current = plugin.getAddonManager().getOreGeneratorAddon().isSpawnRubble();
            plugin.getSettingsCommandHandler().handleOreGeneratorSpawnRubbleSetting(!current);
            oreGenError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (data.createOreGenOre != null) {
            handleCreateOreGenOre(ref, store, player);
            return;
        }

        if (data.applyOreGenSearch != null) {
            oreGenSearchQuery = oreGenSearchInput == null ? "" : oreGenSearchInput.trim();
            oreGenError = null;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            if (isApplyHomesPlayerValueRequested(data, i)) {
                handleApplyPlayerHomesValue(i, ref, store, player);
                return;
            }
        }

        for (int i = 0; i < MAX_LEVELS_ROWS; i++) {
            if (isApplyLevelsBlockRequested(data, i)) {
                handleApplyLevelsBlockValue(i, ref, store, player);
                return;
            }
            if (isDeleteLevelsBlockRequested(data, i)) {
                handleDeleteLevelsBlockValue(i, ref, store, player);
                return;
            }
        }

        for (int i = 0; i < MAX_ORE_GEN_ROWS; i++) {
            if (isApplyOreGenOreRequested(data, i)) {
                handleApplyOreGenOre(i, ref, store, player);
                return;
            }
            if (isDeleteOreGenOreRequested(data, i)) {
                handleDeleteOreGenOre(i, ref, store, player);
                return;
            }
        }

        for (int i = 0; i < MAX_HOMES_ROWS; i++) {
            if (isViewHomesForPlayerRequested(data, i)) {
                handleViewHomesForPlayer(i, ref, store, player);
                return;
            }
        }

        for (int i = 0; i < MAX_ISLAND_ROWS; i++) {
            if (isViewIslandForPlayerRequested(data, i)) {
                handleViewIslandForPlayer(i, ref, store, player);
                return;
            }
        }

        for (int i = 0; i < MAX_RESETS_ROWS; i++) {
            if (isApplyResetsPlayerValueRequested(data, i)) {
                handleApplyPlayerResetsValue(i, ref, store, player);
                return;
            }
        }

        if (data.moveTemplateUp != null) {
            handleMove(data.moveTemplateUp, true, ref, store, player);
            return;
        }

        if (data.moveTemplateDown != null) {
            handleMove(data.moveTemplateDown, false, ref, store, player);
            return;
        }

        if (data.editTemplate != null) {
            handleEdit(data.editTemplate, ref, store, player, translateTemplates, fallbackLanguage);
            return;
        }

        if (data.deleteTemplate != null) {
            handleDelete(data.deleteTemplate, ref, store, player);
            return;
        }

        if (data.confirmDelete != null) {
            handleConfirmDelete(ref, store, player);
            return;
        }

        if (data.saveEditTemplateForm != null) {
            handleSaveEditTemplateForm(ref, store, player, translateTemplates, fallbackLanguage);
        }
    }

    private void handleFinish(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull Player player) {
        String name  = formName == null        ? "" : formName.trim();
        String desc  = formDescription == null ? "" : formDescription.trim();
        String rawId = formId == null          ? "" : formId.trim();

        if (name.isBlank()) {
            formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.island_name_required");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (rawId.isBlank()) {
            formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_id_required");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        String templateId = normalizeTemplateId(rawId);
        if (templateId == null) {
            formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_id_invalid");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (plugin.getIslandTemplateManager().hasTemplate(templateId)) {
            formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_exists");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        boolean translateTemplates = plugin.getConfigManager().getConfig().getSettings().isTranslateTemplates();
        String configLang = plugin.getConfigManager().getConfig().getLanguage();
        String defaultLanguage = (configLang != null && !configLang.isBlank()) ? configLang.trim().toLowerCase(java.util.Locale.ROOT) : "en";

        BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(player, playerRef);
        if (builderState == null) {
            formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.buildertools_unavailable");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        final String finalName = name;
        final String finalDesc = desc.isBlank() ? "." : desc;
        final String finalId   = templateId;

            BuilderToolsPlugin.addToQueue(player, playerRef, (r, state, compAccess) -> {
                BlockSelection selection = state.getSelection();
                if (selection == null || !selection.hasSelectionBounds()) {
                    this.formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.no_selection");
                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                    return;
                }

                Vector3i min = selection.getSelectionMin();
                Vector3i max = selection.getSelectionMax();
                int sizeX = max.x() - min.x() + 1;
                int sizeY = max.y() - min.y() + 1;
                int sizeZ = max.z() - min.z() + 1;

                TransformComponent transform = compAccess.getComponent(r, TransformComponent.getComponentType());
                Vector3d playerPos = transform != null ? transform.getPosition() : new Vector3d(min.x(), min.y(), min.z());
                final int spawnX = clamp((int) Math.floor(playerPos.x()) - min.x(), 0, sizeX - 1);
                final int spawnY = clamp((int) Math.floor(playerPos.y()) - min.y(), 0, sizeY);
                final int spawnZ = clamp((int) Math.floor(playerPos.z()) - min.z(), 0, sizeZ - 1);

                final float spawnPitch = transform != null ? transform.getRotation().pitch() : 0f;
                final float spawnYaw = transform != null ? transform.getRotation().yaw() : 0f;

                // Run on main server thread for safe ECS access
                sourceWorld.execute(() -> {
                    BlockSelection capturedPrefab = new BlockSelection();
                    capturedPrefab.setAnchor(0, 0, 0);
                    boolean hasBedrock = false;

                    for (int x = min.x(); x <= max.x(); x++) {
                        for (int y = min.y(); y <= max.y(); y++) {
                            for (int z = min.z(); z <= max.z(); z++) {
                                int chunkX = x >> 5;
                                int chunkZ = z >> 5;
                                long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
                                WorldChunk chunk = sourceWorld.getChunk(chunkKey);
                                if (chunk == null) continue;

                                int blockIndex = chunk.getBlock(x, y, z);

                                // Capture fluids even when the block is air (blockIndex==0).
                                @SuppressWarnings("deprecation")
                                int fluidIdIndexPre = chunk.getFluidId(x, y, z);
                                @SuppressWarnings("deprecation")
                                byte fluidLevelPre = chunk.getFluidLevel(x, y, z);
                                if (blockIndex == 0) {
                                    if (fluidIdIndexPre > 0 && fluidLevelPre > 0) {
                                        int relX = x - min.x();
                                        int relY = y - min.y();
                                        int relZ = z - min.z();
                                        BlockType airType = (BlockType) BlockType.getAssetMap().getAsset(0);
                                        if (airType != null) {
                                            int airIndex = BlockType.getAssetMap().getIndex(airType.getId());
                                            if (airIndex > 0) {
                                                capturedPrefab.addBlockAtLocalPos(relX, relY, relZ, airIndex, 0, 0, 0);
                                            }
                                        }
                                        capturedPrefab.addFluidAtLocalPos(relX, relY, relZ, fluidIdIndexPre, fluidLevelPre);
                                    }
                                    continue;
                                }

                                BlockType bt = (BlockType) BlockType.getAssetMap().getAsset(blockIndex);
                                if (bt == null) continue;

                                int relX = x - min.x();
                                int relY = y - min.y();
                                int relZ = z - min.z();

                                BlockSection section = chunk.getBlockChunk().getSectionAtBlockY(y);
                                int rotation = section != null ? section.getRotationIndex(x, y, z) : 0;
                                int filler = section != null ? section.getFiller(x, y, z) : 0;
                                int support = chunk.getSupportValue(x, y, z);

                                Holder<ChunkStore> blockEntity = chunk.getBlockComponentHolder(x, y, z);

                                capturedPrefab.addBlockAtLocalPos(relX, relY, relZ, blockIndex, rotation, filler, support, blockEntity);

                                if (!hasBedrock && IslandSchematic.isBedrockId(bt.getId())) {
                                    hasBedrock = true;
                                }

                                @SuppressWarnings("deprecation")
                                int fluidIdIndex = chunk.getFluidId(x, y, z);
                                @SuppressWarnings("deprecation")
                                byte fluidLevel = chunk.getFluidLevel(x, y, z);
                                if (fluidIdIndex > 0 && fluidLevel > 0) {
                                    capturedPrefab.addFluidAtLocalPos(relX, relY, relZ, fluidIdIndex, fluidLevel);
                                }
                            }
                        }
                    }

                    if (!hasBedrock) {
                        this.formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.bedrock_required", Map.of(
                            "block", IslandSchematic.BEDROCK_BLOCK_ID
                        ));
                        Player p = store.getComponent(ref, Player.getComponentType());
                        if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                        return;
                    }

                    // Save using native serializer (same as builder tools copy/paste)
                    boolean saved = plugin.getIslandTemplateManager().saveCapturedPrefabBsdon(finalId, capturedPrefab);
                    if (!saved) {
                        this.formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_save_failed");
                        Player p = store.getComponent(ref, Player.getComponentType());
                        if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                        return;
                    }

                    IslandTemplate template = new IslandTemplate();
                    template.setId(finalId);
                    if (translateTemplates) {
                        String titleKey = buildTemplateTitleKey(finalId);
                        String descriptionKey = buildTemplateDescriptionKey(finalId);
                        template.setTitleKey(titleKey);
                        template.setDescriptionKey(descriptionKey);
                        template.getTitleTranslations().put(defaultLanguage, finalName);
                        template.getDescriptionTranslations().put(defaultLanguage, finalDesc);
                        plugin.getMessageManager().setTranslation(defaultLanguage, titleKey, finalName);
                        plugin.getMessageManager().setTranslation(defaultLanguage, descriptionKey, finalDesc);
                    } else {
                        template.setTitleKey(finalName);
                        template.setDescriptionKey(finalDesc);
                        template.setStaticTitle(finalName);
                        template.setStaticDescription(finalDesc);
                    }
                    template.setPreviewImage(finalId + ".png");
                    template.setPrefabName("island_templates/" + finalId + "/" + finalId + ".prefab.json");
                    template.setDisplayOrder(plugin.getIslandTemplateManager().getNextDisplayOrder());
                    template.setSpawnOffsetX(spawnX);
                    template.setSpawnOffsetY(spawnY);
                    template.setSpawnOffsetZ(spawnZ);
                    template.setSpawnOffsetPitch(spawnPitch);
                    template.setSpawnOffsetYaw(spawnYaw);
                    template.applyDefaults();

                    boolean templateSaved = plugin.getIslandTemplateManager().saveTemplate(template);
                    plugin.getIslandTemplateManager().ensureTemplateImage(finalId);
                    plugin.reloadTemplateImages();

                    if (!templateSaved) {
                        this.formError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_save_failed");
                        Player p = store.getComponent(ref, Player.getComponentType());
                        if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                        return;
                    }

                    this.showCreateForm  = false;
                    this.createSuccess   = true;
                    this.formError       = null;
                    this.formName        = "";
                    this.formDescription = "";
                    this.formId          = "";
                    plugin.getMessageManager().loadLanguage(plugin.getConfigManager().getConfig().getLanguage());

                    Player p = store.getComponent(ref, Player.getComponentType());
                    if (p != null) p.getPageManager().openCustomPage(ref, store, this);
                });
            });
    }

    private void handleToggle(String indexValue,
                              @Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull Player player) {
        TemplateSlot slot = getSlotFromIndex(indexValue);
        if (slot == null || slot.hasError) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getIslandTemplateManager().setTemplateEnabled(slot.id, !slot.enabled);
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleToggleAddon(String indexValue,
                                   @Nonnull Ref<EntityStore> ref,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull Player player) {
        AddonSlot slot = getAddonSlotFromIndex(indexValue);
        if (slot == null) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getAddonManager().setAddonEnabled(slot.id, !slot.enabled);
        if (AddonManager.MOBS_MANAGER_ADDON_KEY.equals(slot.id)
            && !plugin.getAddonManager().isMobsManagerEnabled()
            && activeTab == DashboardTab.MOBS_MANAGER) {
            activeTab = DashboardTab.ADDONS;
        }
        if (AddonManager.HOMES_ADDON_KEY.equals(slot.id)
            && !plugin.getAddonManager().isHomesEnabled()
            && activeTab == DashboardTab.HOMES) {
            activeTab = DashboardTab.ADDONS;
        }
        if (AddonManager.LEVELS_ADDON_KEY.equals(slot.id)
            && !plugin.getAddonManager().isLevelsEnabled()
            && activeTab == DashboardTab.LEVELS) {
            activeTab = DashboardTab.ADDONS;
        }
        if (AddonManager.ORE_GENERATOR_ADDON_KEY.equals(slot.id)
            && !plugin.getAddonManager().isOreGeneratorEnabled()
            && activeTab == DashboardTab.ORE_GENERATOR) {
            activeTab = DashboardTab.ADDONS;
        }
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleSpawnProtectionToggle(String toggleValue,
                                             @Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Player player) {
        boolean current = plugin.getConfigManager().getConfig().getSettings().isSpawnProtectionEnabled();
        plugin.getSettingsCommandHandler().handleSpawnProtectionSetting(!current);
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleVoidKillItemsToggle(String toggleValue,
                                           @Nonnull Ref<EntityStore> ref,
                                           @Nonnull Store<EntityStore> store,
                                           @Nonnull Player player) {
        boolean current = plugin.getConfigManager().getConfig().getSettings().isVoidKillItemsEnabled();
        plugin.getSettingsCommandHandler().handleVoidKillItemsSetting(!current);
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleVoidKillEntitiesToggle(String toggleValue,
                                              @Nonnull Ref<EntityStore> ref,
                                              @Nonnull Store<EntityStore> store,
                                              @Nonnull Player player) {
        boolean current = plugin.getConfigManager().getConfig().getSettings().isVoidKillEntitiesEnabled();
        plugin.getSettingsCommandHandler().handleVoidKillEntitiesSetting(!current);
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleVoidTeleportToggle(String toggleValue,
                                          @Nonnull Ref<EntityStore> ref,
                                          @Nonnull Store<EntityStore> store,
                                          @Nonnull Player player) {
        boolean current = plugin.getConfigManager().getConfig().getSettings().isVoidTeleportEnabled();
        plugin.getSettingsCommandHandler().handleVoidTeleportSetting(!current);
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handlePlaceSpawn(@Nonnull Ref<EntityStore> ref,
                                  @Nonnull Store<EntityStore> store,
                                  @Nonnull Player player) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            Chat.send(playerRef, plugin.tr(playerRef, "lobby.set_failed"));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        int preferredX = (int) Math.floor(transform.getPosition().x());
        int preferredY = (int) Math.floor(transform.getPosition().y());
        int preferredZ = (int) Math.floor(transform.getPosition().z());
        float preferredPitch = transform.getRotation().pitch();
        float preferredYaw = transform.getRotation().yaw();

        plugin.getConfigManager().getConfig().getLobby().setWorldName(sourceWorld.getName());
        plugin.getConfigManager().getConfig().getLobby().setX(preferredX);
        plugin.getConfigManager().getConfig().getLobby().setY(preferredY);
        plugin.getConfigManager().getConfig().getLobby().setZ(preferredZ);
        plugin.getConfigManager().getConfig().getLobby().setPitch(preferredPitch);
        plugin.getConfigManager().getConfig().getLobby().setYaw(preferredYaw);
        plugin.getConfigManager().getConfig().getLobby().setConfigured(true);
        plugin.getConfigManager().save();

        Chat.send(playerRef, plugin.tr(playerRef, "lobby.set_success", Map.of(
            "world", sourceWorld.getName(),
            "x", String.valueOf(preferredX),
            "y", String.valueOf(preferredY),
            "z", String.valueOf(preferredZ)
        )));
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyProtectionRadius(@Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Player player) {
        String raw = settingsProtectionRadiusInput;
        if (raw == null || raw.isBlank()) {
            raw = String.valueOf(plugin.getConfigManager().getConfig().getSettings().getSpawnProtectionRadius());
        }

        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            settingsError = uiText(getAdminDashboardLanguage(), "settings.protection_radius.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (value < 1) {
            settingsError = uiText(getAdminDashboardLanguage(), "settings.protection_radius.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getSettingsCommandHandler().handleSpawnProtectionRadiusSetting(value);
        settingsProtectionRadiusInput = null;
        settingsProtectionRadiusDirtyShown = false;
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyTemplatesY(@Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull Player player) {
        String raw = settingsTemplatesYInput;
        if (raw == null || raw.isBlank()) {
            raw = String.valueOf(plugin.getConfigManager().getConfig().getGrid().getIslandY());
        }

        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            settingsError = uiText(getAdminDashboardLanguage(), "settings.templates_y.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (value < 1) {
            settingsError = uiText(getAdminDashboardLanguage(), "settings.templates_y.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getSettingsCommandHandler().handleTemplatesYSetting(value);
        settingsTemplatesYInput = null;
        settingsTemplatesYDirtyShown = false;
        settingsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleMobsReduceMobsToggle(@Nonnull Ref<EntityStore> ref,
                                            @Nonnull Store<EntityStore> store,
                                            @Nonnull Player player) {
        boolean current = plugin.getAddonManager().getMobsManagerAddon().isReduceMobsEnabled();
        plugin.getSettingsCommandHandler().handleReduceMobsSetting(!current);
        mobsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyMobsReduceRate(@Nonnull Ref<EntityStore> ref,
                                           @Nonnull Store<EntityStore> store,
                                           @Nonnull Player player) {
        String raw = mobsReduceRateInput == null ? "" : mobsReduceRateInput.trim();
        if (raw.isBlank()) {
            mobsError = uiText(getAdminDashboardLanguage(), "settings.reduce_mobs_rate.invalid", Map.of("value", ""));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        int value;
        try {
            value = (int) Math.round(Double.parseDouble(raw.replace(',', '.')));
        } catch (NumberFormatException ex) {
            mobsError = uiText(getAdminDashboardLanguage(), "settings.reduce_mobs_rate.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        value = clamp(value, 0, 100);

        plugin.getSettingsCommandHandler().handleReduceMobsRateSetting(value);
        mobsReduceRateInput = null;
        mobsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleMobsTorchProtectionToggle(@Nonnull Ref<EntityStore> ref,
                                                 @Nonnull Store<EntityStore> store,
                                                 @Nonnull Player player) {
        boolean current = plugin.getAddonManager().getMobsManagerAddon().isTorchProtectionEnabled();
        plugin.getSettingsCommandHandler().handleTorchProtectionSetting(!current);
        mobsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyMobsTorchRadius(@Nonnull Ref<EntityStore> ref,
                                            @Nonnull Store<EntityStore> store,
                                            @Nonnull Player player) {
        String raw = mobsTorchRadiusInput == null ? "" : mobsTorchRadiusInput.trim();
        if (raw.isBlank()) {
            mobsError = uiText(getAdminDashboardLanguage(), "settings.torch_protection_radius.invalid", Map.of("value", ""));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            mobsError = uiText(getAdminDashboardLanguage(), "settings.torch_protection_radius.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getSettingsCommandHandler().handleTorchProtectionRadiusSetting(value);
        mobsTorchRadiusInput = null;
        mobsTorchRadiusDirtyShown = false;
        mobsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyMaxResets(@Nonnull Ref<EntityStore> ref,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull Player player) {
        String raw = resetsMaxResetsInput;
        if (raw == null || raw.isBlank()) {
            raw = String.valueOf(Math.max(0, plugin.getConfigManager().getConfig().getSettings().getMaxResets()));
        }

        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            resetsError = uiText(getAdminDashboardLanguage(), "settings.max_resets.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (value < 0) {
            resetsError = uiText(getAdminDashboardLanguage(), "settings.max_resets.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getSettingsCommandHandler().handleMaxResetsSetting(value);
        resetsMaxResetsInput = null;
        resetsMaxResetsDirtyShown = false;
        resetsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyPlayerResetsValue(int index,
                                              @Nonnull Ref<EntityStore> ref,
                                              @Nonnull Store<EntityStore> store,
                                              @Nonnull Player player) {
        if (index < 0 || index >= resetPlayerSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        ResetPlayerSlot slot = resetPlayerSlots.get(index);
        String raw = resetPlayerDraftInputs.get(slot.uuid);
        if (raw == null || raw.isBlank()) {
            raw = String.valueOf(slot.currentResets);
        }

        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            resetsError = uiText(getAdminDashboardLanguage(), "resets.set.invalid_amount", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (value < 0) {
            resetsError = uiText(getAdminDashboardLanguage(), "resets.set.invalid_amount", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getResetCommandHandler().handleResetsSet(slot.playerName, value);
        resetPlayerDraftInputs.remove(slot.uuid);
        resetsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyMaxHomes(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull Player player) {
        String raw = homesMaxHomesInput;
        if (raw == null || raw.isBlank()) {
            raw = String.valueOf(Math.max(0, plugin.getAddonManager().getHomesAddon().getMaxHomes()));
        }

        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            homesError = uiText(getAdminDashboardLanguage(), "settings.max_homes.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (value < 0) {
            homesError = uiText(getAdminDashboardLanguage(), "settings.max_homes.invalid", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getSettingsCommandHandler().handleMaxHomesSetting(value);
        homesMaxHomesInput = null;
        homesMaxHomesDirtyShown = false;
        homesError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyPlayerHomesValue(int index,
                                             @Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Player player) {
        if (index < 0 || index >= homePlayerSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        HomePlayerSlot slot = homePlayerSlots.get(index);
        String raw = homePlayerDraftInputs.get(slot.uuid);
        if (raw == null || raw.isBlank()) {
            raw = String.valueOf(slot.currentMaxHomes);
        }

        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            homesError = uiText(getAdminDashboardLanguage(), "homes.admin.set.invalid_amount", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (value < 0) {
            homesError = uiText(getAdminDashboardLanguage(), "homes.admin.set.invalid_amount", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getAddonManager().getHomesAddon().getService().setPlayerAddedHomes(slot.uuid, value);
        homePlayerDraftInputs.remove(slot.uuid);
        homesError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyLevelsLevelValue(@Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Player player) {
        String raw = levelsLevelValueInput == null ? "" : levelsLevelValueInput.trim();
        if (raw.isBlank()) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_level_value", Map.of("value", ""));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (Exception ignored) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_level_value", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (value < 1) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_level_value", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getAddonManager().getLevelsAddon().handleLevelValueSetting(value);
        levelsLevelValueInput = null;
        levelsLevelValueDirtyShown = false;
        levelsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyLevelsDefaultBlockValue(@Nonnull Ref<EntityStore> ref,
                                                    @Nonnull Store<EntityStore> store,
                                                    @Nonnull Player player) {
        String raw = levelsDefaultBlockValueInput == null ? "" : levelsDefaultBlockValueInput.trim();
        if (raw.isBlank()) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", ""));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        double value;
        try {
            value = Double.parseDouble(raw.replace(',', '.'));
        } catch (Exception ignored) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (value <= 0D || Double.isNaN(value) || Double.isInfinite(value)) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getAddonManager().getLevelsAddon().handleBlockDefaultValueSetting(value);
        levelsDefaultBlockValueInput = null;
        levelsDefaultBlockValueDirtyShown = false;
        levelsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleCreateLevelsBlockValue(@Nonnull Ref<EntityStore> ref,
                                              @Nonnull Store<EntityStore> store,
                                              @Nonnull Player player) {
        String rawBlockId = levelsCreateBlockIdInput == null ? "" : levelsCreateBlockIdInput.trim();
        String rawBlockValue = levelsCreateBlockValueInput == null ? "" : levelsCreateBlockValueInput.trim();
        if (rawBlockId.isBlank()) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.block_id_required");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        String canonical = plugin.getAddonManager().getLevelsAddon().getIslandValueService().resolveCanonicalBlockId(rawBlockId);
        if (canonical == null) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.block_id_invalid", Map.of("block_id", rawBlockId));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (plugin.getAddonManager().getLevelsAddon().getIslandValueService().hasConfiguredBlockValue(canonical)) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.block_id_duplicate", Map.of("block_id", canonical));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        double value;
        try {
            value = Double.parseDouble(rawBlockValue.replace(',', '.'));
        } catch (Exception ignored) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", rawBlockValue));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (value <= 0D || Double.isNaN(value) || Double.isInfinite(value)) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", rawBlockValue));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getAddonManager().getLevelsAddon().getIslandValueService().setConfiguredBlockValue(canonical, value);
        levelsCreateBlockIdInput = "";
        levelsCreateBlockValueInput = "";
        levelsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyLevelsBlockValue(int index,
                                             @Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Player player) {
        if (index < 0 || index >= levelBlockSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        LevelBlockSlot slot = levelBlockSlots.get(index);
        String rawBlockId = levelBlockIdDraftInputs.get(slot.blockId);
        String rawBlockValue = levelBlockValueDraftInputs.get(slot.blockId);

        String blockId = (rawBlockId == null || rawBlockId.isBlank()) ? slot.blockId : rawBlockId.trim();
        String valueInput = (rawBlockValue == null || rawBlockValue.isBlank()) ? formatDouble(slot.blockValue) : rawBlockValue.trim();

        String canonical = plugin.getAddonManager().getLevelsAddon().getIslandValueService().resolveCanonicalBlockId(blockId);
        if (canonical == null) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.block_id_invalid", Map.of("block_id", blockId));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (!canonical.equalsIgnoreCase(slot.blockId)
            && plugin.getAddonManager().getLevelsAddon().getIslandValueService().hasConfiguredBlockValue(canonical)) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.block_id_duplicate", Map.of("block_id", canonical));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        double value;
        try {
            value = Double.parseDouble(valueInput.replace(',', '.'));
        } catch (Exception ignored) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", valueInput));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (value <= 0D || Double.isNaN(value) || Double.isInfinite(value)) {
            levelsError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.levels.error.invalid_block_value", Map.of("value", valueInput));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (!canonical.equalsIgnoreCase(slot.blockId)) {
            plugin.getAddonManager().getLevelsAddon().getIslandValueService().removeConfiguredBlockValue(slot.blockId);
        }
        plugin.getAddonManager().getLevelsAddon().getIslandValueService().setConfiguredBlockValue(canonical, value);
        levelBlockIdDraftInputs.remove(slot.blockId);
        levelBlockValueDraftInputs.remove(slot.blockId);
        levelsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleDeleteLevelsBlockValue(int index,
                                              @Nonnull Ref<EntityStore> ref,
                                              @Nonnull Store<EntityStore> store,
                                              @Nonnull Player player) {
        if (index < 0 || index >= levelBlockSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        LevelBlockSlot slot = levelBlockSlots.get(index);
        plugin.getAddonManager().getLevelsAddon().getIslandValueService().removeConfiguredBlockValue(slot.blockId);
        levelBlockIdDraftInputs.remove(slot.blockId);
        levelBlockValueDraftInputs.remove(slot.blockId);
        levelsError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleViewHomesForPlayer(int index,
                                          @Nonnull Ref<EntityStore> ref,
                                          @Nonnull Store<EntityStore> store,
                                          @Nonnull Player player) {
        if (index < 0 || index >= homePlayerSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        HomePlayerSlot slot = homePlayerSlots.get(index);
        ISkyblockAdminDashboardHomesViewerPage viewerPage = new ISkyblockAdminDashboardHomesViewerPage(
            playerRef, sourceWorld, plugin, this, slot.uuid, slot.playerName);
        player.getPageManager().openCustomPage(ref, store, viewerPage);
    }

    private void handleViewIslandForPlayer(int index,
                                           @Nonnull Ref<EntityStore> ref,
                                           @Nonnull Store<EntityStore> store,
                                           @Nonnull Player player) {
        if (index < 0 || index >= islandPlayerSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        IslandPlayerSlot slot = islandPlayerSlots.get(index);
        ISkyblockAdminDashboardIslandsViewerPage viewerPage = new ISkyblockAdminDashboardIslandsViewerPage(
            playerRef, sourceWorld, plugin, this, slot.uuid, slot.playerName);
        player.getPageManager().openCustomPage(ref, store, viewerPage);
    }

    private void handleMove(String indexValue,
                            boolean up,
                            @Nonnull Ref<EntityStore> ref,
                            @Nonnull Store<EntityStore> store,
                            @Nonnull Player player) {
        TemplateSlot slot = getSlotFromIndex(indexValue);
        if (slot == null || slot.hasError) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (up) {
            plugin.getIslandTemplateManager().moveTemplateUp(slot.id);
        } else {
            plugin.getIslandTemplateManager().moveTemplateDown(slot.id);
        }
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleEdit(String indexValue,
                            @Nonnull Ref<EntityStore> ref,
                            @Nonnull Store<EntityStore> store,
                            @Nonnull Player player,
                            boolean translateTemplates,
                            String fallbackLanguage) {
        TemplateSlot slot = getSlotFromIndex(indexValue);
        if (slot == null || slot.hasError) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        showCreateForm = false;
        createSuccess = false;
        showDeleteConfirm = false;
        showEditTemplateForm = true;
        editTemplateId = slot.id;
        editError = null;

        List<String> editLanguages = getEditLanguages(translateTemplates, fallbackLanguage);
        if (editLanguage == null || editLanguage.isBlank() || !editLanguages.contains(editLanguage)) {
            String preferred = getDefaultTemplateLanguage(fallbackLanguage);
            editLanguage = editLanguages.contains(preferred)
                ? preferred
                : (editLanguages.isEmpty() ? preferred : editLanguages.get(0));
        }

        loadEditTemplateFormValues(translateTemplates, fallbackLanguage);
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleDelete(String indexValue,
                              @Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull Player player) {
        TemplateSlot slot = getSlotFromIndex(indexValue);
        if (slot == null || slot.hasError || !slot.deletable) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        showCreateForm = false;
        showEditTemplateForm = false;
        showDeleteConfirm = true;
        deleteTemplateId = slot.id;
        deleteError = null;

        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleConfirmDelete(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull Player player) {
        if (deleteTemplateId == null || deleteTemplateId.isBlank()) {
            showDeleteConfirm = false;
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        boolean deleted = plugin.getIslandTemplateManager().deleteTemplate(deleteTemplateId);
        if (!deleted) {
            deleteError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.delete_failed");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.reloadTemplateImages();

        if (deleteTemplateId.equals(editTemplateId)) {
            showEditTemplateForm = false;
            editTemplateId = "";
            editName = "";
            editDescription = "";
            editError = null;
        }

        showDeleteConfirm = false;
        deleteTemplateId = "";
        deleteError = null;

        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleSaveEditTemplateForm(@Nonnull Ref<EntityStore> ref,
                                            @Nonnull Store<EntityStore> store,
                                            @Nonnull Player player,
                                            boolean translateTemplates,
                                            String fallbackLanguage) {
        IslandTemplate template = plugin.getIslandTemplateManager().get(editTemplateId);
        if (template == null) {
            editError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_not_found");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        String newName = editName == null ? "" : editName.trim();
        String newDescription = editDescription == null ? "" : editDescription.trim();
        if (newName.isBlank()) {
            editError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.name_required");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (newDescription.isBlank()) {
            newDescription = ".";
        }

        if (translateTemplates) {
            String language = normalizeLanguageCode(editLanguage);
            if (language.isBlank()) {
                language = getDefaultTemplateLanguage(fallbackLanguage);
                editLanguage = language;
            }

            String titleKey = template.getTitleKey();
            if (titleKey == null || titleKey.isBlank() || titleKey.equals(template.getStaticTitle())) {
                titleKey = buildTemplateTitleKey(template.getId());
                template.setTitleKey(titleKey);
            }

            String descriptionKey = template.getDescriptionKey();
            if (descriptionKey == null || descriptionKey.isBlank() || descriptionKey.equals(template.getStaticDescription())) {
                descriptionKey = buildTemplateDescriptionKey(template.getId());
                template.setDescriptionKey(descriptionKey);
            }

            template.getTitleTranslations().put(language, newName);
            template.getDescriptionTranslations().put(language, newDescription);
            plugin.getMessageManager().setTranslation(language, titleKey, newName);
            plugin.getMessageManager().setTranslation(language, descriptionKey, newDescription);
        } else {
            template.setStaticTitle(newName);
            template.setStaticDescription(newDescription);
        }

        if (!plugin.getIslandTemplateManager().saveTemplate(template)) {
            editError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.error.template_save_failed");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        plugin.getMessageManager().loadLanguage(plugin.getConfigManager().getConfig().getLanguage());
        showEditTemplateForm = false;
        editError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void loadEditTemplateFormValues(boolean translateTemplates, String fallbackLanguage) {
        IslandTemplate template = plugin.getIslandTemplateManager().get(editTemplateId);
        if (template == null) {
            editName = "";
            editDescription = "";
            return;
        }

        if (!translateTemplates) {
            editName = firstNonBlank(template.getStaticTitle(), template.getTitleKey(), template.getId());
            editDescription = firstNonBlank(template.getStaticDescription(), template.getDescriptionKey(), ".");
            return;
        }

        editName = resolveLocalizedValue(template.getTitleKey(), template.getTitleTranslations(), editLanguage, fallbackLanguage);
        editDescription = resolveLocalizedValue(template.getDescriptionKey(), template.getDescriptionTranslations(), editLanguage, fallbackLanguage);
    }

    private String resolveLocalizedValue(String key,
                                         Map<String, String> inlineTranslations,
                                         String language,
                                         String fallbackLanguage) {
        if (inlineTranslations != null) {
            String value = inlineTranslations.get(language);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        String exact = plugin.getMessageManager().getExact(key, language);
        if (exact != null && !exact.isBlank()) {
            return exact;
        }

        if (inlineTranslations != null) {
            String fallback = inlineTranslations.get(fallbackLanguage);
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
        }

        exact = plugin.getMessageManager().getExact(key, fallbackLanguage);
        if (exact != null && !exact.isBlank()) {
            return exact;
        }

        String resolved = plugin.getMessageManager().get(key, language);
        if (!resolved.equals(key)) {
            return resolved;
        }

        resolved = plugin.getMessageManager().get(key, fallbackLanguage);
        if (!resolved.equals(key)) {
            return resolved;
        }

        return key == null ? "" : key;
    }

    private List<String> getEditLanguages(boolean translateTemplates, String fallbackLanguage) {
        List<String> result = new ArrayList<>();
        if (!translateTemplates) {
            result.add(getDefaultTemplateLanguage(fallbackLanguage));
            return result;
        }

        for (String language : plugin.getConfigManager().getConfig().getSettings().getEnabledLanguages()) {
            String resolved = plugin.getLanguageService().resolveInputLanguage(language);
            if (!resolved.isBlank()
                && plugin.getMessageManager().getAvailableLanguages().contains(resolved)
                && !result.contains(resolved)) {
                result.add(resolved);
            }
        }

        if (result.isEmpty()) {
            for (String language : plugin.getMessageManager().getAvailableLanguages()) {
                String resolved = plugin.getLanguageService().resolveInputLanguage(language);
                if (!resolved.isBlank() && !result.contains(resolved)) {
                    result.add(resolved);
                }
            }
        }

        String defaultLanguage = getDefaultTemplateLanguage(fallbackLanguage);
        if (!defaultLanguage.isBlank() && !result.contains(defaultLanguage)) {
            result.add(0, defaultLanguage);
        }
        return result;
    }

    private String getDefaultTemplateLanguage(String fallbackLanguage) {
        String configured = normalizeLanguageCode(plugin.getConfigManager().getConfig().getLanguage());
        if (!configured.isBlank()) {
            return configured;
        }
        return normalizeLanguageCode(fallbackLanguage);
    }

    private TemplateSlot getSlotFromIndex(String indexValue) {
        int idx;
        try {
            idx = Integer.parseInt(indexValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (idx < 0 || idx >= templateSlots.size()) {
            return null;
        }
        return templateSlots.get(idx);
    }

    private AddonSlot getAddonSlotFromIndex(String indexValue) {
        int idx;
        try {
            idx = Integer.parseInt(indexValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (idx < 0 || idx >= addonSlots.size()) {
            return null;
        }
        return addonSlots.get(idx);
    }

    private ZoneStatus computeZoneStatus(Store<EntityStore> store, Ref<EntityStore> ref) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return ZoneStatus.NO_ZONE;

        BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(player, playerRef);
        if (builderState == null) return ZoneStatus.NO_ZONE;

        BlockSelection selection = builderState.getSelection();
        if (selection == null || !selection.hasSelectionBounds()) return ZoneStatus.NO_ZONE;

        Vector3i min = selection.getSelectionMin();
        Vector3i max = selection.getSelectionMax();

        int remaining = 300_000;
        outer:
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    if (remaining-- <= 0) break outer;
                    int chunkX = x >> 5;
                    int chunkZ = z >> 5;
                    long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
                    WorldChunk chunk = sourceWorld.getChunk(chunkKey);
                    if (chunk == null) continue;
                    int blockIndex = chunk.getBlock(x, y, z);
                    if (blockIndex == 0) continue;
                    BlockType bt = (BlockType) BlockType.getAssetMap().getAsset(blockIndex);
                    if (bt != null && IslandSchematic.isBedrockId(bt.getId())) {
                        return ZoneStatus.VALID;
                    }
                }
            }
        }
        return ZoneStatus.NO_BEDROCK;
    }

    private String zoneStatusText(ZoneStatus status, String language) {
        return switch (status) {
            case NO_ZONE    -> uiText(language, "ui.admin_dashboard.zone.no_selection");
            case NO_BEDROCK -> uiText(language, "ui.admin_dashboard.zone.no_bedrock");
            case VALID      -> uiText(language, "ui.admin_dashboard.zone.valid");
        };
    }

    private String uiText(String language, String key) {
        return plugin.getMessageManager().get(key, language);
    }

    private String uiText(String language, String key, Map<String, String> vars) {
        return plugin.getMessageManager().get(key, vars, language);
    }

    private static String normalizeTemplateId(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) return null;
        if (!cleaned.matches("^[a-z0-9_-]{1,48}$")) return null;
        return cleaned;
    }

    private static String buildTemplateTitleKey(String templateId) {
        return "template." + templateId + ".title";
    }

    private static String buildTemplateDescriptionKey(String templateId) {
        return "template." + templateId + ".description";
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private static String normalizeLanguageCode(String language) {
        if (language == null) {
            return "";
        }
        return language.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static String buildOreGenTierPermission(String tierName) {
        String token = normalizePermissionToken(tierName);
        return token.isBlank() ? "" : "iskyblock.oregenerator.tier." + token;
    }

    private static String normalizePermissionToken(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "");
        normalized = normalized.replaceAll("_+$", "");
        return normalized;
    }

    private static String getSettingsInputDisplayValue(String draftValue, int persistedValue) {
        if (draftValue == null || draftValue.isBlank()) {
            return String.valueOf(persistedValue);
        }
        return draftValue.trim();
    }

    private static String getSettingsInputDisplayValue(String draftValue, String persistedValue) {
        if (draftValue == null || draftValue.isBlank()) {
            return persistedValue;
        }
        return draftValue.trim();
    }

    private static boolean isSettingsInputDirty(String draftValue, int persistedValue) {
        if (draftValue == null || draftValue.isBlank()) {
            return false;
        }
        return !draftValue.trim().equals(String.valueOf(persistedValue));
    }

    private static boolean isSettingsInputDirty(String draftValue, String persistedValue) {
        if (draftValue == null || draftValue.isBlank()) {
            return false;
        }
        return !draftValue.trim().equals(persistedValue);
    }

    private static String formatDouble(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private DropdownEntryInfo[] buildLanguageEntries(List<String> languages) {
        DropdownEntryInfo[] entries = new DropdownEntryInfo[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            String locale = languages.get(i);
            entries[i] = new DropdownEntryInfo(LocalizableString.fromString(plugin.getLanguageService().langLabel(locale)), locale);
        }
        return entries;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String getResetsPlayerDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.resetsPlayerValue0;
            case 1 -> data.resetsPlayerValue1;
            case 2 -> data.resetsPlayerValue2;
            case 3 -> data.resetsPlayerValue3;
            case 4 -> data.resetsPlayerValue4;
            case 5 -> data.resetsPlayerValue5;
            case 6 -> data.resetsPlayerValue6;
            case 7 -> data.resetsPlayerValue7;
            case 8 -> data.resetsPlayerValue8;
            case 9 -> data.resetsPlayerValue9;
            default -> null;
        };
    }

    private static boolean isApplyResetsPlayerValueRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.applyResetsPlayerValue0 != null;
            case 1 -> data.applyResetsPlayerValue1 != null;
            case 2 -> data.applyResetsPlayerValue2 != null;
            case 3 -> data.applyResetsPlayerValue3 != null;
            case 4 -> data.applyResetsPlayerValue4 != null;
            case 5 -> data.applyResetsPlayerValue5 != null;
            case 6 -> data.applyResetsPlayerValue6 != null;
            case 7 -> data.applyResetsPlayerValue7 != null;
            case 8 -> data.applyResetsPlayerValue8 != null;
            case 9 -> data.applyResetsPlayerValue9 != null;
            default -> false;
        };
    }

    private static String getHomesPlayerDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.homesPlayerValue0;
            case 1 -> data.homesPlayerValue1;
            case 2 -> data.homesPlayerValue2;
            case 3 -> data.homesPlayerValue3;
            case 4 -> data.homesPlayerValue4;
            case 5 -> data.homesPlayerValue5;
            case 6 -> data.homesPlayerValue6;
            case 7 -> data.homesPlayerValue7;
            case 8 -> data.homesPlayerValue8;
            case 9 -> data.homesPlayerValue9;
            default -> null;
        };
    }

    private static boolean isApplyHomesPlayerValueRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.applyHomesPlayerValue0 != null;
            case 1 -> data.applyHomesPlayerValue1 != null;
            case 2 -> data.applyHomesPlayerValue2 != null;
            case 3 -> data.applyHomesPlayerValue3 != null;
            case 4 -> data.applyHomesPlayerValue4 != null;
            case 5 -> data.applyHomesPlayerValue5 != null;
            case 6 -> data.applyHomesPlayerValue6 != null;
            case 7 -> data.applyHomesPlayerValue7 != null;
            case 8 -> data.applyHomesPlayerValue8 != null;
            case 9 -> data.applyHomesPlayerValue9 != null;
            default -> false;
        };
    }

    private static boolean isViewHomesForPlayerRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.viewHomesForPlayer0 != null;
            case 1 -> data.viewHomesForPlayer1 != null;
            case 2 -> data.viewHomesForPlayer2 != null;
            case 3 -> data.viewHomesForPlayer3 != null;
            case 4 -> data.viewHomesForPlayer4 != null;
            case 5 -> data.viewHomesForPlayer5 != null;
            case 6 -> data.viewHomesForPlayer6 != null;
            case 7 -> data.viewHomesForPlayer7 != null;
            case 8 -> data.viewHomesForPlayer8 != null;
            case 9 -> data.viewHomesForPlayer9 != null;
            default -> false;
        };
    }

    private static boolean isViewIslandForPlayerRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.viewIslandForPlayer0 != null;
            case 1 -> data.viewIslandForPlayer1 != null;
            case 2 -> data.viewIslandForPlayer2 != null;
            case 3 -> data.viewIslandForPlayer3 != null;
            case 4 -> data.viewIslandForPlayer4 != null;
            case 5 -> data.viewIslandForPlayer5 != null;
            case 6 -> data.viewIslandForPlayer6 != null;
            case 7 -> data.viewIslandForPlayer7 != null;
            case 8 -> data.viewIslandForPlayer8 != null;
            case 9 -> data.viewIslandForPlayer9 != null;
            default -> false;
        };
    }

    private static String getLevelsBlockIdDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.levelsBlockId0;
            case 1 -> data.levelsBlockId1;
            case 2 -> data.levelsBlockId2;
            case 3 -> data.levelsBlockId3;
            case 4 -> data.levelsBlockId4;
            default -> null;
        };
    }

    private static String getLevelsBlockValueDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.levelsBlockValue0;
            case 1 -> data.levelsBlockValue1;
            case 2 -> data.levelsBlockValue2;
            case 3 -> data.levelsBlockValue3;
            case 4 -> data.levelsBlockValue4;
            default -> null;
        };
    }

    private static boolean isApplyLevelsBlockRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.applyLevelsBlock0 != null;
            case 1 -> data.applyLevelsBlock1 != null;
            case 2 -> data.applyLevelsBlock2 != null;
            case 3 -> data.applyLevelsBlock3 != null;
            case 4 -> data.applyLevelsBlock4 != null;
            default -> false;
        };
    }

    private static boolean isDeleteLevelsBlockRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.deleteLevelsBlock0 != null;
            case 1 -> data.deleteLevelsBlock1 != null;
            case 2 -> data.deleteLevelsBlock2 != null;
            case 3 -> data.deleteLevelsBlock3 != null;
            case 4 -> data.deleteLevelsBlock4 != null;
            default -> false;
        };
    }

    private void buildOreGenRows(UICommandBuilder commandBuilder, String searchQuery) {
        Map<String, Double> ores = plugin.getAddonManager().getOreGeneratorAddon().getOresSnapshot();
        oreSlots = new ArrayList<>();
        String normalizedSearch = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Double> entry : ores.entrySet()) {
            if (oreSlots.size() >= MAX_ORE_GEN_ROWS) {
                break;
            }
            String blockId = entry.getKey();
            double weight = entry.getValue();
            if (blockId == null || blockId.isBlank()) {
                continue;
            }
            if (!normalizedSearch.isBlank() && !blockId.toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
                continue;
            }
            oreSlots.add(new OreSlot(blockId, weight));
        }

        for (int i = 0; i < MAX_ORE_GEN_ROWS; i++) {
            if (i < oreSlots.size()) {
                OreSlot slot = oreSlots.get(i);
                String idDraft = oreBlockIdDraftInputs.get(slot.blockId);
                String weightDraft = oreWeightDraftInputs.get(slot.blockId);
                String idDisplay = (idDraft == null || idDraft.isBlank()) ? slot.blockId : idDraft;
                String weightDisplay = (weightDraft == null || weightDraft.isBlank()) ? formatDouble(slot.weight) : weightDraft;
                boolean dirty = (idDraft != null && !idDraft.isBlank() && !idDraft.trim().equals(slot.blockId))
                    || (weightDraft != null && !weightDraft.isBlank() && !weightDraft.trim().equals(formatDouble(slot.weight)));

                commandBuilder.set("#OreGenRow" + i + ".Visible", true);
                commandBuilder.set("#OreGenBlockIdField" + i + ".Value", idDisplay);
                commandBuilder.set("#OreGenWeightField" + i + ".Value", weightDisplay);
                commandBuilder.set("#OreGenBlockIdFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#OreGenWeightFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#OreGenOreApplyBox" + i + ".Visible", dirty);
                commandBuilder.set("#OreGenOreDelete" + i + ".Text", uiText(getAdminDashboardLanguage(), "ui.common.delete"));
            } else {
                commandBuilder.set("#OreGenRow" + i + ".Visible", false);
            }
        }
    }

    private void handleApplyOreGenOreChance(@Nonnull Ref<EntityStore> ref,
                                            @Nonnull Store<EntityStore> store,
                                            @Nonnull Player player) {
        String raw = oreGenOreChanceInput == null ? "" : oreGenOreChanceInput.trim();
        if (raw.isBlank()) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_ore_chance", Map.of("value", ""));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        double value;
        try {
            value = Double.parseDouble(raw.replace(',', '.'));
        } catch (Exception ignored) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_ore_chance", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (value < 0D || value > 1D || Double.isNaN(value) || Double.isInfinite(value)) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_ore_chance", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getSettingsCommandHandler().handleOreGeneratorOreChanceSetting(value);
        oreGenOreChanceInput = null;
        oreGenOreChanceDirtyShown = false;
        oreGenError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyOreGenRegenDelay(@Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Player player) {
        String raw = oreGenRegenDelayInput == null ? "" : oreGenRegenDelayInput.trim();
        if (raw.isBlank()) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_regen_delay", Map.of("value", ""));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        long value;
        try {
            value = Long.parseLong(raw);
        } catch (Exception ignored) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_regen_delay", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (value < 0) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_regen_delay", Map.of("value", raw));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getSettingsCommandHandler().handleOreGeneratorRegenDelaySetting(value);
        oreGenRegenDelayInput = null;
        oreGenRegenDelayDirtyShown = false;
        oreGenError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleCreateOreGenOre(@Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull Player player) {
        String rawBlockId = oreGenCreateBlockIdInput == null ? "" : oreGenCreateBlockIdInput.trim();
        String rawWeight = oreGenCreateWeightInput == null ? "" : oreGenCreateWeightInput.trim();
        if (rawBlockId.isBlank()) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.block_id_required");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        double weight;
        try {
            weight = Double.parseDouble(rawWeight.replace(',', '.'));
        } catch (Exception ignored) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_weight", Map.of("value", rawWeight));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (weight <= 0D || Double.isNaN(weight) || Double.isInfinite(weight)) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_weight", Map.of("value", rawWeight));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getAddonManager().getOreGeneratorAddon().addOre(rawBlockId, weight);
        oreGenCreateBlockIdInput = "";
        oreGenCreateWeightInput = "";
        oreGenError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyOreGenOre(int index,
                                      @Nonnull Ref<EntityStore> ref,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull Player player) {
        if (index < 0 || index >= oreSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        OreSlot slot = oreSlots.get(index);
        String rawBlockId = oreBlockIdDraftInputs.get(slot.blockId);
        String rawWeight = oreWeightDraftInputs.get(slot.blockId);

        String blockId = (rawBlockId == null || rawBlockId.isBlank()) ? slot.blockId : rawBlockId.trim();
        String weightInput = (rawWeight == null || rawWeight.isBlank()) ? formatDouble(slot.weight) : rawWeight.trim();

        double weight;
        try {
            weight = Double.parseDouble(weightInput.replace(',', '.'));
        } catch (Exception ignored) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_weight", Map.of("value", weightInput));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (weight <= 0D || Double.isNaN(weight) || Double.isInfinite(weight)) {
            oreGenError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_weight", Map.of("value", weightInput));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }

        if (!blockId.equals(slot.blockId)) {
            plugin.getAddonManager().getOreGeneratorAddon().removeOre(slot.blockId);
        }
        plugin.getAddonManager().getOreGeneratorAddon().updateOre(slot.blockId, blockId, weight);
        oreBlockIdDraftInputs.remove(slot.blockId);
        oreWeightDraftInputs.remove(slot.blockId);
        oreGenError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleDeleteOreGenOre(int index,
                                       @Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull Player player) {
        if (index < 0 || index >= oreSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        OreSlot slot = oreSlots.get(index);
        plugin.getAddonManager().getOreGeneratorAddon().removeOre(slot.blockId);
        oreBlockIdDraftInputs.remove(slot.blockId);
        oreWeightDraftInputs.remove(slot.blockId);
        oreGenError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private static String getOreGenBlockIdDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.oreGenBlockId0;
            case 1 -> data.oreGenBlockId1;
            case 2 -> data.oreGenBlockId2;
            case 3 -> data.oreGenBlockId3;
            case 4 -> data.oreGenBlockId4;
            case 5 -> data.oreGenBlockId5;
            case 6 -> data.oreGenBlockId6;
            case 7 -> data.oreGenBlockId7;
            case 8 -> data.oreGenBlockId8;
            case 9 -> data.oreGenBlockId9;
            default -> null;
        };
    }

    private static String getOreGenWeightDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.oreGenWeight0;
            case 1 -> data.oreGenWeight1;
            case 2 -> data.oreGenWeight2;
            case 3 -> data.oreGenWeight3;
            case 4 -> data.oreGenWeight4;
            case 5 -> data.oreGenWeight5;
            case 6 -> data.oreGenWeight6;
            case 7 -> data.oreGenWeight7;
            case 8 -> data.oreGenWeight8;
            case 9 -> data.oreGenWeight9;
            default -> null;
        };
    }

    private static boolean isApplyOreGenOreRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.applyOreGenOre0 != null;
            case 1 -> data.applyOreGenOre1 != null;
            case 2 -> data.applyOreGenOre2 != null;
            case 3 -> data.applyOreGenOre3 != null;
            case 4 -> data.applyOreGenOre4 != null;
            case 5 -> data.applyOreGenOre5 != null;
            case 6 -> data.applyOreGenOre6 != null;
            case 7 -> data.applyOreGenOre7 != null;
            case 8 -> data.applyOreGenOre8 != null;
            case 9 -> data.applyOreGenOre9 != null;
            default -> false;
        };
    }

    private static boolean isDeleteOreGenOreRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.deleteOreGenOre0 != null;
            case 1 -> data.deleteOreGenOre1 != null;
            case 2 -> data.deleteOreGenOre2 != null;
            case 3 -> data.deleteOreGenOre3 != null;
            case 4 -> data.deleteOreGenOre4 != null;
            case 5 -> data.deleteOreGenOre5 != null;
            case 6 -> data.deleteOreGenOre6 != null;
            case 7 -> data.deleteOreGenOre7 != null;
            case 8 -> data.deleteOreGenOre8 != null;
            case 9 -> data.deleteOreGenOre9 != null;
            default -> false;
        };
    }

    private void buildOreGenTierRows(UICommandBuilder commandBuilder) {
        List<OreGeneratorAddonConfig.GeneratorTier> tiers = plugin.getAddonManager().getOreGeneratorAddon().getTiersSnapshot();
        tierSlots = new ArrayList<>();
        for (OreGeneratorAddonConfig.GeneratorTier tier : tiers) {
            if (tierSlots.size() >= MAX_TIER_ROWS) break;
            if (tier.getName() == null || tier.getName().isBlank()) continue;
            tierSlots.add(new TierSlot(tier.getName(), tier.getPermission() != null ? tier.getPermission() : "", tier.getOreChance() != null ? tier.getOreChance() : 0.0));
        }
        for (int i = 0; i < MAX_TIER_ROWS; i++) {
            if (i < tierSlots.size()) {
                TierSlot slot = tierSlots.get(i);
                String nameDraft = tierNameDraftInputs.get(slot.name);
                String chanceDraft = tierChanceDraftInputs.get(slot.name);
                String nameDisplay = (nameDraft == null || nameDraft.isBlank()) ? slot.name : nameDraft;
                String permDisplay = buildOreGenTierPermission(nameDisplay);
                String chanceDisplay = (chanceDraft == null || chanceDraft.isBlank()) ? formatDouble(slot.oreChance) : chanceDraft;
                boolean dirty = (nameDraft != null && !nameDraft.isBlank() && !nameDraft.trim().equals(slot.name))
                    || !permDisplay.equals(slot.permission)
                    || (chanceDraft != null && !chanceDraft.isBlank() && !chanceDraft.trim().equals(formatDouble(slot.oreChance)));
                commandBuilder.set("#OreGenTierRow" + i + ".Visible", true);
                commandBuilder.set("#OreGenTierNameField" + i + ".Value", nameDisplay);
                commandBuilder.set("#OreGenTierPermText" + i + ".Text", permDisplay);
                commandBuilder.set("#OreGenTierChanceField" + i + ".Value", chanceDisplay);
                commandBuilder.set("#OreGenTierNameFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#OreGenTierPermFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#OreGenTierChanceFieldBorder" + i + ".Background", dirty ? "#f2c94c" : TEMPLATE_BG_ENABLED);
                commandBuilder.set("#OreGenTierApplyBox" + i + ".Visible", dirty);
                commandBuilder.set("#OreGenTierDelete" + i + ".Text", uiText(getAdminDashboardLanguage(), "ui.common.delete"));
            } else {
                commandBuilder.set("#OreGenTierRow" + i + ".Visible", false);
            }
        }
    }

    private void handleCreateOreGenTier(@Nonnull Ref<EntityStore> ref,
                                        @Nonnull Store<EntityStore> store,
                                        @Nonnull Player player) {
        String rawName = oreGenTierCreateNameInput == null ? "" : oreGenTierCreateNameInput.trim();
        String rawChance = oreGenTierCreateChanceInput == null ? "" : oreGenTierCreateChanceInput.trim();
        if (rawName.isBlank()) {
            oreGenTierError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.tier_name_required");
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        for (TierSlot slot : tierSlots) {
            if (rawName.equals(slot.name)) {
                oreGenTierError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.tier_name_duplicate", Map.of("name", rawName));
                player.getPageManager().openCustomPage(ref, store, this);
                return;
            }
        }
        double chance;
        try {
            chance = Double.parseDouble(rawChance.replace(',', '.'));
        } catch (Exception ignored) {
            oreGenTierError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_tier_ore_chance", Map.of("value", rawChance));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (chance < 0D || chance > 1D || Double.isNaN(chance) || Double.isInfinite(chance)) {
            oreGenTierError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_tier_ore_chance", Map.of("value", rawChance));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        String permission = buildOreGenTierPermission(rawName);
        plugin.getAddonManager().getOreGeneratorAddon().addTier(rawName, permission, chance);
        oreGenTierCreateNameInput = "";
        oreGenTierCreateChanceInput = "";
        oreGenTierError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleApplyOreGenTier(int index,
                                       @Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull Player player) {
        if (index < 0 || index >= tierSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        TierSlot slot = tierSlots.get(index);
        String rawName = tierNameDraftInputs.get(slot.name);
        String rawChance = tierChanceDraftInputs.get(slot.name);
        String name = (rawName == null || rawName.isBlank()) ? slot.name : rawName.trim();
        String perm = buildOreGenTierPermission(name);
        String chanceInput = (rawChance == null || rawChance.isBlank()) ? formatDouble(slot.oreChance) : rawChance.trim();
        double chance;
        try {
            chance = Double.parseDouble(chanceInput.replace(',', '.'));
        } catch (Exception ignored) {
            oreGenTierError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_tier_ore_chance", Map.of("value", chanceInput));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        if (chance < 0D || chance > 1D || Double.isNaN(chance) || Double.isInfinite(chance)) {
            oreGenTierError = uiText(getAdminDashboardLanguage(), "ui.admin_dashboard.ore_generator.error.invalid_tier_ore_chance", Map.of("value", chanceInput));
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        plugin.getAddonManager().getOreGeneratorAddon().updateTier(slot.name, name, perm, chance);
        tierNameDraftInputs.remove(slot.name);
        tierChanceDraftInputs.remove(slot.name);
        oreGenTierError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private void handleDeleteOreGenTier(int index,
                                        @Nonnull Ref<EntityStore> ref,
                                        @Nonnull Store<EntityStore> store,
                                        @Nonnull Player player) {
        if (index < 0 || index >= tierSlots.size()) {
            player.getPageManager().openCustomPage(ref, store, this);
            return;
        }
        TierSlot slot = tierSlots.get(index);
        plugin.getAddonManager().getOreGeneratorAddon().removeTier(slot.name);
        tierNameDraftInputs.remove(slot.name);
        tierChanceDraftInputs.remove(slot.name);
        oreGenTierError = null;
        player.getPageManager().openCustomPage(ref, store, this);
    }

    private static String getOreGenTierNameDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.oreGenTierName0;
            case 1 -> data.oreGenTierName1;
            case 2 -> data.oreGenTierName2;
            case 3 -> data.oreGenTierName3;
            case 4 -> data.oreGenTierName4;
            default -> null;
        };
    }

    private static String getOreGenTierChanceDraftValue(PageData data, int index) {
        return switch (index) {
            case 0 -> data.oreGenTierChance0;
            case 1 -> data.oreGenTierChance1;
            case 2 -> data.oreGenTierChance2;
            case 3 -> data.oreGenTierChance3;
            case 4 -> data.oreGenTierChance4;
            default -> null;
        };
    }

    private static boolean isApplyOreGenTierRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.applyOreGenTier0 != null;
            case 1 -> data.applyOreGenTier1 != null;
            case 2 -> data.applyOreGenTier2 != null;
            case 3 -> data.applyOreGenTier3 != null;
            case 4 -> data.applyOreGenTier4 != null;
            default -> false;
        };
    }

    private static boolean isDeleteOreGenTierRequested(PageData data, int index) {
        return switch (index) {
            case 0 -> data.deleteOreGenTier0 != null;
            case 1 -> data.deleteOreGenTier1 != null;
            case 2 -> data.deleteOreGenTier2 != null;
            case 3 -> data.deleteOreGenTier3 != null;
            case 4 -> data.deleteOreGenTier4 != null;
            default -> false;
        };
    }

    private String getAdminDashboardLanguage() {
        return plugin.getConfigManager().getConfig().getLanguage();
    }

    /** Called by AdminChoosePlayerPage — unused in this dashboard but kept for compatibility. */
    public void onPlayerChosen(UUID playerUuid, String playerName) {
        // No-op: this dashboard does not use a selected player
    }
}
