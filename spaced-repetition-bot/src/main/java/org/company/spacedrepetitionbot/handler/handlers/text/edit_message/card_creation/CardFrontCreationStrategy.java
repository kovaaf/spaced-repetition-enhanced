package org.company.spacedrepetitionbot.handler.handlers.text.edit_message.card_creation;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.handler.handlers.text.edit_message.BaseEditTextStrategy;
import org.company.spacedrepetitionbot.service.CardDraftService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class CardFrontCreationStrategy extends BaseEditTextStrategy {
    private final CardDraftService cardDraftService;

    public CardFrontCreationStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            CardDraftService cardDraftService) {
        super(telegramClient, messageStateService);
        this.cardDraftService = cardDraftService;
    }

    @Override
    public void handle(Long chatId, String text) throws TelegramApiException {
        String state = messageStateService.getUserState(chatId);
        Long deckId = parseIdFromState(state);

        // Удаляем предыдущее меню
        clearPreviousMenu(chatId);

        // сохраняем ответ (фронт) в черновик карты
        cardDraftService.saveDraft(chatId, deckId, text, null);

        // устанавливаем следующий шаг - пользователь должен ввести ответ на вопрос
        messageStateService.setUserState(chatId, MessageState.CARD_BACK_CREATION.getAlias());

        // Отправляем новое сообщение, которое станет текущим меню после ввода ответа пользователем
        sendNewMenu(chatId, "Введите ответ на вопрос:\n" + text, null // Без клавиатуры
        );
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.CARD_FRONT_CREATION;
    }
}