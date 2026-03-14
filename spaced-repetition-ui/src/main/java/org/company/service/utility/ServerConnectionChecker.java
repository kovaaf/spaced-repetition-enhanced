package org.company.service.utility;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Utility to check if a gRPC server is reachable by performing a minimal request.
 */
@Slf4j
public class ServerConnectionChecker {
    /**
     * Attempts to fetch a single last day event from the server to verify connectivity.
     *
     * @param target server address (e.g., "localhost:50051")
     * @return {@code true} if the server responds within the timeout, {@code false} otherwise
     */
    public boolean checkConnection(String target) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            AnalyticsServiceGrpc.AnalyticsServiceBlockingStub stub = AnalyticsServiceGrpc.newBlockingStub(channel);
            Instant now = Instant.now();
            AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                    .setStartTime(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(now.minusSeconds(24 * 3600).getEpochSecond()))
                    .setEndTime(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond()))
                    .build();
            stub.withDeadlineAfter(3, TimeUnit.SECONDS).getAnalytics(request);
            log.info("Connection check successful for {}", target);
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Connection check failed for {}: {}", target, e.getMessage());
            return false;
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}