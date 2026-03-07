package org.company.spacedrepetitionbot.handler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.NonCommandHandler;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.handler.handlers.text.TextStateStrategy;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextStateHandler implements NonCommandHandler {
    private static final String DESCRIPTION = "Текстовый обработчик";

    private final TelegramClient telegramClient;
    private final MessageStateService messageStateService;
    private final Map<MessageState, TextStateStrategy> textStateStrategies;

    @Autowired
    public TextStateHandler(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            List<TextStateStrategy> strategies) {
        this.telegramClient = telegramClient;
        this.messageStateService = messageStateService;
        this.textStateStrategies = strategies.stream()
                .collect(Collectors.toMap(TextStateStrategy::getStateHandlerName, Function.identity()));
    }

    @Override
    public void handle(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        String userState = messageStateService.getUserState(chatId);

        try {
            TextStateStrategy strategy = getTextStateStrategy(userState);
            strategy.handle(chatId, text);
        } catch (TelegramApiException e) {
            log.error("Ошибка обработки текста: {}", e.getMessage(), e);
            sendErrorMessage(chatId);
        }
    }

    private TextStateStrategy getTextStateStrategy(String userState) {
        if (userState == null) {
            return textStateStrategies.get(MessageState.DEFAULT);
        }

        String stateAlias = userState.split(MessageState.STATE_DELIMITER.getAlias())[0];

        return textStateStrategies.getOrDefault(
                MessageState.from(stateAlias),
                textStateStrategies.get(MessageState.DEFAULT));
    }

    private void sendErrorMessage(Long chatId) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("Произошла ошибка. Попробуйте еще раз.")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage());
        }
    }

    @Override
    public String getHandlerName() {
        return "textHandler";
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }
}
