package org.company.data.service.health;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that monitors overall health and provides status.
 */
@Slf4j
public class HealthService {
    private final DatabaseHealthChecker databaseHealthChecker;
    private final ScheduledExecutorService scheduler;
    private final long checkIntervalSeconds;

    @Getter
    private volatile boolean healthy = false;

    public HealthService(DatabaseHealthChecker databaseHealthChecker,
            ScheduledExecutorService scheduler,
            long checkIntervalSeconds) {
        this.databaseHealthChecker = databaseHealthChecker;
        this.scheduler = scheduler;
        this.checkIntervalSeconds = checkIntervalSeconds;
        startMonitoring();
    }

    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean dbHealthy = databaseHealthChecker.check();
                healthy = dbHealthy;
                if (dbHealthy) {
                    log.debug("Health check passed: database OK");
                } else {
                    log.warn("Health check failed: database unreachable");
                }
            } catch (Exception e) {
                log.error("Error during health check", e);
                healthy = false;
            }
        }, 0, checkIntervalSeconds, TimeUnit.SECONDS);
        log.info("Started health monitoring (interval: {} seconds)", checkIntervalSeconds);
    }
}