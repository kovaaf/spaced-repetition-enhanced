package org.company.spacedrepetitiondata.model;

import lombok.Getter;

/**
 * Quality values for spaced repetition answers.
 */
@Getter
public enum Quality {
    AGAIN(0),
    HARD(3),
    GOOD(4),
    EASY(5);

    private final int value;

    Quality(int value) {
        this.value = value;
    }

    public static Quality fromValue(int value) {
        return switch (value) {
            case 0 -> AGAIN;
            case 3 -> HARD;
            case 4 -> GOOD;
            case 5 -> EASY;
            default -> throw new IllegalArgumentException("Invalid quality value: " + value);
        };
    }
}