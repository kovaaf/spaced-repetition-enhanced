package org.company.spacedrepetitionbot.handler.handlers.text;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MessageState {
    STATE_DELIMITER(":"),
    DEFAULT("DEFAULT"),
    DECK_CREATION("DECK_CREATION"),
    CARD_FRONT_CREATION("CARD_FRONT_CREATION"),
    CARD_BACK_CREATION("CARD_BACK_CREATION"),
    CARD_CONFIRMATION("CARD_CONFIRMATION"),
    EDIT_CARD_DRAFT_FRONT("EDIT_CARD_DRAFT_FRONT"),
    EDIT_CARD_DRAFT_BACK("EDIT_CARD_DRAFT_BACK"),
    EDIT_EXISTING_CARD_FRONT("EDIT_EXISTING_CARD_FRONT"),
    EDIT_EXISTING_CARD_BACK("EDIT_EXISTING_CARD_BACK"),
    COPY_DEFAULT_DECK("COPY_DEFAULT_DECK");

    private final String alias;

    MessageState(String alias) {
        this.alias = alias;
    }

    public static MessageState from(String alias) {
        return Arrays.stream(MessageState.values())
                .filter(state -> state.getAlias().equalsIgnoreCase(alias))
                .findFirst()
                .orElse(null);
    }
}
