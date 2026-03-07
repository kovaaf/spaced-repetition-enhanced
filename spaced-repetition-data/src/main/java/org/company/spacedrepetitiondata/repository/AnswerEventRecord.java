package org.company.spacedrepetitiondata.repository;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a row in the answer_events table.
 * Immutable value object.
 */
public final class AnswerEventRecord {

    private final Long userId;
    private final Long deckId;
    private final Long cardId;
    private final int quality; // 0, 3, 4, 5 per Quality enum
    private final Instant timestamp;
    private final String userName; // optional, joined from bot.user_info
    private final String deckName; // optional, joined from bot.deck
    private final String cardTitle; // optional, joined from bot.card

    public AnswerEventRecord(Long userId, Long deckId, Long cardId, int quality, Instant timestamp) {
        this(userId, deckId, cardId, quality, timestamp, null, null, null);
    }

    public AnswerEventRecord(Long userId, Long deckId, Long cardId, int quality, Instant timestamp,
                            String userName, String deckName, String cardTitle) {
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

    public Long getUserId() {
        return userId;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Long getCardId() {
        return cardId;
    }

    public int getQuality() {
        return quality;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public String getDeckName() {
        return deckName;
    }

    public String getCardTitle() {
        return cardTitle;
    }

    private static boolean isValidQuality(int quality) {
        return quality == 0 || quality == 3 || quality == 4 || quality == 5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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