package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class DeckMenuStrategy extends BaseEditCallbackStrategy {
    private final DeckService deckService;
    private final KeyboardManager keyboardManager;
    private final LearningSessionService learningSessionService;

    public DeckMenuStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            DeckService deckService,
            KeyboardManager keyboardManager,
            LearningSessionService learningSessionService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.deckService = deckService;
        this.keyboardManager = keyboardManager;
        this.learningSessionService = learningSessionService;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        String deckName = deckService.getDeckById(deckId).map(Deck::getName).orElse("Неизвестная колода");

        return String.format("Меню колоды \"%s\":", deckName);
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        LearningSession session = learningSessionService.getOrCreateSession(deckId);

        int newCards = learningSessionService.countNewCardsInSession(session.getSessionId());
        int reviewCards = learningSessionService.countReviewCardsInSession(session.getSessionId());

        // Возвращаемся в меню колоды
        return keyboardManager.getDeckMenuKeyboard(deckId, newCards, reviewCards);
    }

    @Override
    public Callback getPrefix() {
        return Callback.DECK_MENU;
    }
}
