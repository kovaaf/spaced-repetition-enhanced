package org.company.spacedrepetitionbot.service.default_deck.processors;

import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;

public interface SyncEventProcessor {
    void processSyncEvent(SyncEventDTO event);
}
