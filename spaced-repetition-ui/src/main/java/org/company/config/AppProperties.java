package org.company.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Slf4j
public class AppProperties {
    @Getter
    private final String grpcServerUrl;

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

            // Извлекаем data.service.url
            Map<String, Object> data = (Map<String, Object>) config.get("data");
            if (data == null) {
                throw new RuntimeException("Missing 'data' section in application.yml");
            }
            Map<String, Object> service = (Map<String, Object>) data.get("service");
            if (service == null) {
                throw new RuntimeException("Missing 'service' section under 'data' in application.yml");
            }
            this.grpcServerUrl = (String) service.get("url");
            if (grpcServerUrl == null || grpcServerUrl.isEmpty()) {
                throw new RuntimeException("data.service.url is not configured or empty");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
        log.info("Loaded gRPC server URL: {}", grpcServerUrl);
    }
}