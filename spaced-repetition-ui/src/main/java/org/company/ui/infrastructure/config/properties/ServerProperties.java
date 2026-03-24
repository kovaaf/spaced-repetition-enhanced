package org.company.ui.infrastructure.config.properties;

/**
 * ServerConfig mirrors each entry in the {@code servers} list.
 */
public record ServerProperties(String name, String url, boolean isDefault) { }
