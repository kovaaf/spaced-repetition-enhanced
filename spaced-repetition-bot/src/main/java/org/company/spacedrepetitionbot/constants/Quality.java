package org.company.spacedrepetitionbot.constants;

import lombok.Getter;

@Getter
public enum Quality {
    AGAIN(0),
    HARD(3),
    GOOD(4),
    EASY(5);

    private final int quality;

    Quality(int quality) {
        this.quality = quality;
    }

    public static Quality fromInt(int value) {
        for (Quality quality : Quality.values()) {
            if (quality.getQuality() == value) {
                return quality;
            }
        }
        throw new IllegalArgumentException("Unknown quality value: " + value);
    }
}
