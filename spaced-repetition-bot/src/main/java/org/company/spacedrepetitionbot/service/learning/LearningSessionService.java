package org.company.spacedrepetitionbot.service.learning;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.config.SessionProperties;
import org.company.spacedrepetitionbot.constants.Quality;
import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.exception.DeckNotFoundException;
import org.company.spacedrepetitionbot.exception.SessionCompletedException;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.repository.CardRepository;
import org.company.spacedrepetitionbot.repository.DeckRepository;
import org.company.spacedrepetitionbot.repository.LearningSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningSessionService {
    private final LearningSessionRepository learningSessionRepository;
    private final CardRepository cardRepository;
    private final SessionProperties sessionProperties;
    private final SM2Algorithm sm2Algorithm;
    private final DeckRepository deckRepository;

    //    @Transactional(readOnly = true)
    //    public Card getCardFromDeckWithMinimumInterval(Long userChatId, String deckName) {
    //        Deck deck = getDeckByOwnerIdAndDeckNameOrThrow(userChatId, deckName);
    //        return cardRepository.findCardByDeckWithNearestNextReviewTime(deck, List.of(Status.SUSPENDED, Status
    //        .BURIED))
    //                .orElseThrow(() -> new EntityNotFoundException("No cards found"));
    //    }

    //    @Scheduled(cron = "0 0 0 * * ?")
    //    public void resetBuriedCards() {
    //        cardRepository.findByStatus(Status.BURIED).forEach(card -> {
    //            // TODO назначать статус в зависимости от старости как в алгоритме
    //            card.setStatus(Status.REVIEW_YOUNG);
    //            cardRepository.save(card);
    //        });
    //    }

    @Transactional(readOnly = true)
    public String getCardAnswerById(Long cardId) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException("Card not found"));
        return "Вопрос:\n" + card.getFront() + "\n\nОтвет:\n" + card.getBack();
    }

    @Transactional
    public Card updateCardWithAnswer(Long cardId, Quality quality) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException("Card not found"));

        if (card.getStatus() == Status.BURIED || card.getStatus() == Status.SUSPENDED) {
            return card; // Игнорируем обработку
        }

        sm2Algorithm.updateCardWithSMTwoAlgorithm(card, quality);
        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public Card getNextCard(Long deckId) {
        LearningSession session = getOrCreateSession(deckId);
        return getNextCardInSession(session.getSessionId());
    }

    @Transactional
    public LearningSession getOrCreateSession(Long deckId) {
        LocalDateTime now = LocalDateTime.now();
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new DeckNotFoundException("Колода не найдена"));

        Optional<LearningSession> existingSession = learningSessionRepository.findByDeck(deck);

        // Если сессия существует и содержит карточки - возвращаем её
        if (existingSession.isPresent() && !existingSession.get().getCards().isEmpty()) {
            return existingSession.get();
        }

        // Если сессия существует, но пустая - удаляем и создаём новую
        existingSession.ifPresent(learningSessionRepository::delete);

        return createNewSession(deck, now);
    }

    private LearningSession createNewSession(Deck deck, LocalDateTime now) {
        log.debug("Creating new session for deck: {}", deck.getDeckId());
        LearningSession session = new LearningSession();
        session.setDeck(deck);
        session.setCreatedAt(now);

        // Получаем карточки для изучения с приоритетом: LEARNING/RELEARNING
        List<Card> learningCards = cardRepository.findCardsForSession(
                deck,
                List.of(Status.LEARNING, Status.RELEARNING),
                PageRequest.of(0, sessionProperties.getMaxNewCards()));

        // Добавляем карты на ревью со следующим временем ревью меньше текущего времени
        List<Card> reviewCardsToday = cardRepository.findOverdueReviewCards(
                deck, LocalDateTime.now(), PageRequest.of(
                        0,
                        sessionProperties.getMaxReviewCards() + sessionProperties.getMaxNewCards() -
                                learningCards.size()));

        // Добавляем NEW карточки
        int remainingNewCards = sessionProperties.getMaxNewCards() - learningCards.size();
        List<Card> newCards = Collections.emptyList();
        if (remainingNewCards > 0) {
            newCards = cardRepository.findCardsForSession(
                    deck,
                    List.of(Status.NEW),
                    PageRequest.of(0, remainingNewCards));
        }
        learningCards.addAll(newCards);

        // Добавляем карты на ревью по минимальному следующему времени ревью, если осталось место
        int remainingReviewSlots = sessionProperties.getMaxReviewCards() - reviewCardsToday.size();
        List<Card> reviewCards = Collections.emptyList();
        if (remainingReviewSlots > 0) {
            reviewCards = cardRepository.findCardsForSession(
                    deck,
                    List.of(Status.REVIEW_YOUNG, Status.REVIEW_MATURE),
                    PageRequest.of(0, remainingReviewSlots));
        }

        List<Card> allCards = Stream.of(learningCards, reviewCardsToday, reviewCards)
                .flatMap(List::stream)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Card::getCardId,
                                Function.identity(),
                                (existing, replacement) -> existing),
                        map -> new ArrayList<>(map.values())));

        session.setCards(allCards);
        log.debug(
                "Session created with {} cards ({} learning, {} review today, {} new, {} review)",
                allCards.size(),
                learningCards.size(),
                reviewCardsToday.size(),
                newCards.size(),
                reviewCards.size());
        return learningSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Card getNextCardInSession(Long sessionId) {
        return learningSessionRepository.findById(sessionId).map(session -> {
            if (session.getCards().isEmpty()) {
                throw new SessionCompletedException("Сессия завершена! Все карточки изучены.");
            }
            return session.getCards().get(0);
        }).orElseThrow(() -> new EntityNotFoundException("Session not found"));
    }

    @Scheduled(cron = "${app.session.reset-cron}")
    @Transactional
    public void resetSessions() {
        try {
            log.info("Resetting learning sessions");
            learningSessionRepository.deleteAll();
            log.info("All learning sessions have been reset");
        } catch (Exception e) {
            log.error("Error resetting learning sessions", e);
        }
    }

    @Transactional
    public void removeCardFromSession(Long sessionId, Long cardId) {
        learningSessionRepository.findById(sessionId).ifPresent(session -> {
            session.getCards().removeIf(card -> card.getCardId().equals(cardId));
            learningSessionRepository.save(session);
        });
    }

    @Transactional
    public int getRemainingCardsCount(Long sessionId) {
        return learningSessionRepository.findById(sessionId).map(session -> session.getCards().size()).orElse(0);
    }

    @Transactional(readOnly = true)
    public int countNewCardsInSession(Long sessionId) {
        return learningSessionRepository.findById(sessionId)
                .map(session -> (int) session.getCards()
                        .stream()
                        .filter(card -> EnumSet.of(Status.NEW, Status.LEARNING, Status.RELEARNING)
                                .contains(card.getStatus()))
                        .count())
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int countReviewCardsInSession(Long sessionId) {
        return learningSessionRepository.findById(sessionId)
                .map(session -> (int) session.getCards()
                        .stream()
                        .filter(card -> EnumSet.of(Status.REVIEW_YOUNG, Status.REVIEW_MATURE)
                                .contains(card.getStatus()))
                        .count())
                .orElse(0);
    }
}
