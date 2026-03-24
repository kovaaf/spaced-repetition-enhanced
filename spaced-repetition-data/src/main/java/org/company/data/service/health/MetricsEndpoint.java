package org.company.data.service.health;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and exposes metrics in Prometheus format.
 */
public class MetricsEndpoint {
    private final AtomicLong recordAnswerEventRequests = new AtomicLong(0);
    private final AtomicLong getAnalyticsRequests = new AtomicLong(0);
    private final AtomicLong recordAnswerEventErrors = new AtomicLong(0);
    private final AtomicLong getAnalyticsErrors = new AtomicLong(0);
    private final AtomicLong healthChecks = new AtomicLong(0);
    private final AtomicLong healthCheckErrors = new AtomicLong(0);

    public void incrementRecordAnswerEventRequests() {
        recordAnswerEventRequests.incrementAndGet();
    }

    public void incrementGetAnalyticsRequests() {
        getAnalyticsRequests.incrementAndGet();
    }

    public void incrementRecordAnswerEventErrors() {
        recordAnswerEventErrors.incrementAndGet();
    }

    public void incrementGetAnalyticsErrors() {
        getAnalyticsErrors.incrementAndGet();
    }

    public void incrementHealthChecks() {
        healthChecks.incrementAndGet();
    }

    public void incrementHealthCheckErrors() {
        healthCheckErrors.incrementAndGet();
    }

    public String getMetricsAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# HELP grpc_requests_total Total number of gRPC requests\n");
        sb.append("# TYPE grpc_requests_total counter\n");
        sb.append("grpc_requests_total{method=\"RecordAnswerEvent\"} ").append(recordAnswerEventRequests.get()).append("\n");
        sb.append("grpc_requests_total{method=\"GetAnalytics\"} ").append(getAnalyticsRequests.get()).append("\n");

        sb.append("# HELP grpc_errors_total Total number of gRPC errors\n");
        sb.append("# TYPE grpc_errors_total counter\n");
        sb.append("grpc_errors_total{method=\"RecordAnswerEvent\"} ").append(recordAnswerEventErrors.get()).append("\n");
        sb.append("grpc_errors_total{method=\"GetAnalytics\"} ").append(getAnalyticsErrors.get()).append("\n");

        sb.append("# HELP health_checks_total Total number of health checks\n");
        sb.append("# TYPE health_checks_total counter\n");
        sb.append("health_checks_total ").append(healthChecks.get()).append("\n");

        sb.append("# HELP health_check_errors_total Total number of health check errors\n");
        sb.append("# TYPE health_check_errors_total counter\n");
        sb.append("health_check_errors_total ").append(healthCheckErrors.get()).append("\n");

        sb.append("# HELP metrics_timestamp Unix timestamp of metrics collection\n");
        sb.append("# TYPE metrics_timestamp gauge\n");
        sb.append("metrics_timestamp ").append(System.currentTimeMillis()).append("\n");
        return sb.toString();
    }
}