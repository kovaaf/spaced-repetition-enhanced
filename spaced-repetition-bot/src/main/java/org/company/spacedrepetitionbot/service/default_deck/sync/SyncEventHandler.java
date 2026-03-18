package org.company.spacedrepetitionbot.service.default_deck.sync;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.exception.SyncException;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.sync.processors.direct.SyncEventCoreProcessor;
import org.springframework.stereotype.Service;

/**
 * Entry point for handling synchronization events.
 * <p>
 * Delegates to {@link SyncEventCoreProcessor} and wraps any exception
 * into a {@link SyncException}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SyncEventHandler {
    private final SyncEventCoreProcessor coreProcessor;

    /**
     * Handles a synchronization event.
     *
     * @param event the event to process
     * @throws SyncException if any error occurs during processing
     */
    public void handleSyncEvent(SyncEventDTO event) {
        try {
            coreProcessor.process(event);
        } catch (Exception e) {
            throw new SyncException("Direct sync failed for event: " + event, e);
        }
    }
}
