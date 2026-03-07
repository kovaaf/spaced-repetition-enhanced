package org.company.spacedrepetitionbot.service.default_deck.processors;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.service.default_deck.SyncEventHandler;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
