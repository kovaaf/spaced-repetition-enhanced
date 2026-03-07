package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.service.CardDraftService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class EditCardDraftBackStrategy extends BaseEditCallbackStrategy {
    private final CardDraftService cardDraftService;
    private final MessageStateService messageStateService;

    public EditCardDraftBackStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            CardDraftService cardDraftService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.cardDraftService = cardDraftService;
        this.messageStateService = messageStateService;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        return cardDraftService.getDraft(chatId)
                .map(draft -> "✏️ Текущий ответ: " + draft.getBack() + "\n\nВведите новый ответ:")
                .orElse("❌ Черновик карты не найден");
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        return null; // Без клавиатуры для ввода текста
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        try {
            cardDraftService.getDraft(chatId)
                    .ifPresentOrElse(
                            draft -> messageStateService.setUserState(
                                    chatId,
                                    MessageState.EDIT_CARD_DRAFT_BACK.getAlias()),
                            () -> sendDraftNotFound(chatId, callbackQuery));
            super.executeCallbackQuery(callbackQuery);
        } catch (Exception e) {
            log.error("Ошибка редактирования ответа: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Ошибка при редактировании ответа");
        }
    }

    private void sendDraftNotFound(Long chatId, CallbackQuery callbackQuery) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("❌ Черновик карты не найден")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    @Override
    public Callback getPrefix() {
        return Callback.EDIT_CARD_DRAFT_BACK;
    }
}