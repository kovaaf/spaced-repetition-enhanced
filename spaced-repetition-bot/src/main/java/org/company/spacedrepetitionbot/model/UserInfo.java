package org.company.spacedrepetitionbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.Objects;
import java.util.Set;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserInfo {
    @Id
    private Long userChatId;

    private String userName;

    @OneToMany(mappedBy = "owner")
    private Set<Deck> decks;

    @Builder.Default
    private boolean hasCopiedDefaultDeck = false;

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof UserInfo userInfo)) {
            return false;
        }

        return Objects.equals(userChatId, userInfo.userChatId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userChatId);
    }
}
