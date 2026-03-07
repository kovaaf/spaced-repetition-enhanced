package org.company.spacedrepetitionbot.handler.handlers.text.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.handler.handlers.text.edit_message.BaseEditTextStrategy;
import org.company.spacedrepetitionbot.model.CardDraft;
import org.company.spacedrepetitionbot.service.CardDraftService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class EditCardDraftFrontTextStrategy extends BaseEditTextStrategy {
    private final CardDraftService cardDraftService;
    private final KeyboardManager keyboardManager;

    @Autowired
    public EditCardDraftFrontTextStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            CardDraftService cardDraftService,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService);
        this.cardDraftService = cardDraftService;
        this.keyboardManager = keyboardManager;
    }

    @Override
    public void handle(Long chatId, String newFront) throws TelegramApiException {
        CardDraft draft = cardDraftService.getDraft(chatId).orElseThrow(() -> {
            try {
                sendErrorMessage(chatId, "❌ Черновик карты не найден");
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки сообщения: {}", e.getMessage());
            }
            return new IllegalStateException("Draft not found for chat: " + chatId);
        });

        // Обновляем только фронт
        cardDraftService.updateFront(chatId, newFront);

        // Возвращаем в состояние подтверждения карты
        messageStateService.setUserState(chatId, MessageState.CARD_CONFIRMATION.getAlias());

        clearPreviousMenu(chatId);

        // Формируем текст с обновленными данными
        String cardText = "✅ Вопрос обновлен:\n\n" +
                "Вопрос: " +
                newFront +
                "\n" +
                "Ответ: " +
                (draft.getBack() != null ? draft.getBack() : "[ещё не введен]");

        sendNewMenu(chatId, cardText, keyboardManager.getCardDraftConfirmationKeyboard(draft.getDeckId()));
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.EDIT_CARD_DRAFT_FRONT;
    }

    private void sendErrorMessage(Long chatId, String message) throws TelegramApiException {
        sendNewMenu(chatId, message, null);
    }
}
