package org.company.spacedrepetitionbot.service.default_deck.processors.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryExecutor {
    @Value("${spring.kafka.topic.sync.max-attempts}")
    private int maxAttempts;

    @Value("${spring.kafka.topic.sync.initial-delay}")
    private long initialDelay;

    public void executeWithRetry(Runnable action) {
        int attempt = 0;
        long delay = initialDelay;

        while (attempt < maxAttempts) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                attempt++;
                log.error("Operation failed (attempt {}/{})", attempt, maxAttempts, e);
                sleep(delay);
                delay *= 2;
            }
        }
        log.error("Operation failed after {} attempts", maxAttempts);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted", e);
        }
    }
}
