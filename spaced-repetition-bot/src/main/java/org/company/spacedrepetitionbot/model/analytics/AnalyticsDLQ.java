package org.company.spacedrepetitionbot.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import org.company.spacedrepetitionbot.constants.Quality;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "analytics_dlq")
public class AnalyticsDLQ {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analytics_dlq_seq")
    @SequenceGenerator(name = "analytics_dlq_seq", sequenceName = "analytics_dlq_seq", allocationSize = 1)
    @Column(name = "dlq_id")
    private Long dlqId;

    @Column(name = "outbox_id", nullable = false)
    private Long outboxId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Integer quality;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "failure_reason", nullable = false, columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_attempt_at", nullable = false)
    private LocalDateTime lastAttemptAt;

    public Quality getQualityEnum() {
        return Quality.fromInt(quality);
    }

    public void setQualityEnum(Quality qualityEnum) {
        this.quality = qualityEnum.getQuality();
    }
}