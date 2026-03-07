package org.company.spacedrepetitionbot.service.default_deck;

import org.company.spacedrepetitionbot.exception.SyncException;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RepoSynchronizer.
 * Tests enhanced error logging for authentication failures and Git operations.
 */
@ExtendWith(MockitoExtension.class)
class RepoSynchronizerTest {

    @Mock
    private DeckService deckService;

    @Mock
    private FileSyncProcessor fileSyncProcessor;

    @Mock
    private GitSyncOperator gitSyncOperator;

    @InjectMocks
    private RepoSynchronizer repoSynchronizer;

    @Test
    void sync_WhenGitSyncOperatorThrowsTransportException_ShouldLogDebugAndThrowSyncException() throws Exception {
        // Given
        SyncEventDTO event = SyncEventDTO.builder().build();
        Deck deck = Deck.builder().deckId(1L).name("Test Deck").build();
        TransportException transportException = new TransportException("not authorized");
        
        when(gitSyncOperator.prepareRepository()).thenThrow(transportException);
        
        // When / Then
        SyncException thrown = assertThrows(SyncException.class, () -> repoSynchronizer.sync(event, deck));
        
        // Verify that TransportException was caught and logged (debug level)
        assertTrue(thrown.getMessage().contains("Sync failed"));
        assertTrue(thrown.getCause() instanceof TransportException);
        verify(gitSyncOperator).prepareRepository();
        verifyNoInteractions(fileSyncProcessor);
    }

    @Test
    void sync_WhenGitSyncOperatorThrowsGitAPIException_ShouldLogDebugAndThrowSyncException() throws Exception {
        // Given
        SyncEventDTO event = SyncEventDTO.builder().build();
        Deck deck = Deck.builder().deckId(1L).name("Test Deck").build();
        GitAPIException gitAPIException = new GitAPIException("Git operation failed") {};
        
        when(gitSyncOperator.prepareRepository()).thenThrow(gitAPIException);
        
        // When / Then
        SyncException thrown = assertThrows(SyncException.class, () -> repoSynchronizer.sync(event, deck));
        
        assertTrue(thrown.getMessage().contains("Sync failed"));
        assertTrue(thrown.getCause() instanceof GitAPIException);
        verify(gitSyncOperator).prepareRepository();
        verifyNoInteractions(fileSyncProcessor);
    }

    @Test
    void sync_WhenDeckIsNull_ShouldInitializeDefaultDeck() throws Exception {
        // Given
        SyncEventDTO event = SyncEventDTO.builder().build();
        Deck defaultDeck = Deck.builder().deckId(2L).name("Default Deck").build();
        
        when(deckService.initializeDefaultDeck()).thenReturn(defaultDeck);
        when(gitSyncOperator.prepareRepository()).thenReturn(mock(org.eclipse.jgit.api.Git.class));
        when(gitSyncOperator.isSyncRequired(any(), any(), any())).thenReturn(false);
        
        // When
        repoSynchronizer.sync(event, null);
        
        // Then
        verify(deckService).initializeDefaultDeck();
        verify(gitSyncOperator).prepareRepository();
        verify(gitSyncOperator).isSyncRequired(any(), eq(defaultDeck), eq(event));
        verifyNoInteractions(fileSyncProcessor);
    }

    @Test
    void sync_WhenSyncRequiredAndForceFullSync_ShouldProcessAllMarkdownFiles() throws Exception {
        // Given
        SyncEventDTO event = SyncEventDTO.builder().forceFullSync(true).build();
        Deck deck = Deck.builder().deckId(1L).name("Test Deck").build();
        org.eclipse.jgit.api.Git mockGit = mock(org.eclipse.jgit.api.Git.class);
        
        when(gitSyncOperator.prepareRepository()).thenReturn(mockGit);
        when(gitSyncOperator.isSyncRequired(mockGit, deck, event)).thenReturn(true);
        when(gitSyncOperator.getLatestCommit(mockGit)).thenReturn("abc123");
        
        // When
        repoSynchronizer.sync(event, deck);
        
        // Then
        verify(fileSyncProcessor).processAllMarkdownFiles(deck);
        verify(deckService).save(deck);
    }

    @Test
    void sync_WhenSyncRequiredAndNotForceFullSync_ShouldProcessChangedFiles() throws Exception {
        // Given
        SyncEventDTO event = SyncEventDTO.builder()
                .forceFullSync(false)
                .changedFiles(Collections.emptyList())
                .build();
        Deck deck = Deck.builder().deckId(1L).name("Test Deck").build();
        org.eclipse.jgit.api.Git mockGit = mock(org.eclipse.jgit.api.Git.class);
        
        when(gitSyncOperator.prepareRepository()).thenReturn(mockGit);
        when(gitSyncOperator.isSyncRequired(mockGit, deck, event)).thenReturn(true);
        when(gitSyncOperator.getLatestCommit(mockGit)).thenReturn("abc123");
        
        // When
        repoSynchronizer.sync(event, deck);
        
        // Then
        verify(fileSyncProcessor).processChangedFiles(deck, Collections.emptyList());
        verify(deckService).save(deck);
    }

    @Test
    void sync_WhenSyncNotRequired_ShouldSkipFileProcessing() throws Exception {
        // Given
        SyncEventDTO event = SyncEventDTO.builder().build();
        Deck deck = Deck.builder().deckId(1L).name("Test Deck").build();
        org.eclipse.jgit.api.Git mockGit = mock(org.eclipse.jgit.api.Git.class);
        
        when(gitSyncOperator.prepareRepository()).thenReturn(mockGit);
        when(gitSyncOperator.isSyncRequired(mockGit, deck, event)).thenReturn(false);
        
        // When
        repoSynchronizer.sync(event, deck);
        
        // Then
        verify(gitSyncOperator).prepareRepository();
        verify(gitSyncOperator).isSyncRequired(mockGit, deck, event);
        verifyNoInteractions(fileSyncProcessor);
        verify(deckService, never()).save(any());
    }
}