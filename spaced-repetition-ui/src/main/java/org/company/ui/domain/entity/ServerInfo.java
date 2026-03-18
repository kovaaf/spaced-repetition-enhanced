package org.company.ui.domain.entity;

/**
 * Immutable record holding server connection information.
 *
 * @param name human-readable server name (e.g., "Production")
 * @param url  gRPC server address (e.g., "localhost:50051")
 */
public record ServerInfo(String name, String url) {}
