package org.company.spacedrepetitionbot.handler.handlers.text.edit_message.edit_card;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.handler.handlers.text.edit_message.BaseEditTextStrategy;
import org.company.spacedrepetitionbot.model.LearningSession;
import org.company.spacedrepetitionbot.service.CardService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.learning.LearningSessionService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class EditExistingCardBackTextStrategy extends BaseEditTextStrategy {
    private final CardService cardService;
    private final KeyboardManager keyboardManager;
    private final LearningSessionService learningSessionService;

    public EditExistingCardBackTextStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            CardService cardService,
            KeyboardManager keyboardManager,
            LearningSessionService learningSessionService) {
        super(telegramClient, messageStateService);
        this.cardService = cardService;
        this.keyboardManager = keyboardManager;
        this.learningSessionService = learningSessionService;
    }

    @Override
    public void handle(Long chatId, String newBack) throws TelegramApiException {
        String state = messageStateService.getUserState(chatId);
        Long cardId = Long.parseLong(state.split(MessageState.STATE_DELIMITER.getAlias())[1]);

        try {
            String result = cardService.updateCardBack(cardId, newBack);
            messageStateService.clearUserState(chatId);

            Long deckId = cardService.getDeckIdByCardId(cardId)
                    .orElseThrow(() -> new IllegalStateException("Deck not found for card: " + cardId));

            clearPreviousMenu(chatId);

            LearningSession session = learningSessionService.getOrCreateSession(deckId);

            int newCards = learningSessionService.countNewCardsInSession(session.getSessionId());
            int reviewCards = learningSessionService.countReviewCardsInSession(session.getSessionId());

            sendNewMenu(chatId, result, keyboardManager.getDeckMenuKeyboard(deckId, newCards, reviewCards));
        } catch (Exception e) {
            log.error("Ошибка обновления ответа карты {}: {}", cardId, e.getMessage());
            sendErrorMessage(chatId, "Ошибка при обновлении ответа");
        }
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.EDIT_EXISTING_CARD_BACK;
    }

    private void sendErrorMessage(Long chatId, String message) throws TelegramApiException {
        sendNewMenu(chatId, message, null);
    }
}
