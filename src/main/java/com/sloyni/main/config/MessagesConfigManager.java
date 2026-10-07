package com.sloyni.main.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class MessagesConfigManager {
    private static final String FILE_NAME = "messagesConfig.json";

    private final File file;
    private final Gson gson;
    private MessagesConfig config;

    public MessagesConfigManager(File dataFolder) {
        this.file = new File(dataFolder, FILE_NAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.config = load();
    }

    private MessagesConfig load() {
        if (!file.exists()) {
            MessagesConfig defaults = new MessagesConfig();
            save(defaults);
            return defaults;
        }
        try (Reader reader = new FileReader(file)) {
            MessagesConfig loaded = gson.fromJson(reader, MessagesConfig.class);
            return loaded != null ? loaded : new MessagesConfig();
        } catch (IOException ex) {
            return new MessagesConfig();
        }
    }

    private void save(MessagesConfig cfg) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(cfg, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public MessagesConfig getConfig() {
        return config;
    }

    public void reload() {
        this.config = load();
    }
}
