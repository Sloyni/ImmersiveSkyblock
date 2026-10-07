package com.sloyni.main.island;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.prefab.config.SelectionPrefabSerializer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.BsonDocument;
import org.bson.json.JsonWriterSettings;

public class IslandTemplateManager {
    private final File templateDir;
    private final Gson gson;
    private final Map<String, IslandTemplate> templates;
    private TemplateLoadReport lastLoadReport;

    public static final class TemplateLoadReport {
        private final List<String> loadedTemplateIds = new ArrayList<>();
        private final Map<String, String> failedTemplates = new LinkedHashMap<>();

        public List<String> getLoadedTemplateIds() {
            return Collections.unmodifiableList(loadedTemplateIds);
        }

        public Map<String, String> getFailedTemplates() {
            return Collections.unmodifiableMap(failedTemplates);
        }

        private void addLoaded(String id) {
            loadedTemplateIds.add(id);
        }

        private void addFailed(String idOrFolder, String reason) {
            failedTemplates.put(idOrFolder, reason);
        }
    }

    public IslandTemplateManager(File dataFolder) {
        this.templateDir = new File(dataFolder, "island_templates");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.templates = new LinkedHashMap<>();
        this.lastLoadReport = new TemplateLoadReport();
        ensureDefaults();
        loadTemplates();
    }

    public IslandTemplate get(String id) {
        return templates.get(id);
    }

    public IslandTemplate getDefault() {
        return templates.getOrDefault("default", templates.values().stream().findFirst().orElse(null));
    }

    public java.util.List<IslandTemplate> getAll() {
        List<IslandTemplate> all = new ArrayList<>(templates.values());
        all.sort(Comparator
            .comparingInt(IslandTemplate::getDisplayOrder)
            .thenComparing(IslandTemplate::getId, String.CASE_INSENSITIVE_ORDER));
        return all;
    }

    public java.util.List<IslandTemplate> getEnabled() {
        List<IslandTemplate> enabled = new ArrayList<>();
        for (IslandTemplate template : getAll()) {
            if (template != null && template.isEnabled()) {
                enabled.add(template);
            }
        }
        return enabled;
    }

    public synchronized void reloadTemplates() {
        loadTemplates();
    }

    public synchronized TemplateLoadReport getLastLoadReport() {
        return lastLoadReport;
    }

    public boolean hasTemplate(String id) {
        return id != null && templates.containsKey(id);
    }

    public synchronized boolean setTemplateEnabled(String id, boolean enabled) {
        IslandTemplate template = templates.get(id);
        if (template == null) {
            return false;
        }
        template.setEnabled(enabled);
        return saveTemplate(template);
    }

    public synchronized int getNextDisplayOrder() {
        return getAll().stream()
            .mapToInt(IslandTemplate::getDisplayOrder)
            .max()
            .orElse(-1) + 1;
    }

    public synchronized boolean moveTemplateUp(String id) {
        return moveTemplate(id, true);
    }

    public synchronized boolean moveTemplateDown(String id) {
        return moveTemplate(id, false);
    }

    public synchronized boolean deleteTemplate(String id) {
        if (id == null || id.isBlank() || "default".equals(id)) {
            return false;
        }

        File folder = new File(templateDir, id);
        if (!folder.exists()) {
            templates.remove(id);
            return false;
        }

        boolean deleted = deleteRecursively(folder);
        if (deleted) {
            templates.remove(id);
            loadTemplates();
        }
        return deleted;
    }

