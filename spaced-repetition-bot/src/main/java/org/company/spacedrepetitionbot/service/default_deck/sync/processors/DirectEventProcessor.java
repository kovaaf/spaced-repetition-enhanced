package org.company.spacedrepetitionbot.service.default_deck.sync.processors;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.sync.SyncEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Direct (synchronous) implementation of {@link SyncEventProcessor}.
 * <p>
 * Active only when {@code app.sync.mode} is set to {@code DIRECT}.
 * </p>
 */
@Service
@ConditionalOnProperty(name = "app.sync.mode", havingValue = "DIRECT")
@RequiredArgsConstructor
public class DirectEventProcessor implements SyncEventProcessor {
    private final SyncEventHandler eventHandler;

    @Override
    public void processSyncEvent(SyncEventDTO event) {
        eventHandler.handleSyncEvent(event);
    }
}
