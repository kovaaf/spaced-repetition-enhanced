package org.company.spacedrepetitiondata;

import org.company.spacedrepetitiondata.health.GrpcHealthCheck;
import org.company.spacedrepetitiondata.health.HealthService;
import org.company.spacedrepetitiondata.health.HttpHealthEndpoint;
import org.company.spacedrepetitiondata.health.MetricsEndpoint;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;
import org.company.spacedrepetitiondata.service.AnalyticsServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Main gRPC server for Spaced Repetition Data Service.
 */
public class DataServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(DataServiceApplication.class);
    
    private final int port;
    private final Server server;
    private final HealthService healthService;
    private final GrpcHealthCheck grpcHealthCheck;
    private final MetricsEndpoint metricsEndpoint;
    private final HttpHealthEndpoint httpHealthEndpoint;
    
    public DataServiceApplication(int port) throws IOException {
        this.port = port;
        
        // Initialize health and metrics components
        this.healthService = new HealthService();
        this.grpcHealthCheck = new GrpcHealthCheck(healthService);
        this.metricsEndpoint = new MetricsEndpoint();
        
        // Create gRPC server with health check
        AnswerEventRepository answerEventRepository = new AnswerEventRepository();
        this.server = ServerBuilder.forPort(port)
                .addService(new AnalyticsServiceImpl(answerEventRepository, metricsEndpoint))
                .addService(grpcHealthCheck.getHealthStatusManager().getHealthService())
                .addService(ProtoReflectionService.newInstance()) // for grpcurl discovery
                .build();
        
        // Start HTTP health endpoint on port 8081 (configured in application.yml)
        int httpPort = getHttpPort();
        this.httpHealthEndpoint = new HttpHealthEndpoint(healthService, metricsEndpoint, httpPort);
        this.httpHealthEndpoint.start();
        
        logger.info("Data service initialized with gRPC port {} and HTTP health endpoint on port {}", port, httpPort);
    }
    
    public void start() throws IOException {
        server.start();
        logger.info("gRPC server started on port {}", port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server");
            try {
                DataServiceApplication.this.stop();
            } catch (InterruptedException e) {
                logger.error("Error during server shutdown", e);
                Thread.currentThread().interrupt();
            }
            logger.info("gRPC server shut down");
        }));
    }
    
    public void stop() throws InterruptedException {
        logger.info("Shutting down data service...");
        
        // Stop HTTP health endpoint
        if (httpHealthEndpoint != null) {
            httpHealthEndpoint.stop();
        }
        
        // Stop gRPC health check
        if (grpcHealthCheck != null) {
            grpcHealthCheck.shutdown();
        }
        
        // Stop health service
        if (healthService != null) {
            healthService.shutdown();
        }
        
        // Stop gRPC server
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
        
        logger.info("Data service shut down complete");
    }
    
    /**
     * Gets the HTTP port from environment variable or default.
     * 
     * @return HTTP port number
     */
    private int getHttpPort() {
        String portEnv = System.getenv("HTTP_PORT");
        if (portEnv == null || portEnv.trim().isEmpty()) {
            portEnv = System.getenv("SERVER_PORT");
        }
        
        int defaultPort = 8081; // default from application.yml
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                return Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid HTTP port value '{}', using default {}", portEnv, defaultPort);
            }
        }
        
        return defaultPort;
    }
    
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public int getPort() {
        return server.getPort();
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        // Determine port from environment variable or default
        // Priority: DATA_SERVICE_PORT (matches docker-compose) then GRPC_PORT then default 9091
        String portEnv = System.getenv("DATA_SERVICE_PORT");
        if (portEnv == null || portEnv.trim().isEmpty()) {
            portEnv = System.getenv("GRPC_PORT");
        }
        int port = 9091; // default dev port
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid port value '{}', using default {}", portEnv, port);
            }
        }
        
        logger.info("Starting Spaced Repetition Data Service on port {}", port);
        final DataServiceApplication server = new DataServiceApplication(port);
        server.start();
        server.blockUntilShutdown();
    }
}