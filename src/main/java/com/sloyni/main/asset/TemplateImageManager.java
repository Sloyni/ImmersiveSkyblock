package com.sloyni.main.asset;

import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TemplateImageManager {
    private static final String ASSET_PACK = "com.sloyni:ISkyblock";

    private final File templateDirectory;
    private final Map<String, String> imageAssetPaths = new HashMap<>();
    private final List<ByteArrayCommonAsset> registeredAssets = new ArrayList<>();

    public TemplateImageManager(File dataFolder) {
        this.templateDirectory = new File(dataFolder, "island_templates");
    }

    public synchronized void init() {
        reload();
    }

    public synchronized void reload() {
        unregisterAssets();
        imageAssetPaths.clear();
        if (!templateDirectory.exists()) {
            templateDirectory.mkdirs();
        }

        CommonAssetModule assetModule = CommonAssetModule.get();
        if (assetModule == null) {
            return;
        }

        File[] templateFolders = templateDirectory.listFiles(File::isDirectory);
        if (templateFolders == null) {
            return;
        }

        for (File templateFolder : templateFolders) {
            File[] pngFiles = templateFolder.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
            if (pngFiles == null) {
                continue;
            }
            for (File pngFile : pngFiles) {
                registerExternalImage(assetModule, templateFolder.getName(), pngFile);
            }
        }

        if (!registeredAssets.isEmpty()) {
            assetModule.sendAssets(new ArrayList<>(registeredAssets), true);
        }
    }

    public synchronized String getImagePath(String templateId, String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }

        String normalized = configuredPath.trim().replace('\\', '/');
        String normalizedKey = normalized.toLowerCase(Locale.ROOT);

        String external = imageAssetPaths.get(normalizedKey);
        if (external != null) {
            return external;
        }

        String fileName = extractFileName(normalized);
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        if (templateId != null && !templateId.isBlank()) {
            String byTemplate = imageAssetPaths.get((templateId + "/" + fileName).toLowerCase(Locale.ROOT));
            if (byTemplate != null) {
                return byTemplate;
            }

            String byTemplatePath = imageAssetPaths.get(("island_templates/" + templateId + "/" + fileName).toLowerCase(Locale.ROOT));
            if (byTemplatePath != null) {
                return byTemplatePath;
            }
        }

        String byFileName = imageAssetPaths.get(fileName.toLowerCase(Locale.ROOT));
        if (byFileName != null) {
            return byFileName;
        }

        return "Textures/" + fileName;
    }

    private void registerExternalImage(CommonAssetModule assetModule, String templateId, File pngFile) {
        try {
            byte[] imageData = Files.readAllBytes(pngFile.toPath());
            String fileName = pngFile.getName();
            String assetPath = "UI/Custom/Textures/IslandTemplates/" + templateId + "/" + fileName;
            ByteArrayCommonAsset asset = new ByteArrayCommonAsset(assetPath, imageData);
            assetModule.addCommonAsset(ASSET_PACK, asset, false);
            registeredAssets.add(asset);

            String uiPath = "Textures/IslandTemplates/" + templateId + "/" + fileName;
            imageAssetPaths.put(fileName.toLowerCase(Locale.ROOT), uiPath);
            imageAssetPaths.put((templateId + "/" + fileName).toLowerCase(Locale.ROOT), uiPath);
            imageAssetPaths.put(("island_templates/" + templateId + "/" + fileName).toLowerCase(Locale.ROOT), uiPath);
        } catch (IOException ignored) {
        }
    }

    private void unregisterAssets() {
        for (ByteArrayCommonAsset asset : registeredAssets) {
            try {
                CommonAssetRegistry.removeCommonAssetByName(ASSET_PACK, asset.getName());
            } catch (Exception ignored) {
            }
        }
        registeredAssets.clear();
    }

    private String extractFileName(String configuredPath) {
        int separatorIndex = configuredPath.lastIndexOf('/');
        return separatorIndex >= 0 ? configuredPath.substring(separatorIndex + 1) : configuredPath;
    }
}