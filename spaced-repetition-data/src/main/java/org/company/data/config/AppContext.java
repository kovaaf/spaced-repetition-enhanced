package org.company.data.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import lombok.extern.slf4j.Slf4j;
import org.company.data.config.properties.AppProperties;
import org.company.data.config.properties.AppPropertiesFactory;
import org.company.data.repository.AnswerEventRepository;
import org.company.data.repository.JdbcAnswerEventRepository;
import org.company.data.repository.JdbcUserRepository;
import org.company.data.repository.UserRepository;
import org.company.data.service.grpc.AnalyticsGrpcService;
import org.company.data.service.grpc.PollingStreamingStrategy;
import org.company.data.service.grpc.StreamingStrategy;
import org.company.data.service.health.*;
import org.company.data.service.usecase.GetAnalyticsUseCase;
import org.company.data.service.usecase.GetUsersUseCase;
import org.company.data.service.usecase.RecordAnswerUseCase;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Composition root for the data service.
 */
@Slf4j
public class AppContext {
    private final AppProperties appProperties;
    private final DataSource dataSource;
    private final ScheduledExecutorService healthScheduler;
    private final ScheduledExecutorService streamingScheduler;
    private final HttpHealthEndpoint httpHealthEndpoint;
    private final Server grpcServer;

    public AppContext() {
        this.appProperties = AppPropertiesFactory.create();
        this.dataSource = new DataSourceFactory(appProperties.datasource()).createDataSource();
        this.healthScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "health-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.streamingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "streaming-scheduler");
            t.setDaemon(true);
            return t;
        });
        AnswerEventRepository answerEventRepository = new JdbcAnswerEventRepository(dataSource);
        UserRepository userRepository = new JdbcUserRepository(dataSource);
        MetricsEndpoint metricsEndpoint = new MetricsEndpoint();

        // Health components
        DatabaseHealthChecker dbHealthChecker = new DatabaseHealthChecker(dataSource);
        HealthService healthService = new HealthService(dbHealthChecker, healthScheduler, 30);
        GrpcHealthCheck grpcHealthCheck = new GrpcHealthCheck(healthService, healthScheduler, 30);
        this.httpHealthEndpoint = new HttpHealthEndpoint(
                healthService, metricsEndpoint,
                appProperties.server().port(),
                healthScheduler);

        // Use cases
        RecordAnswerUseCase recordAnswerUseCase = new RecordAnswerUseCase(answerEventRepository);
        GetAnalyticsUseCase getAnalyticsUseCase = new GetAnalyticsUseCase(answerEventRepository);
        GetUsersUseCase getUsersUseCase = new GetUsersUseCase(userRepository);

        // Streaming strategy
        StreamingStrategy streamingStrategy = new PollingStreamingStrategy(answerEventRepository, streamingScheduler);

        // gRPC service
        AnalyticsGrpcService analyticsService = new AnalyticsGrpcService(
                recordAnswerUseCase,
                getAnalyticsUseCase,
                getUsersUseCase, metricsEndpoint,
                streamingStrategy);

        // gRPC server
        this.grpcServer = ServerBuilder.forPort(appProperties.grpc().port())
                .addService(analyticsService)
                .addService(grpcHealthCheck.getHealthStatusManager().getHealthService())
                .addService(ProtoReflectionServiceV1.newInstance())
                .build();

        log.info("AppContext initialized. gRPC port: {}, HTTP health port: {}",
                appProperties.grpc().port(), appProperties.server().port());
    }

    public void start() throws IOException {
        grpcServer.start();
        httpHealthEndpoint.start();
        log.info("gRPC server started on port {}", appProperties.grpc().port());

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void shutdown() {
        log.info("Shutting down data service...");
        try {
            httpHealthEndpoint.stop();
            grpcServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            healthScheduler.shutdownNow();
            streamingScheduler.shutdownNow();
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
            log.info("Data service shut down complete");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }

    /**
     * Starts the service and blocks until shutdown.
     * Handles all exceptions and exits the JVM with appropriate status.
     */
    public static void run() {
        try {
            AppContext context = new AppContext();
            context.start();
            context.blockUntilShutdown();
        } catch (IOException e) {
            log.error("Fatal error: failed to start server", e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.error("Server interrupted", e);
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (RuntimeException e) {
            log.error("Unexpected runtime error", e);
            System.exit(1);
        }
    }
}