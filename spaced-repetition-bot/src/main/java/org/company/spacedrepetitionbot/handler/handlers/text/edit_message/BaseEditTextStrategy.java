package org.company.spacedrepetitionbot.handler.handlers.text.edit_message;

import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.handler.handlers.text.TextStateStrategy;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public abstract class BaseEditTextStrategy implements TextStateStrategy {
    protected final TelegramClient telegramClient;
    protected final MessageStateService messageStateService;

    protected BaseEditTextStrategy(TelegramClient telegramClient, MessageStateService messageStateService) {
        this.telegramClient = telegramClient;
        this.messageStateService = messageStateService;
    }

    protected void clearPreviousMenu(Long chatId) throws TelegramApiException {
        Integer menuMessageId = messageStateService.getMenuMessageId(chatId);
        if (menuMessageId != null) {
            telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(menuMessageId).build());
        }
    }

    protected void sendNewMenu(Long chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        Message message = telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build());

        messageStateService.saveMenuMessageId(chatId, message.getMessageId());
    }

    protected Long parseIdFromState(String state) {
        if (!state.contains(MessageState.STATE_DELIMITER.getAlias())) {
            throw new IllegalArgumentException("State doesn't contain ID: " + state);
        }

        String[] parts = state.split(MessageState.STATE_DELIMITER.getAlias());
        return Long.parseLong(parts[parts.length - 1]);
    }
}
