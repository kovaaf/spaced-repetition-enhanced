package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.learn;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.exception.SessionCompletedException;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class LearnDeckStrategy extends BaseEditCallbackStrategy {
    private final KeyboardManager keyboardManager;
    private final DeckService deckService;
    private final LearningSessionService learningSessionService;

    public LearnDeckStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            KeyboardManager keyboardManager,
            DeckService deckService,
            LearningSessionService learningSessionService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.keyboardManager = keyboardManager;
        this.deckService = deckService;
        this.learningSessionService = learningSessionService;
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());

        if (isDeckEmpty(deckId)) {
            handleEmptyDeck(chatId, deckId, callbackQuery);
            return;
        }

        LearningSession session = learningSessionService.getOrCreateSession(deckId);
        long remaining = learningSessionService.getRemainingCardsCount(session.getSessionId());

        if (remaining == 0) {
            sendSessionCompletedMessage(chatId, deckId, callbackQuery);
            return;
        }

        messageStateService.clearUserState(callbackQuery.getMessage().getChatId());
        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());

        Long sessionId = learningSessionService.getOrCreateSession(deckId).getSessionId();

        try {
            Card nextCard = learningSessionService.getNextCardInSession(sessionId);
            int remaining = learningSessionService.getRemainingCardsCount(sessionId);
            return String.format("Осталось карт: %d\nВопрос:\n%s", remaining, nextCard.getFront());
        } catch (SessionCompletedException e) {
            return e.getMessage();
        } catch (EntityNotFoundException e) {
            return getDeckName(deckId) + " не содержит карточек для изучения";
        }
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        Card nextCard = learningSessionService.getNextCard(deckId);
        Long nextCardId = nextCard.getCardId();
        LearningSession activeSession = learningSessionService.getOrCreateSession(deckId);

        return keyboardManager.getLearnDeckKeyboard(
                nextCardId,
                deckId,
                activeSession.getSessionId(),
                nextCard.getStatus());
    }

    @Override
    public Callback getPrefix() {
        return Callback.LEARN_DECK;
    }

    private boolean isDeckEmpty(Long deckId) {
        return deckService.getDeckByIdWithCards(deckId).map(deck -> deck.getCards().isEmpty()).orElse(true);
    }

    private void handleEmptyDeck(Long chatId, Long deckId, CallbackQuery callbackQuery) {
        try {
            String deckName = getDeckName(deckId);
            String message = deckName + " не содержит карточек для изучения";

            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text(message)
                    .replyMarkup(keyboardManager.getDeckMenuKeyboard(deckId, 0, 0))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Ошибка при обработке пустой колоды: {}", e.getMessage());
        }
    }

    private String getDeckName(Long deckId) {
        return deckService.getDeckById(deckId).map(Deck::getName).orElse("Эта колода");
    }

    private void sendSessionCompletedMessage(Long chatId, Long deckId, CallbackQuery callbackQuery) {
        LearningSession session = learningSessionService.getOrCreateSession(deckId);

        int newCards = learningSessionService.countNewCardsInSession(session.getSessionId());
        int reviewCards = learningSessionService.countReviewCardsInSession(session.getSessionId());

        try {
            String message = "🎉 Сессия завершена! Все карточки изучены. " +
                    "Новая сессия будет создана при следующем запуске.";
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text(message)
                    .replyMarkup(keyboardManager.getDeckMenuKeyboard(deckId, newCards, reviewCards))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения о завершении сессии: {}", e.getMessage());
        }
    }
}
