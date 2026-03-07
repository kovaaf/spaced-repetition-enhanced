package org.company.spacedrepetitionbot.controller_advice;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@ControllerAdvice
public class StrategyExceptionHandler {
    private final TelegramClient telegramClient;

    public StrategyExceptionHandler(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    // TODO сейчас некорректно работает с меню - пишет новые сообщения, а должен в самом меню сообщение менять
    @ExceptionHandler(EntityNotFoundException.class)
    public void handleEntityNotFound(EntityNotFoundException e, CallbackQuery callbackQuery) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .text(e.getMessage())
                    .build());
        } catch (TelegramApiException ex) {
            log.error("Ошибка отправки сообщения об ошибке: {}", ex.getMessage(), ex);
        }
    }
}
