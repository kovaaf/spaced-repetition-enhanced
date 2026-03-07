package org.company.spacedrepetitionbot.service.default_deck.executors;

import org.company.spacedrepetitionbot.config.AppProperties;
import org.company.spacedrepetitionbot.config.GitSyncProperties;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.processors.SyncEventProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncSchedulerTest {

    @Mock
    private GitSyncProperties gitSyncProperties;

    @Mock
    private DeckService deckService;

    @Mock
    private SyncEventProcessor syncEventProcessor;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.DefaultDeckConfig defaultDeckConfig;

    @InjectMocks
    private SyncScheduler syncScheduler;

    @Test
    void scheduledSync_WhenGitSyncEnabledAndDeckExists_ShouldProcessSyncEventWithDeckId() {
        // Given
        when(gitSyncProperties.isEnabled()).thenReturn(true);
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getName()).thenReturn("Default Deck");
        Deck mockDeck = Deck.builder().deckId(1L).build();
        when(deckService.getOptionalDeckByName("Default Deck")).thenReturn(Optional.of(mockDeck));

        // When
        syncScheduler.scheduledSync();

        // Then
        verify(deckService).getOptionalDeckByName("Default Deck");
        verify(syncEventProcessor).processSyncEvent(argThat(event ->
                event.getDeckId().equals(1L) &&
                !event.isForceFullSync() &&
                event.getChangedFiles().isEmpty()
        ));
    }

    @Test
    void scheduledSync_WhenGitSyncEnabledAndDeckDoesNotExist_ShouldProcessSyncEventWithForceFullSync() {
        // Given
        when(gitSyncProperties.isEnabled()).thenReturn(true);
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getName()).thenReturn("Default Deck");
        when(deckService.getOptionalDeckByName("Default Deck")).thenReturn(Optional.empty());

        // When
        syncScheduler.scheduledSync();

        // Then
        verify(deckService).getOptionalDeckByName("Default Deck");
        verify(syncEventProcessor).processSyncEvent(argThat(event ->
                event.getDeckId() == null &&
                event.isForceFullSync() &&
                event.getChangedFiles().isEmpty()
        ));
    }

    @Test
    void scheduledSync_WhenGitSyncDisabled_ShouldDoNothing() {
        // Given
        when(gitSyncProperties.isEnabled()).thenReturn(false);

        // When
        syncScheduler.scheduledSync();

        // Then
        verifyNoInteractions(deckService);
        verifyNoInteractions(syncEventProcessor);
        verifyNoInteractions(appProperties);
    }
}