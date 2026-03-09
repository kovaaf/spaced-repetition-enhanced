package org.company.spacedrepetitiondata.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * HTTP health endpoint for readiness and liveness probes.
 * Provides simple HTTP endpoints for container orchestration (Kubernetes, Docker).
 */
@Slf4j
public class HttpHealthEndpoint {
    private static final String READINESS_PATH = "/health/ready";
    private static final String LIVENESS_PATH = "/health/live";
    private static final String METRICS_PATH = "/metrics";
    
    private final HealthService healthService;
    private final MetricsEndpoint metricsEndpoint;
    private HttpServer httpServer;
    @Getter
    private final int port;
    
    public HttpHealthEndpoint(HealthService healthService, MetricsEndpoint metricsEndpoint, int port) {
        this.healthService = healthService;
        this.metricsEndpoint = metricsEndpoint;
        this.port = port;
    }
    
    /**
     * Starts the HTTP health endpoint server.
     * 
     * @throws IOException if server cannot be started
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Create thread pool for handling requests
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        httpServer.setExecutor(threadPoolExecutor);
        
        // Register handlers
        httpServer.createContext(READINESS_PATH, new ReadinessHandler());
        httpServer.createContext(LIVENESS_PATH, new LivenessHandler());
        httpServer.createContext(METRICS_PATH, new MetricsHandler());
        
        httpServer.start();
        log.info("HTTP health endpoint started on port {}", port);
    }
    
    /**
     * Stops the HTTP health endpoint server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            log.info("HTTP health endpoint stopped");
        }
    }
    
    /**
     * Handler for readiness probes.
     * Returns 200 OK if service is ready to serve traffic (database connected).
     * Returns 503 Service Unavailable if service is not ready.
     */
    private class ReadinessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean isReady = healthService.isServiceHealthy();
            String response = isReady ? "READY" : "NOT_READY";
            int statusCode = isReady ? 200 : 503;
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            
            log.debug("Readiness probe: {} (status: {})", response, statusCode);
        }
    }
    
    /**
     * Handler for liveness probes.
     * Returns 200 OK if service process is alive.
     * Always returns 200 since we're checking if the HTTP server itself is running.
     */
    private static class LivenessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "ALIVE";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            
            log.debug("Liveness probe: {}", response);
        }
    }
    
    /**
     * Handler for metrics endpoint.
     * Returns Prometheus-style metrics.
     */
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String metrics = metricsEndpoint.getMetricsAsText();
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4");
            exchange.sendResponseHeaders(200, metrics.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(metrics.getBytes(StandardCharsets.UTF_8));
            }
            
            log.debug("Metrics endpoint accessed");
        }
    }
}