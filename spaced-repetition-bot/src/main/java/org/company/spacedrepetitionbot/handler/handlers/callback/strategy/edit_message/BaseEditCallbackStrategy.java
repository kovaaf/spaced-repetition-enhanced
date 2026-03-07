package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.CallbackStrategy;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
public abstract class BaseEditCallbackStrategy implements CallbackStrategy {
    protected final TelegramClient telegramClient;
    protected final MessageStateService messageStateService;
    protected final MarkdownEscaper markdownEscaper;

    protected BaseEditCallbackStrategy(TelegramClient telegramClient, MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper) {
        this.telegramClient = telegramClient;
        this.messageStateService = messageStateService;
        this.markdownEscaper = markdownEscaper;
    }

    protected abstract String getMessageText(CallbackQuery callbackQuery);

    protected abstract InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery);

    /**
     * Обрабатывает входящий callback-запрос от Telegram.
     * <p>
     * Метод выполняет следующие действия:
     * 1. Получает chatId из входящего сообщения
     * 2. Пытается получить ID сообщения меню из кеша
     * 3. Если ID сообщения не найден в кеше, использует ID текущего сообщения и сохраняет его
     * 4. Вызывает метод для редактирования сообщения с меню
     * 5. В случае возникновения ошибки при редактировании вызывает обработчик исключений
     *
     * @param callbackQuery входящий callback-запрос от Telegram
     */
    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = messageStateService.getMenuMessageId(chatId);

        if (messageId == null) {
            messageId = callbackQuery.getMessage().getMessageId();
            messageStateService.saveMenuMessageId(chatId, messageId);
        }

        try {
            editMenuMessage(chatId, messageId, callbackQuery);
        } catch (TelegramApiException e) {
            handleEditException(chatId, callbackQuery, e);
        }
    }

    protected void editMenuMessage(Long chatId, Integer messageId, CallbackQuery callbackQuery) throws
            TelegramApiException {

        String messageText = getMessageText(callbackQuery);
        String escapedText = markdownEscaper.escapeMarkdownV2(messageText);

        log.debug("Original text: {}", messageText);
        log.debug("Escaped text: {}", escapedText);
        log.debug("Contains '(': {}", messageText.contains("("));
        log.debug("Contains '\\(': {}", escapedText.contains("\\("));

        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(escapedText)
                .parseMode("MarkdownV2")
                .replyMarkup(getKeyboard(callbackQuery))
                .build();

        telegramClient.execute(editMessage);
    }

    protected void handleEditException(Long chatId, CallbackQuery callbackQuery, TelegramApiException e) {
        if (e.getMessage().contains("message to edit not found")) {
            resendMenuMessage(chatId, callbackQuery);
        } else {
            log.error("Ошибка редактирования сообщения: {}", e.getMessage());
        }
    }

    protected void resendMenuMessage(Long chatId, CallbackQuery callbackQuery) {
        try {
            clearPreviousMenu(chatId);

            String messageText = getMessageText(callbackQuery);
            String escapedText = markdownEscaper.escapeMarkdownV2(messageText);

            Message sentMessage = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(escapedText)
                    .parseMode("MarkdownV2")
                    .replyMarkup(getKeyboard(callbackQuery))
                    .build());

            messageStateService.saveMenuMessageId(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException ex) {
            log.error("Ошибка переотправки меню: {}", ex.getMessage());
        }
    }

    private void clearPreviousMenu(Long chatId) throws TelegramApiException {
        Integer menuMessageId = messageStateService.getMenuMessageId(chatId);
        if (menuMessageId != null) {
            telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(menuMessageId).build());
        }
    }

    // TODO Поменять на получения данных по индексу, чтобы можно было в колбэке передавать несколько данных
    // TODO поменять на хранение объектов в колбэках (json?)
    protected Long getLastDataElementFromCallback(String data) {
        if (!data.contains(Callback.CALLBACK_DELIMITER.getAlias())) {
            throw new IllegalArgumentException("Callback data doesn't contain ID: " + data);
        }

        String[] parts = data.split(Callback.CALLBACK_DELIMITER.getAlias());
        return Long.parseLong(parts[parts.length - 1]);
    }

    protected String getCallbackDataByIndex(String data, int index) {
        String[] parts = parseDataFromCallback(data);
        if (index < 0 || index >= parts.length) {
            throw new IllegalArgumentException("Invalid index " + index + " for callback data: " + data);
        }
        return parts[index];
    }

    protected String[] parseDataFromCallback(String data) {
        if (!data.contains(Callback.CALLBACK_DELIMITER.getAlias())) {
            throw new IllegalArgumentException("Callback data doesn't contain delimiter: " + data);
        }
        return data.split(Callback.CALLBACK_DELIMITER.getAlias());
    }

    protected void sendErrorMessage(Long chatId, String message) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(message).build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage());
        }
    }
}
