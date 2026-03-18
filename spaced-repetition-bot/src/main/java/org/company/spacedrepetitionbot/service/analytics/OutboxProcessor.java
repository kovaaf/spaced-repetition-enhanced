package org.company.spacedrepetitionbot.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.client.analytics.AnalyticsServiceClient;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsDLQ;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsDLQRepository;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final AnalyticsOutboxRepository analyticsOutboxRepository;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final AppProperties appProperties;
    private final AnalyticsDLQRepository analyticsDLQRepository;
    private final OutboxMetrics outboxMetrics;

    @Transactional
    @Scheduled(cron = "${app.analytics.outbox.processor.cron}")
    public void processOutbox() {
        int batchSize = appProperties.getAnalytics().getOutbox().getProcessor().getBatchSize();
        Pageable pageable = PageRequest.of(0, batchSize);
        List<OutboxStatus> statuses = List.of(OutboxStatus.PENDING, OutboxStatus.FAILED);
        List<AnalyticsOutbox> pendingRecords = analyticsOutboxRepository.findPendingForProcessing(statuses, pageable);
        log.debug("Found {} pending outbox records to process", pendingRecords.size());
        for (AnalyticsOutbox record : pendingRecords) {
            processRecord(record);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRecord(AnalyticsOutbox record) {
        // Mark as PROCESSING to prevent concurrent processing
        record.setStatus(OutboxStatus.PROCESSING);
        analyticsOutboxRepository.save(record);

        try {
            // Call analytics service
            analyticsServiceClient.recordAnswerEvent(
                    record.getUserId().toString(),
                    record.getDeckId().toString(),
                    record.getCardId().toString(),
                    record.getQualityEnum(),
                    record.getEventTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant()
            );
            // Success
            record.setStatus(OutboxStatus.COMPLETED);
            record.setProcessedAt(LocalDateTime.now());
            record.setErrorMessage(null);
            outboxMetrics.incrementProcessed();
            log.debug("Successfully processed outbox record {}", record.getEventId());
        } catch (RuntimeException e) {
            log.error("Failed to process outbox record {}", record.getEventId(), e);
            outboxMetrics.incrementFailure();
            handleFailure(record, e);
        }
        analyticsOutboxRepository.save(record);
    }

    private void handleFailure(AnalyticsOutbox record, RuntimeException e) {
        int retryCount = record.getRetryCount();
        int maxRetries = appProperties.getAnalytics().getOutbox().getProcessor().getMaxRetries();
        long initialDelay = appProperties.getAnalytics().getOutbox().getProcessor().getInitialDelay();

        record.setLastRetryAt(LocalDateTime.now());
        record.setRetryCount(retryCount + 1);

        if (retryCount >= maxRetries) {
            // Move to DLQ
            record.setStatus(OutboxStatus.DLQ);
            record.setErrorMessage(e.getMessage());
            copyToDLQ(record, e.getMessage());
            outboxMetrics.incrementDLQ();
            log.warn("Outbox record {} moved to DLQ after {} retries", record.getEventId(), retryCount);
        } else {
            // Exponential backoff with jitter (±20%)
            long baseDelay = initialDelay * (1L << retryCount); // 2^retryCount
            double jitterFactor = 0.8 + 0.4 * Math.random(); // Random between 0.8 and 1.2
            long delay = (long) (baseDelay * jitterFactor);
            record.setNextRetryAt(LocalDateTime.now().plus(delay, ChronoUnit.MILLIS));
            record.setStatus(OutboxStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            outboxMetrics.incrementRetry();
            log.warn("Outbox record {} failed, will retry in {} ms (retry {}/{}) with jitter factor {:.2f}",
                    record.getEventId(), delay, retryCount + 1, maxRetries, jitterFactor);
        }
    }

    private void copyToDLQ(AnalyticsOutbox record, String failureReason) {
        LocalDateTime lastAttempt = record.getLastRetryAt() != null ? record.getLastRetryAt() : LocalDateTime.now();
        AnalyticsDLQ dlq = AnalyticsDLQ.builder()
                .outboxId(record.getEventId())
                .userId(record.getUserId())
                .deckId(record.getDeckId())
                .cardId(record.getCardId())
                .quality(record.getQuality())
                .eventTimestamp(record.getEventTimestamp())
                .failureReason(failureReason)
                .retryCount(record.getRetryCount())
                .lastAttemptAt(lastAttempt)
                .build();
        analyticsDLQRepository.save(dlq);
        log.debug("Copied outbox record {} to DLQ with ID {}", record.getEventId(), dlq.getDlqId());
    }
}