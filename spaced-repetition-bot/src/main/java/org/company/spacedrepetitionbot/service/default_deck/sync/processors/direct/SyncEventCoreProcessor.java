package org.company.spacedrepetitionbot.service.default_deck.sync.processors.direct;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.sync.RepoSynchronizer;
import org.company.spacedrepetitionbot.service.default_deck.sync.UserDeckSynchronizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core processor that executes a synchronization event within a new transaction.
 * <p>
 * It delegates to {@link RepoSynchronizer} for repository synchronization and,
 * if the deck is the default one, to {@link UserDeckSynchronizer} to propagate changes
 * to user‑specific decks.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SyncEventCoreProcessor {
    private final RepoSynchronizer repoSynchronizer;
    private final UserDeckSynchronizer userDeckSynchronizer;
    private final DeckService deckService;

    /**
     * Processes a synchronization event.
     *
     * @param event the event containing the deck ID, forced flag, and changed files
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(SyncEventDTO event) {
        Deck deck = event.getDeckId() != null ? deckService.getDeckById(event.getDeckId()).orElse(null) : null;

        repoSynchronizer.sync(event, deck);

        if (deck != null && deck.isDefault()) {
            userDeckSynchronizer.syncUserDecks(deck);
        }
    }
}
