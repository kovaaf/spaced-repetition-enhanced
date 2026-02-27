package org.company.spacedrepetitionbot.service.default_deck.executors;

import org.company.spacedrepetitionbot.config.AppProperties;
import org.company.spacedrepetitionbot.config.GitSyncProperties;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.processors.SyncEventProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApplicationInitializerTest {

    @Mock
    private GitSyncProperties gitSyncProperties;

    @Mock
    private DeckService deckService;

    @Mock
    private SyncEventProcessor syncEventProcessor;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private ApplicationInitializer applicationInitializer;

    @Test
    void initialize_WhenGitSyncEnabled_ShouldInitializeDeckAndProcessSyncEvent() {
        // Given
        when(gitSyncProperties.isEnabled()).thenReturn(true);
        Deck mockDeck = Deck.builder().deckId(1L).build();
        when(deckService.initializeDefaultDeck()).thenReturn(mockDeck);

        // When
        applicationInitializer.initialize();

        // Then
        verify(deckService).initializeDefaultDeck();
        verify(syncEventProcessor).processSyncEvent(argThat(event ->
                event.getDeckId().equals(1L) &&
                event.isForceFullSync() &&
                event.getChangedFiles() == null
        ));
        verifyNoInteractions(appProperties);
    }

    @Test
    void initialize_WhenGitSyncDisabled_ShouldDoNothing() {
        // Given
        when(gitSyncProperties.isEnabled()).thenReturn(false);

        // When
        applicationInitializer.initialize();

        // Then
        verifyNoInteractions(deckService);
        verifyNoInteractions(syncEventProcessor);
        verifyNoInteractions(appProperties);
    }
}