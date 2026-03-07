package org.company.spacedrepetitionbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private DefaultDeckConfig defaultDeck;
    private SyncMode syncMode;
    private AnalyticsConfig analytics;

    @Data
    public static class DefaultDeckConfig {
        private String name;
        private RepoConfig repo;
        private SyncConfig sync;

        @Data
        public static class RepoConfig {
            private String url;
            private String branch;
            private String path;
            private String webhookSecret;
            private String token;
            private List<String> sourceFolders = Collections.emptyList();
            private List<String> excludeFolders = Collections.emptyList();
        }

        @Data
        public static class SyncConfig {
            private boolean initialEnabled;
            private String cron;
        }
    }

    @Data
    public static class SyncMode {
        private Mode mode = Mode.KAFKA;

        public enum Mode {
            KAFKA,
            DIRECT
        }
    }

    @Data
    public static class OutboxConfig {
        private ProcessorConfig processor;
    }

    @Data
    public static class ProcessorConfig {
        private String cron;
        private int batchSize;
        private int maxRetries;
        private long initialDelay;
    }

    @Data
    public static class AnalyticsConfig {
        private OutboxConfig outbox;
    }
}
