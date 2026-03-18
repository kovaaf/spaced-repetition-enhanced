package org.company.data.service.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

/**
 * HTTP server for readiness, liveness, and metrics endpoints.
 */
@Slf4j
public class HttpHealthEndpoint {
    private static final String READINESS_PATH = "/health/ready";
    private static final String LIVENESS_PATH = "/health/live";
    private static final String METRICS_PATH = "/metrics";

    private final HealthService healthService;
    private final MetricsEndpoint metricsEndpoint;
    private final int port;
    private final ExecutorService executor;
    private HttpServer server;

    public HttpHealthEndpoint(HealthService healthService,
            MetricsEndpoint metricsEndpoint,
            int port,
            ExecutorService executor) {
        this.healthService = healthService;
        this.metricsEndpoint = metricsEndpoint;
        this.port = port;
        this.executor = executor;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);
        server.createContext(READINESS_PATH, new ReadinessHandler());
        server.createContext(LIVENESS_PATH, new LivenessHandler());
        server.createContext(METRICS_PATH, new MetricsHandler());
        server.start();
        log.info("HTTP health endpoint started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("HTTP health endpoint stopped");
        }
    }

    private class ReadinessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean isReady = healthService.isHealthy();
            String response = isReady ? "READY" : "NOT_READY";
            int statusCode = isReady ? 200 : 503;
            sendResponse(exchange, statusCode, response);
            log.debug("Readiness probe: {} (status: {})", response, statusCode);
        }
    }

    private static class LivenessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "ALIVE");
            log.debug("Liveness probe: ALIVE");
        }
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String metrics = metricsEndpoint.getMetricsAsText();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4");
            sendResponse(exchange, 200, metrics);
            log.debug("Metrics endpoint accessed");
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}