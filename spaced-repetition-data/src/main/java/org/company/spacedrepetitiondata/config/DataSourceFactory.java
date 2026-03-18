package org.company.spacedrepetitiondata.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.config.properties.DatasourceProperties;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating a HikariCP DataSource from {@link DatasourceProperties}.
 */
@Slf4j
public class DataSourceFactory {
    private final DatasourceProperties datasourceProperties;

    public DataSourceFactory(DatasourceProperties datasourceProperties) {
        this.datasourceProperties = datasourceProperties;
    }

    public DataSource createDataSource() {
        HikariConfig hikariConfig = new HikariConfig();

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?currentSchema=%s",
                datasourceProperties.host(),
                datasourceProperties.port(),
                datasourceProperties.name(),
                datasourceProperties.schema());

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(datasourceProperties.username());
        hikariConfig.setPassword(datasourceProperties.password());

        hikariConfig.setMaximumPoolSize(datasourceProperties.maxPoolSize());
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
}