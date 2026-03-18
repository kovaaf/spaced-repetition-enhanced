package org.company.config.properties;

/**
 * ServerConfig mirrors each entry in the {@code servers} list.
 */
public record ServerProperties(String name, String url, boolean isDefault) { }
