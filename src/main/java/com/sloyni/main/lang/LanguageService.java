package com.sloyni.main.lang;

import com.sloyni.main.config.ConfigManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LanguageService {
    private static final Map<String, String> LANGUAGE_LABELS = buildLanguageLabels();

    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public LanguageService(ConfigManager configManager, MessageManager messageManager) {
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    public String resolveInputLanguage(String requestedLanguage) {
        List<String> supported = getSupportedLanguages();
        String normalizedRequested = normalizeLanguageCode(requestedLanguage);

        if (!normalizedRequested.isBlank()) {
            for (String supportedLanguage : supported) {
                if (supportedLanguage.equals(normalizedRequested)) {
                    return supportedLanguage;
                }
            }

            for (String supportedLanguage : supported) {
                if (normalizedRequested.startsWith(supportedLanguage + "-")) {
                    return supportedLanguage;
                }
            }

            String requestedBase = extractBaseLanguage(normalizedRequested);
            for (String supportedLanguage : supported) {
                if (extractBaseLanguage(supportedLanguage).equals(requestedBase)) {
                    return supportedLanguage;
                }
            }
        }

        return getConfiguredFallbackLanguage();
    }

    public String langLabel(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "Unknown";
        }
        String normalized = languageCode.trim().toLowerCase(Locale.ROOT);
        // Prefer a language.name from the language file if present
        try {
            String fromFile = messageManager.getExact("language.name", normalized);
            if (fromFile != null && !fromFile.isBlank()) {
                return fromFile;
            }
        } catch (Exception ignored) {
        }

        String label = LANGUAGE_LABELS.get(normalized);
        if (label != null && !label.isBlank()) {
            return label;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String getConfiguredFallbackLanguage() {
        String fallback = normalizeLanguageCode(configManager.getConfig().getSettings().getFallbackLanguage());
        if (fallback.isBlank() || !isAvailable(fallback)) {
            fallback = normalizeLanguageCode(configManager.getConfig().getLanguage());
        }
        if (fallback.isBlank() || !isAvailable(fallback)) {
            fallback = "en";
        }
        return fallback;
    }

    private List<String> getSupportedLanguages() {
        List<String> available = messageManager.getAvailableLanguages();
        List<String> enabled = configManager.getConfig().getSettings().getEnabledLanguages();
        List<String> result = new ArrayList<>();

        for (String language : enabled) {
            String normalized = normalizeLanguageCode(language);
            if (!normalized.isBlank() && available.contains(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }

        if (result.isEmpty()) {
            for (String language : available) {
                String normalized = normalizeLanguageCode(language);
                if (!normalized.isBlank() && !result.contains(normalized)) {
                    result.add(normalized);
                }
            }
        }

        return result;
    }

    private boolean isAvailable(String language) {
        return !language.isBlank() && messageManager.getAvailableLanguages().contains(language);
    }

    private static String normalizeLanguageCode(String language) {
        if (language == null) {
            return "";
        }
        return language.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static String extractBaseLanguage(String language) {
        int separator = language.indexOf('-');
        return separator < 0 ? language : language.substring(0, separator);
    }

    private static Map<String, String> buildLanguageLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("en", "English (EN)");
        labels.put("fr", "Francais (FR)");
        return labels;
    }
}
