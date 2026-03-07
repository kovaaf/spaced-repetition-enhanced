package org.company.spacedrepetitionbot.utils;

import org.company.spacedrepetitionbot.constants.Status;
import org.company.spacedrepetitionbot.handler.handlers.callback.Callback;
import org.company.spacedrepetitionbot.model.Deck;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static org.company.spacedrepetitionbot.handler.handlers.callback.Callback.CALLBACK_DELIMITER;

@Component
public class KeyboardManager {
    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }

    private List<InlineKeyboardRow> createDynamicRows(List<InlineKeyboardButton> buttons, List<Integer> rowSizes) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        int buttonIndex = 0;

        for (int rowSize : rowSizes) {
            if (buttonIndex >= buttons.size()) {
                break;
            }
            int endIndex = Math.min(buttonIndex + rowSize, buttons.size());
            rows.add(new InlineKeyboardRow(buttons.subList(buttonIndex, endIndex)));
            buttonIndex = endIndex;
        }

        if (buttonIndex < buttons.size()) {
            rows.add(new InlineKeyboardRow(buttons.subList(buttonIndex, buttons.size())));
        }
        return rows;
    }

    public InlineKeyboardMarkup getMainMenuKeyboard() {
        List<InlineKeyboardButton> buttons = List.of(
                // TODO добавить кнопку настроек пользователя
                button(Callback.DECK_LIST.getText(), Callback.DECK_LIST.getAlias()),
                button(Callback.CLOSE_MENU.getText(), Callback.CLOSE_MENU.getAlias()));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(2))).build();
    }

    public InlineKeyboardMarkup getDeckListKeyboard(List<Deck> decks, boolean showCopyDefaultButton) {
        List<InlineKeyboardButton> defaultDeckButtons = new ArrayList<>();
        if (showCopyDefaultButton) {
            defaultDeckButtons.add(button("Скопировать дефолтную колоду", Callback.COPY_DEFAULT_DECK.getAlias()));
        }

        List<InlineKeyboardButton> systemButtons = List.of(
                button(Callback.MAIN_MENU.getText(), Callback.MAIN_MENU.getAlias()),
                button(Callback.ADD_DECK.getText(), Callback.ADD_DECK.getAlias()),
                button(Callback.CLOSE_MENU.getText(), Callback.CLOSE_MENU.getAlias()));

        List<InlineKeyboardButton> deckButtons = decks.stream()
                .map(deck -> button(
                        deck.getName(),
                        Callback.DECK_MENU.getAlias() + CALLBACK_DELIMITER.getAlias() + deck.getDeckId()))
                .toList();

        List<InlineKeyboardButton> allButtons = new ArrayList<>();
        allButtons.addAll(defaultDeckButtons);
        allButtons.addAll(systemButtons);
        allButtons.addAll(deckButtons);

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(allButtons, List.of(1, 1, 1, 4))).build();
    }

    public InlineKeyboardMarkup getDeckMenuKeyboard(Long deckId, int newCards, int reviewCards) {
        List<InlineKeyboardButton> buttons = List.of(
                // TODO добавить кнопку настроек колоды
                button(
                        String.format("%s (%d+%d)", Callback.LEARN_DECK.getText(), newCards, reviewCards),
                        Callback.LEARN_DECK.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId),
                button(
                        Callback.ADD_CARD.getText(),
                        Callback.ADD_CARD.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId),
                button(
                        Callback.CARD_LIST.getText(),
                        Callback.CARD_LIST.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId),
                button(Callback.DECK_LIST.getText(), Callback.DECK_LIST.getAlias()),
                button(Callback.CLOSE_MENU.getText(), Callback.CLOSE_MENU.getAlias()));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1, 3, 1))).build();
    }

    public InlineKeyboardMarkup getAddDeckKeyboard() {
        List<InlineKeyboardButton> buttons = List.of(button(
                Callback.DECK_LIST.getText(),
                Callback.DECK_LIST.getAlias()));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1))).build();
    }

    public InlineKeyboardMarkup getAddCardKeyboard(Long deckId) {
        List<InlineKeyboardButton> buttons = List.of(button(
                Callback.DECK_MENU.getText(),
                Callback.DECK_MENU.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1))).build();
    }

    public InlineKeyboardMarkup getCardListKeyboard(Long deckId) {
        List<InlineKeyboardButton> buttons = List.of(
                button(
                        Callback.DECK_MENU.getText(),
                        Callback.DECK_MENU.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId),
                button(
                        Callback.ADD_CARD.getText(),
                        Callback.ADD_CARD.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1))).build();
    }

    public InlineKeyboardMarkup getLearnDeckKeyboard(Long cardId, Long deckId, Long sessionId, Status currentStatus) {
        List<InlineKeyboardButton> buttons = new ArrayList<>(List.of(
                button(
                        Callback.SHOW_ANSWER.getText(),
                        Callback.SHOW_ANSWER.getAlias() +
                                CALLBACK_DELIMITER.getAlias() +
                                cardId +
                                CALLBACK_DELIMITER.getAlias() +
                                deckId +
                                CALLBACK_DELIMITER.getAlias() +
                                sessionId),
// TODO разработать функционал возврата к предыдущей карте
//                button(
//                        Callback.PREVIOUS.getText(),
//                        Callback.PREVIOUS.getAlias() + CALLBACK_DELIMITER.getAlias() + cardId),
                button(
                        Callback.EDIT_EXISTING_CARD.getText(),
                        Callback.EDIT_EXISTING_CARD.getAlias() + CALLBACK_DELIMITER.getAlias() + cardId)

        ));

        //        if (currentStatus == Status.BURIED) {
        //            buttons.add(button(Callback.UNBURY.getText(), Callback.UNBURY.getAlias() + CALLBACK_DELIMITER
        //            .getAlias() + cardId));
        //        } else if (currentStatus == Status.SUSPENDED) {
        //            buttons.add(button(Callback.UNSUSPEND.getText(), Callback.UNSUSPEND.getAlias() +
        //            CALLBACK_DELIMITER.getAlias() + cardId));
        //        } else {
        //            buttons.add(button(Callback.BURY.getText(), Callback.BURY.getAlias() + CALLBACK_DELIMITER
        //            .getAlias() + cardId));
        //            buttons.add(button(Callback.SUSPEND.getText(), Callback.SUSPEND.getAlias() + CALLBACK_DELIMITER
        //            .getAlias() + cardId));
        //        }

        buttons.add(button(
                Callback.DECK_MENU.getText(),
                Callback.DECK_MENU.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId));
        buttons.add(button(Callback.CLOSE_MENU.getText(), Callback.CLOSE_MENU.getAlias()));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1, 2, 2, 1))).build();
    }

    public InlineKeyboardMarkup getShowAnswerKeyboard(Long cardId, Long deckId, Long sessionId, Status currentStatus) {
        List<InlineKeyboardButton> buttons = new ArrayList<>(List.of(
                button(
                        Callback.AGAIN.getText(),
                        Callback.AGAIN.getAlias() +
                                CALLBACK_DELIMITER.getAlias() +
                                cardId +
                                CALLBACK_DELIMITER.getAlias() +
                                deckId +
                                CALLBACK_DELIMITER.getAlias() +
                                sessionId),
                button(
                        Callback.HARD.getText(),
                        Callback.HARD.getAlias() +
                                CALLBACK_DELIMITER.getAlias() +
                                cardId +
                                CALLBACK_DELIMITER.getAlias() +
                                deckId +
                                CALLBACK_DELIMITER.getAlias() +
                                sessionId),
                button(
                        Callback.GOOD.getText(),
                        Callback.GOOD.getAlias() +
                                CALLBACK_DELIMITER.getAlias() +
                                cardId +
                                CALLBACK_DELIMITER.getAlias() +
                                deckId +
                                CALLBACK_DELIMITER.getAlias() +
                                sessionId),
                button(
                        Callback.EASY.getText(),
                        Callback.EASY.getAlias() +
                                CALLBACK_DELIMITER.getAlias() +
                                cardId +
                                CALLBACK_DELIMITER.getAlias() +
                                deckId +
                                CALLBACK_DELIMITER.getAlias() +
                                sessionId),
// TODO разработать функционал возврата к предыдущей карте
//                button(
//                        Callback.PREVIOUS.getText(),
//                        Callback.PREVIOUS.getAlias() + CALLBACK_DELIMITER.getAlias() + cardId),
                button(
                        Callback.EDIT_EXISTING_CARD.getText(),
                        Callback.EDIT_EXISTING_CARD.getAlias() + CALLBACK_DELIMITER.getAlias() + cardId)

        ));

        //        if (currentStatus == Status.BURIED) {
        //            buttons.add(button(Callback.UNBURY.getText(), Callback.UNBURY.getAlias() + CALLBACK_DELIMITER
        //            .getAlias() + cardId));
        //        } else if (currentStatus == Status.SUSPENDED) {
        //            buttons.add(button(Callback.UNSUSPEND.getText(), Callback.UNSUSPEND.getAlias() +
        //            CALLBACK_DELIMITER.getAlias() + cardId));
        //        } else {
        //            buttons.add(button(Callback.BURY.getText(), Callback.BURY.getAlias() + CALLBACK_DELIMITER
        //            .getAlias() + cardId));
        //            buttons.add(button(Callback.SUSPEND.getText(), Callback.SUSPEND.getAlias() + CALLBACK_DELIMITER
        //            .getAlias() + cardId));
        //        }

        buttons.add(button(
                Callback.DECK_MENU.getText(),
                Callback.DECK_MENU.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId));
        buttons.add(button(Callback.CLOSE_MENU.getText(), Callback.CLOSE_MENU.getAlias()));

        return InlineKeyboardMarkup.builder()
                .keyboard(createDynamicRows(buttons, List.of(4, 1, 1, 2, 2, 1, 1)))
                .build();
    }

    public InlineKeyboardMarkup getCardDraftConfirmationKeyboard(Long deckId) {
        List<InlineKeyboardButton> buttons = List.of(
                button(
                        Callback.CONFIRM_CARD_CREATION.getText(),
                        Callback.CONFIRM_CARD_CREATION.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId),
                button(Callback.EDIT_CARD_DRAFT_FRONT.getText(), Callback.EDIT_CARD_DRAFT_FRONT.getAlias()),
                button(Callback.EDIT_CARD_DRAFT_BACK.getText(), Callback.EDIT_CARD_DRAFT_BACK.getAlias()),
                button(
                        Callback.CANCEL_CARD_CREATION.getText(),
                        Callback.CANCEL_CARD_CREATION.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId));

        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1, 2, 1))).build();
    }

    public InlineKeyboardMarkup getExistingCardEditKeyboard(Long cardId, Long deckId) {
        List<InlineKeyboardButton> buttons = List.of(
                button(
                        Callback.EDIT_EXISTING_CARD_FRONT.getText(),
                        Callback.EDIT_EXISTING_CARD_FRONT.getAlias() + CALLBACK_DELIMITER.getAlias() + cardId), button(
                        Callback.EDIT_EXISTING_CARD_BACK.getText(),
                        Callback.EDIT_EXISTING_CARD_BACK.getAlias() + CALLBACK_DELIMITER.getAlias() + cardId),
                // TODO реализовать кнопку возврата к изучению определённой карты, сейчас возвращает к меню колоды
                button(
                        Callback.DECK_MENU.getText(),
                        Callback.DECK_MENU.getAlias() + CALLBACK_DELIMITER.getAlias() + deckId), button(
                        Callback.DELETE_CARD.getText(),
                        Callback.DELETE_CARD.getAlias() +
                                CALLBACK_DELIMITER.getAlias() +
                                cardId +
                                CALLBACK_DELIMITER.getAlias() +
                                deckId));
        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1, 1, 1, 1))).build();
    }

    public InlineKeyboardMarkup getBackToDeckListKeyboard() {
        List<InlineKeyboardButton> buttons = List.of(button("Вернуться к списку колод", Callback.DECK_LIST.getAlias()));
        return InlineKeyboardMarkup.builder().keyboard(createDynamicRows(buttons, List.of(1))).build();
    }
}
