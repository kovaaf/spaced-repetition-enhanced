package org.company.spacedrepetitionbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "card_draft_seq")
    @SequenceGenerator(name = "card_draft_seq", sequenceName = "card_draft_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private Long deckId;

    @Column
    private String front;

    @Column
    private String back;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
