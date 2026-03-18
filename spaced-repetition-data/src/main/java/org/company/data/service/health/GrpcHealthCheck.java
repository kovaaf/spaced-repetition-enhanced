package org.company.data.service.health;

import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Integrates with gRPC health checking protocol.
 */
@Slf4j
public class GrpcHealthCheck {
    private static final String SERVICE_NAME = "spaced-repetition-data";
    private static final String DATABASE_SERVICE_NAME = "database";

    @Getter
    private final HealthStatusManager healthStatusManager;
    private final HealthService healthService;
    private final ScheduledExecutorService scheduler;
    private final long updateIntervalSeconds;

    public GrpcHealthCheck(HealthService healthService,
            ScheduledExecutorService scheduler,
            long updateIntervalSeconds) {
        this.healthStatusManager = new HealthStatusManager();
        this.healthService = healthService;
        this.scheduler = scheduler;
        this.updateIntervalSeconds = updateIntervalSeconds;
        startPeriodicUpdates();
    }

    private void startPeriodicUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateHealthStatus();
            } catch (Exception e) {
                log.error("Error updating gRPC health status", e);
                setNotServing();
            }
        }, 0, updateIntervalSeconds, TimeUnit.SECONDS);
        log.info("Started periodic gRPC health updates (interval: {} seconds)", updateIntervalSeconds);
    }

    private void updateHealthStatus() {
        boolean isHealthy = healthService.isHealthy();
        HealthCheckResponse.ServingStatus status = isHealthy
                ? HealthCheckResponse.ServingStatus.SERVING
                : HealthCheckResponse.ServingStatus.NOT_SERVING;
        healthStatusManager.setStatus(SERVICE_NAME, status);
        healthStatusManager.setStatus(DATABASE_SERVICE_NAME, status);
        if (isHealthy) {
            log.debug("gRPC health status updated: SERVICE=SERVING, DATABASE=SERVING");
        } else {
            log.warn("gRPC health status updated: SERVICE=NOT_SERVING, DATABASE=NOT_SERVING");
        }
    }

    private void setNotServing() {
        healthStatusManager.setStatus(SERVICE_NAME, HealthCheckResponse.ServingStatus.NOT_SERVING);
        healthStatusManager.setStatus(DATABASE_SERVICE_NAME, HealthCheckResponse.ServingStatus.NOT_SERVING);
    }
}