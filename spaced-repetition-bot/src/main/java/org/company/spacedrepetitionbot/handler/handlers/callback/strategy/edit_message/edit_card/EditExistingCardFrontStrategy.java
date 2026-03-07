// EditExistingCardFrontStrategy.java
package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.edit_card;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message.BaseEditCallbackStrategy;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class EditExistingCardFrontStrategy extends BaseEditCallbackStrategy {
    private final CardService cardService;

    public EditExistingCardFrontStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            CardService cardService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.cardService = cardService;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long cardId = getLastDataElementFromCallback(callbackQuery.getData());
        return cardService.getCardDetails(cardId)
                .map(details -> "✏️ Текущий вопрос:\n" + details.split("\nОтвет:")[0] + "\n\nВведите новый вопрос:")
                .orElse("❌ Карточка не найдена");
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        return null; // Без клавиатуры для ввода текста
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Long cardId = getLastDataElementFromCallback(callbackQuery.getData());

        try {
            messageStateService.setUserState(
                    chatId,
                    MessageState.EDIT_EXISTING_CARD_FRONT.getAlias() +
                            MessageState.STATE_DELIMITER.getAlias() +
                            cardId);
            super.executeCallbackQuery(callbackQuery);
        } catch (Exception e) {
            log.error("Ошибка редактирования вопроса карты {}: {}", cardId, e.getMessage());
            sendErrorMessage(chatId, "Ошибка при редактировании вопроса");
        }
    }

    @Override
    public Callback getPrefix() {
        return Callback.EDIT_EXISTING_CARD_FRONT;
    }
}