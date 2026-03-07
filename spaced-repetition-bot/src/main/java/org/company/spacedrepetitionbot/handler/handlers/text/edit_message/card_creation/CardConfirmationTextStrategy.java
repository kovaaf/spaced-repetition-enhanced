package org.company.spacedrepetitionbot.handler.handlers.text.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.handler.handlers.text.edit_message.BaseEditTextStrategy;
import org.company.spacedrepetitionbot.model.CardDraft;
import org.company.spacedrepetitionbot.service.CardDraftService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class CardConfirmationTextStrategy extends BaseEditTextStrategy {
    private final CardDraftService cardDraftService;
    private final KeyboardManager keyboardManager;

    public CardConfirmationTextStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            CardDraftService cardDraftService,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService);
        this.cardDraftService = cardDraftService;
        this.keyboardManager = keyboardManager;
    }

    // Обработка текстовых сообщений в состоянии подтверждения
    @Override
    public void handle(Long chatId, String text) throws TelegramApiException {
        // Это означает, что текст пришел после нажатия "Редактировать вопрос"
        CardDraft draft = cardDraftService.getDraft(chatId)
                .orElseThrow(() -> new IllegalStateException("Draft not found"));

        // Переносим меню ниже сообщения пользователя
        clearPreviousMenu(chatId);
        sendNewMenu(
                chatId,
                "✅ Черновик карты:\n\nВопрос: " + draft.getFront() + "\nОтвет: " + draft.getBack(),
                keyboardManager.getCardDraftConfirmationKeyboard(draft.getDeckId()));
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.CARD_CONFIRMATION;
    }
}