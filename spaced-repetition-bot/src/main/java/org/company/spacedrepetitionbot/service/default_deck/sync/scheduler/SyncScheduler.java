package org.company.spacedrepetitionbot.service.default_deck.sync.scheduler;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.config.properties.GitSyncProperties;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.sync.processors.SyncEventProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Scheduled synchronization of the default deck.
 * <p>
 * Periodically triggers a full sync of the default deck based on a cron expression.
 * The scheduler is active only when {@code git.sync.enabled} is true.
 * </p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "git.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {
    private final SyncEventProcessor syncEventProcessor;
    private final DeckService deckService;
    private final AppProperties appProperties;
    private final GitSyncProperties gitSyncProperties;

    /**
     * Performs a scheduled synchronization.
     * <p>
     * If the default deck exists, a non‑forced sync with an empty change list is triggered.
     * Otherwise, a forced full sync is triggered to initialize the deck.
     * </p>
     */
    @Scheduled(cron = "${app.default-deck.sync.cron}")
    public void scheduledSync() {
        if (!gitSyncProperties.isEnabled()) {
            return;
        }
        
        deckService.getOptionalDeckByName(appProperties.getDefaultDeck().getName()).ifPresentOrElse(
                deck -> syncEventProcessor.processSyncEvent(new SyncEventDTO(
                        deck.getDeckId(),
                        false,
                        Collections.emptyList())),
                () -> syncEventProcessor.processSyncEvent(new SyncEventDTO(null, true, Collections.emptyList())));
    }
}
