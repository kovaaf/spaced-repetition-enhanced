package org.company.data.config.properties;

/**
 * Immutable configuration for the HTTP health server.
 */
public record ServerProperties(
        int port
) {}