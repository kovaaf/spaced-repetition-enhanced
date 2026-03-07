package org.company.spacedrepetitionbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSession {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "learning_session_seq")
    @SequenceGenerator(name = "learning_session_seq", sequenceName = "learning_session_seq", allocationSize = 1)
    private Long sessionId;

    @OneToOne(optional = false)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    // Нужно отношение многие ко многим т.к. сессия - временная сущность и карта через некоторое время будет
    // принадлежать уже другой сессии
    @ManyToMany
    @JoinTable(name = "session_cards",
            joinColumns = @JoinColumn(name = "session_id"),
            inverseJoinColumns = @JoinColumn(name = "card_id"))
    @OrderBy("nextReviewTime ASC")
    private List<Card> cards;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
