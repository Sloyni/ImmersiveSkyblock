package com.sloyni.main.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageManager {
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final File langDir;
    private final Gson gson;
    private Map<String, String> active;

    public MessageManager(File dataFolder) {
        this.langDir = new File(dataFolder, "lang");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.active = new LinkedHashMap<>();
        ensureFiles();
    }

    public void ensureFiles() {
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        ensureLangFile("en", readBundledLang("en"));
        ensureLangFile("fr", readBundledLang("fr"));
    }

    public void loadLanguage(String language) {
        this.active = readLang(language);
        if (this.active.isEmpty()) {
            this.active = readLang("en");
        }
        if (this.active.isEmpty()) {
            this.active = readBundledLang("en");
        }
    }

    public String get(String key) {
        return active.getOrDefault(key, key);
    }

    public String get(String key, String language) {
        Map<String, String> lang = readLang(language);
        if (lang.isEmpty()) {
            lang = readLang("en");
        }
        if (lang.isEmpty()) {
            lang = readBundledLang("en");
        }
        return lang.getOrDefault(key, key);
    }

    public String getExact(String key, String language) {
        Map<String, String> lang = readLang(language);
        if (lang.isEmpty()) {
            return null;
        }
        return lang.get(key);
    }

    public synchronized void setTranslation(String language, String key, String value) {
        if (language == null || language.isBlank() || key == null || key.isBlank() || value == null) {
            return;
        }

        String normalizedLanguage = language.trim().toLowerCase();
        Map<String, String> lang = readLang(normalizedLanguage);
        lang.put(key, value);
        writeLang(normalizedLanguage, lang);
    }

    /**
     * Create a new language file prefilled from English defaults.
     * If the file already exists, this is a no-op.
     */
    public synchronized void createLanguageFromEnglish(String code, String displayName) {
        if (code == null || code.isBlank()) return;
        String normalized = code.trim().toLowerCase();
        File file = new File(langDir, normalized + ".json");
        if (file.exists()) return;

        Map<String, String> defaults = readBundledLang("en");
        if (defaults.isEmpty()) {
            defaults = new LinkedHashMap<>();
        }
        // ensure the language display name is present and formatted "Name (ID)"
        String label;
        if (displayName == null || displayName.isBlank()) {
            label = normalized.toUpperCase();
        } else {
            // Strip any trailing parenthesized suffix like " (ES)" the user may have provided
            String trimmed = displayName.trim();
            int paren = trimmed.lastIndexOf('(');
            if (paren > 0 && trimmed.endsWith(")")) {
                // remove the trailing parentheses block
                trimmed = trimmed.substring(0, paren).trim();
            }
            label = trimmed + " (" + normalized.toUpperCase() + ")";
        }
        defaults.put("language.name", label);

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(defaults, writer);
        } catch (IOException ignored) {
        }
    }

    public String get(String key, Map<String, String> vars) {
        String msg = get(key);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    public String get(String key, Map<String, String> vars, String language) {
        String msg = get(key, language);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    public List<String> getAvailableLanguages() {
        if (!langDir.exists()) {
            return List.of("en");
        }
        File[] files = langDir.listFiles((dir, name) -> name != null && name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return List.of("en");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (File file : files) {
            String name = file.getName();
            unique.add(name.substring(0, name.length() - 5).toLowerCase());
        }
        List<String> languages = new ArrayList<>(unique);
        Collections.sort(languages);
        return languages;
    }

    private Map<String, String> readLang(String code) {
        File file = new File(langDir, code + ".json");
        if (!file.exists()) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, String> map = gson.fromJson(reader, MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (IOException ex) {
            return new LinkedHashMap<>();
        }
    }

    private void writeLang(String code, Map<String, String> values) {
        File file = new File(langDir, code + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(values, writer);
        } catch (IOException ignored) {
        }
    }

    private void ensureLangFile(String code, Map<String, String> defaults) {
        File file = new File(langDir, code + ".json");
        Map<String, String> existing = file.exists() ? readLang(code) : new LinkedHashMap<>();
        boolean changed = !file.exists();
        if ("fr".equalsIgnoreCase(code)) {
            changed |= migrateLegacyFrenchIslandMenuAccents(existing, defaults);
        }
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!existing.containsKey(entry.getKey())) {
                existing.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(existing, writer);
            } catch (IOException ignored) {
            }
        }
    }

    private boolean migrateLegacyFrenchIslandMenuAccents(Map<String, String> existing, Map<String, String> defaults) {
        boolean changed = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String key = entry.getKey();
            if (key == null || !(key.startsWith("ui.menu.") || key.startsWith("ui.rename."))) {
                continue;
            }

            String current = existing.get(key);
            String desired = entry.getValue();
            if (current == null || current.isBlank() || desired == null || desired.isBlank() || current.equals(desired)) {
                continue;
            }

            String normalizedCurrent = stripDiacritics(current).toLowerCase(Locale.ROOT);
            String normalizedDesired = stripDiacritics(desired).toLowerCase(Locale.ROOT);
            if (normalizedCurrent.equals(normalizedDesired)) {
                existing.put(key, desired);
                changed = true;
            }
        }
        return changed;
    }

    private String stripDiacritics(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int type = Character.getType(c);
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK) {
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private Map<String, String> readBundledLang(String code) {
        if (code == null || code.isBlank()) {
            return new LinkedHashMap<>();
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        String resourcePath = "/langs/" + normalized + ".json";
        InputStream stream = MessageManager.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, String> map = gson.fromJson(reader, MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (IOException ex) {
            return new LinkedHashMap<>();
        }
    }
}
