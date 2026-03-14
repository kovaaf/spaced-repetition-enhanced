package org.company.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.ServerInfo;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads application configuration from {@code application.yml} placed in the classpath.
 * Provides a list of available servers and the default server URL.
 */
@Slf4j
public class AppProperties {
    @Getter
    private final List<ServerInfo> servers = new ArrayList<>();
    @Getter
    private final String defaultServerUrl;

    public AppProperties() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            if (inputStream == null) {
                throw new RuntimeException("application.yml not found in classpath");
            }
            Map<String, Object> config = yaml.load(inputStream);
            if (config == null) {
                throw new RuntimeException("application.yml is empty");
            }

            Map<String, Object> data = (Map<String, Object>) config.get("data");
            if (data == null) {
                throw new RuntimeException("Missing 'data' section in application.yml");
            }

            List<Map<String, String>> serversList = (List<Map<String, String>>) data.get("servers");
            if (serversList != null) {
                for (Map<String, String> serverMap : serversList) {
                    String name = serverMap.get("name");
                    String url = serverMap.get("url");
                    if (name != null && url != null) {
                        servers.add(new ServerInfo(name, url));
                    }
                }
            }

            Map<String, Object> service = (Map<String, Object>) data.get("service");
            if (service != null && service.get("url") != null) {
                defaultServerUrl = (String) service.get("url");
            } else if (!servers.isEmpty()) {
                defaultServerUrl = servers.get(0).url();
            } else {
                throw new RuntimeException("No server configuration found in application.yml");
            }

            log.info("Loaded {} servers, default URL: {}", servers.size(), defaultServerUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }
}