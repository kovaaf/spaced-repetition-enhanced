package org.company.spacedrepetitionbot.service.default_deck.sync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.properties.GitSyncProperties;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.sync.processors.SyncEventProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Initializes the default deck synchronization when the application starts.
 * <p>
 * Attempts to perform an initial full sync with retries. The behavior is enabled only
 * when {@code git.sync.enabled} is true (default).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "git.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApplicationInitializer {
    private final GitSyncProperties gitSyncProperties;
    private final DeckService deckService;
    private final SyncEventProcessor syncEventProcessor;

    @Value("${git.sync.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${git.sync.retry.initial-delay-ms:2000}")
    private long initialDelayMs;

    /**
     * Listens for the {@link ContextRefreshedEvent} and triggers the initial sync.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initialize() {
        if (!gitSyncProperties.isEnabled()) {
            return;
        }

        Deck deck = deckService.initializeDefaultDeck();
        SyncEventDTO event = new SyncEventDTO(deck.getDeckId(), true, null);

        long delay = initialDelayMs;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                syncEventProcessor.processSyncEvent(event);
                log.info("Initial sync completed successfully on attempt {}", attempt);
                return;
            } catch (Exception e) {
                log.warn("Initial sync failed on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("All retry attempts exhausted. Initial sync failed.", e);
                    // По желанию можно не бросать исключение, чтобы контекст продолжил запуск без синхронизации
                    throw e;
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
                delay *= 2; // экспоненциальное увеличение задержки
            }
        }
    }
}