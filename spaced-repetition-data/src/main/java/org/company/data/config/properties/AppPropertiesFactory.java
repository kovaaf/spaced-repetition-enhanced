package org.company.data.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

/**
 * Factory for creating immutable {@link AppProperties} from YAML and environment variables.
 * <p>
 * The factory loads {@code application.yml}, maps it to mutable intermediate classes,
 * applies environment variable overrides, and finally builds immutable records.
 * </p>
 */
@Slf4j
public final class AppPropertiesFactory {
    private static final String YAML_FILE = "application.yml";

    private AppPropertiesFactory() {}

    public static AppProperties create() {
        RawConfig raw = loadYaml();
        return buildAppProperties(raw);
    }

    // ===== Mutable intermediate classes for YAML deserialisation =====

    @Getter
    @Setter
    public static class RawConfig {
        private RawDatasource datasource;
        private RawGrpc grpc;
        private RawServer server;
    }

    @Getter
    @Setter
    public static class RawDatasource {
        private String host;
        private Integer port;
        private String name;
        private String username;
        private String password;
        private String schema;
        private Integer maxPoolSize;
    }

    @Getter
    @Setter
    public static class RawGrpc {
        private Integer port;
    }

    @Getter
    @Setter
    public static class RawServer {
        private Integer port;
    }

    // ===== YAML loading =====

    private static RawConfig loadYaml() {
        try (InputStream input = AppPropertiesFactory.class.getClassLoader().getResourceAsStream(YAML_FILE)) {
            if (input == null) {
                log.warn("{} not found, using default configuration", YAML_FILE);
                return new RawConfig();
            }
            Yaml yaml = new Yaml();
            return yaml.loadAs(input, RawConfig.class);
        } catch (Exception e) {
            log.error("Failed to load {}, using default configuration", YAML_FILE, e);
            return new RawConfig();
        }
    }

    // ===== Construction logic with environment overrides =====

    private static AppProperties buildAppProperties(RawConfig raw) {
        // Build datasource properties
        DatasourceProperties datasource = buildDatasource(raw.datasource);
        // Build gRPC properties
        GrpcProperties grpc = buildGrpc(raw.grpc);
        // Build HTTP server properties
        ServerProperties server = buildServer(raw.server);

        log.info("Configuration loaded: database={}:{}/{} (schema: {}), gRPC port {}, HTTP port {}",
                datasource.host(), datasource.port(), datasource.name(),
                datasource.schema(), grpc.port(), server.port());

        return new AppProperties(datasource, grpc, server);
    }

    private static DatasourceProperties buildDatasource(RawDatasource raw) {
        // Start with defaults
        String host = "localhost";
        int port = 5432;
        String name = "spaced-repetition-bot-db";
        String username = "postgres";
        String password = "postgres";
        String schema = "public";
        int maxPoolSize = 10;

        // Override with YAML values if present
        if (raw != null) {
            if (raw.host != null) host = raw.host;
            if (raw.port != null) port = raw.port;
            if (raw.name != null) name = raw.name;
            if (raw.username != null) username = raw.username;
            if (raw.password != null) password = raw.password;
            if (raw.schema != null) schema = raw.schema;
            if (raw.maxPoolSize != null) maxPoolSize = raw.maxPoolSize;
        }

        // Override with environment variables
        host = EnvUtility.getEnv("DATA_SERVICE_DB_HOST", host);
        port = EnvUtility.getEnvInt("DATA_SERVICE_DB_PORT", port);
        name = EnvUtility.getEnv("DATA_SERVICE_DB_NAME", name);
        username = EnvUtility.getEnv("DATA_SERVICE_DB_USER", username);
        password = EnvUtility.getEnv("DATA_SERVICE_DB_PASSWORD", password);
        schema = EnvUtility.getEnv("DATA_SERVICE_DB_SCHEMA", schema);
        maxPoolSize = EnvUtility.getEnvInt("DATA_SERVICE_DB_MAX_POOL_SIZE", maxPoolSize);

        return new DatasourceProperties(host, port, name, username, password, schema, maxPoolSize);
    }

    private static GrpcProperties buildGrpc(RawGrpc raw) {
        int port = 50051;
        if (raw != null && raw.port != null) port = raw.port;
        port = EnvUtility.getEnvInt("DATA_SERVICE_PORT", port);
        return new GrpcProperties(port);
    }

    private static ServerProperties buildServer(RawServer raw) {
        int port = 8081;
        if (raw != null && raw.port != null) port = raw.port;
        port = EnvUtility.getEnvInt("HEALTH_SERVER_PORT", port);
        return new ServerProperties(port);
    }
}