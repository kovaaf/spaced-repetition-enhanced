package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class GitSyncOperator {
    private final GitService gitService;

    public Git prepareRepository() throws GitAPIException {
        return gitService.getGitInstanceWithHardReset();
    }

    public boolean isSyncRequired(Git git, Deck deck, SyncEventDTO event) throws IOException {
        if (event.isForceFullSync()) {
            return true;
        }

        String currentCommit = gitService.getLatestCommit(git);
        String lastKnownCommit = deck.getLastSyncCommit();

        return !currentCommit.equals(lastKnownCommit);
    }

    public String getLatestCommit(Git git) throws IOException {
        return gitService.getLatestCommit(git);
    }
}
