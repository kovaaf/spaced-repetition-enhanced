package org.company.spacedrepetitionbot.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.constants.MessageConstants;
import org.company.spacedrepetitionbot.exception.CardAlreadyExistsException;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.UserInfo;
import org.company.spacedrepetitionbot.repository.CardRepository;
import org.company.spacedrepetitionbot.repository.DeckRepository;
import org.company.spacedrepetitionbot.repository.UserInfoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.company.spacedrepetitionbot.constants.MessageConstants.*;

@Slf4j
@Component
public class CardService {
    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final UserInfoRepository userInfoRepository;

    public CardService(
            CardRepository cardRepository,
            DeckRepository deckRepository,
            UserInfoRepository userInfoRepository) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.userInfoRepository = userInfoRepository;
    }

    private static String formatErrorMessage(MessageConstants template, Object... args) {
        return String.format(template.getMessage(), args);
    }

    @Transactional(readOnly = true)
    public String getCardDetails(Long userChatId, String deckName, String front) {
        try {
            Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
            Card card = getCardByFrontOrThrow(deck, front);

            return formatCardDetails(card);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка получения карточки: {}", e.getMessage());
            return formatErrorMessage(ERROR_RETRIEVING_CARD, e);
        }
    }

    @Transactional
    public String addCard(Long userChatId, String deckName, String front, String back) {
        Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
        validateCardNotExists(deck, front);

        Card card = Card.builder().front(front).back(back).deck(deck).build();

        cardRepository.save(card);
        return String.format(CARD_ADDED_SUCCESSFULLY.getMessage(), front, deckName);
    }

    @Transactional
    public void addCard(Long deckId, String front, String back) {
        try {
            Deck deck = deckRepository.findById(deckId)
                    .orElseThrow(() -> new IllegalArgumentException("Колода не найдена"));

            // Проверка существования карточки
            if (cardRepository.existsByDeckAndFrontIgnoreCase(deck, front)) {
                return;
            }

            Card card = Card.builder().front(front).back(back).deck(deck).build();

            cardRepository.save(card);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка добавления карточки: {}", e.getMessage());
            formatErrorMessage(ERROR_ADDING_CARD, e);
        }
    }

    @Transactional
    public String deleteCard(Long userChatId, String deckName, String front) {
        try {
            Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
            Card card = getCardByFrontOrThrow(deck, front);

            cardRepository.delete(card);
            return String.format(CARD_DELETED_SUCCESSFULLY.getMessage(), front, deckName);
        } catch (Exception e) {
            log.error("Ошибка удаления карты: {}", e.getMessage(), e);
            return formatErrorMessage(ERROR_DELETING_CARD, e);
        }
    }

    @Transactional
    public boolean deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карточка не найдена"));
        cardRepository.delete(card);
        return true;
    }

    public void deleteCard(Card card) {
        cardRepository.delete(card);
    }

    @Transactional
    public String updateCardFront(Long userChatId, String deckName, String currentFront, String newFront) {
        try {
            Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
            Card card = getCardByFrontOrThrow(deck, currentFront);

            if (cardRepository.existsByDeckAndFrontIgnoreCase(deck, newFront)) {
                return String.format(CARD_ALREADY_EXISTS_SIMPLE.getMessage(), newFront, deckName);
            }

            card.setFront(newFront);
            cardRepository.save(card);
            return String.format(CARD_FRONT_UPDATED.getMessage(), currentFront, newFront);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка переименования карточки: {}", e.getMessage());
            return formatErrorMessage(ERROR_UPDATING_CARD_FRONT, e);
        }
    }

    @Transactional
    public String updateCardBack(Long userChatId, String deckName, String front, String newBack) {
        try {
            Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
            Card card = getCardByFrontOrThrow(deck, front);

            card.setBack(newBack);
            cardRepository.save(card);
            return String.format(CARD_BACK_UPDATED.getMessage(), front);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка редактирования ответа карточки: {}", e.getMessage());
            return formatErrorMessage(ERROR_UPDATING_CARD_BACK, e);
        }
    }

    @Transactional
    public String moveCard(Long userChatId, String sourceDeckName, String cardFront, String targetDeckName) {
        try {
            Deck sourceDeck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, sourceDeckName);
            Card card = getCardByFrontOrThrow(sourceDeck, cardFront);
            Deck targetDeck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, targetDeckName);

            if (sourceDeck.equals(targetDeck)) {
                return String.format(CARD_ALREADY_IN_DECK.getMessage(), cardFront, targetDeckName);
            }

            if (cardRepository.existsByDeckAndFrontIgnoreCase(targetDeck, cardFront)) {
                return formatErrorMessage(CARD_ALREADY_EXISTS_TEMPLATE, cardFront, targetDeckName);
            }

            card.setDeck(targetDeck);
            cardRepository.save(card);
            return String.format(CARD_MOVED_SUCCESSFULLY.getMessage(), cardFront, sourceDeckName, targetDeckName);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка перемещения карты: {}", e.getMessage());
            return formatErrorMessage(ERROR_MOVING_CARD, e);
        }
    }

    // TODO доставать все записи тяжело, нужно прикрутить пейджинг(плюс кнопки в меню) и вытаскивать с лимитом
    @Transactional
    public String getAllCardsInDeck(Long userChatId, String deckName) {
        try {
            Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
            List<Card> cards = cardRepository.findByDeck(deck);

            if (cards.isEmpty()) {
                return String.format(NO_CARDS_IN_DECK.getMessage(), deckName);
            }

            return cards.stream()
                    .map(card -> String.format(CARD_FORMAT.getMessage(), card.getFront(), card.getBack()))
                    .collect(Collectors.joining("\n"));
        } catch (IllegalArgumentException e) {
            log.error("Ошибка получения карточек: {}", e.getMessage());
            return formatErrorMessage(ERROR_RETRIEVING_CARDS, e);
        }
    }

    private Deck getDeckByOwnerIdAndDeckNameOrThrow(Long userChatId, String deckName) {
        UserInfo owner = getUserInfoOrThrow(userChatId);
        return getDeckOrThrow(owner, deckName);
    }

    private UserInfo getUserInfoOrThrow(Long userChatId) {
        return userInfoRepository.findById(userChatId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        USER_NOT_FOUND.getMessage(),
                        userChatId)));
    }

    private Deck getDeckOrThrow(UserInfo owner, String deckName) {
        return deckRepository.findByNameIgnoreCaseAndOwner(deckName, owner)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        DECK_NOT_FOUND_SIMPLE.getMessage(),
                        deckName)));
    }

    private Card getCardByFrontOrThrow(Deck deck, String cardFront) {
        return cardRepository.findByDeckAndFrontIgnoreCase(deck, cardFront)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        CARD_NOT_FOUND_SIMPLE.getMessage(),
                        cardFront,
                        deck.getName())));
    }

    private String formatErrorMessage(MessageConstants constant, Exception e) {
        return String.format("%s%s", constant.getMessage(), e.getMessage());
    }

    // TODO в зависимости от длительности интервала разные хроноюниты
    private String formatCardDetails(Card card) {
        long seconds = 0;
        if (card.getNextReviewTime() != null) {
            seconds = Duration.between(LocalDateTime.now(), card.getNextReviewTime()).getSeconds();
        }

        return String.format(
                CARD_DETAILS_TEMPLATE.getMessage(),
                card.getRepeatCount(),
                formatReviewInterval(seconds),
                formatNextReviewTime(card.getNextReviewTime()),
                card.getStatus(),
                card.getFront(),
                card.getBack());
    }

    private String formatReviewInterval(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return String.format(INTERVAL_DAYS.getMessage(), days);
        } else if (hours > 0) {
            return String.format(INTERVAL_HOURS.getMessage(), hours);
        } else {
            return String.format(INTERVAL_MINUTES.getMessage(), minutes);
        }
    }

    private String formatNextReviewTime(LocalDateTime nextReviewTime) {
        if (nextReviewTime == null) {
            return NOT_SCHEDULED.getMessage();
        }

        return nextReviewTime.isBefore(LocalDateTime.now()) ?
                DUE_TODAY.getMessage() :
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT.getMessage()).format(nextReviewTime);
    }

    public Optional<String> getCardDetails(Long cardId) {
        return cardRepository.findById(cardId)
                .map(card -> String.format("Вопрос: %s\nОтвет: %s", card.getFront(), card.getBack()));
    }

    public Optional<Long> getDeckIdByCardId(Long cardId) {
        return cardRepository.findById(cardId).map(card -> card.getDeck().getDeckId());
    }

    @Transactional
    public String updateCardFront(Long cardId, String newFront) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карточка не найдена"));

        String oldFront = card.getFront();
        card.setFront(newFront);
        cardRepository.save(card);

        return String.format("✅ Вопрос обновлен:\nБыло: %s\nСтало: %s", oldFront, newFront);
    }

    @Transactional
    public String updateCardBack(Long cardId, String newBack) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карточка не найдена"));

        String oldBack = card.getBack();
        card.setBack(newBack);
        cardRepository.save(card);

        return String.format("✅ Ответ обновлен:\nБыло: %s\nСтало: %s", oldBack, newBack);
    }

    @Transactional(readOnly = true)
    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException("Card not found"));
    }

    public void save(Card card) {
        cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public int getActualCardCount(Deck deck) {
        return cardRepository.countByDeck(deck);
    }

    public void deleteByDeckAndSourceFilePath(Deck deck, String folderPath) {
        cardRepository.deleteByDeckAndSourceFilePath(deck, folderPath);
    }

    public void deleteByDeckAndSourceFilePathAndFrontNotIn(Deck deck, String sourceFilePath, List<String> validFronts) {
        cardRepository.deleteByDeckAndSourceFilePathAndFrontNotIn(deck, sourceFilePath, validFronts);
    }

    public Optional<Card> getBySourceFilePathAndSourceHeading(String sourceFilePath, String sourceHeading) {
        return cardRepository.findBySourceFilePathAndSourceHeading(sourceFilePath, sourceHeading);
    }

    public int countByDeckAndSourceFilePath(Deck deck, String relativePath) {
        return cardRepository.countByDeckAndSourceFilePath(deck, relativePath);
    }

    public int deleteByDeckAndSourceFilePathNotIn(Deck deck, Set<String> processedFilePaths) {
        return cardRepository.deleteByDeckAndSourceFilePathNotIn(deck, processedFilePaths);
    }

    private void validateCardNotExists(Deck deck, String front) {
        if (cardRepository.existsByDeckAndFrontIgnoreCase(deck, front)) {
            throw new CardAlreadyExistsException("Card with front '%s' already exists in deck '%s'".formatted(
                    front,
                    deck.getName()));
        }
    }
}
