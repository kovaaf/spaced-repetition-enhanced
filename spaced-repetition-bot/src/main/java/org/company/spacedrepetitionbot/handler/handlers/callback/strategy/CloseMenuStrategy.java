package org.company.spacedrepetitionbot.handler.handlers.callback.strategy;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class CloseMenuStrategy implements CallbackStrategy {
    private final TelegramClient telegramClient;
    private final MessageStateService messageStateService;

    public CloseMenuStrategy(TelegramClient telegramClient, MessageStateService messageStateService) {
        this.telegramClient = telegramClient;
        this.messageStateService = messageStateService;
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = messageStateService.getMenuMessageId(chatId);

        try {
            telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

            telegramClient.execute(SendMessage.builder().chatId(chatId).text("Общение завершено").build());
        } catch (TelegramApiException e) {
            log.error("Ошибка закрытия меню: {}", e.getMessage());
        } finally {
            messageStateService.clearMenuMessageId(chatId);
        }
        messageStateService.clearUserState(chatId);
    }

    @Override
    public Callback getPrefix() {
        return Callback.CLOSE_MENU;
    }
}
