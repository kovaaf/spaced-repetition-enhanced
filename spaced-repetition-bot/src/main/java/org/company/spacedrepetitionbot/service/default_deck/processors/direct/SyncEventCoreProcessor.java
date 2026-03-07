package org.company.spacedrepetitionbot.service.default_deck.processors.direct;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.RepoSynchronizer;
import org.company.spacedrepetitionbot.service.default_deck.UserDeckSynchronizer;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncEventCoreProcessor {
    private final RepoSynchronizer repoSynchronizer;
    private final UserDeckSynchronizer userDeckSynchronizer;
    private final DeckService deckService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(SyncEventDTO event) {
        Deck deck = event.getDeckId() != null ? deckService.getDeckById(event.getDeckId()).orElse(null) : null;

        repoSynchronizer.sync(event, deck);

        if (deck != null && deck.isDefault()) {
            userDeckSynchronizer.syncUserDecks(deck);
        }
    }
}
