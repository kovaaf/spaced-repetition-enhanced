package org.company.data.service.grpc;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.data.model.AnswerEvent;
import org.company.data.repository.AnswerEventRepository;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Streaming strategy that polls the database at regular intervals.
 */
@Slf4j
@RequiredArgsConstructor
public class PollingStreamingStrategy implements StreamingStrategy {
    private static final long POLLING_INTERVAL_SECONDS = 5;
    private static final long INITIAL_DELAY_SECONDS = 5;

    private final AnswerEventRepository answerEventRepository;
    private final ScheduledExecutorService scheduler;

    @Override
    public void stream(AnalyticsProto.StreamAnalyticsRequest request,
            StreamObserver<AnalyticsProto.StreamAnalyticsResponse> responseObserver) {
        // Parse request
        Long userId = request.hasUserId() && !request.getUserId().isEmpty()
                ? Long.parseLong(request.getUserId())
                : null;
        Instant startTime = request.hasStartTime()
                ? Instant.ofEpochSecond(request.getStartTime().getSeconds(), request.getStartTime().getNanos())
                : null;
        Instant endTime = request.hasEndTime()
                ? Instant.ofEpochSecond(request.getEndTime().getSeconds(), request.getEndTime().getNanos())
                : null;

        // Send initial batch
        List<AnswerEvent> initialEvents = answerEventRepository.findByUserAndTimeRange(userId, startTime, endTime);
        for (AnswerEvent event : initialEvents) {
            responseObserver.onNext(toStreamResponse(event));
        }

        AtomicReference<Instant> lastTimestamp = new AtomicReference<>(
                initialEvents.stream()
                        .map(AnswerEvent::timestamp)
                        .max(Instant::compareTo)
                        .orElse(startTime != null ? startTime : Instant.now())
        );

        // Schedule polling
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (Context.current().isCancelled()) {
                throw new RuntimeException("Client cancelled");
            }
            try {
                List<AnswerEvent> newEvents = answerEventRepository.findByUserAndTimeRange(
                        userId, lastTimestamp.get(), endTime);
                List<AnswerEvent> filtered = newEvents.stream()
                        .filter(e -> e.timestamp().isAfter(lastTimestamp.get()))
                        .toList();
                if (!filtered.isEmpty()) {
                    for (AnswerEvent event : filtered) {
                        responseObserver.onNext(toStreamResponse(event));
                    }
                    Instant max = filtered.stream()
                            .map(AnswerEvent::timestamp)
                            .max(Instant::compareTo)
                            .orElse(lastTimestamp.get());
                    lastTimestamp.set(max);
                }
            } catch (Exception e) {
                log.error("Error in polling", e);
                throw new RuntimeException(e);
            }
        }, INITIAL_DELAY_SECONDS, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // Cancel on context cancellation
        Context.current().addListener(context -> {
            if (context.isCancelled()) {
                future.cancel(true);
            }
        }, scheduler);
    }

    private AnalyticsProto.StreamAnalyticsResponse toStreamResponse(AnswerEvent event) {
        AnalyticsProto.AnswerEvent proto = AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId(String.valueOf(event.userId()))
                .setDeckId(String.valueOf(event.deckId()))
                .setCardId(String.valueOf(event.cardId()))
                .setUserName(String.valueOf(event.userName()))
                .setDeckName(String.valueOf(event.deckName()))
                .setCardTitle(String.valueOf(event.cardTitle()))
                .setQualityValue(event.quality())
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(event.timestamp().getEpochSecond())
                        .setNanos(event.timestamp().getNano())
                        .build())
                .build();
        return AnalyticsProto.StreamAnalyticsResponse.newBuilder()
                .setEvent(proto)
                .build();
    }
}