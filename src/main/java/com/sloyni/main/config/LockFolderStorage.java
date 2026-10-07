package com.sloyni.main.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class LockFolderStorage {
    public static final String LOCK_DIRECTORY = "lock";

    private LockFolderStorage() {
    }

    public static File ensureLockDirectory(File dataFolder) {
        File lockDirectory = new File(dataFolder, LOCK_DIRECTORY);
        if (!lockDirectory.exists()) {
            lockDirectory.mkdirs();
        }
        return lockDirectory;
    }

    public static File resolveLockedFile(File dataFolder, String fileName) {
        File lockDirectory = ensureLockDirectory(dataFolder);
        File lockedFile = new File(lockDirectory, fileName);
        File legacyFile = new File(dataFolder, fileName);

        if (!lockedFile.exists() && legacyFile.exists()) {
            migrateLegacyFile(legacyFile, lockedFile);
        }

        return lockedFile;
    }

    private static void migrateLegacyFile(File legacyFile, File lockedFile) {
        try {
            Files.move(legacyFile.toPath(), lockedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (IOException ignored) {
        }

        try {
            Files.copy(legacyFile.toPath(), lockedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(legacyFile.toPath());
        } catch (IOException ignored) {
        }
    }
}