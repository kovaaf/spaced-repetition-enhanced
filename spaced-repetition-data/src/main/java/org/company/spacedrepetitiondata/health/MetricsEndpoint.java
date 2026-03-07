package org.company.spacedrepetitiondata.health;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple metrics endpoint that tracks gRPC request counts and errors.
 * Provides Prometheus-style metrics output.
 */
public class MetricsEndpoint {
    // Counters for gRPC requests
    private final AtomicLong recordAnswerEventRequests = new AtomicLong(0);
    private final AtomicLong getAnalyticsRequests = new AtomicLong(0);
    
    // Counters for errors
    private final AtomicLong recordAnswerEventErrors = new AtomicLong(0);
    private final AtomicLong getAnalyticsErrors = new AtomicLong(0);
    
    // Health check metrics
    private final AtomicLong healthChecks = new AtomicLong(0);
    private final AtomicLong healthCheckErrors = new AtomicLong(0);
    
    public MetricsEndpoint() {
        // Initialize metrics
    }
    
    /**
     * Increments the counter for RecordAnswerEvent requests.
     */
    public void incrementRecordAnswerEventRequests() {
        recordAnswerEventRequests.incrementAndGet();
    }
    
    /**
     * Increments the counter for GetAnalytics requests.
     */
    public void incrementGetAnalyticsRequests() {
        getAnalyticsRequests.incrementAndGet();
    }
    
    /**
     * Increments the counter for RecordAnswerEvent errors.
     */
    public void incrementRecordAnswerEventErrors() {
        recordAnswerEventErrors.incrementAndGet();
    }
    
    /**
     * Increments the counter for GetAnalytics errors.
     */
    public void incrementGetAnalyticsErrors() {
        getAnalyticsErrors.incrementAndGet();
    }
    
    /**
     * Increments the counter for health checks.
     */
    public void incrementHealthChecks() {
        healthChecks.incrementAndGet();
    }
    
    /**
     * Increments the counter for health check errors.
     */
    public void incrementHealthCheckErrors() {
        healthCheckErrors.incrementAndGet();
    }
    
    /**
     * Gets the current metrics as Prometheus-style text format.
     * 
     * @return metrics in text format
     */
    public String getMetricsAsText() {
        StringBuilder sb = new StringBuilder();
        
        // Add HELP and TYPE lines (Prometheus format)
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
        
        // Add timestamp (milliseconds since epoch)
        sb.append("# HELP metrics_timestamp Unix timestamp of metrics collection\n");
        sb.append("# TYPE metrics_timestamp gauge\n");
        sb.append("metrics_timestamp ").append(System.currentTimeMillis()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Gets the current value of RecordAnswerEvent requests counter.
     * 
     * @return request count
     */
    public long getRecordAnswerEventRequests() {
        return recordAnswerEventRequests.get();
    }
    
    /**
     * Gets the current value of GetAnalytics requests counter.
     * 
     * @return request count
     */
    public long getGetAnalyticsRequests() {
        return getAnalyticsRequests.get();
    }
    
    /**
     * Gets the current value of RecordAnswerEvent errors counter.
     * 
     * @return error count
     */
    public long getRecordAnswerEventErrors() {
        return recordAnswerEventErrors.get();
    }
    
    /**
     * Gets the current value of GetAnalytics errors counter.
     * 
     * @return error count
     */
    public long getGetAnalyticsErrors() {
        return getAnalyticsErrors.get();
    }
    
    /**
     * Resets all metrics counters to zero.
     * Useful for testing or periodic reset.
     */
    public void reset() {
        recordAnswerEventRequests.set(0);
        getAnalyticsRequests.set(0);
        recordAnswerEventErrors.set(0);
        getAnalyticsErrors.set(0);
        healthChecks.set(0);
        healthCheckErrors.set(0);
    }
}