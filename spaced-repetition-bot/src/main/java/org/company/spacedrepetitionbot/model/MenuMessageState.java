package org.company.spacedrepetitionbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class MenuMessageState {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_message_seq")
    @SequenceGenerator(name = "menu_message_seq", sequenceName = "menu_message_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private Integer messageId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column
    private String userState;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
