package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.model.CardDraft;
import org.company.spacedrepetitionbot.service.CardDraftService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class EditCardDraftFrontStrategy extends BaseEditCallbackStrategy {
    private final CardDraftService cardDraftService;
    private final MessageStateService messageStateService;

    public EditCardDraftFrontStrategy(
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
                .map(draft -> "✏️ Текущий вопрос: " + draft.getFront() + "\n\nВведите новый вопрос:")
                .orElse("❌ Черновик карты не найден");
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        return null;
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        try {
            // Устанавливаем следующий шаг - состояние подтверждения
            messageStateService.setUserState(chatId, MessageState.CARD_CONFIRMATION.getAlias());

            CardDraft draft = cardDraftService.getDraft(chatId).orElseThrow(() -> {
                sendErrorMessage(chatId, "❌ Черновик карты не найден");
                return new IllegalStateException("Draft not found for chat: " + chatId);
            });
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text("Введите новый вопрос для карточки:\n(Текущий: " + draft.getFront() + ")")
                    .replyMarkup(null)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при редактировании вопроса: {}", e.getMessage());
            sendErrorMessage(chatId, "Ошибка при редактировании вопроса");
        }
    }

    @Override
    public Callback getPrefix() {
        return Callback.EDIT_CARD_DRAFT_FRONT;
    }
}