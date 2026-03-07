package org.company.spacedrepetitionbot.handler.handlers.callback;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Callback {
    CALLBACK_DELIMITER("Делимитер", ":="),
    DECK_LIST("Список колод", "DECK_LIST"),
    CLOSE_MENU("Завершить общение", "CLOSE_MENU"),
    MAIN_MENU("Перейти в главное меню", "MAIN_MENU"),
    DECK_MENU("Перейти в меню колоды", "DECK_MENU"),
    ADD_DECK("Добавить колоду", "ADD_DECK"),
    CARD_LIST("Список карт", "CARD_LIST"),
    ADD_CARD("Добавить карту", "ADD_CARD"),
    LEARN_DECK("Учить карты колоды", "LEARN_DECK"),
    SHOW_ANSWER("Показать ответ", "SHOW_ANSWER"),
    CONFIRM_CARD_CREATION("Подтвердить", "CONFIRM_CARD"),
    EDIT_CARD_DRAFT_FRONT("Редактировать вопрос черновика", "EDIT_CARD_DRAFT_FRONT"),
    EDIT_CARD_DRAFT_BACK("Редактировать ответ черновика", "EDIT_CARD_DRAFT_BACK"),
    CANCEL_CARD_CREATION("Удалить черновик и вернуться в меню колоды", "CANCEL_CARD_CREATION"),
    EDIT_EXISTING_CARD("Редактировать карту", "EDIT_EXISTING_CARD"),
    EDIT_EXISTING_CARD_FRONT("Редактировать вопрос карты", "EDIT_EXISTING_CARD_FRONT"),
    EDIT_EXISTING_CARD_BACK("Редактировать ответ карты", "EDIT_EXISTING_CARD_BACK"),
    DELETE_CARD("Удалить карту", "DELETE_CARD"),
    AGAIN("Снова", "AGAIN"),
    HARD("Трудно", "HARD"),
    GOOD("Хорошо", "GOOD"),
    EASY("Легко", "EASY"),
    BURY("Закопать карточку", "BURY"), // TODO ещё не реализовано
    UNBURY("Откопать карточку", "UNBURY"), // TODO ещё не реализовано
    SUSPEND("Приостановить карточку", "SUSPEND"), // TODO ещё не реализовано
    UNSUSPEND("Возобновить карточку", "UNSUSPEND"), // TODO ещё не реализовано
    PREVIOUS("Вернуться к предыдущей карте", "PREVIOUS"), // TODO ещё не реализовано
    COPY_DEFAULT_DECK("Скопировать дефолтную колоду", "COPY_DEFAULT_DECK");
    private final String text;
    private final String alias;

    Callback(String text, String alias) {
        this.text = text;
        this.alias = alias;
    }

    public static Callback from(String alias) {
        return Arrays.stream(Callback.values())
                .filter(callBack -> callBack.getAlias().equalsIgnoreCase(alias))
                .findFirst()
                .orElse(null);
    }
}
