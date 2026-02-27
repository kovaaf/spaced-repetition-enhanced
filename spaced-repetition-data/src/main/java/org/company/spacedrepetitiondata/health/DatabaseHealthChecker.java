package org.company.spacedrepetitiondata.health;

import org.company.spacedrepetitiondata.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Health checker for database connectivity.
 * Periodically tests database connection and logs status.
 */
public class DatabaseHealthChecker {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthChecker.class);
    
    private static final String HEALTH_CHECK_QUERY = "SELECT 1";
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 30;
    
    private final DataSource dataSource;
    private final AtomicBoolean isHealthy;
    private final ScheduledExecutorService scheduler;
    
    public DatabaseHealthChecker() {
        this.dataSource = DatabaseConfig.getDataSource();
        this.isHealthy = new AtomicBoolean(false);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "database-health-checker");
            t.setDaemon(true);
            return t;
        });
        
        startPeriodicHealthCheck();
    }
    
    /**
     * Performs a database health check by executing a simple query.
     * 
     * @return true if database is reachable and responsive, false otherwise
     */
    public boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(HEALTH_CHECK_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int result = resultSet.getInt(1);
                boolean healthy = result == 1;
                isHealthy.set(healthy);
                
                if (healthy) {
                    logger.debug("Database health check passed");
                } else {
                    logger.warn("Database health check failed: unexpected result {}", result);
                }
                
                return healthy;
            } else {
                logger.warn("Database health check failed: no result returned");
                isHealthy.set(false);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Database health check failed with SQL exception", e);
            isHealthy.set(false);
            return false;
        } catch (Exception e) {
            logger.error("Database health check failed with unexpected exception", e);
            isHealthy.set(false);
            return false;
        }
    }
    
    /**
     * Gets the current health status.
     * 
     * @return true if database was healthy in last check, false otherwise
     */
    public boolean isHealthy() {
        return isHealthy.get();
    }
    
    /**
     * Starts periodic health check execution.
     */
    private void startPeriodicHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkDatabaseHealth();
                if (isHealthy.get()) {
                    logger.info("Database health status: HEALTHY");
                } else {
                    logger.warn("Database health status: UNHEALTHY");
                }
            } catch (Exception e) {
                logger.error("Error during scheduled health check", e);
            }
        }, 0, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        logger.info("Started periodic database health check (interval: {} seconds)", 
                   HEALTH_CHECK_INTERVAL_SECONDS);
    }
    
    /**
     * Shuts down the health checker and releases resources.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Database health checker shut down");
    }
}