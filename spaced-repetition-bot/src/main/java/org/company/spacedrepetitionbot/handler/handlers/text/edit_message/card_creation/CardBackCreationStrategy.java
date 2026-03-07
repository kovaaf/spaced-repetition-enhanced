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
public class CardBackCreationStrategy extends BaseEditTextStrategy {
    private final CardDraftService cardDraftService;
    private final KeyboardManager keyboardManager;

    public CardBackCreationStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            CardDraftService cardDraftService,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService);
        this.cardDraftService = cardDraftService;
        this.keyboardManager = keyboardManager;
    }

    @Override
    public void handle(Long chatId, String text) throws TelegramApiException {
        // Удаляем предыдущее меню, чтобы оно после пересоздания оказалось ниже
        clearPreviousMenu(chatId);

        // сохраняем ответ (бэк) в черновик карты
        cardDraftService.updateBack(chatId, text);

        // Устанавливаем следующий шаг - состояние подтверждения
        messageStateService.setUserState(chatId, MessageState.CARD_CONFIRMATION.getAlias());

        // Отправляем новое сообщение, которое станет текущим меню после ввода ответа пользователем
        CardDraft draft = cardDraftService.getDraft(chatId).orElseThrow(() -> {
            try {
                sendErrorMessage(chatId);
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage(), e);
            }
            return new IllegalStateException("Draft not found for chat: " + chatId);
        });
        String cardText = "✅ Черновик карты создан:\n\nВопрос: " + draft.getFront() + "\nОтвет: " + text;
        sendNewMenu(chatId, cardText, keyboardManager.getCardDraftConfirmationKeyboard(draft.getDeckId()));
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.CARD_BACK_CREATION;
    }

    private void sendErrorMessage(Long chatId) throws TelegramApiException {
        sendNewMenu(chatId, "Ошибка создания карты. Начните заново.", null);
    }
}