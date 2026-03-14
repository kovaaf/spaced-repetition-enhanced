package org.company.infrastructure.data;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.company.domain.AnswerEvent;
import org.company.domain.TimeFilter;
import org.company.domain.exception.DataServiceException;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcDataService implements DataService {
    private final String target;
    private final ManagedChannel channel;
    @Getter
    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;

    public GrpcDataService(String target) {
        this.target = target;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.blockingStub = AnalyticsServiceGrpc.newBlockingStub(channel);
        log.info("gRPC channel created to {}", target);
    }

    @Override
    public List<AnswerEvent> fetchData(TimeFilter filter) throws DataServiceException {
        try {
            Instant now = Instant.now();
            Instant start = calculateStartTime(filter, now);
            Instant end = now;

            AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                    .setStartTime(toProtoTimestamp(start))
                    .setEndTime(toProtoTimestamp(end))
                    .build();

            AnalyticsProto.AnalyticsResponse response = blockingStub.withDeadlineAfter(10, TimeUnit.SECONDS)
                    .getAnalytics(request);

            List<AnswerEvent> events = new ArrayList<>();
            for (AnalyticsProto.AnswerEvent protoEvent : response.getEventsList()) {
                events.add(toAnswerEvent(protoEvent));
            }
            log.info("Loaded {} events from gRPC", events.size());
            return events;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus(), e);
            throw new DataServiceException("Failed to fetch data from gRPC server: " + e.getMessage(), e);
        }
    }

    private Instant calculateStartTime(TimeFilter filter, Instant now) {
        return switch (filter) {
            case LAST_DAY -> now.minusSeconds(24 * 60 * 60);
            case LAST_WEEK -> now.minusSeconds(7 * 24 * 60 * 60);
            case LAST_MONTH -> now.minusSeconds(30 * 24 * 60 * 60);
            case LAST_YEAR -> now.minusSeconds(365 * 24 * 60 * 60);
            case ALL_TIME -> Instant.EPOCH;
        };
    }

    private Timestamp toProtoTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private AnswerEvent toAnswerEvent(AnalyticsProto.AnswerEvent proto) {
        Instant timestamp = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos());
        return new AnswerEvent(
                proto.getUserId(),
                proto.hasUserName() ? proto.getUserName() : null,
                proto.getDeckId(),
                proto.hasDeckName() ? proto.getDeckName() : null,
                proto.getCardId(),
                proto.hasCardTitle() ? proto.getCardTitle() : null,
                proto.getQualityValue(),
                timestamp);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown();
        log.info("gRPC channel shutdown initiated for {}", target);
    }
}