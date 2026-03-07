package org.company.spacedrepetitiondata.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Health service that manages database health status and integrates with gRPC health protocol.
 * Periodically checks database connectivity and updates health status.
 */
public class HealthService {
    private static final Logger logger = LoggerFactory.getLogger(HealthService.class);
    
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 30;
    
    private final DatabaseHealthChecker databaseHealthChecker;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isServiceHealthy;
    
    public HealthService() {
        this.databaseHealthChecker = new DatabaseHealthChecker();
        this.isServiceHealthy = false;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-service-monitor");
            t.setDaemon(true);
            return t;
        });
        
        startHealthMonitoring();
        logger.info("Health service initialized");
    }
    
    /**
     * Gets the database health checker instance.
     * 
     * @return DatabaseHealthChecker instance
     */
    public DatabaseHealthChecker getDatabaseHealthChecker() {
        return databaseHealthChecker;
    }
    
    /**
     * Checks if the service is healthy (database connectivity OK).
     * 
     * @return true if service is healthy, false otherwise
     */
    public boolean isServiceHealthy() {
        return isServiceHealthy;
    }
    
    /**
     * Performs a health check and updates service health status.
     * 
     * @return true if health check passed, false otherwise
     */
    public boolean checkHealth() {
        boolean databaseHealthy = databaseHealthChecker.checkDatabaseHealth();
        isServiceHealthy = databaseHealthy;
        
        if (databaseHealthy) {
            logger.info("Health check passed: database connectivity OK");
        } else {
            logger.warn("Health check failed: database connectivity issue");
        }
        
        return databaseHealthy;
    }
    
    /**
     * Starts periodic health monitoring.
     */
    private void startHealthMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkHealth();
            } catch (Exception e) {
                logger.error("Error during health monitoring", e);
                isServiceHealthy = false;
            }
        }, 0, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        logger.info("Started health monitoring (interval: {} seconds)", HEALTH_CHECK_INTERVAL_SECONDS);
    }
    
    /**
     * Shuts down the health service and releases resources.
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
        
        databaseHealthChecker.shutdown();
        logger.info("Health service shut down");
    }
}