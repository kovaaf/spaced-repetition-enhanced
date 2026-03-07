package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.edit_card;

import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class EditExistingCardStrategy extends BaseEditCallbackStrategy {
    private final CardService cardService;
    private final KeyboardManager keyboardManager;

    public EditExistingCardStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            CardService cardService,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.cardService = cardService;
        this.keyboardManager = keyboardManager;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long cardId = getLastDataElementFromCallback(callbackQuery.getData());
        return cardService.getCardDetails(cardId).orElse("❌ Карточка не найдена");
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long cardId = getLastDataElementFromCallback(callbackQuery.getData());
        Long deckId = cardService.getDeckIdByCardId(cardId).orElseThrow();
        return keyboardManager.getExistingCardEditKeyboard(cardId, deckId);
    }

    @Override
    public Callback getPrefix() {
        return Callback.EDIT_EXISTING_CARD;
    }
}