    public synchronized boolean saveTemplate(IslandTemplate template) {
        if (template == null || template.getId() == null || template.getId().isBlank()) {
            return false;
        }

        template.applyDefaults();
        File folder = new File(templateDir, template.getId());
        File file = new File(folder, template.getId() + ".json");
        try {
            Files.createDirectories(folder.toPath());
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(template, writer);
            }
            templates.put(template.getId(), template);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Save a BlockSelection as a native .prefab.json using the game's
     * SelectionPrefabSerializer (same system as builder tools copy/paste).
     * This preserves blocks, rotation, filler, fluids, chest inventories,
     * sign text, and all block entity data correctly.
     */
    public synchronized boolean saveCapturedPrefabBsdon(String templateId, BlockSelection prefab) {
        if (templateId == null || templateId.isBlank() || prefab == null) {
            return false;
        }

        File folder = new File(templateDir, templateId);
        File file = new File(folder, templateId + ".prefab.json");
        try {
            Files.createDirectories(folder.toPath());
            BsonDocument doc = SelectionPrefabSerializer.serialize(prefab);
            String json = doc.toJson(JsonWriterSettings.builder().indent(true).build());
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Load a native .prefab.json into a BlockSelection using the game's
     * SelectionPrefabSerializer (same system as builder tools copy/paste).
     * Returns null for any file that is not a valid native prefab
     * (including old-format IslandSchematic files, empty files, and malformed JSON).
     */
    public BlockSelection loadCapturedPrefabBsdon(String prefabPathOrTemplateId) {
        if (prefabPathOrTemplateId == null || prefabPathOrTemplateId.isBlank()) {
            return null;
        }

        File file;
        if (prefabPathOrTemplateId.endsWith(".prefab.json")) {
            file = new File(templateDir.getParentFile(), prefabPathOrTemplateId);
        } else {
            File folder = new File(templateDir, prefabPathOrTemplateId);
            file = new File(folder, prefabPathOrTemplateId + ".prefab.json");
        }

        if (!file.exists() || file.length() == 0) {
            return null;
        }
        try {
            String json = Files.readString(file.toPath());
            // Heuristic: old-format IslandSchematic JSON uses "blockId" key;
            // native prefab JSON uses "version" and "blocks" with "name" key.
            if (!json.contains("\"version\"")) {
                return null;
            }
            if (json.contains("\"blockId\"")) {
                return null;
            }
            BsonDocument doc = BsonDocument.parse(json);
            if (!doc.containsKey("version") || !doc.containsKey("blocks")) {
                return null;
            }
            return SelectionPrefabSerializer.deserialize(doc);
        } catch (Exception ex) {
            // Any failure means this isn't a valid native prefab — return null
            return null;
        }
    }

    @Deprecated
    public synchronized boolean saveCapturedPrefab(String templateId, IslandSchematic schematic) {
        if (schematic == null || schematic.getId() == null || schematic.getId().isBlank()) {
            return false;
        }
        if (!schematic.hasBedrockBlock()) {
            return false;
        }

        File folder = new File(templateDir, templateId);
        File file = new File(folder, templateId + ".prefab.json");
        try {
            Files.createDirectories(folder.toPath());
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(schematic, writer);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Deprecated
    public IslandSchematic loadCapturedPrefab(String prefabPathOrTemplateId) {
        if (prefabPathOrTemplateId == null || prefabPathOrTemplateId.isBlank()) {
            return null;
        }

        File file;
        if (prefabPathOrTemplateId.endsWith(".prefab.json")) {
            file = new File(templateDir.getParentFile(), prefabPathOrTemplateId);
        } else {
            File folder = new File(templateDir, prefabPathOrTemplateId);
            file = new File(folder, prefabPathOrTemplateId + ".prefab.json");
        }

        if (!file.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(file)) {
            IslandSchematic schematic = gson.fromJson(reader, IslandSchematic.class);
            if (schematic == null || !schematic.hasBedrockBlock()) {
                return null;
            }
            return schematic;
        } catch (IOException ex) {
            return null;
        }
    }

    private void ensureDefaults() {
        if (!templateDir.exists()) {
            templateDir.mkdirs();
        }

        File defaultFolder = new File(templateDir, "default");
        if (!defaultFolder.exists()) {
            defaultFolder.mkdirs();
        }

        File defaultTemplate = new File(defaultFolder, "default.json");
        if (!defaultTemplate.exists()) {
            IslandTemplate template = new IslandTemplate();
            template.applyDefaults();
            template.setPreviewImage("default.png");
            template.setPrefabName("island.prefab.json");
            ensureDefaultSpawnOffsets(template);
            try (FileWriter writer = new FileWriter(defaultTemplate)) {
                gson.toJson(template, writer);
            } catch (IOException ignored) {
            }
        } else {
            // Migrate: fix wrong prefabName written by a previous version of the plugin
            try (FileReader reader = new FileReader(defaultTemplate)) {
                IslandTemplate existing = gson.fromJson(reader, IslandTemplate.class);
                if (existing != null) {
                    boolean changed = false;
                    String normalizedPreview = IslandTemplate.normalizePreviewImage(existing.getPreviewImage());
                    if (!normalizedPreview.equals(existing.getPreviewImage())) {
                        existing.setPreviewImage(normalizedPreview);
                        changed = true;
                    }
                    if (!"island.prefab.json".equals(existing.getPrefabName())) {
                        existing.setPrefabName("island.prefab.json");
                        changed = true;
                    }
                    if (ensureDefaultSpawnOffsets(existing)) {
                        changed = true;
                    }
                    if (changed) {
                    try (FileWriter writer = new FileWriter(defaultTemplate)) {
                        gson.toJson(existing, writer);
                    }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        // The prefab is copied to data for admin customization; generation uses bundled resources.
        File dataPrefab = new File(defaultFolder, "default.prefab.json");
        if (!dataPrefab.exists()) {
            copyResource("Server/Prefabs/island.prefab.json", dataPrefab);
        }

        File defaultImage = new File(defaultFolder, "default.png");
        if (!defaultImage.exists()) {
            copyResource("Common/UI/Custom/Textures/default.png", defaultImage);
        }
    }

    private void loadTemplates() {
        templates.clear();
        TemplateLoadReport report = new TemplateLoadReport();
        File[] dirs = templateDir.listFiles(File::isDirectory);
        if (dirs == null) {
            this.lastLoadReport = report;
            return;
        }

        for (File dir : dirs) {
            String id = dir.getName();
            File file = new File(dir, id + ".json");
            if (!file.exists()) {
                report.addFailed(id, "Missing template json: " + file.getName());
                continue;
            }
            try (FileReader reader = new FileReader(file)) {
                IslandTemplate t = gson.fromJson(reader, IslandTemplate.class);
                if (t == null) {
                    report.addFailed(id, "Invalid json (deserialized null)");
                    continue;
                }
                if (t.getId() == null || t.getId().isBlank()) {
                    report.addFailed(id, "Template id is missing or blank");
                    continue;
                }
                if (t.getPreviewImage() == null || t.getPreviewImage().isBlank()) {
                    t.setPreviewImage("default.png");
                }
                if (t.getPrefabName() == null || t.getPrefabName().isBlank()) {
                    t.setPrefabName("island_templates/" + id + "/" + id + ".prefab.json");
                }
                t.applyDefaults();

                // Any local/captured template must contain at least one bedrock block.
                if (!"default".equals(t.getId()) && t.getPrefabName() != null
                    && t.getPrefabName().startsWith("island_templates/")) {
                    BlockSelection prefabBsdon = loadCapturedPrefabBsdon(t.getPrefabName());
                    boolean valid = prefabBsdon != null;
                    if (!valid) {
                        // Fallback: try legacy IslandSchematic
                        IslandSchematic schematic = loadCapturedPrefab(t.getPrefabName());
                        valid = (schematic != null && schematic.hasBedrockBlock());
                    }
                    if (!valid) {
                        report.addFailed(id, "Captured prefab missing/invalid or without Rock_Bedrock");
                        continue;
                    }
                }

                if (templates.containsKey(t.getId())) {
                    report.addFailed(id, "Duplicate template id already loaded: " + t.getId());
                    continue;
                }

                templates.put(t.getId(), t);
                report.addLoaded(t.getId());
            } catch (IOException ignored) {
                report.addFailed(id, "IO error while reading template json");
            }
        }

        this.lastLoadReport = report;
    }

    public void ensureTemplateImage(String templateId) {
        File folder = new File(templateDir, templateId);
        File image = new File(folder, templateId + ".png");
        if (!image.exists()) {
            copyResource(IslandTemplate.DEFAULT_PREVIEW_IMAGE, image);
        }
    }

    private void copyResource(String resourcePath, File target) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return;
            }
            Files.createDirectories(target.toPath().getParent());
            try (OutputStream out = Files.newOutputStream(target.toPath())) {
                in.transferTo(out);
            }
        } catch (IOException ignored) {
        }
    }

    private boolean ensureDefaultSpawnOffsets(IslandTemplate template) {
        // Keep a safe known spawn for the bundled default island.
        if (template == null || !"default".equals(template.getId())) {
            return false;
        }
        if (template.getSpawnOffsetX() == 0 && template.getSpawnOffsetY() == 0 && template.getSpawnOffsetZ() == 0) {
            template.setSpawnOffsetX(1);
            template.setSpawnOffsetY(5);
            template.setSpawnOffsetZ(-4);
            return true;
        }
        return false;
    }

    private boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return true;
        }

        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursively(child)) {
                    return false;
                }
            }
        }

        return file.delete();
    }

    private boolean moveTemplate(String id, boolean up) {
        if (id == null || id.isBlank()) {
            return false;
        }

        List<IslandTemplate> ordered = getAll();
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (id.equals(ordered.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }

        int target = up ? index - 1 : index + 1;
        if (target < 0 || target >= ordered.size()) {
            return false;
        }

        IslandTemplate current = ordered.get(index);
        IslandTemplate other = ordered.get(target);
        ordered.set(index, other);
        ordered.set(target, current);

        return persistOrder(ordered);
    }

    private boolean persistOrder(List<IslandTemplate> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setDisplayOrder(i);
        }

        for (IslandTemplate template : ordered) {
            if (!saveTemplate(template)) {
                return false;
            }
        }
        return true;
    }
}
