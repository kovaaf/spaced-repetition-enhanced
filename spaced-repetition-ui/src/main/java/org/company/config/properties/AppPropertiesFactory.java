package org.company.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.company.constants.ENV_CONSTANTS;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.company.config.properties.EnvUtility.parseServerListFromEnv;

/**
 * Factory for creating immutable {@link AppProperties} from YAML and environment variables.
 */
@Slf4j
public final class AppPropertiesFactory {
    private static final String YAML_FILE = "application.yml";

    private AppPropertiesFactory() {}

    public static AppProperties create() {
        RawConfig raw = loadYaml();
        return buildAppProperties(raw);
    }

    // ===== Mutable classes for YAML deserialisation =====

    @Getter
    @Setter
    public static class RawConfig {
        private RawData data;
    }

    @Getter
    @Setter
    public static class RawData {
        private List<RawServer> servers;
    }

    @Getter
    @Setter
    public static class RawServer {
        private String name;
        private String url;
        private boolean isDefault;
    }

    // ===== YAML loading =====

    private static RawConfig loadYaml() {
        try (InputStream yamlIS = AppPropertiesFactory.class.getClassLoader().getResourceAsStream(AppPropertiesFactory.YAML_FILE)) {
            if (yamlIS == null) {
                throw new RuntimeException("application.yml not found in classpath");
            }
            Yaml yaml = new Yaml();
            return yaml.loadAs(yamlIS, RawConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    // ===== Construction logic =====

    private static AppProperties buildAppProperties(RawConfig raw) {
        // Build server list from YAML
        DataProperties data = buildDataProperties(raw.data);

        return new AppProperties(data);
    }

    private static DataProperties buildDataProperties(RawData rawData) {
        List<ServerProperties> servers = new ArrayList<>();
        if (rawData != null && rawData.servers != null) {
            for (RawServer rawServer : rawData.servers) {
                servers.add(buildServerProperties(rawServer));
            }
        }

        // Environment override for servers list
        List<ServerProperties> envServers = parseServerListFromEnv(String.valueOf(ENV_CONSTANTS.DATA_SERVERS));
        if (envServers != null && !envServers.isEmpty()) {
            servers = envServers;
            log.info("Overridden servers from environment variable DATA_SERVERS");
        }

        if (servers.isEmpty()) {
            servers.add(new ServerProperties("local", "localhost:50051", true));
            log.info("No servers configured, using default server: local (localhost:50051)");
        }

        DataProperties data = new DataProperties(servers);
        log.info("Loaded {} servers", data.servers().size());
        return data;
    }

    private static ServerProperties buildServerProperties(RawServer rawServer) {
        return new ServerProperties(rawServer.name, rawServer.url, rawServer.isDefault);
    }
}