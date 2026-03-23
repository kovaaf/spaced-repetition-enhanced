package org.company.spacedrepetitionbot.service.default_deck.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.exception.SyncException;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.sync.git.GitSyncOperator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.TransportException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates the synchronization of the default deck with its Git repository.
 * <p>
 * This class orchestrates the entire sync process:
 * <ul>
 *   <li>Prepares the local Git repository (clones or resets).</li>
 *   <li>Checks whether a sync is needed (commit change or forced sync).</li>
 *   <li>Delegates file processing to {@link FileSyncProcessor}.</li>
 *   <li>Updates the deck's last sync commit.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepoSynchronizer {
    private final DeckService deckService;
    private final FileSyncProcessor fileSyncProcessor;
    private final GitSyncOperator gitSyncOperator;

    /**
     * Performs synchronization for the given event and deck.
     *
     * @param event the sync event (may force full sync or contain changed files)
     * @param deck  the deck to synchronize; if {@code null}, the default deck is initialized
     * @throws SyncException if any error occurs during the sync process
     */
    @Transactional
    public void sync(SyncEventDTO event, Deck deck) {
        try {
            if (deck == null) {
                deck = deckService.initializeDefaultDeck();
            }

            try (Git git = gitSyncOperator.prepareRepository()) {
                log.info("Подготовка репозитория окончена.");
                if (gitSyncOperator.isSyncRequired(git, deck, event)) {
                    log.info("Начинаю синхронизацию");
                    executeSync(git, deck, event);
                    log.info("Синхронизация БД с репозиторием выполнена");
                }
            }
        } catch (Exception e) {
            log.debug("Sync failed with exception type: {}, message: {}", e.getClass().getSimpleName(), e.getMessage());
            
            // Log specific exception details for common auth/network failures
            if (e instanceof TransportException) {
                log.debug("TransportException details - likely network or authentication issue", e);
            } else if (e instanceof GitAPIException) {
                log.debug("GitAPIException details - Git operation failed", e);
            }
            handleSyncError(deck, e);
        }
    }

    /**
     * Executes the actual sync after the repository is ready.
     *
     * @param git   the prepared Git repository
     * @param deck  the deck being synced
     * @param event the sync event
     * @throws Exception if any operation fails
     */
    private void executeSync(Git git, Deck deck, SyncEventDTO event) throws Exception {
        log.debug("Sync started for deck: {}", deck.getName());
        String latestCommit = gitSyncOperator.getLatestCommit(git);

        if (event.isForceFullSync()) {
            fileSyncProcessor.processAllMarkdownFiles(deck);
        } else {
            fileSyncProcessor.processChangedFiles(deck, event.getChangedFiles());
        }

        updateDeckMetadata(deck, latestCommit);
    }

    /**
     * Updates the deck's last known commit hash and persists it.
     *
     * @param deck       the deck to update
     * @param commitHash the new commit hash
     */
    private void updateDeckMetadata(Deck deck, String commitHash) {
        log.debug("Deck hash {} replaced with {}", deck.getLastSyncCommit(), commitHash);
        deck.setLastSyncCommit(commitHash);
        deckService.save(deck);
    }

    /**
     * Handles errors during sync by logging and throwing a {@link SyncException}.
     *
     * @param deck the deck being synced (may be null)
     * @param e    the caught exception
     * @throws SyncException always thrown with details
     */
    private void handleSyncError(Deck deck, Exception e) {
        String deckInfo = (deck != null) ? "deckId: " + deck.getDeckId() : "global sync";
        log.error("Sync failed for {}", deckInfo, e);
        throw new SyncException("Sync failed: " + deckInfo, e);
    }
}
