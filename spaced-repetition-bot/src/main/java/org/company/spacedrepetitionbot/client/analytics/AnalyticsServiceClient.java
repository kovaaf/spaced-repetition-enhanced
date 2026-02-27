package org.company.spacedrepetitionbot.client.analytics;

import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Client for interacting with the Analytics gRPC service.
 */
@Slf4j
@Service
public class AnalyticsServiceClient {

    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub analyticsStub;

    public AnalyticsServiceClient(
            @GrpcClient("analytics-service") AnalyticsServiceGrpc.AnalyticsServiceBlockingStub analyticsStub) {
        this.analyticsStub = analyticsStub;
    }

    /**
     * Records an answer event in the analytics service.
     *
     * @param userId   the user ID
     * @param deckId   the deck ID
     * @param cardId   the card ID
     * @param quality  the answer quality
     * @param timestamp the timestamp of the answer
     * @throws RuntimeException if the gRPC call fails
     */
    public void recordAnswerEvent(String userId, String deckId, String cardId,
                                  Quality quality, Instant timestamp) {
        AnalyticsProto.AnswerEvent event = buildAnswerEvent(userId, deckId, cardId, quality, timestamp);
        try {
            Empty response = analyticsStub.recordAnswerEvent(event);
            log.debug("Successfully recorded answer event for user {}, card {}", userId, cardId);
        } catch (StatusRuntimeException e) {
            log.error("Failed to record answer event for user {}, card {}: {}", userId, cardId, e.getStatus(), e);
            throw new RuntimeException("Failed to record answer event", e);
        }
    }

    private AnalyticsProto.AnswerEvent buildAnswerEvent(String userId, String deckId, String cardId,
                                                        Quality quality, Instant timestamp) {
        AnalyticsProto.AnswerEvent.Builder builder = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId(userId)
                .setDeckId(deckId)
                .setCardId(cardId)
                .setQuality(mapQuality(quality));
        if (timestamp != null) {
            builder.setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(timestamp.getEpochSecond())
                    .setNanos(timestamp.getNano())
                    .build());
        }
        return builder.build();
    }

    private AnalyticsProto.Quality mapQuality(Quality quality) {
        // The numeric values match between the constants and protobuf enum
        return AnalyticsProto.Quality.forNumber(quality.getQuality());
    }
}