package org.company.spacedrepetitiondata;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.config.Config;
import org.company.spacedrepetitiondata.health.GrpcHealthCheck;
import org.company.spacedrepetitiondata.health.HealthService;
import org.company.spacedrepetitiondata.health.HttpHealthEndpoint;
import org.company.spacedrepetitiondata.health.MetricsEndpoint;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;
import org.company.spacedrepetitiondata.service.AnalyticsServiceImpl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataServiceApplication {
    private final int port;
    private final Server server;
    private final HealthService healthService;
    private final GrpcHealthCheck grpcHealthCheck;
    private final HttpHealthEndpoint httpHealthEndpoint;

    public static void main(String[] args) throws IOException, InterruptedException {
        Config config = Config.getInstance();
        int port = config.getGrpcPort();

        log.info("Starting Spaced Repetition Data Service on port {}", port);
        final DataServiceApplication server = new DataServiceApplication();
        server.start();
        server.blockUntilShutdown();
    }

    public DataServiceApplication() throws IOException {
        Config config = Config.getInstance();
        this.port = config.getGrpcPort();
        this.healthService = new HealthService();
        this.grpcHealthCheck = new GrpcHealthCheck(healthService);
        MetricsEndpoint metricsEndpoint = new MetricsEndpoint();

        AnswerEventRepository answerEventRepository = new AnswerEventRepository();
        this.server = ServerBuilder.forPort(port)
                .addService(new AnalyticsServiceImpl(answerEventRepository, metricsEndpoint))
                .addService(grpcHealthCheck.getHealthStatusManager().getHealthService())
                .addService(ProtoReflectionService.newInstance())
                .build();

        int httpPort = config.getHttpPort();
        this.httpHealthEndpoint = new HttpHealthEndpoint(healthService, metricsEndpoint, httpPort);
        this.httpHealthEndpoint.start();

        log.info("Data service initialized with gRPC port {} and HTTP health endpoint on port {}", port, httpPort);
    }

    public void start() throws IOException {
        server.start();
        log.info("gRPC server started on port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server");
            try {
                DataServiceApplication.this.stop();
            } catch (InterruptedException e) {
                log.error("Error during server shutdown", e);
                Thread.currentThread().interrupt();
            }
            log.info("gRPC server shut down");
        }));
    }

    public void stop() throws InterruptedException {
        log.info("Shutting down data service...");

        if (httpHealthEndpoint != null) {
            httpHealthEndpoint.stop();
        }
        if (grpcHealthCheck != null) {
            grpcHealthCheck.shutdown();
        }
        if (healthService != null) {
            healthService.shutdown();
        }
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
        log.info("Data service shut down complete");
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}