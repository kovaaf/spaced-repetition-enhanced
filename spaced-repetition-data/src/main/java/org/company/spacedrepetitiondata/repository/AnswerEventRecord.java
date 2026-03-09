package org.company.spacedrepetitiondata.repository;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a row in the answer_events table.
 * Immutable value object.
 *
 * @param quality   0, 3, 4, 5 per Quality enum
 * @param userName  optional, joined from bot.user_info
 * @param deckName  optional, joined from bot.deck
 * @param cardTitle optional, joined from bot.card
 */
public record AnswerEventRecord(Long userId,
        Long deckId,
        Long cardId,
        int quality,
        Instant timestamp,
        String userName,
        String deckName,
        String cardTitle) {

    public AnswerEventRecord(Long userId, Long deckId, Long cardId, int quality, Instant timestamp) {
        this(userId, deckId, cardId, quality, timestamp, null, null, null);
    }

    public AnswerEventRecord(
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnswerEventRecord that = (AnswerEventRecord) o;
        return quality == that.quality &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(deckId, that.deckId) &&
                Objects.equals(cardId, that.cardId) &&
                Objects.equals(timestamp, that.timestamp);
        // Note: userName, deckName, cardTitle intentionally excluded from equals/hashCode
        // as they are joined data not part of the primary identity
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, deckId, cardId, quality, timestamp);
    }

    @Override
    public String toString() {
        return "AnswerEventRecord{" +
                "userId=" + userId +
                ", deckId=" + deckId +
                ", cardId=" + cardId +
                ", quality=" + quality +
                ", timestamp=" + timestamp +
                (userName != null ? ", userName='" + userName + '\'' : "") +
                (deckName != null ? ", deckName='" + deckName + '\'' : "") +
                (cardTitle != null ? ", cardTitle='" + cardTitle + '\'' : "") +
                '}';
    }
}