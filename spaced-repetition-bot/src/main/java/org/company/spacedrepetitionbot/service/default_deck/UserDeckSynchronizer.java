package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.DeckService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeckSynchronizer {
    private final DeckService deckService;
    private final CardService cardService;

    @Transactional
    public void syncUserDecks(Deck defaultDeck) {
        List<Deck> userDecks = deckService.findByDefaultDeckSuffix();
        for (Deck userDeck : userDecks) {
            syncSingleUserDeck(userDeck, defaultDeck);
        }
    }

    private void syncSingleUserDeck(Deck userDeck, Deck defaultDeck) {
        Map<Long, Card> defaultCardsMap = defaultDeck.getCards()
                .stream()
                .collect(Collectors.toMap(Card::getCardId, c -> c));

        Map<Long, Card> userCardsMap = userDeck.getCards()
                .stream()
                .filter(card -> card.getOriginalCardId() != null)
                .collect(Collectors.toMap(Card::getOriginalCardId, c -> c));

        // Обновление существующих карточек
        defaultCardsMap.forEach((origId, defaultCard) -> {
            if (userCardsMap.containsKey(origId)) {
                Card userCard = userCardsMap.get(origId);
                updateCardIfChanged(userCard, defaultCard);
            } else {
                createNewCard(userDeck, defaultCard);
            }
        });

        // Удаление отсутствующих карточек
        Set<Long> currentIds = defaultCardsMap.keySet();
        userDeck.getCards()
                .stream()
                .filter(card -> card.getOriginalCardId() != null)
                .filter(card -> !currentIds.contains(card.getOriginalCardId()))
                .forEach(cardService::deleteCard);
    }

    private void updateCardIfChanged(Card userCard, Card defaultCard) {
        if (!userCard.getFront().equals(defaultCard.getFront()) || !userCard.getBack().equals(defaultCard.getBack())) {

            userCard.setFront(defaultCard.getFront());
            userCard.setBack(defaultCard.getBack());
            resetCardProgress(userCard);
            cardService.save(userCard);
        }
    }

    private void resetCardProgress(Card card) {
        card.setRepeatCount(0);
        card.setEasinessFactor(2.5);
        card.setNextReviewTime(LocalDateTime.now());
        card.setStatus(Status.NEW);
    }

    private void createNewCard(Deck deck, Card defaultCard) {
        Card newCard = Card.builder()
                .front(defaultCard.getFront())
                .back(defaultCard.getBack())
                .originalCardId(defaultCard.getCardId())
                .deck(deck)
                .build();
        cardService.save(newCard);
    }
}
