package org.company.spacedrepetitionbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deck_seq")
    @SequenceGenerator(name = "deck_seq", sequenceName = "deck_seq", allocationSize = 1)
    private Long deckId;
    private String name;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.PERSIST)
    private Set<Card> cards;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_chat_id", nullable = false)
    private UserInfo owner;

    @Builder.Default
    private boolean isDefault = false;

    private String lastSyncCommit;

    private LocalDateTime lastUpdated;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deck_source_folders", joinColumns = @JoinColumn(name = "deck_id"))
    @Column(name = "folder")
    @Builder.Default
    private List<String> sourceFolders = new ArrayList<>();

    @Override
    public String toString() {
        return name;
    }
}
