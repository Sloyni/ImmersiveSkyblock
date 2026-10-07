package com.sloyni.main.menu;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandData;
import com.sloyni.main.addons.levels.IslandValueService;
import com.sloyni.main.island.IslandService;
import javax.annotation.Nonnull;

public class IslandMenuHud extends CustomUIHud {
    public static final String KEY = "iskyblock:island_menu_hud";

    private volatile boolean destroyed = false;

    public IslandMenuHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        if (destroyed) {
            return;
        }
        builder.append("Hud/IslandMenuHud.ui");

        try {
            PlayerRef ref = getPlayerRef();
            ISkyblockPlugin plugin = ISkyblockPlugin.getInstance();
            if (ref == null || plugin == null) {
                return;
            }

            IslandService islandService = plugin.getIslandService();
            IslandData island = islandService == null ? null : islandService.getPlayerIsland(ref.getUuid());

            boolean levelsEnabled = plugin.getAddonManager().isLevelsEnabled();

            if (island == null) {
                // No island: show fallback text
                String lang = plugin.resolvePlayerLanguage(ref);
                String noIsland = plugin.rawText(ref, "ui.menu.hud.no_island");
                if (noIsland == null || noIsland.isBlank()) {
                    noIsland = plugin.trForLanguage(lang, "ui.menu.hud.no_island").toString();
                }
                if (noIsland == null || noIsland.isBlank()) {
                    noIsland = plugin.trForLanguage(lang, "ui.menu.hud.no_island").toString();
                }
                builder.set("#HudFallback.Visible", true);
                builder.set("#LevelsBar.Visible", false);
                builder.set("#HudNoIslandText.Text", noIsland == null ? "Open the menu and create your island" : noIsland);
                // hide image/name
                builder.set("#HudIslandName.Text", "");
                return;
            }

            // Island exists: set name and image
            String islandName = plugin.getIslandService().getIslandDisplayName(island);
            builder.set("#HudIslandName.Text", islandName == null ? "" : islandName);

            if (!levelsEnabled) {
                builder.set("#LevelsBar.Visible", false);
            } else {
                builder.set("#LevelsBar.Visible", true);
                IslandValueService.IslandValueResult cached = plugin.getIslandValueService().getCachedIslandValue(island.getOwnerUuid());
                long level = cached == null ? 0L : cached.level();
                long topRank = 0L;
                var topEntries = plugin.getIslandValueService().getTopCachedIslands(0);
                int rank = 1;
                for (IslandValueService.CachedIslandTopEntry entry : topEntries) {
                    if (island.getOwnerUuid().equals(entry.ownerUuid())) {
                        topRank = rank;
                        break;
                    }
                    rank++;
                }
                builder.set("#HudTopValue.Text", String.valueOf(topRank == 0 ? "-" : String.valueOf(topRank)));
                String levelPrefix = plugin.rawText(ref, "ui.menu.level.prefix");
                if (levelPrefix == null || levelPrefix.isBlank()) {
                    levelPrefix = "Lvl.";
                }
                builder.set("#HudIslandLevel.Text", levelPrefix + " " + level);

                // Levels remaining to reach the next higher-ranked island
                long remainingLevels = 0L;
                try {
                    if (topRank > 1 && topEntries != null && topEntries.size() >= (int) (topRank - 1)) {
                        IslandValueService.CachedIslandTopEntry target = topEntries.get((int) topRank - 2);
                        long targetLevel = target == null ? level : target.level();
                        remainingLevels = Math.max(0L, targetLevel - level);
                    } else {
                        remainingLevels = 0L;
                    }
                } catch (Exception ignore) {
                    remainingLevels = 0L;
                }
                builder.set("#HudNextValue.Text", remainingLevels + " lvls");
            }
        } catch (Exception ex) {
            // Don't crash HUD building
        }
    }

    public void markDestroyed() {
        this.destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    protected void onRemove() {
        markDestroyed();
    }
}
