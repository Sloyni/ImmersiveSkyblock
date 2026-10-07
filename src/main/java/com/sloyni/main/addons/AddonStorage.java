package com.sloyni.main.addons;

import com.sloyni.main.config.LockFolderStorage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class AddonStorage {
    private static final String ADDONS_DIRECTORY = "addons";

    private AddonStorage() {
    }

    public static File resolveAddonDirectory(File pluginDataFolder, String addonKey) {
        File addonsDirectory = new File(pluginDataFolder, ADDONS_DIRECTORY);
        if (!addonsDirectory.exists()) {
            addonsDirectory.mkdirs();
        }

        File addonDirectory = new File(addonsDirectory, addonKey);
        if (!addonDirectory.exists()) {
            addonDirectory.mkdirs();
        }
        return addonDirectory;
    }

    public static File resolveAddonLockedFile(File pluginDataFolder, String addonKey, String fileName) {
        return LockFolderStorage.resolveLockedFile(resolveAddonDirectory(pluginDataFolder, addonKey), fileName);
    }

    public static File resolveAddonConfigFile(File pluginDataFolder, String addonKey) {
        return new File(resolveAddonDirectory(pluginDataFolder, addonKey), "config.json");
    }

    public static File resolveAddonDangerousConfigFile(File pluginDataFolder, String addonKey) {
        File addonDirectory = resolveAddonDirectory(pluginDataFolder, addonKey);
        return new File(LockFolderStorage.ensureLockDirectory(addonDirectory), "config-dangerous.json");
    }

    public static void migrateLegacyFile(File legacyFile, File targetFile) {
        if (legacyFile == null || targetFile == null || !legacyFile.exists() || targetFile.exists()) {
            return;
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            Files.move(legacyFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (IOException ignored) {
        }

        try {
            Files.copy(legacyFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(legacyFile.toPath());
        } catch (IOException ignored) {
        }
    }
}