package com.sloyni.main.addons;

import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.addons.homes.HomesAddon;
import com.sloyni.main.addons.islandwalls.IslandWallsAddon;
import com.sloyni.main.addons.levels.LevelsAddon;
import com.sloyni.main.addons.mobsmanager.MobsManagerAddon;
import com.sloyni.main.addons.oregenerator.OreGeneratorAddon;
import com.sloyni.main.addons.warps.WarpsAddon;
import com.sloyni.main.config.PluginConfig;
import com.sloyni.main.island.IslandRepository;
import com.sloyni.main.island.IslandService;
import com.sloyni.main.island.IslandTemplateManager;
import com.sloyni.main.addons.warps.IslandWarpRepository;
import com.sloyni.main.addons.homes.PlayerHomeLimitRepository;
import com.sloyni.main.world.WorldManager;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AddonManager {
    public static final String LEVELS_ADDON_KEY = "levels";
    public static final String MOBS_MANAGER_ADDON_KEY = "mobs_manager";
    public static final String WARPS_ADDON_KEY = "warps";
    public static final String HOMES_ADDON_KEY = "homes";
    public static final String ISLAND_WALLS_ADDON_KEY = "island_walls";
    public static final String ORE_GENERATOR_ADDON_KEY = "ore_generator";

    private final ISkyblockPlugin plugin;
    private final LevelsAddon levelsAddon;
    private final MobsManagerAddon mobsManagerAddon;
    private final WarpsAddon warpsAddon;
    private final HomesAddon homesAddon;
    private final IslandWallsAddon islandWallsAddon;
    private final OreGeneratorAddon oreGeneratorAddon;

    public AddonManager(ISkyblockPlugin plugin,
                        File pluginDataFolder,
                        PluginConfig mainConfig,
                        IslandRepository islandRepository,
                        IslandWarpRepository islandWarpRepository,
                        PlayerHomeLimitRepository playerHomeLimitRepository,
                        IslandTemplateManager islandTemplateManager,
                        IslandService islandService,
                        WorldManager worldManager) {
        this.plugin = plugin;
        this.levelsAddon = new LevelsAddon(plugin, pluginDataFolder, mainConfig, islandRepository, islandTemplateManager, islandService);
        this.mobsManagerAddon = new MobsManagerAddon(pluginDataFolder);
        this.warpsAddon = new WarpsAddon(plugin, pluginDataFolder, mainConfig, islandRepository, islandWarpRepository, worldManager, islandService);
        this.homesAddon = new HomesAddon(plugin, pluginDataFolder, mainConfig, islandRepository, playerHomeLimitRepository, worldManager);
        this.islandWallsAddon = new IslandWallsAddon(plugin, pluginDataFolder, mainConfig, islandRepository, worldManager);
        this.oreGeneratorAddon = new OreGeneratorAddon(pluginDataFolder);
    }

    public LevelsAddon getLevelsAddon() {
        return levelsAddon;
    }

    public boolean isLevelsEnabled() {
        return levelsAddon.isEnabled();
    }

    public MobsManagerAddon getMobsManagerAddon() {
        return mobsManagerAddon;
    }

    public boolean isMobsManagerEnabled() {
        return mobsManagerAddon.isEnabled();
    }

    public WarpsAddon getWarpsAddon() {
        return warpsAddon;
    }

    public boolean isWarpsEnabled() {
        return warpsAddon.isEnabled();
    }

    public HomesAddon getHomesAddon() {
        return homesAddon;
    }

    public boolean isHomesEnabled() {
        return homesAddon.isEnabled();
    }

    public IslandWallsAddon getIslandWallsAddon() {
        return islandWallsAddon;
    }

    public boolean isIslandWallsEnabled() {
        return islandWallsAddon.isEnabled();
    }

    public OreGeneratorAddon getOreGeneratorAddon() {
        return oreGeneratorAddon;
    }

    public boolean isOreGeneratorEnabled() {
        return oreGeneratorAddon.isEnabled();
    }

    public boolean isAddonEnabled(String addonKey) {
        return switch (normalizeKey(addonKey)) {
            case LEVELS_ADDON_KEY -> levelsAddon.isEnabled();
            case MOBS_MANAGER_ADDON_KEY -> mobsManagerAddon.isEnabled();
            case WARPS_ADDON_KEY -> warpsAddon.isEnabled();
            case HOMES_ADDON_KEY -> homesAddon.isEnabled();
            case ISLAND_WALLS_ADDON_KEY -> islandWallsAddon.isEnabled();
            case ORE_GENERATOR_ADDON_KEY -> oreGeneratorAddon.isEnabled();
            default -> false;
        };
    }

    public boolean setAddonEnabled(String addonKey, boolean enabled) {
        boolean changed = switch (normalizeKey(addonKey)) {
            case LEVELS_ADDON_KEY -> {
                levelsAddon.setEnabled(enabled);
                yield true;
            }
            case MOBS_MANAGER_ADDON_KEY -> {
                mobsManagerAddon.setEnabled(enabled);
                yield true;
            }
            case WARPS_ADDON_KEY -> {
                warpsAddon.setEnabled(enabled);
                yield true;
            }
            case HOMES_ADDON_KEY -> {
                homesAddon.setEnabled(enabled);
                yield true;
            }
            case ISLAND_WALLS_ADDON_KEY -> {
                islandWallsAddon.setEnabled(enabled);
                yield true;
            }
            case ORE_GENERATOR_ADDON_KEY -> {
                oreGeneratorAddon.setEnabled(enabled);
                yield true;
            }
            default -> false;
        };

        if (changed) {
            plugin.refreshAddonCommandTrees();
        }
        return changed;
    }

    public Map<String, Boolean> getAddonStates() {
        Map<String, Boolean> states = new LinkedHashMap<>();
        states.put(LEVELS_ADDON_KEY, levelsAddon.isEnabled());
        states.put(MOBS_MANAGER_ADDON_KEY, mobsManagerAddon.isEnabled());
        states.put(WARPS_ADDON_KEY, warpsAddon.isEnabled());
        states.put(HOMES_ADDON_KEY, homesAddon.isEnabled());
        states.put(ISLAND_WALLS_ADDON_KEY, islandWallsAddon.isEnabled());
        states.put(ORE_GENERATOR_ADDON_KEY, oreGeneratorAddon.isEnabled());
        return states;
    }

    public boolean managesIslandSubcommand(String subcommand) {
        String normalized = normalizeKey(subcommand);
        return "value".equals(normalized)
            || "level".equals(normalized)
            || "top".equals(normalized)
            || "warps".equals(normalized)
            || "warp".equals(normalized)
            || "home".equals(normalized)
            || "walls".equals(normalized)
            || "walls_color".equals(normalized)
            || "walls_color_alternate".equals(normalized);
    }

    public boolean managesSettingsSubcommand(String subcommand) {
        String normalized = normalizeKey(subcommand);
        return "template_value".equals(normalized)
            || "level_value".equals(normalized)
            || "block_default_value".equals(normalized)
            || "max_homes".equals(normalized)
            || "reduce_mobs".equals(normalized)
            || "reduce_mobs_rate".equals(normalized)
            || "torch_protection".equals(normalized)
            || "torch_protection_radius".equals(normalized)
            || "ore_chance".equals(normalized)
            || "regen_delay".equals(normalized)
            || "spawn_rubble".equals(normalized);
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}