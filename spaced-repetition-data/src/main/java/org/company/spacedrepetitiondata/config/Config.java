package org.company.spacedrepetitiondata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static volatile Config instance;

    private final Map<String, Object> yamlConfig;

    private Config() {
        this.yamlConfig = loadYamlConfig();
        logger.info("Configuration loaded from YAML. Keys: {}", yamlConfig.keySet());
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYamlConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            if (input == null) {
                logger.warn("application.yml not found in classpath, using empty config");
                return Map.of();
            }
            Yaml yaml = new Yaml();
            return yaml.load(input);
        } catch (Exception e) {
            logger.error("Failed to load application.yml", e);
            return Map.of();
        }
    }

    // ---- Database settings ----

    public String getDatabaseHost() {
        return getString("DATA_SERVICE_DB_HOST", "datasource.host", "localhost");
    }

    public int getDatabasePort() {
        return getInt("DATA_SERVICE_DB_PORT", "datasource.port", 5432);
    }

    public String getDatabaseName() {
        return getString("DATA_SERVICE_DB_NAME", "datasource.name", "spaced-repetition-bot-db");
    }

    public String getDatabaseUser() {
        return getString("DATA_SERVICE_DB_USER", "datasource.username", "postgres");
    }

    public String getDatabasePassword() {
        return getString("DATA_SERVICE_DB_PASSWORD", "datasource.password", "postgres");
    }

    public String getDatabaseSchema() {
        return getString("DATA_SERVICE_DB_SCHEMA", "datasource.schema", "bot");
    }

    public int getDatabaseMaxPoolSize() {
        return getInt("DATA_SERVICE_DB_MAX_POOL_SIZE", "datasource.maxPoolSize", 10);
    }

    // ---- gRPC port ----

    public int getGrpcPort() {
        return getInt("DATA_SERVICE_PORT", "grpc.port", 50051);
    }

    // ---- HTTP port for health/metrics ----

    public int getHttpPort() {
        return getInt("HTTP_PORT", "server.port", 8081);
    }

    // ---- Helper methods ----

    private String getString(String envVar, String yamlPath, String defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.trim().isEmpty()) {
            logger.debug("Using env var {} = {}", envVar, envValue);
            return envValue.trim();
        }
        String yamlValue = getYamlString(yamlPath);
        if (yamlValue != null) {
            logger.debug("Using YAML value {} = {}", yamlPath, yamlValue);
            return yamlValue;
        }
        logger.debug("Using default value for {} = {}", envVar, defaultValue);
        return defaultValue;
    }

    private int getInt(String envVar, String yamlPath, int defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.trim().isEmpty()) {
            try {
                int val = Integer.parseInt(envValue.trim());
                logger.debug("Using env var {} = {}", envVar, val);
                return val;
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer in env var {}: '{}', falling back to YAML/default", envVar, envValue);
            }
        }
        Integer yamlValue = getYamlInteger(yamlPath);
        if (yamlValue != null) {
            logger.debug("Using YAML value {} = {}", yamlPath, yamlValue);
            return yamlValue;
        }
        logger.debug("Using default value for {} = {}", envVar, defaultValue);
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private String getYamlString(String path) {
        Object value = getYamlValue(path);
        if (value == null) return null;
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private Integer getYamlInteger(String path) {
        Object value = getYamlValue(path);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("YAML value at '{}' is not an integer: {}", path, value);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object getYamlValue(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = yamlConfig;
        for (String part : parts) {
            if (current == null) return null;
            Object val = current.get(part);
            if (val == null) return null;
            if (val instanceof Map) {
                current = (Map<String, Object>) val;
            } else {
                return val;
            }
        }
        return current; // если путь закончился на Map? но такого не должно быть по логике
    }
}