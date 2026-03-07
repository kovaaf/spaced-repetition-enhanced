package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

//@Component
public class SuspendCardStrategy extends BaseEditCallbackStrategy {
    private final CardService cardService;
    private final KeyboardManager keyboardManager;
    private final LearningSessionService learningSessionService;

    public SuspendCardStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            CardService cardService,
            KeyboardManager keyboardManager,
            LearningSessionService learningSessionService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.keyboardManager = keyboardManager;
        this.cardService = cardService;
        this.learningSessionService = learningSessionService;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long cardId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 1));
        Card card = cardService.getCardById(cardId);

        return "Статус карточки: " + (card.getStatus() == Status.SUSPENDED ? "Возобновлена" : "Приостановлена");
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long cardId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 1));
        Long deckId = Long.valueOf(getCallbackDataByIndex(callbackQuery.getData(), 2));
        Card card = cardService.getCardById(cardId);

        // Обновляем статус
        card.setStatus(card.getStatus() == Status.SUSPENDED ? Status.REVIEW_YOUNG : Status.SUSPENDED);
        cardService.save(card);

        LearningSession session = learningSessionService.getOrCreateSession(deckId);

        int newCards = learningSessionService.countNewCardsInSession(session.getSessionId());
        int reviewCards = learningSessionService.countReviewCardsInSession(session.getSessionId());

        // Возвращаемся в меню колоды
        return keyboardManager.getDeckMenuKeyboard(deckId, newCards, reviewCards);
    }

    @Override
    public Callback getPrefix() {
        return Callback.SUSPEND;
    }
}