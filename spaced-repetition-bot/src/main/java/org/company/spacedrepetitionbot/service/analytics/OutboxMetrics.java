package org.company.spacedrepetitionbot.service.analytics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxMetrics {
    private final MeterRegistry meterRegistry;

    private static final String METRIC_PREFIX = "analytics.outbox.";
    private static final String PROCESSED = METRIC_PREFIX + "processed";
    private static final String FAILURES = METRIC_PREFIX + "failures";
    private static final String RETRIES = METRIC_PREFIX + "retries";
    private static final String DLQ = METRIC_PREFIX + "dlq";

    public void incrementProcessed() {
        Counter.builder(PROCESSED)
                .description("Number of successfully processed analytics outbox records")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailure() {
        Counter.builder(FAILURES)
                .description("Number of failed analytics outbox record processing attempts")
                .register(meterRegistry)
                .increment();
    }

    public void incrementRetry() {
        Counter.builder(RETRIES)
                .description("Number of retries performed for analytics outbox records")
                .register(meterRegistry)
                .increment();
    }

    public void incrementDLQ() {
        Counter.builder(DLQ)
                .description("Number of records moved to Dead Letter Queue")
                .register(meterRegistry)
                .increment();
    }
}