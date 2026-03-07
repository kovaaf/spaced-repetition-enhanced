package org.company.spacedrepetitionbot.constants;

import lombok.Getter;

@Getter
public enum OtherConstants {
    DEFAULT_DECK_SUFFIX(" (created from default)");

    private final String value;

    OtherConstants(String value) {
        this.value = value;
    }
}
