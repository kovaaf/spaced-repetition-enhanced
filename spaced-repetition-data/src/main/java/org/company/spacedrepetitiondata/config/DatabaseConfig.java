package org.company.spacedrepetitiondata.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class DatabaseConfig {
    private static volatile DataSource dataSource;

    private DatabaseConfig() {}

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }

    private static HikariDataSource createDataSource() {
        Config config = Config.getInstance();

        HikariConfig hikariConfig = new HikariConfig();

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?currentSchema=%s",
                config.getDatabaseHost(),
                config.getDatabasePort(),
                config.getDatabaseName(),
                config.getDatabaseSchema());

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getDatabaseUser());
        hikariConfig.setPassword(config.getDatabasePassword());

        hikariConfig.setMaximumPoolSize(config.getDatabaseMaxPoolSize());
        hikariConfig.setMinimumIdle(Math.min(2, hikariConfig.getMaximumPoolSize()));
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
        hikariConfig.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        hikariConfig.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
        hikariConfig.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(10));
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setPoolName("SpacedRepetitionDataPool");

        hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
        hikariConfig.addDataSourceProperty("socketTimeout", "30");

        log.info("Configured HikariCP connection pool for database: {} (max pool size: {})",
                jdbcUrl, hikariConfig.getMaximumPoolSize());

        return new HikariDataSource(hikariConfig);
    }

    public static void closeDataSource() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("HikariCP connection pool closed.");
        }
    }
}