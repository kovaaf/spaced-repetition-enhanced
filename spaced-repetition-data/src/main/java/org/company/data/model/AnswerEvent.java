package org.company.data.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an answer event.
 */
@Builder
public record AnswerEvent(Long userId,
        Long deckId,
        Long cardId,
        int quality,
        Instant timestamp,
        String userName,
        String deckName,
        String cardTitle) {

    public AnswerEvent(
            Long userId,
            Long deckId,
            Long cardId,
            int quality,
            Instant timestamp,
            String userName,
            String deckName,
            String cardTitle) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.deckId = Objects.requireNonNull(deckId, "deckId must not be null");
        this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
        if (!isValidQuality(quality)) {
            throw new IllegalArgumentException("quality must be 0, 3, 4, or 5");
        }
        this.quality = quality;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.userName = userName;
        this.deckName = deckName;
        this.cardTitle = cardTitle;
    }

    private static boolean isValidQuality(int quality) {
        return quality == 0 || quality == 3 || quality == 4 || quality == 5;
    }
}