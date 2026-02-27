package org.company.spacedrepetitiondata.service;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import org.company.spacedrepetitiondata.health.MetricsEndpoint;
import org.company.spacedrepetitiondata.repository.AnswerEventRecord;
import org.company.spacedrepetitiondata.repository.AnswerEventRepository;
import com.google.protobuf.Timestamp;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Integration test for streaming analytics using real gRPC server on random port.
 * Verifies that the streaming RPC accepts connections and sends data.
 */
@ExtendWith(MockitoExtension.class)
class StreamAnalyticsIntegrationTest {
    @Mock
    private AnswerEventRepository answerEventRepository;
    @Mock
    private MetricsEndpoint metricsEndpoint;
    private Server server;
    private ManagedChannel channel;
    private AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;
    private AnalyticsServiceGrpc.AnalyticsServiceStub asyncStub;
    private int port;
    @BeforeEach
    void setUp() throws IOException {
        // Create service instance with mocked dependencies
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(answerEventRepository, metricsEndpoint);
        // Build server on random port
        server = ServerBuilder.forPort(0)
                .addService(service)
                .directExecutor() // Use same thread for simplicity
                .build()
                .start();
        // Get actual port assigned
        port = server.getPort();

        // Create channel to localhost
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext() // For testing only
                .directExecutor()
                .build();
        // Create stubs
        blockingStub = AnalyticsServiceGrpc.newBlockingStub(channel);
        asyncStub = AnalyticsServiceGrpc.newStub(channel);
    }
    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdown();
            try {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void streamAnalytics_validRequest_streamsInitialEvents() throws Exception {
        // Given: mock repository returns some events
        Instant startTime = Instant.ofEpochSecond(1000);
        Instant endTime = Instant.ofEpochSecond(2000);
        
        List<AnswerEventRecord> mockEvents = List.of(
            new AnswerEventRecord(123L, 456L, 789L, 4, Instant.ofEpochSecond(1500)),
            new AnswerEventRecord(123L, 456L, 790L, 5, Instant.ofEpochSecond(1600))
        );
        when(answerEventRepository.findByUserIdAndTimeRange(123L, startTime, endTime))
            .thenReturn(mockEvents);
        // Prepare request
        AnalyticsProto.StreamAnalyticsRequest request = AnalyticsProto.StreamAnalyticsRequest.newBuilder()
                .setUserId("123")
                .setStartTime(Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        // When: call streaming RPC
        List<AnalyticsProto.StreamAnalyticsResponse> responses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);

        AtomicInteger receivedCount = new AtomicInteger(0);
        StreamObserver<AnalyticsProto.StreamAnalyticsResponse> responseObserver =
            new StreamObserver<>() {
                @Override
                public void onNext(AnalyticsProto.StreamAnalyticsResponse response) {
                    responses.add(response);
                    if (receivedCount.incrementAndGet() == 2) {
                        latch.countDown();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    errorCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    latch.countDown();
                }
            };

        asyncStub.streamAnalytics(request, responseObserver);
        // Wait for initial events (stream will send 2 events)
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Should receive 2 events within timeout");
        // Then: verify we received the expected events
        assertEquals(0, errorCount.get(), "Should not have errors");
        assertEquals(2, responses.size(), "Should receive two events");
        // Verify event data
        AnalyticsProto.StreamAnalyticsResponse response1 = responses.get(0);
        AnalyticsProto.StreamAnalyticsResponse response2 = responses.get(1);
        assertEquals("123", response1.getEvent().getUserId());
        assertEquals(4, response1.getEvent().getQualityValue());
        assertEquals("123", response2.getEvent().getUserId());
        assertEquals(5, response2.getEvent().getQualityValue());
    }

    @Test
    void streamAnalytics_emptyUserId_streamsEventsForAllUsers() throws Exception {
        // Given: mock repository returns events for all users
        Instant startTime = Instant.ofEpochSecond(1000);
        Instant endTime = Instant.ofEpochSecond(2000);
        
        List<AnswerEventRecord> mockEvents = List.of(
            new AnswerEventRecord(123L, 456L, 789L, 4, Instant.ofEpochSecond(1500)),
            new AnswerEventRecord(456L, 789L, 101L, 3, Instant.ofEpochSecond(1700))
        );
        when(answerEventRepository.findByTimeRange(startTime, endTime))
            .thenReturn(mockEvents);
        // Prepare request with empty user ID
        AnalyticsProto.StreamAnalyticsRequest request = AnalyticsProto.StreamAnalyticsRequest.newBuilder()
                .setStartTime(Timestamp.newBuilder().setSeconds(1000).build())
                .setEndTime(Timestamp.newBuilder().setSeconds(2000).build())
                .build();

        // When: call streaming RPC
        List<AnalyticsProto.StreamAnalyticsResponse> responses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger receivedCount = new AtomicInteger(0);

        StreamObserver<AnalyticsProto.StreamAnalyticsResponse> responseObserver =
            new StreamObserver<>() {
                @Override
                public void onNext(AnalyticsProto.StreamAnalyticsResponse response) {
                    responses.add(response);
                    if (receivedCount.incrementAndGet() == 2) {
                        latch.countDown();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    errorCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    latch.countDown();
                }
            };

        asyncStub.streamAnalytics(request, responseObserver);
        // Wait for initial events (stream will send 2 events)
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Should receive 2 events within timeout");
        // Then: verify we received events for all users
        assertEquals(0, errorCount.get(), "Should not have errors");
        assertEquals(2, responses.size(), "Should receive two events");
    }
}