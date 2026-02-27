package org.company.spacedrepetitionbot.service.analytics;

import org.company.spacedrepetitionbot.client.analytics.AnalyticsServiceClient;
import org.company.spacedrepetitionbot.config.AppProperties;
import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsOutbox;
import org.company.spacedrepetitionbot.model.analytics.AnalyticsDLQ;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsOutboxRepository;
import org.company.spacedrepetitionbot.repository.analytics.AnalyticsDLQRepository;
import org.company.spacedrepetitionbot.service.analytics.OutboxMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OutboxProcessor}.
 */
@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private AnalyticsOutboxRepository analyticsOutboxRepository;

    @Mock
    private AnalyticsServiceClient analyticsServiceClient;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.AnalyticsConfig analyticsConfig;

    @Mock
    private AppProperties.OutboxConfig outboxConfig;

    @Mock
    private AppProperties.ProcessorConfig processorConfig;

    @Mock
    private AnalyticsDLQRepository analyticsDLQRepository;

    @Mock
    private OutboxMetrics outboxMetrics;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processOutbox_NoPendingRecords_ShouldDoNothing() {
        // Given
        when(appProperties.getAnalytics()).thenReturn(analyticsConfig);
        when(analyticsConfig.getOutbox()).thenReturn(outboxConfig);
        when(outboxConfig.getProcessor()).thenReturn(processorConfig);
        when(processorConfig.getBatchSize()).thenReturn(50);
        when(analyticsOutboxRepository.findPendingForProcessing(ArgumentMatchers.<List<OutboxStatus>>any(), any(Pageable.class)))
                .thenReturn(List.of());

        // When
        outboxProcessor.processOutbox();

        // Then
        verify(analyticsOutboxRepository).findPendingForProcessing(ArgumentMatchers.<List<OutboxStatus>>any(), any(Pageable.class));
        verifyNoInteractions(analyticsServiceClient);
        verifyNoInteractions(outboxMetrics);
    }

    @Test
    void processOutbox_SuccessfulProcessing_ShouldUpdateRecordToCompleted() {
        // Given
        when(appProperties.getAnalytics()).thenReturn(analyticsConfig);
        when(analyticsConfig.getOutbox()).thenReturn(outboxConfig);
        when(outboxConfig.getProcessor()).thenReturn(processorConfig);
        when(processorConfig.getBatchSize()).thenReturn(50);

        AnalyticsOutbox record = createPendingRecord();
        ArgumentCaptor<List<OutboxStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(analyticsOutboxRepository.findPendingForProcessing(statusCaptor.capture(), pageableCaptor.capture()))
                .thenReturn(List.of(record));

        // When
        outboxProcessor.processOutbox();

        // Then
        verify(analyticsOutboxRepository).findPendingForProcessing(any(), any());
        List<OutboxStatus> capturedStatuses = statusCaptor.getValue();
        assertTrue(capturedStatuses.contains(OutboxStatus.PENDING));
        assertTrue(capturedStatuses.contains(OutboxStatus.FAILED));
        assertEquals(2, capturedStatuses.size());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(50, capturedPageable.getPageSize());
        assertEquals(0, capturedPageable.getPageNumber());

        verify(analyticsServiceClient).recordAnswerEvent(
                eq("123"), eq("456"), eq("789"),
                eq(Quality.GOOD), any()
        );
        verify(outboxMetrics).incrementProcessed();
        verify(outboxMetrics, never()).incrementFailure();
        verify(outboxMetrics, never()).incrementRetry();
        verify(outboxMetrics, never()).incrementDLQ();
        assertEquals(OutboxStatus.COMPLETED, record.getStatus());
        assertNotNull(record.getProcessedAt());
        assertNull(record.getErrorMessage());
        verify(analyticsOutboxRepository, times(2)).save(record);
    }

    @Test
    void processOutbox_FailureWithRetries_ShouldUpdateRecordToFailedWithNextRetry() {
        // Given
        when(appProperties.getAnalytics()).thenReturn(analyticsConfig);
        when(analyticsConfig.getOutbox()).thenReturn(outboxConfig);
        when(outboxConfig.getProcessor()).thenReturn(processorConfig);
        when(processorConfig.getBatchSize()).thenReturn(50);
        when(processorConfig.getMaxRetries()).thenReturn(5);
        when(processorConfig.getInitialDelay()).thenReturn(1000L);

        AnalyticsOutbox record = createPendingRecord();
        when(analyticsOutboxRepository.findPendingForProcessing(ArgumentMatchers.<List<OutboxStatus>>any(), any(Pageable.class)))
                .thenReturn(List.of(record));

        RuntimeException exception = new RuntimeException("Service unavailable");
        doThrow(exception).when(analyticsServiceClient).recordAnswerEvent(any(), any(), any(), any(), any());

        // When
        outboxProcessor.processOutbox();

        // Then
        verify(analyticsServiceClient).recordAnswerEvent(any(), any(), any(), any(), any());
        verify(outboxMetrics).incrementFailure();
        verify(outboxMetrics).incrementRetry();
        assertEquals(OutboxStatus.FAILED, record.getStatus());
        assertEquals(1, record.getRetryCount());
        assertNotNull(record.getNextRetryAt());
        assertNotNull(record.getLastRetryAt());
        assertEquals("Service unavailable", record.getErrorMessage());
        // Verify jitter: delay should be within ±20% of base delay (1000ms) with small tolerance for execution time
        long baseDelay = 1000L; // initialDelay * (1L << retryCount) where retryCount = 0
        long delay = java.time.Duration.between(LocalDateTime.now(), record.getNextRetryAt()).toMillis();
        long tolerance = 50L; // milliseconds for test execution
        assertTrue(delay >= baseDelay * 0.8 - tolerance && delay <= baseDelay * 1.2 + tolerance,
                "Delay " + delay + "ms not within ±20% of base " + baseDelay + "ms (with tolerance " + tolerance + "ms)");
        verify(analyticsOutboxRepository, times(2)).save(record);
    }

    @Test
    void processOutbox_MaxRetriesExceeded_ShouldMoveToDlq() {
        // Given
        when(appProperties.getAnalytics()).thenReturn(analyticsConfig);
        when(analyticsConfig.getOutbox()).thenReturn(outboxConfig);
        when(outboxConfig.getProcessor()).thenReturn(processorConfig);
        when(processorConfig.getBatchSize()).thenReturn(50);
        when(processorConfig.getMaxRetries()).thenReturn(5);
        when(processorConfig.getInitialDelay()).thenReturn(1000L);

        AnalyticsOutbox record = createPendingRecord();
        record.setRetryCount(5); // Already at max retries
        when(analyticsOutboxRepository.findPendingForProcessing(ArgumentMatchers.<List<OutboxStatus>>any(), any(Pageable.class)))
                .thenReturn(List.of(record));

        RuntimeException exception = new RuntimeException("Persistent failure");
        doThrow(exception).when(analyticsServiceClient).recordAnswerEvent(any(), any(), any(), any(), any());

        // When
        outboxProcessor.processOutbox();

        // Then
        verify(analyticsServiceClient).recordAnswerEvent(any(), any(), any(), any(), any());
        verify(outboxMetrics).incrementFailure();
        verify(outboxMetrics).incrementDLQ();
        assertEquals(OutboxStatus.DLQ, record.getStatus());
        assertEquals(6, record.getRetryCount()); // Incremented
        assertEquals("Persistent failure", record.getErrorMessage());
        verify(analyticsOutboxRepository, times(2)).save(record);
        // Verify DLQ entry
        ArgumentCaptor<AnalyticsDLQ> dlqCaptor = ArgumentCaptor.forClass(AnalyticsDLQ.class);
        verify(analyticsDLQRepository).save(dlqCaptor.capture());
        AnalyticsDLQ dlq = dlqCaptor.getValue();
        assertNotNull(dlq);
        assertEquals(record.getEventId(), dlq.getOutboxId());
        assertEquals(record.getUserId(), dlq.getUserId());
        assertEquals(record.getDeckId(), dlq.getDeckId());
        assertEquals(record.getCardId(), dlq.getCardId());
        assertEquals(record.getQuality(), dlq.getQuality());
        assertEquals(record.getEventTimestamp(), dlq.getEventTimestamp());
        assertEquals("Persistent failure", dlq.getFailureReason());
        assertEquals(6, dlq.getRetryCount()); // Incremented retry count
        assertNotNull(dlq.getLastAttemptAt());
        assertNotNull(dlq.getCreatedAt());
    }

    @Test
    void processOutbox_BatchSizeRespected_ShouldOnlyProcessBatchSizeRecords() {
        // Given
        when(appProperties.getAnalytics()).thenReturn(analyticsConfig);
        when(analyticsConfig.getOutbox()).thenReturn(outboxConfig);
        when(outboxConfig.getProcessor()).thenReturn(processorConfig);
        int batchSize = 2;
        when(processorConfig.getBatchSize()).thenReturn(batchSize);

        List<AnalyticsOutbox> records = List.of(
                createPendingRecord(),
                createPendingRecord(),
                createPendingRecord() // third record beyond batch size
        );
        // Simulate repository returning only first two records due to pageable
        when(analyticsOutboxRepository.findPendingForProcessing(ArgumentMatchers.<List<OutboxStatus>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    int limit = pageable.getPageSize();
                    return records.subList(0, Math.min(limit, records.size()));
                });

        // When
        outboxProcessor.processOutbox();

        // Then
        verify(analyticsServiceClient, times(batchSize)).recordAnswerEvent(any(), any(), any(), any(), any());
        verify(outboxMetrics, times(batchSize)).incrementProcessed();
        verify(outboxMetrics, never()).incrementFailure();
        verify(outboxMetrics, never()).incrementRetry();
        verify(outboxMetrics, never()).incrementDLQ();
        verify(analyticsOutboxRepository, times(batchSize * 2)).save(any()); // each record saved twice
    }

    @Test
    void processRecord_StatusProcessingSetBeforeClientCall_ShouldPreventConcurrentProcessing() {
        // Given
        lenient().when(appProperties.getAnalytics()).thenReturn(analyticsConfig);
        lenient().when(analyticsConfig.getOutbox()).thenReturn(outboxConfig);
        lenient().when(outboxConfig.getProcessor()).thenReturn(processorConfig);
        lenient().when(processorConfig.getMaxRetries()).thenReturn(5);
        lenient().when(processorConfig.getInitialDelay()).thenReturn(1000L);

        AnalyticsOutbox record = createPendingRecord();
        List<OutboxStatus> capturedStatuses = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            AnalyticsOutbox r = invocation.getArgument(0);
            capturedStatuses.add(r.getStatus());
            return null;
        }).when(analyticsOutboxRepository).save(any(AnalyticsOutbox.class));

        // When
        outboxProcessor.processRecord(record);

        // Then
        verify(analyticsServiceClient).recordAnswerEvent(any(), any(), any(), any(), any());
        verify(outboxMetrics).incrementProcessed();
        verify(outboxMetrics, never()).incrementFailure();
        verify(outboxMetrics, never()).incrementRetry();
        verify(outboxMetrics, never()).incrementDLQ();
        verify(analyticsOutboxRepository, times(2)).save(any(AnalyticsOutbox.class));
        assertEquals(2, capturedStatuses.size());
        assertEquals(OutboxStatus.PROCESSING, capturedStatuses.get(0));
        assertEquals(OutboxStatus.COMPLETED, capturedStatuses.get(1));
    }

    private AnalyticsOutbox createPendingRecord() {
        return AnalyticsOutbox.builder()
                .eventId(1L)
                .userId(123L)
                .deckId(456L)
                .cardId(789L)
                .quality(Quality.GOOD.getQuality())
                .eventTimestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }
}