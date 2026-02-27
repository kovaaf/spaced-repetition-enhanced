package org.company.spacedrepetitiondata.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for HikariCP connection pool to PostgreSQL database.
 * Reads configuration from environment variables with fallback defaults.
 */
public final class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    // Environment variable names
    private static final String DB_HOST_ENV = "DATA_SERVICE_DB_HOST";
    private static final String DB_PORT_ENV = "DATA_SERVICE_DB_PORT";
    private static final String DB_NAME_ENV = "DATA_SERVICE_DB_NAME";
    private static final String DB_USER_ENV = "DATA_SERVICE_DB_USER";
    private static final String DB_PASSWORD_ENV = "DATA_SERVICE_DB_PASSWORD";
    private static final String DB_MAX_POOL_SIZE_ENV = "DATA_SERVICE_DB_MAX_POOL_SIZE";
    private static final String DB_SCHEMA_ENV = "DATA_SERVICE_DB_SCHEMA";

    // Default values (development)
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DB_NAME = "spaced-repetition-bot-db";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final String DEFAULT_SCHEMA = "bot";

    // Singleton DataSource instance
    private static volatile DataSource dataSource;

    private DatabaseConfig() {
        // Prevent instantiation
    }

    /**
     * Provides a singleton DataSource configured with HikariCP connection pool.
     * Thread-safe initialization.
     *
     * @return configured DataSource
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Creates and configures a new HikariDataSource from environment variables.
     * Uses sensible defaults for connection pool parameters.
     *
     * @return configured HikariDataSource
     */
    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        // Build JDBC URL from environment variables or defaults
        String host = getEnv(DB_HOST_ENV, DEFAULT_HOST);
        String port = getEnv(DB_PORT_ENV, DEFAULT_PORT);
        String dbName = getEnv(DB_NAME_ENV, DEFAULT_DB_NAME);
        String schema = getEnv(DB_SCHEMA_ENV, DEFAULT_SCHEMA);
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s%s", host, port, dbName,
                schema.isEmpty() ? "" : "?currentSchema=" + schema);
        config.setJdbcUrl(jdbcUrl);

        // Credentials
        config.setUsername(getEnv(DB_USER_ENV, DEFAULT_USER));
        config.setPassword(getEnv(DB_PASSWORD_ENV, DEFAULT_PASSWORD));

        // Connection pool settings
        config.setMaximumPoolSize(getIntEnv(DB_MAX_POOL_SIZE_ENV, DEFAULT_MAX_POOL_SIZE));
        config.setMinimumIdle(Math.min(2, config.getMaximumPoolSize())); // keep a few idle connections
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30)); // 30 seconds
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10)); // 10 minutes
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30)); // 30 minutes
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(10)); // detect leaks after 10 seconds
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SpacedRepetitionDataPool");

        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("socketTimeout", "30");

        logger.info("Configured HikariCP connection pool for database: {} (max pool size: {})",
                jdbcUrl, config.getMaximumPoolSize());
        return new HikariDataSource(config);
    }

    /**
     * Gets environment variable value or returns default if not set.
     *
     * @param name environment variable name
     * @param defaultValue default value if variable not set
     * @return value or default
     */
    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            logger.debug("Environment variable {} not set, using default: {}", name, defaultValue);
            return defaultValue;
        }
        logger.debug("Environment variable {} = {}", name, value);
        return value.trim();
    }

    /**
     * Gets integer environment variable value or returns default if not set.
     *
     * @param name environment variable name
     * @param defaultValue default integer value
     * @return parsed integer or default
     */
    private static int getIntEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for environment variable {}: '{}', using default {}",
                    name, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Closes the DataSource (for application shutdown).
     * Call this method during graceful shutdown.
     */
    public static void closeDataSource() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            logger.info("HikariCP connection pool closed.");
        }
    }
}