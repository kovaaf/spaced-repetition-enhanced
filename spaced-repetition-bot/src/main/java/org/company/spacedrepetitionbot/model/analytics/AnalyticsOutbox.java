package org.company.spacedrepetitionbot.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import org.company.spacedrepetitionbot.constants.OutboxStatus;
import org.company.spacedrepetitionbot.constants.Quality;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "analytics_outbox")
public class AnalyticsOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "deck_name")
    private String deckName;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "card_title")
    private String cardTitle;

    @Column(nullable = false)
    private Integer quality;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PreUpdate
    public void preUpdate() {
        if (processedAt == null && status == OutboxStatus.COMPLETED) {
            processedAt = LocalDateTime.now();
        }
    }

    public Quality getQualityEnum() {
        return Quality.fromInt(quality);
    }

    public void setQualityEnum(Quality qualityEnum) {
        this.quality = qualityEnum.getQuality();
    }
}