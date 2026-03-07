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
public class CopyDefaultDeckStrategy extends BaseEditCallbackStrategy {
    private final MessageStateService messageStateService;
    private final KeyboardManager keyboardManager;

    public CopyDefaultDeckStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.messageStateService = messageStateService;
        this.keyboardManager = keyboardManager;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        return "Введите название для новой колоды:";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        return keyboardManager.getBackToDeckListKeyboard();
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        messageStateService.setUserState(chatId, MessageState.COPY_DEFAULT_DECK.getAlias());
        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    public Callback getPrefix() {
        return Callback.COPY_DEFAULT_DECK;
    }
}
