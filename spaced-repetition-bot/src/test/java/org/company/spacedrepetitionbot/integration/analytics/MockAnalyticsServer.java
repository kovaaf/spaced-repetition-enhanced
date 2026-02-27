package org.company.spacedrepetitionbot.integration.analytics;

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Mock gRPC server for AnalyticsService that records received events for verification.
 */
public class MockAnalyticsServer extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {
    private Server server;
    private final List<AnalyticsProto.AnswerEvent> receivedEvents = new ArrayList<>();
    private final CountDownLatch eventLatch = new CountDownLatch(1);
    private volatile boolean shouldFail = false;
    private volatile RuntimeException failureException = null;
    private int port;

    public void start(int port) throws IOException {
        this.port = port;
        HealthStatusManager health = new HealthStatusManager();
        server = ServerBuilder.forPort(port)
                .addService(this)
                .addService(health.getHealthService())
                .addService(ProtoReflectionService.newInstance())
                .build();
        server.start();
        health.setStatus("", io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING);
    }

    public void startOnRandomPort() throws IOException {
        start(0); // 0 means random port
    }

    public int getPort() {
        return server.getPort();
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public List<AnalyticsProto.AnswerEvent> getReceivedEvents() {
        return new ArrayList<>(receivedEvents);
    }

    public void clearReceivedEvents() {
        receivedEvents.clear();
    }

    public void waitForEvent(long timeout, TimeUnit unit) throws InterruptedException {
        eventLatch.await(timeout, unit);
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public void setFailureException(RuntimeException failureException) {
        this.failureException = failureException;
    }

    @Override
    public void recordAnswerEvent(AnalyticsProto.AnswerEvent request,
                                  StreamObserver<Empty> responseObserver) {
        if (shouldFail && failureException != null) {
            responseObserver.onError(failureException);
            return;
        } else if (shouldFail) {
            responseObserver.onError(new RuntimeException("Mock server failure"));
            return;
        }
        receivedEvents.add(request);
        eventLatch.countDown();
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getAnalytics(AnalyticsProto.AnalyticsRequest request,
                             StreamObserver<AnalyticsProto.AnalyticsResponse> responseObserver) {
        // For integration tests, we can return empty response
        AnalyticsProto.AnalyticsResponse response = AnalyticsProto.AnalyticsResponse.newBuilder()
                .setTotalCount(0)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}