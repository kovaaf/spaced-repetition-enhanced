package org.company.spacedrepetitiondata.service.health;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks database connectivity.
 */
@Slf4j
public class DatabaseHealthChecker {
    private static final String HEALTH_CHECK_QUERY = "SELECT 1";

    private final DataSource dataSource;
    private final AtomicBoolean isHealthy = new AtomicBoolean(false);

    public DatabaseHealthChecker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Performs a synchronous health check.
     *
     * @return true if database is reachable, false otherwise
     */
    public boolean check() {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(HEALTH_CHECK_QUERY);
                ResultSet rs = stmt.executeQuery()) {
            boolean healthy = rs.next() && rs.getInt(1) == 1;
            isHealthy.set(healthy);
            if (healthy) {
                log.debug("Database health check passed");
            } else {
                log.warn("Database health check failed: unexpected result");
            }
            return healthy;
        } catch (SQLException e) {
            log.warn("Database health check failed with SQL exception", e);
            isHealthy.set(false);
            return false;
        } catch (Exception e) {
            log.warn("Database health check failed with unexpected exception", e);
            isHealthy.set(false);
            return false;
        }
    }

    /**
     * Returns the last known health status.
     */
    public boolean isHealthy() {
        return isHealthy.get();
    }
}