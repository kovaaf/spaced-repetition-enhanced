package perf;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class AnalyticsQueryPerformanceTest {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        try {
            AnalyticsServiceGrpc.AnalyticsServiceBlockingStub stub = AnalyticsServiceGrpc.newBlockingStub(channel);
            // Build request for all users, last 365 days
            Instant now = Instant.now();
            Instant start = now.minusSeconds(365 * 24 * 60 * 60);
            AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                    .setUserId("") // empty means all users
                    .setStartTime(Timestamp.newBuilder()
                            .setSeconds(start.getEpochSecond())
                            .setNanos(start.getNano())
                            .build())
                    .setEndTime(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();
            System.out.println("Sending GetAnalytics request...");
            long startTime = System.currentTimeMillis();
            AnalyticsProto.AnalyticsResponse response = stub.getAnalytics(request);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("Query completed in " + duration + " ms");
            System.out.println("Total events returned: " + response.getTotalCount());
            System.out.println("Events in response: " + response.getEventsCount());
            // Verify first few events
            if (response.getEventsCount() > 0) {
                AnalyticsProto.AnswerEvent first = response.getEvents(0);
                System.out.println("First event user: " + first.getUserId());
            }
            if (duration > 500) {
                System.out.println("WARNING: Query exceeded 500ms target");
            }
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}