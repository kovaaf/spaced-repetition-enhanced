package org.company.spacedrepetitiondata.health;

import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * gRPC health check implementation that integrates with HealthStatusManager.
 * Periodically updates health status based on database connectivity.
 */
public class GrpcHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(GrpcHealthCheck.class);
    
    private static final String SERVICE_NAME = "spaced-repetition-data";
    private static final String DATABASE_SERVICE_NAME = "database";
    private static final long HEALTH_UPDATE_INTERVAL_SECONDS = 30;
    
    private final HealthStatusManager healthStatusManager;
    private final HealthService healthService;
    private final ScheduledExecutorService scheduler;
    
    public GrpcHealthCheck(HealthService healthService) {
        this.healthStatusManager = new HealthStatusManager();
        this.healthService = healthService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grpc-health-check-updater");
            t.setDaemon(true);
            return t;
        });
        
        // Set initial health status
        updateHealthStatus();
        startPeriodicHealthUpdates();
        
        logger.info("gRPC health check initialized");
    }
    
    /**
     * Gets the HealthStatusManager for integration with gRPC server.
     * 
     * @return HealthStatusManager instance
     */
    public HealthStatusManager getHealthStatusManager() {
        return healthStatusManager;
    }
    
    /**
     * Updates health status in HealthStatusManager based on current service health.
     */
    public void updateHealthStatus() {
        boolean isHealthy = healthService.isServiceHealthy();
        
        HealthCheckResponse.ServingStatus status = isHealthy 
                ? HealthCheckResponse.ServingStatus.SERVING 
                : HealthCheckResponse.ServingStatus.NOT_SERVING;
        
        // Update overall service health
        healthStatusManager.setStatus(SERVICE_NAME, status);
        healthStatusManager.setStatus(DATABASE_SERVICE_NAME, status);
        
        if (isHealthy) {
            logger.debug("gRPC health status updated: SERVICE=SERVING, DATABASE=SERVING");
        } else {
            logger.warn("gRPC health status updated: SERVICE=NOT_SERVING, DATABASE=NOT_SERVING");
        }
    }
    
    /**
     * Starts periodic health status updates.
     */
    private void startPeriodicHealthUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateHealthStatus();
            } catch (Exception e) {
                logger.error("Error updating gRPC health status", e);
                // Set to NOT_SERVING on error
                healthStatusManager.setStatus(SERVICE_NAME, HealthCheckResponse.ServingStatus.NOT_SERVING);
                healthStatusManager.setStatus(DATABASE_SERVICE_NAME, HealthCheckResponse.ServingStatus.NOT_SERVING);
            }
        }, HEALTH_UPDATE_INTERVAL_SECONDS, HEALTH_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        logger.info("Started periodic gRPC health status updates (interval: {} seconds)", 
                   HEALTH_UPDATE_INTERVAL_SECONDS);
    }
    
    /**
     * Shuts down the gRPC health check and releases resources.
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
        
        // Set all services to NOT_SERVING on shutdown
        healthStatusManager.setStatus(SERVICE_NAME, HealthCheckResponse.ServingStatus.NOT_SERVING);
        healthStatusManager.setStatus(DATABASE_SERVICE_NAME, HealthCheckResponse.ServingStatus.NOT_SERVING);
        
        logger.info("gRPC health check shut down");
    }
}