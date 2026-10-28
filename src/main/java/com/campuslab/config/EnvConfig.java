package com.campuslab.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads configuration from OS environment variables first, then a local .env file. */
public final class EnvConfig {
    private static volatile Map<String, String> fileValues;

    private EnvConfig() {}

    public static String get(String key, String defaultValue) {
        String processValue = System.getenv(key);
        if (processValue != null && !processValue.isBlank()) return processValue.trim();
        Map<String, String> values = loadFileOnce();
        String fileValue = values.get(key);
        return fileValue == null || fileValue.isBlank() ? defaultValue : fileValue;
    }

    private static Map<String, String> loadFileOnce() {
        if (fileValues != null) return fileValues;
        synchronized (EnvConfig.class) {
            if (fileValues != null) return fileValues;
            Map<String, String> values = new LinkedHashMap<>();
            Path envFile = configuredEnvPath();
            if (envFile == null) envFile = findEnvFile();
            if (envFile != null && Files.isRegularFile(envFile)) {
                try {
                    for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                        String line = raw.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int split = line.indexOf('=');
                        if (split < 1) continue;
                        String key = line.substring(0, split).trim();
                        String value = line.substring(split + 1).trim();
                        if (value.length() >= 2) {
                            char first = value.charAt(0), last = value.charAt(value.length() - 1);
                            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                                value = value.substring(1, value.length() - 1);
                            }
                        }
                        values.put(key, value);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Cannot read .env file at " + envFile + ". Check file permissions.", ex);
                }
            }
            fileValues = Map.copyOf(values);
            return fileValues;
        }
    }

    private static Path configuredEnvPath() {
        String property = System.getProperty("campuslab.env");
        if (property != null && !property.isBlank()) return Path.of(property).toAbsolutePath().normalize();
        String fromEnvironment = System.getenv("CAMPUSLAB_ENV_FILE");
        if (fromEnvironment != null && !fromEnvironment.isBlank()) return Path.of(fromEnvironment).toAbsolutePath().normalize();
        return null;
    }

    private static Path findEnvFile() {
        Path cursor = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 8 && cursor != null; i++, cursor = cursor.getParent()) {
            Path candidate = cursor.resolve(".env");
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }
}
