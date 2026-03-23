package org.company.spacedrepetitionbot.model;

import jakarta.persistence.*;
import lombok.*;
import org.company.spacedrepetitionbot.constants.Status;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "card_seq")
    @SequenceGenerator(name = "card_seq", sequenceName = "card_seq", allocationSize = 1)
    private Long cardId;

    private String front;
    private String back;

    @Builder.Default
    private Integer repeatCount = 0;

    @Builder.Default
    private Double easinessFactor = 2.5;

    @Builder.Default
    private LocalDateTime nextReviewTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.NEW;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    private String sourceFilePath;
    private String sourceHeading;

    @Column(name = "original_card_id")
    private Long originalCardId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Override
    public String toString() {
        return front;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}