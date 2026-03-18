package org.company.spacedrepetitiondata.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.config.properties.AppProperties;
import org.company.spacedrepetitiondata.config.properties.AppPropertiesFactory;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;
import org.company.spacedrepetitiondata.repository.JdbcAnswerEventRepository;
import org.company.spacedrepetitiondata.repository.JdbcUserRepository;
import org.company.spacedrepetitiondata.repository.UserRepository;
import org.company.spacedrepetitiondata.service.grpc.AnalyticsGrpcService;
import org.company.spacedrepetitiondata.service.grpc.PollingStreamingStrategy;
import org.company.spacedrepetitiondata.service.grpc.StreamingStrategy;
import org.company.spacedrepetitiondata.service.health.*;
import org.company.spacedrepetitiondata.service.usecase.GetAnalyticsUseCase;
import org.company.spacedrepetitiondata.service.usecase.GetUsersUseCase;
import org.company.spacedrepetitiondata.service.usecase.RecordAnswerUseCase;

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

    public static void run() throws IOException, InterruptedException {
        AppContext context = new AppContext();
        context.start();
        context.blockUntilShutdown();
    }
}