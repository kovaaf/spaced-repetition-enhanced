package org.company.spacedrepetitionbot.service.default_deck.executors;

import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.config.properties.GitSyncProperties;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.sync.processors.SyncEventProcessor;
import org.company.spacedrepetitionbot.service.default_deck.sync.scheduler.ApplicationInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        // Инициализация полей с @Value
        ReflectionTestUtils.setField(applicationInitializer, "maxAttempts", 3);
        ReflectionTestUtils.setField(applicationInitializer, "initialDelayMs", 2000);
    }

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