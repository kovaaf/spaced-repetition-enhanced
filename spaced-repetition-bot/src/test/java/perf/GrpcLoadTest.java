package perf;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcLoadTest {
    private static final int TOTAL_EVENTS = 100;
    private static final int CONCURRENT_THREADS = 10;
    
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        System.out.println("Testing gRPC service at " + host + ":" + port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        try {
            // Sequential test
            System.out.println("=== Sequential Test ===");
            AnalyticsServiceGrpc.AnalyticsServiceBlockingStub stub = AnalyticsServiceGrpc.newBlockingStub(channel);
            long start = System.currentTimeMillis();
            for (int i = 0; i < TOTAL_EVENTS; i++) {
                AnalyticsProto.AnswerEvent event = AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("0")
                        .setDeckId("1")
                        .setCardId("591")
                        .setQuality(AnalyticsProto.Quality.GOOD)
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(Instant.now().getEpochSecond())
                                .setNanos(Instant.now().getNano())
                                .build())
                        .build();
                Empty response = stub.recordAnswerEvent(event);
            }
            long end = System.currentTimeMillis();
            long duration = end - start;
            double eventsPerSecond = TOTAL_EVENTS / (duration / 1000.0);
            System.out.println("Sequential: " + TOTAL_EVENTS + " events in " + duration + " ms, " + 
                               String.format("%.2f", eventsPerSecond) + " events/sec");
            
            // Concurrent test
            System.out.println("=== Concurrent Test (" + CONCURRENT_THREADS + " threads) ===");
            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            CountDownLatch latch = new CountDownLatch(TOTAL_EVENTS);
            AtomicInteger errors = new AtomicInteger();
            start = System.currentTimeMillis();
            for (int i = 0; i < TOTAL_EVENTS; i++) {
                executor.submit(() -> {
                    try {
                        AnalyticsServiceGrpc.AnalyticsServiceBlockingStub threadStub = AnalyticsServiceGrpc.newBlockingStub(channel);
                        AnalyticsProto.AnswerEvent event = AnalyticsProto.AnswerEvent.newBuilder()
                                .setUserId("0")
                                .setDeckId("1")
                                .setCardId("591")
                                .setQuality(AnalyticsProto.Quality.GOOD)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setSeconds(Instant.now().getEpochSecond())
                                        .setNanos(Instant.now().getNano())
                                        .build())
                                .build();
                        threadStub.recordAnswerEvent(event);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        System.err.println("Error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            end = System.currentTimeMillis();
            duration = end - start;
            eventsPerSecond = TOTAL_EVENTS / (duration / 1000.0);
            System.out.println("Concurrent: " + TOTAL_EVENTS + " events in " + duration + " ms, " + 
                               String.format("%.2f", eventsPerSecond) + " events/sec, errors: " + errors.get());
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            
            // Target check
            if (eventsPerSecond >= 100) {
                System.out.println("SUCCESS: Throughput >= 100 events/sec");
            } else {
                System.out.println("FAILURE: Throughput below 100 events/sec");
            }
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}