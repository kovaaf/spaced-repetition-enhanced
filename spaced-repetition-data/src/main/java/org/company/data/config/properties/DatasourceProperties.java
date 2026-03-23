package org.company.data.config.properties;

/**
 * Immutable configuration for the database connection pool.
 */
public record DatasourceProperties(
        String host,
        int port,
        String name,
        String username,
        String password,
        String schema,
        int maxPoolSize
) {}