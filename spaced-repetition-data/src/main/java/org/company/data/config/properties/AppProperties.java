package org.company.data.config.properties;

/**
 * Immutable top-level configuration for the data service.
 * Contains three independent sections: datasource, grpc, and http server.
 */
public record AppProperties(
        DatasourceProperties datasource,
        GrpcProperties grpc,
        ServerProperties server
) {}