package org.company.config.properties;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class EnvUtility {
    /**
     * Retrieves a string value from an environment variable.
     * If the variable is not set or is blank, the given default is returned.
     *
     * @param key          the name of the environment variable
     * @param defaultValue the fallback value
     * @return the trimmed environment value, or the default
     */
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    /**
     * Parses a string in format "name1:url1,name2:url2" into a list of ServerProperties.
     * Returns null if the env var is not set.
     */
    public static List<ServerProperties> parseServerListFromEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        List<ServerProperties> list = new ArrayList<>();
        String[] items = value.split(",");
        for (String item : items) {
            String[] parts = item.trim().split(":", 2);
            if (parts.length == 2) {
                list.add(new ServerProperties(parts[0].trim(), parts[1].trim(), false));
            } else {
                log.warn("Skipping invalid server entry in env {}: '{}'", key, item);
            }
        }
        return list;
    }
}
