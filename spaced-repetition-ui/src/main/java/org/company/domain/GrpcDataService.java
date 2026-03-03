package org.company.domain;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.company.spacedrepetitiondata.grpc.AnalyticsServiceGrpc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class GrpcDataService implements DataService {
    private final String target;
    private ManagedChannel channel;
    private AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;

    public GrpcDataService(String target) {
        this.target = target;
        initChannel();
    }

    private void initChannel() {
        channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        blockingStub = AnalyticsServiceGrpc.newBlockingStub(channel);
        log.info("gRPC channel created to {}", target);
    }

    public List<AnswerEvent> fetchData(TimeFilter filter) throws Exception {
        try {
            Instant start = calculateStartTime(filter, Instant.now());
            Instant end = Instant.now();

            AnalyticsProto.AnalyticsRequest request = AnalyticsProto.AnalyticsRequest.newBuilder()
                    .setStartTime(Timestamp.newBuilder().setSeconds(start.getEpochSecond()).setNanos(start.getNano()))
                    .setEndTime(Timestamp.newBuilder().setSeconds(end.getEpochSecond()).setNanos(end.getNano()))
                    .build();

            // Добавляем таймаут
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
            throw new Exception("Failed to fetch data from gRPC server", e);
        }
    }

    public Runnable startStreaming(TimeFilter filter, Consumer<AnswerEvent> onEvent, Runnable onError, Runnable onCompleted) {
        AnalyticsProto.StreamAnalyticsRequest request = createStreamRequest(filter);
        Iterator<AnalyticsProto.StreamAnalyticsResponse> iterator = blockingStub.streamAnalytics(request);

        log.info("Creating new gRPC stream");

        Thread streamingThread = new Thread(() -> {
            try {
                while (iterator.hasNext()) {
                    AnalyticsProto.StreamAnalyticsResponse response = iterator.next();
                    AnswerEvent event = toAnswerEvent(response.getEvent());
                    log.debug("gRPC stream received event: user={}, deck={}, card={}, time={}",
                            event.userId(), event.deckId(), event.cardId(), event.timestamp());
                    onEvent.accept(event);
                }
                log.info("gRPC stream completed normally");
                onCompleted.run();
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == io.grpc.Status.Code.CANCELLED && Thread.currentThread().isInterrupted()) {
                    log.info("gRPC stream cancelled by user (filter switch)");
                    onCompleted.run();
                } else {
                    log.error("gRPC streaming error", e);
                    onError.run();
                }
            } catch (Exception e) {
                log.error("gRPC streaming error", e);
                onError.run();
            }
        }, "gRPC-streaming-thread");

        streamingThread.setDaemon(true);
        streamingThread.start();

        return streamingThread::interrupt;
    }

    private AnalyticsProto.StreamAnalyticsRequest createStreamRequest(TimeFilter filter) {
        AnalyticsProto.StreamAnalyticsRequest.Builder builder = AnalyticsProto.StreamAnalyticsRequest.newBuilder();
        if (filter != TimeFilter.ALL_TIME) {
            Instant start = calculateStartTime(filter, Instant.now());
            builder.setStartTime(Timestamp.newBuilder()
                    .setSeconds(start.getEpochSecond())
                    .setNanos(start.getNano()));
        }
        return builder.build();
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

    private AnswerEvent toAnswerEvent(AnalyticsProto.AnswerEvent proto) {
        Instant timestamp = Instant.ofEpochSecond(proto.getTimestamp().getSeconds(), proto.getTimestamp().getNanos());
        return new AnswerEvent(
                proto.getUserId(),
                proto.hasUserName() ? proto.getUserName() : null,
                proto.getDeckId(),
                proto.hasDeckName() ? proto.getDeckName() : null,
                proto.getCardId(),
                proto.hasCardTitle() ? proto.getCardTitle() : null,
                proto.getQualityValue(),
                timestamp
        );
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown(); // инициирует закрытие, но не ждёт
        // При необходимости можно добавить логирование
        log.info("gRPC channel shutdown initiated");
    }
}