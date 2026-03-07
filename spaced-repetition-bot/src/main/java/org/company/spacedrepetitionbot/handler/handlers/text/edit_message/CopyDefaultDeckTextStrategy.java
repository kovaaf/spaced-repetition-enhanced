package org.company.spacedrepetitionbot.handler.handlers.text.edit_message;

import org.company.spacedrepetitionbot.handler.handlers.text.MessageState;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.UserInfoService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
public class CopyDefaultDeckTextStrategy extends BaseEditTextStrategy {
    private final DeckService deckService;
    private final UserInfoService userInfoService;
    private final KeyboardManager keyboardManager;

    public CopyDefaultDeckTextStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            DeckService deckService,
            UserInfoService userInfoService,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService);
        this.deckService = deckService;
        this.userInfoService = userInfoService;
        this.keyboardManager = keyboardManager;
    }

    @Override
    public void handle(Long chatId, String deckName) throws TelegramApiException {
        clearPreviousMenu(chatId);

        if (deckName.length() > 100) {
            sendNewMenu(
                    chatId,
                    "❌ Слишком длинное название (макс. 100 символов)",
                    keyboardManager.getBackToDeckListKeyboard());
            return;
        }

        String result = deckService.copyDefaultDeck(chatId, deckName);

        // Проверка успешности операции
        if (result.startsWith("✅")) {
            List<Deck> decks = deckService.getUserDecks(chatId);
            boolean showCopyButton = !userInfoService.hasUserCopiedDefaultDeck(chatId);
            sendNewMenu(chatId, result, keyboardManager.getDeckListKeyboard(decks, showCopyButton));
        } else {
            // В случае ошибки показываем сообщение с кнопкой отмены
            sendNewMenu(chatId, result, keyboardManager.getBackToDeckListKeyboard());
        }

        messageStateService.clearUserState(chatId);
    }

    @Override
    public MessageState getStateHandlerName() {
        return MessageState.COPY_DEFAULT_DECK;
    }
}