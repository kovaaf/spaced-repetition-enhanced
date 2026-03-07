package org.company.spacedrepetitionbot.service.default_deck.processors.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.service.default_deck.SyncEventHandler;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.sync.mode", havingValue = "KAFKA")
@RequiredArgsConstructor
public class KafkaSyncEventListener {
    private final SyncEventHandler eventHandler;
    private final RetryExecutor retryExecutor;

    @KafkaListener(topics = "sync-events", groupId = "sync-group")
    public void handleSyncEvent(SyncEventDTO event) {
        retryExecutor.executeWithRetry(() -> eventHandler.handleSyncEvent(event));
    }
}
