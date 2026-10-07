package com.sloyni.main.commands.admin;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sloyni.main.ISkyblockPlugin;
import com.sloyni.main.island.IslandSchematic;
import com.sloyni.main.island.IslandTemplate;
import com.sloyni.main.island.IslandTemplateManager;
import com.sloyni.main.util.Chat;
import java.util.Map;
import javax.annotation.Nonnull;

public class TemplateCommand extends AbstractCommandCollection {
    public TemplateCommand(ISkyblockPlugin plugin) {
        super("template", "Manage island templates");
        addSubCommand((AbstractCommand) new CreateTemplateCommand(plugin));
        addSubCommand((AbstractCommand) new RefreshTemplateCommand(plugin));
        addSubCommand((AbstractCommand) new ListTemplateCommand(plugin));
    }

    private static class CreateTemplateCommand extends AbstractPlayerCommand {
        private final ISkyblockPlugin plugin;

        @Nonnull
        @SuppressWarnings("unchecked")
        private final RequiredArg<String> idArg = withRequiredArg(
            "id",
            "Unique template id",
            (ArgumentType<String>) ArgTypes.STRING
        );

        public CreateTemplateCommand(ISkyblockPlugin plugin) {
            super("create", "Create template from in-game selection");
            this.plugin = plugin;
            requirePermission("iskyblock.template.create");
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

            String rawId = idArg.get(context);
            String templateId = normalizeTemplateId(rawId);
            if (templateId == null) {
                Chat.send(context, plugin.tr("template.invalid_id"));
                return;
            }

            if (plugin.getIslandTemplateManager().hasTemplate(templateId)) {
                Chat.send(context, plugin.tr("template.exists"));
                return;
            }

            BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(player, playerRef);
            if (builderState == null) {
                Chat.send(context, plugin.tr("template.selection_unavailable"));
                return;
            }

            Chat.send(context, plugin.tr("template.saving_started"));
            BuilderToolsPlugin.addToQueue(player, playerRef, (r, state, componentAccessor) -> {
                BlockSelection selection = state.getSelection();
                if (selection == null || !selection.hasSelectionBounds()) {
                    Chat.send(context, plugin.tr("template.selection_required"));
                    return;
                }

                Vector3i min = selection.getSelectionMin();
                Vector3i max = selection.getSelectionMax();

                int sizeX = max.x() - min.x() + 1;
                int sizeY = max.y() - min.y() + 1;
                int sizeZ = max.z() - min.z() + 1;

                TransformComponent transform = componentAccessor.getComponent(r, TransformComponent.getComponentType());
                Vector3d playerPos = transform != null ? transform.getPosition() : new Vector3d(min.x(), min.y(), min.z());
                final int spawnOffsetX = clamp((int) Math.floor(playerPos.x()) - min.x(), 0, sizeX - 1);
                final int spawnOffsetY = clamp((int) Math.floor(playerPos.y()) - min.y(), 0, sizeY);
                final int spawnOffsetZ = clamp((int) Math.floor(playerPos.z()) - min.z(), 0, sizeZ - 1);

                // Run on main server thread for safe ECS access
                world.execute(() -> {
                    BlockSelection capturedPrefab = new BlockSelection();
                    capturedPrefab.setAnchor(0, 0, 0);
                    boolean hasBedrock = false;

                    for (int x = min.x(); x <= max.x(); x++) {
                        for (int y = min.y(); y <= max.y(); y++) {
                            for (int z = min.z(); z <= max.z(); z++) {
                                int chunkX = x >> 5;
                                int chunkZ = z >> 5;
                                long chunkIndex = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
                                WorldChunk chunk = world.getChunk(chunkIndex);
                                if (chunk == null) {
                                    continue;
                                }

                                int blockIndex = chunk.getBlock(x, y, z);

                                // Even when there's no solid block (blockIndex==0), there may be a fluid
                                // (water/lava) occupying the position. Capture fluids regardless of
                                // blockIndex so liquids are preserved in the prefab.
                                @SuppressWarnings("deprecation")
                                int fluidIdIndexPre = chunk.getFluidId(x, y, z);
                                @SuppressWarnings("deprecation")
                                byte fluidLevelPre = chunk.getFluidLevel(x, y, z);
                                if (blockIndex == 0) {
                                    if (fluidIdIndexPre > 0 && fluidLevelPre > 0) {
                                        int relX = x - min.x();
                                        int relY = y - min.y();
                                        int relZ = z - min.z();
                                        // Ensure a valid non-zero block palette index for air is present
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

                                BlockType blockType = (BlockType) BlockType.getAssetMap().getAsset(blockIndex);
                                if (blockType == null) {
                                    continue;
                                }

                                int relX = x - min.x();
                                int relY = y - min.y();
                                int relZ = z - min.z();

                                BlockSection section = chunk.getBlockChunk().getSectionAtBlockY(y);
                                int rotation = section != null ? section.getRotationIndex(x, y, z) : 0;
                                int filler = section != null ? section.getFiller(x, y, z) : 0;
                                int support = chunk.getSupportValue(x, y, z);

                                // Get block entity data (chest inventory, sign text, etc.)
                                Holder<ChunkStore> blockEntity = chunk.getBlockComponentHolder(x, y, z);

                                // Add block with full data to native BlockSelection
                                capturedPrefab.addBlockAtLocalPos(relX, relY, relZ, blockIndex, rotation, filler, support, blockEntity);

                                // Track bedrock
                                if (!hasBedrock && IslandSchematic.isBedrockId(blockType.getId())) {
                                    hasBedrock = true;
                                }

                                // Capture fluid (water/lava)
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
                        Chat.send(context, plugin.tr("template.bedrock_required"));
                        return;
                    }

                    // Save using the game's native SelectionPrefabSerializer (same as builder tools copy/paste)
                    boolean saved = plugin.getIslandTemplateManager().saveCapturedPrefabBsdon(templateId, capturedPrefab);
                    if (!saved) {
                        Chat.send(context, plugin.tr("template.save_failed"));
                        return;
                    }

                    IslandTemplate template = new IslandTemplate();
                    template.setId(templateId);
                    template.setTitleKey(generateDefaultTitle(templateId));
                    template.setDescriptionKey("Template generated from in-game selection.");
                    template.setPreviewImage(templateId + ".png");
                    template.setPrefabName("island_templates/" + templateId + "/" + templateId + ".prefab.json");
                    template.setSpawnOffsetX(spawnOffsetX);
                    template.setSpawnOffsetY(spawnOffsetY);
                    template.setSpawnOffsetZ(spawnOffsetZ);
                    template.applyDefaults();

                    boolean templateSaved = plugin.getIslandTemplateManager().saveTemplate(template);
                    plugin.getIslandTemplateManager().ensureTemplateImage(templateId);
                    plugin.reloadTemplateImages();

                    if (!templateSaved) {
                        Chat.send(context, plugin.tr("template.save_failed"));
                        return;
                    }

                    Chat.send(context, plugin.tr("template.save_success")
                        + " id=" + templateId
                        + " size=" + sizeX + "x" + sizeY + "x" + sizeZ);
                });
            });
        }

        private String normalizeTemplateId(String raw) {
            if (raw == null) {
                return null;
            }
            String cleaned = raw.trim().toLowerCase();
            if (cleaned.isEmpty()) {
                return null;
            }
            if (!cleaned.matches("^[a-z0-9_-]{1,48}$")) {
                return null;
            }
            return cleaned;
        }

        private String generateDefaultTitle(String id) {
            String[] parts = id.split("[_-]");
            StringBuilder title = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (title.length() > 0) {
                    title.append(' ');
                }
                title.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    title.append(part.substring(1));
                }
            }
            if (title.length() == 0) {
                return "Custom Template";
            }
            return title.toString();
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static class RefreshTemplateCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        public RefreshTemplateCommand(ISkyblockPlugin plugin) {
            super("refresh", "Reload templates from island_templates folder");
            this.plugin = plugin;
            requirePermission("iskyblock.template.refresh");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            plugin.getIslandTemplateManager().reloadTemplates();
            plugin.reloadTemplateImages();
            Chat.send(context, plugin.tr("template.refresh_done"));
        }
    }

    private static class ListTemplateCommand extends CommandBase {
        private final ISkyblockPlugin plugin;

        public ListTemplateCommand(ISkyblockPlugin plugin) {
            super("list", "List template load success and failures with reasons");
            this.plugin = plugin;
            requirePermission("iskyblock.template.list");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            plugin.getIslandTemplateManager().reloadTemplates();
            IslandTemplateManager.TemplateLoadReport report = plugin.getIslandTemplateManager().getLastLoadReport();

            int loaded = report.getLoadedTemplateIds().size();
            int failed = report.getFailedTemplates().size();
            Chat.send(context, plugin.tr("template.list.summary", Map.of(
                "loaded", String.valueOf(loaded),
                "failed", String.valueOf(failed)
            )));

            if (loaded == 0) {
                Chat.send(context, plugin.tr("template.list.loaded.none"));
            } else {
                Chat.send(context, plugin.tr("template.list.loaded.header"));
                for (String id : report.getLoadedTemplateIds()) {
                    Chat.send(context, "- " + id);
                }
            }

            if (failed == 0) {
                Chat.send(context, plugin.tr("template.list.failed.none"));
            } else {
                Chat.send(context, plugin.tr("template.list.failed.header"));
                for (Map.Entry<String, String> entry : report.getFailedTemplates().entrySet()) {
                    Chat.send(context, "- " + entry.getKey() + " -> " + entry.getValue());
                }
            }
        }
    }
}
