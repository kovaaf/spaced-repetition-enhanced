package org.company.spacedrepetitionbot.handler.handlers.callback.strategy.edit_message;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.model.Card;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.MessageStateService;
import org.company.spacedrepetitionbot.utils.KeyboardManager;
import org.company.spacedrepetitionbot.utils.MarkdownEscaper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CardListStrategy extends BaseEditCallbackStrategy {
    private final DeckService deckService;
    private final KeyboardManager keyboardManager;

    public CardListStrategy(
            TelegramClient telegramClient,
            DeckService deckService,
            MessageStateService messageStateService,
            MarkdownEscaper markdownEscaper,
            KeyboardManager keyboardManager) {
        super(telegramClient, messageStateService, markdownEscaper);
        this.deckService = deckService;
        this.keyboardManager = keyboardManager;
    }

    @Override
    protected String getMessageText(CallbackQuery callbackQuery) {
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        Optional<Deck> deckOpt = deckService.getDeckByIdWithCards(deckId);

        if (deckOpt.isEmpty()) {
            return "Колода не найдена";
        }

        Deck deck = deckOpt.get();
        // TODO отправлять карты не в виде списка в сообщении, а в виде кнопок
        //  кнопки предоставляют меню, аналогичное тому, что используется в LearnDeckStrategy
        return String.format("Карточки в колоде \"%s\":\n\n%s", deck.getName(), formatCards(deck.getCards()));
    }

    // TODO исправить колоду на гибкую
    @Override
    protected InlineKeyboardMarkup getKeyboard(CallbackQuery callbackQuery) {
        Long deckId = getLastDataElementFromCallback(callbackQuery.getData());
        return keyboardManager.getCardListKeyboard(deckId);
    }

    @Override
    public Callback getPrefix() {
        return Callback.CARD_LIST;
    }

    private String formatCards(Set<Card> cards) {
        if (cards.isEmpty()) {
            return "В колоде пока нет карточек";
        }

        return cards.stream()
                .limit(10) // Ограничиваем количество показываемых карточек
                .map(card -> String.format("• %s → %s", card.getFront(), card.getBack()))
                .collect(Collectors.joining("\n"));
    }
}
