package org.company.spacedrepetitionbot.service.default_deck.sync.git;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * High‑level operator for Git synchronization tasks.
 * <p>
 * Delegates to {@link GitService} for actual Git operations and adds
 * decision logic (whether a sync is required).
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GitSyncOperator {
    private final GitService gitService;

    /**
     * Prepares the local Git repository for synchronization (clones or resets).
     *
     * @return a {@link Git} instance ready for use
     * @throws GitAPIException if any Git operation fails
     */
    public Git prepareRepository() throws GitAPIException {
        return gitService.getGitInstanceWithHardReset();
    }

    /**
     * Determines whether a synchronization is required.
     *
     * @param git   the prepared Git repository
     * @param deck  the deck being synced
     * @param event the sync event (may force a full sync)
     * @return {@code true} if the commit has changed or forced sync is requested
     * @throws IOException if the current commit cannot be read
     */
    public boolean isSyncRequired(Git git, Deck deck, SyncEventDTO event) throws IOException {
        if (event.isForceFullSync()) {
            return true;
        }

        String currentCommit = gitService.getLatestCommit(git);
        String lastKnownCommit = deck.getLastSyncCommit();

        return !currentCommit.equals(lastKnownCommit);
    }

    /**
     * Returns the latest commit hash of the current branch.
     *
     * @param git the Git repository
     * @return the commit hash as a string
     * @throws IOException if the commit cannot be read
     */
    public String getLatestCommit(Git git) throws IOException {
        return gitService.getLatestCommit(git);
    }
}
