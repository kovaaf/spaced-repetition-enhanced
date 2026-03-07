package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class AddCardStrategy extends BaseEditCallbackStrategy {
    private final MessageStateService messageStateService;
    private final KeyboardManager keyboardManager;

    public AddCardStrategy(
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
        return "Введите вопрос для новой карточки:";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        return keyboardManager.getAddCardKeyboard(deckId);
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        messageStateService.setUserState(
                chatId,
                MessageState.CARD_FRONT_CREATION.getAlias() + MessageState.STATE_DELIMITER.getAlias() + deckId);

        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    public Callback getPrefix() {
        return Callback.ADD_CARD;
    }
}