package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class AddDeckStrategy extends BaseEditCallbackStrategy {
    private final KeyboardManager keyboardManager;

    protected AddDeckStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.keyboardManager = keyboardManager;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        return "Введите название новой колоды:";
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        messageStateService.setUserState(chatId, MessageState.DECK_CREATION.getAlias());

        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        return keyboardManager.getAddDeckKeyboard();
    }

    @Override
    public Callback getPrefix() {
        return Callback.ADD_DECK;
    }
}
