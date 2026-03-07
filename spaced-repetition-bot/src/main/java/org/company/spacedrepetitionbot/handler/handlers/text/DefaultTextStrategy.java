package org.company.spacedrepetitionbot.handler.handlers.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTextStrategy implements TextStateStrategy {
    private final TelegramClient telegramClient;

    // TODO на основе текущего стейта отправить меню, если возможно
    @Override
    public void handle(Long chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text("""
                Некорректная команда.
                Для помощи введите /help
                Для открытия меню введите /menu""").build());
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.DEFAULT;
    }
}
