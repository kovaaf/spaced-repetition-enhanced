package org.company.spacedrepetitionbot.handler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.NonCommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class NonTextHandler implements NonCommandHandler {
    private static final String DESCRIPTION = "Нетекстовый обработчик";
    private final TelegramClient telegramClient;

    public NonTextHandler(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void handle(Update update) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(update.getMessage().getChatId())
                    .text(DESCRIPTION)
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String getHandlerName() {
        return "nonTextHandler";
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }
}
