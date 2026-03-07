package org.company.spacedrepetitionbot.handler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.NonCommandHandler;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.CallbackStrategy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.company.spacedrepetitionbot.handler.handlers.callback.Callback.CALLBACK_DELIMITER;

@Slf4j
@Component
public class CallbackQueryHandler implements NonCommandHandler {
    private final TelegramClient telegramClient;
    private final Map<Callback, CallbackStrategy> callbackStrategyMap;

    public CallbackQueryHandler(TelegramClient telegramClient, List<CallbackStrategy> callbackStrategies) {
        this.telegramClient = telegramClient;
        this.callbackStrategyMap = callbackStrategies.stream()
                .collect(Collectors.toMap(CallbackStrategy::getPrefix, Function.identity()));
    }

    @Override
    public void handle(Update update) {
        if (!update.hasCallbackQuery()) {
            return;
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        try {
            CallbackStrategy strategy = getCallbackStrategy(callbackData);
            if (strategy != null) {
                strategy.executeCallbackQuery(callbackQuery);
            } else {
                log.warn("Неизвестный callback: {}", callbackData);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки callback {}: {}", callbackData, e.getMessage(), e);
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте еще раз.");
        }
    }

    @Override
    public String getHandlerName() {
        return "callBackHandler";
    }

    private CallbackStrategy getCallbackStrategy(String callbackData) {
        String callbackDelimiter = callbackData.split(CALLBACK_DELIMITER.getAlias())[0];
        return callbackStrategyMap.get(Callback.from(callbackDelimiter));
    }

    private void sendErrorMessage(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage());
        }
    }
}
