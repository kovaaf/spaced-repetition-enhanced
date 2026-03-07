package org.company.spacedrepetitionbot.handler.handlers.text;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface TextStateStrategy {
    void handle(Long chatId, String text) throws TelegramApiException;

    MessageState getStateHandlerName();
}
