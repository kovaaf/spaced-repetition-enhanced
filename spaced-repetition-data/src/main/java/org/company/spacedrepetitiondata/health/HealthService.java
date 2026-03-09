package org.company.spacedrepetitiondata.health;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Health service that manages database health status and integrates with gRPC health protocol.
 * Periodically checks database connectivity and updates health status.
 */
@Slf4j
public class HealthService {
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 30;
    
    private final DatabaseHealthChecker databaseHealthChecker;
    private final ScheduledExecutorService scheduler;
    /**
     * -- GETTER --
     *  Checks if the service is healthy (database connectivity OK).
     *
     */
    @Getter
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
        log.info("Health service initialized");
    }

    /**
     * Performs a health check and updates service health status.
     */
    public void checkHealth() {
        boolean databaseHealthy = databaseHealthChecker.checkDatabaseHealth();
        isServiceHealthy = databaseHealthy;
        
        if (databaseHealthy) {
            log.info("Health check passed: database connectivity OK");
        } else {
            log.warn("Health check failed: database connectivity issue");
        }

    }
    
    /**
     * Starts periodic health monitoring.
     */
    private void startHealthMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkHealth();
            } catch (Exception e) {
                log.error("Error during health monitoring", e);
                isServiceHealthy = false;
            }
        }, 0, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        log.info("Started health monitoring (interval: {} seconds)", HEALTH_CHECK_INTERVAL_SECONDS);
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
        log.info("Health service shut down");
    }
}