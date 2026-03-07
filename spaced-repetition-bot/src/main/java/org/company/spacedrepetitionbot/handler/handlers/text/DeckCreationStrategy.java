package org.company.spacedrepetitionbot.handler.handlers.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.company.spacedrepetitionbot.handler.handlers.text.MessageState.DECK_CREATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckCreationStrategy implements TextStateStrategy {
    private final TelegramClient telegramClient;
    private final DeckService deckService;
    private final MessageStateService messageStateService;
    private final KeyboardManager keyboardManager;

    @Override
    public void handle(Long chatId, String deckName) throws TelegramApiException {
        clearPreviousStateAndMenu(chatId);

        String successMessage = saveDeckAndGetSuccessMessage(chatId, deckName);
        sendSuccessResponseWithMenu(chatId, successMessage);
    }

    private void clearPreviousStateAndMenu(Long chatId) throws TelegramApiException {
        messageStateService.clearUserState(chatId);
        deletePreviousMenu(chatId);
    }

    private void deletePreviousMenu(Long chatId) throws TelegramApiException {
        // Нужно пересоздавать меню, чтобы меню всегда было последним. Из-за сообщений пользователя оно смещается наверх
        Integer menuMessageId = messageStateService.getMenuMessageId(chatId);
        if (menuMessageId != null) {
            telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(menuMessageId).build());
        }
    }

    private String saveDeckAndGetSuccessMessage(Long chatId, String deckName) {
        return deckService.addDeck(chatId, deckName);
    }

    private void sendSuccessResponseWithMenu(Long chatId, String successMessage) throws TelegramApiException {
        String combinedText = successMessage + " Выберите колоду:";
        List<Deck> decks = deckService.getUserDecks(chatId);
        InlineKeyboardMarkup deckListKeyboard = keyboardManager.getDeckListKeyboard(decks, true);

        Message message = telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(combinedText)
                .replyMarkup(deckListKeyboard)
                .build());
        messageStateService.saveMenuMessageId(chatId, message.getMessageId());
    }

    @Override
    public MessageState getStateHandlerName() {
        return DECK_CREATION;
    }
}
