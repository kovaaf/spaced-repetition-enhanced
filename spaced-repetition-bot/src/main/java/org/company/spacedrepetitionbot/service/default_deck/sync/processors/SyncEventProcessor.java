package org.company.spacedrepetitionbot.service.default_deck.sync.processors;

import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;

/**
 * Contract for processing synchronization events.
 * <p>
 * Different implementations can handle events in various ways (e.g., direct processing,
 * Kafka‑based, etc.).
 * </p>
 */
public interface SyncEventProcessor {
    /**
     * Processes a synchronization event.
     *
     * @param event the event to process
     */
    void processSyncEvent(SyncEventDTO event);
}
