package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.service.UserInfoService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

// TODO что-то сломалось, ни клавиатура не отображается
@Component
public class DeckListMenuStrategy extends BaseEditCallbackStrategy {
    private final KeyboardManager keyboardManager;
    private final DeckService deckService;
    private final UserInfoService userInfoService;

    public DeckListMenuStrategy(
            TelegramClient telegramClient,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            KeyboardManager keyboardManager,
            DeckService deckService,
            UserInfoService userInfoService) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.keyboardManager = keyboardManager;
        this.deckService = deckService;
        this.userInfoService = userInfoService;
    }

    @Override
    public void executeCallbackQuery(CallbackQuery callbackQuery) {
        messageStateService.clearUserState(callbackQuery.getMessage().getChatId());
        super.executeCallbackQuery(callbackQuery);
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        return "Список колод:";
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        List<Deck> decks = deckService.getUserDecks(chatId);
        boolean showCopyButton = !userInfoService.hasUserCopiedDefaultDeck(chatId);
        return keyboardManager.getDeckListKeyboard(decks, showCopyButton);
    }

    @Override
    public Callback getPrefix() {
        return Callback.DECK_LIST;
    }
}
