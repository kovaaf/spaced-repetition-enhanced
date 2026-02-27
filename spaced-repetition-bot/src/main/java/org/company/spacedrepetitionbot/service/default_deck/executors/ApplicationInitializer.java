package org.company.spacedrepetitionbot.service.default_deck.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.AppProperties;
import org.company.spacedrepetitionbot.config.GitSyncProperties;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.processors.SyncEventProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "git.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApplicationInitializer {
    private final AppProperties appProperties;
    private final GitSyncProperties gitSyncProperties;
    private final DeckService deckService;
    private final SyncEventProcessor syncEventProcessor;

    @EventListener(ContextRefreshedEvent.class)
    public void initialize() {
        if (gitSyncProperties.isEnabled()) {
            Deck deck = deckService.initializeDefaultDeck();
            syncEventProcessor.processSyncEvent(new SyncEventDTO(deck.getDeckId(), true, null));
        }
    }
}
