package org.company.spacedrepetitiondata.config.properties;

/**
 * Immutable configuration for the gRPC server.
 */
public record GrpcProperties(
        int port
) {}