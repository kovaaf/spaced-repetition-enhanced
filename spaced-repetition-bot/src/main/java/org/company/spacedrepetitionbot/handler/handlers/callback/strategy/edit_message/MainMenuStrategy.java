package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class MainMenuStrategy extends BaseEditCallbackStrategy {
    private final KeyboardManager keyboardManager;

    public MainMenuStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.keyboardManager = keyboardManager;
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        messageStateService.clearUserState(callbackQuery.getMessage().getChatId());
        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        return "Главное меню:";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        return keyboardManager.getMainMenuKeyboard();
    }

    @Override
    public Callback getPrefix() {
        return Callback.MAIN_MENU;
    }
}
