package org.company.spacedrepetitiondata.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class HttpHealthEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(HttpHealthEndpoint.class);
    
    private static final String READINESS_PATH = "/health/ready";
    private static final String LIVENESS_PATH = "/health/live";
    private static final String METRICS_PATH = "/metrics";
    
    private final HealthService healthService;
    private final MetricsEndpoint metricsEndpoint;
    private HttpServer httpServer;
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
        logger.info("HTTP health endpoint started on port {}", port);
    }
    
    /**
     * Stops the HTTP health endpoint server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            logger.info("HTTP health endpoint stopped");
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
            
            logger.debug("Readiness probe: {} (status: {})", response, statusCode);
        }
    }
    
    /**
     * Handler for liveness probes.
     * Returns 200 OK if service process is alive.
     * Always returns 200 since we're checking if the HTTP server itself is running.
     */
    private class LivenessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "ALIVE";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            
            logger.debug("Liveness probe: {}", response);
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
            
            logger.debug("Metrics endpoint accessed");
        }
    }
    
    /**
     * Gets the port the HTTP server is listening on.
     * 
     * @return port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Checks if the HTTP server is running.
     * 
     * @return true if server is running, false otherwise
     */
    public boolean isRunning() {
        return httpServer != null;
    }
}