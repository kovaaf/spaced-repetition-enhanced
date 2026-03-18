package org.company.spacedrepetitiondata.config.properties;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility for reading environment variables.
 */
@Slf4j
public final class EnvUtility {
    private EnvUtility() {}

    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    public static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid integer in env var {}: '{}', using default", key, value);
            }
        }
        return defaultValue;
    }
}