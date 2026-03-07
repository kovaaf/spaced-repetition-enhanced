package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.exception.SyncException;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.processors.direct.SyncEventCoreProcessor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncEventHandler {
    private final SyncEventCoreProcessor coreProcessor;

    public void handleSyncEvent(SyncEventDTO event) {
        try {
            coreProcessor.process(event);
        } catch (Exception e) {
            throw new SyncException("Direct sync failed for event: " + event, e);
        }
    }
}
